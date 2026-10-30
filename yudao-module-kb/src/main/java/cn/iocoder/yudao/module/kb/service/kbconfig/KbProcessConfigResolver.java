package cn.iocoder.yudao.module.kb.service.kbconfig;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.kb.dal.dataobject.chunkmethod.ChunkMethodDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.modelconfig.ModelConfigDO;
import cn.iocoder.yudao.module.kb.dal.mysql.chunkmethod.ChunkMethodMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.modelconfig.ModelConfigMapper;
import cn.iocoder.yudao.module.kb.service.ragconfig.RAGSystemConfigService;
import cn.iocoder.yudao.module.kb.service.vectortask.RagConfigPublisher;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * KB 级处理配置解析器
 *
 * <p>实现"上传任务参数 > KB 级 process_config > 租户级系统配置 > 内置默认"的覆盖链。
 * 键名与系统配置保持一致：chunking 对齐 kb_chunk_method.default_parameters / 任务消息
 * chunking_config，retrieval/rerank 对齐 kb_rag_config 同模块键名，model 对齐 kb_model_config.uid。</p>
 */
@Component
@Slf4j
public class KbProcessConfigResolver {

    /** chunking 组白名单（与 kb_chunk_method.default_parameters / chunking_config 键名一致） */
    private static final Set<String> CHUNKING_KEYS = new HashSet<>(Arrays.asList(
            "strategy", "chunk_size", "chunk_overlap", "min_chunk_size", "max_chunk_size", "image_strategy",
            // 父-子两级切块（#1）：父块/子块尺寸与重叠
            "parent_chunk_size", "child_chunk_size", "child_chunk_overlap",
            // ContextHeader（#4）：切分后给 chunk 前缀拼标题面包屑（通用布尔开关，任何切片器可用）
            "context_header"));
    /** retrieval 组白名单（与 kb_rag_config retrieval 模块键名一致） */
    private static final Set<String> RETRIEVAL_KEYS = new HashSet<>(Arrays.asList(
            "top_k", "search_type", "use_parallel", "score_threshold", "vector_weight",
            "keyword_weight", "parallel_workers", "parallel_timeout"));
    /** rerank 组白名单（与 kb_rag_config rerank 模块键名一致） */
    private static final Set<String> RERANK_KEYS = new HashSet<>(Arrays.asList(
            "enabled", "timeout", "top_k"));
    /** model 组白名单（与 kb_model_config.uid 对应） */
    private static final Set<String> MODEL_KEYS = new HashSet<>(Arrays.asList(
            "embedding_model_uid", "rerank_model_uid"));
    /** postprocess 组白名单（#10 可勾选后处理：独立布尔开关，缺模型时 python 侧静默跳过） */
    private static final Set<String> POSTPROCESS_KEYS = new HashSet<>(Arrays.asList(
            "question_enabled", "summary_enabled", "graph_enabled",
            "extract_enabled", "wiki_enabled"));
    /** postprocess.wiki 二级子对象白名单（Wiki 抽取粒度等，对齐 WeKnora WikiConfig 子集） */
    private static final Set<String> POSTPROCESS_WIKI_KEYS = new HashSet<>(Arrays.asList(
            "granularity", "citation_batch_runes", "citation_concurrency", "reduce_concurrency"));

    /** 内置默认切片参数（与 python-vector ingest/task.py 默认对齐） */
    private static final Map<String, Object> DEFAULT_CHUNKING = new LinkedHashMap<>();
    static {
        DEFAULT_CHUNKING.put("strategy", "fixed_size");
        DEFAULT_CHUNKING.put("chunk_size", 500);
        DEFAULT_CHUNKING.put("chunk_overlap", 50);
        DEFAULT_CHUNKING.put("min_chunk_size", 100);
        DEFAULT_CHUNKING.put("max_chunk_size", 2000);
        DEFAULT_CHUNKING.put("image_strategy", "");
    }

    @Resource
    private LibraryMapper libraryMapper;
    @Resource
    private ChunkMethodMapper chunkMethodMapper;
    @Resource
    private RAGSystemConfigService ragSystemConfigService;
    @Resource
    private RagConfigPublisher ragConfigPublisher;
    @Resource
    private ModelConfigMapper modelConfigMapper;

    // ==================== 读取 KB 级配置 ====================

    /**
     * 读取 KB 的 process_config，返回 {chunking:{}, retrieval:{}, rerank:{}, model:{}}；
     * 未配置时返回空 Map（不抛异常）。
     */
    public Map<String, Object> getKbProcessConfig(Long kbId) {
        if (kbId == null) {
            return Collections.emptyMap();
        }
        LibraryDO library = libraryMapper.selectById(kbId);
        if (library == null || StrUtil.isBlank(library.getProcessConfig())) {
            return Collections.emptyMap();
        }
        Map<String, Object> raw = JsonUtils.parseMap(library.getProcessConfig());
        return raw == null ? Collections.emptyMap() : raw;
    }

    // ==================== 切片配置合并 ====================

    /**
     * 合并切片配置：任务显式参数 > KB 级 chunking > kb_chunk_method 默认方法 > 内置默认。
     *
     * @param kbId       知识库ID（可为 null，跳过 KB 级）
     * @param taskParams 上传任务显式传入的切片参数（可为 null），键名与 CHUNKING_KEYS 一致
     */
    public Map<String, Object> resolveChunkingConfig(Long kbId, Map<String, Object> taskParams) {
        Map<String, Object> result = new LinkedHashMap<>(DEFAULT_CHUNKING);
        // 2. 租户级：kb_chunk_method 默认方法的 default_parameters
        result.putAll(resolveDefaultChunkMethodParams());
        // 3. KB 级
        mergeGroup(result, getKbProcessConfig(kbId), "chunking", CHUNKING_KEYS);
        // 4. 任务参数（最高优先）
        if (taskParams != null) {
            for (String key : CHUNKING_KEYS) {
                if (taskParams.containsKey(key) && taskParams.get(key) != null) {
                    result.put(key, taskParams.get(key));
                }
            }
        }
        return result;
    }

    // ==================== 检索 / 重排配置合并 ====================

    /**
     * 合并检索配置：KB 级 retrieval > kb_rag_config retrieval 模块 > 内置默认。
     */
    public Map<String, Object> resolveRetrievalConfig(Long kbId) {
        Map<String, Object> result = new LinkedHashMap<>(defaultRetrievalConfig());
        result.putAll(ragSystemConfigService.getConfigByModule("retrieval"));
        mergeGroup(result, getKbProcessConfig(kbId), "retrieval", RETRIEVAL_KEYS);
        return result;
    }

    /**
     * 合并重排配置：KB 级 rerank > kb_rag_config rerank 模块 > 内置默认。
     * 账户信息（endpoint/api_key/default_model）优先取 KB 级 model.rerank_model_uid 指向的
     * kb_model_config，未显式指定时回退租户激活 rerank 模型（getConfigByModule 附带）。
     */
    public Map<String, Object> resolveRerankConfig(Long kbId) {
        Map<String, Object> result = new LinkedHashMap<>(defaultRerankConfig());
        result.putAll(ragSystemConfigService.getConfigByModule("rerank"));
        mergeRerankModelByUid(result, kbId);
        mergeGroup(result, getKbProcessConfig(kbId), "rerank", RERANK_KEYS);
        return result;
    }

    /**
     * KB 级显式 rerank_model_uid：解析其账户信息（endpoint/api_key/default_model）
     * 覆盖租户默认 rerank 模型，保证「KB 显式 > 租户默认」两级一致。
     */
    private void mergeRerankModelByUid(Map<String, Object> rerankConfig, Long kbId) {
        String uid = resolveModelUid(kbId, "rerank");
        if (StrUtil.isBlank(uid)) {
            return;
        }
        ModelConfigDO cfg = modelConfigMapper.selectByUid(uid);
        if (cfg == null) {
            return;
        }
        if (StrUtil.isNotBlank(cfg.getUrl())) {
            rerankConfig.put("endpoint", cfg.getUrl());
        }
        if (StrUtil.isNotBlank(cfg.getAppkey())) {
            rerankConfig.put("api_key", cfg.getAppkey());
        }
        String model = StrUtil.isNotBlank(cfg.getModel()) ? cfg.getModel() : cfg.getUid();
        if (StrUtil.isNotBlank(model)) {
            rerankConfig.put("default_model", model);
        }
        if (StrUtil.isNotBlank(cfg.getConfig())) {
            Map<String, Object> map = JsonUtils.parseMap(cfg.getConfig());
            if (map != null) {
                for (Map.Entry<String, Object> e : map.entrySet()) {
                    if (e.getValue() != null && !rerankConfig.containsKey(e.getKey())) {
                        rerankConfig.put(e.getKey(), e.getValue());
                    }
                }
            }
        }
    }

    /**
     * 合并后处理配置（#10）：KB 级 postprocess > 全部默认 false。
     * 返回布尔开关映射：{question_enabled, summary_enabled, graph_enabled, extract_enabled, wiki_enabled}，
     * 以及可选的二级子对象 postprocess.wiki（Wiki 抽取粒度等）。
     * 供入库后处理阶段（python-vector）按开关执行；缺模型时 python 侧静默跳过。
     */
    public Map<String, Object> resolvePostprocessConfig(Long kbId) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : POSTPROCESS_KEYS) {
            result.put(key, false);
        }
        Map<String, Object> kbConfig = getKbProcessConfig(kbId);
        mergeGroup(result, kbConfig, "postprocess", POSTPROCESS_KEYS);
        // 二级子对象：postprocess.wiki.{granularity/citation_batch_runes/citation_concurrency}
        mergeNestedGroup(result, kbConfig, "postprocess", "wiki", POSTPROCESS_WIKI_KEYS);
        return result;
    }

    // ==================== 模型选择 ====================

    /**
     * 解析 KB 级模型选择：KB 级 model.{embedding_model_uid|rerank_model_uid}；
     * 未配置返回 null（调用方回退租户级激活模型）。
     *
     * @param kbId      知识库ID
     * @param modelType embedding / rerank
     */
    public String resolveModelUid(Long kbId, String modelType) {
        Object group = getKbProcessConfig(kbId).get("model");
        if (!(group instanceof Map)) {
            return null;
        }
        Object uid = "embedding".equals(modelType)
                ? ((Map<?, ?>) group).get("embedding_model_uid")
                : ((Map<?, ?>) group).get("rerank_model_uid");
        return uid instanceof String s && StrUtil.isNotBlank(s) ? s : null;
    }

    /**
     * 解析 KB 合并后的完整模型配置（供 publishKbConfig 推送 / 入库侧读取）。
     *
     * <p>两级优先级在 Java 端完成：KB 级 model.{embedding_model_uid|rerank_model_uid}
     * 显式指定则用该模型，否则回退租户级激活默认。返回：
     * {embedding:{adapter,base_url,api_key,model,vl_supported}, llm:{...}, ocr:{...}, mineru:{...}}。
     * rerank 已在顶层 merged.rerank（含账户信息），不在此重复。
     * 与 VectorTaskServiceImpl.buildEmbeddingConfig 同源解析，保证入库与检索一致。</p>
     */
    public Map<String, Object> resolveModelConfig(Long kbId) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("embedding", resolveEmbedding(kbId));
        model.put("llm", resolveModelByType(null, "llm"));
        model.put("ocr", resolveOcrModel());
        model.put("mineru", resolveMineruModel());
        return model;
    }

    /**
     * 解析 embedding 完整配置：KB 级 model.embedding_model_uid -> 否则租户级激活默认。
     */
    public Map<String, Object> resolveEmbedding(Long kbId) {
        String kbModelUid = resolveModelUid(kbId, "embedding");
        ModelConfigDO cfg = StrUtil.isNotBlank(kbModelUid) ? modelConfigMapper.selectByUid(kbModelUid) : null;
        if (cfg == null) {
            cfg = resolveActiveDefault("embedding");
        }
        return buildModelConfigEntry(cfg, "embedding");
    }

    /**
     * 解析指定类型模型完整配置（llm 等，无 KB 级 uid，直接租户激活默认）。
     */
    public Map<String, Object> resolveModelByType(String ignored, String modelType) {
        return buildModelConfigEntry(resolveActiveDefault(modelType), modelType);
    }

    /**
     * 解析 OCR 配置（key 对齐 python-vector parse_options：ocr_api_url/ocr_model/...）。
     */
    public Map<String, Object> resolveOcrModel() {
        ModelConfigDO cfg = resolveActiveDefault("ocr");
        if (cfg == null) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> ocr = new LinkedHashMap<>();
        if (StrUtil.isNotBlank(cfg.getUrl())) {
            ocr.put("ocr_api_url", cfg.getUrl());
        }
        String model = StrUtil.isNotBlank(cfg.getModel()) ? cfg.getModel() : cfg.getUid();
        if (StrUtil.isNotBlank(model)) {
            ocr.put("ocr_model", model);
        }
        if (StrUtil.isNotBlank(cfg.getAppkey())) {
            ocr.put("ocr_api_key", cfg.getAppkey());
        }
        putExtras(ocr, cfg);
        ocr.put("ocr_enabled", true);
        ocr.putIfAbsent("ocr_kind", resolveOcrKind(cfg));
        return ocr;
    }

    /**
     * 解析 MinerU 配置（PDF 版式解析通道）。
     */
    public Map<String, Object> resolveMineruModel() {
        ModelConfigDO cfg = resolveActiveDefault("mineru");
        if (cfg == null) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> mineru = new LinkedHashMap<>();
        if (StrUtil.isNotBlank(cfg.getUrl())) {
            mineru.put("base_url", cfg.getUrl());
        }
        if (StrUtil.isNotBlank(cfg.getAppkey())) {
            mineru.put("api_key", cfg.getAppkey());
        }
        putExtras(mineru, cfg);
        return mineru;
    }

    /**
     * 读取 kb_model_config 中指定用途分类的默认(激活)配置；按租户自动隔离。
     */
    private ModelConfigDO resolveActiveDefault(String modelType) {
        List<ModelConfigDO> active = modelConfigMapper.selectActiveByType(modelType);
        return active.isEmpty() ? null : active.get(0);
    }

    /**
     * 将 ModelConfigDO 组装为 python-vector 通用的模型配置条目（adapter/base_url/api_key/model/vl_supported）。
     */
    private Map<String, Object> buildModelConfigEntry(ModelConfigDO cfg, String modelType) {
        if (cfg == null) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("adapter", mapProviderToEmbeddingAdapter(cfg));
        if (StrUtil.isNotBlank(cfg.getUrl())) {
            entry.put("base_url", cfg.getUrl());
        }
        if (StrUtil.isNotBlank(cfg.getAppkey())) {
            entry.put("api_key", cfg.getAppkey());
        }
        String model = StrUtil.isNotBlank(cfg.getModel()) ? cfg.getModel() : cfg.getUid();
        if (StrUtil.isNotBlank(model)) {
            entry.put("model", model);
        }
        putExtras(entry, cfg);
        entry.put("vl_supported", Boolean.TRUE.equals(cfg.getVlSupported()));
        return entry;
    }

    /**
     * 将 kb_model_config.config.adapter 映射为 python-vector 嵌入适配器名；
     * config JSON 显式指定则优先，缺省 qwen（OpenAI 兼容）。
     */
    private String mapProviderToEmbeddingAdapter(ModelConfigDO cfg) {
        String explicit = extraAdapter(cfg);
        return StrUtil.isNotBlank(explicit) ? explicit : "qwen";
    }

    private String extraAdapter(ModelConfigDO cfg) {
        if (StrUtil.isNotBlank(cfg.getConfig())) {
            Map<String, Object> map = JsonUtils.parseMap(cfg.getConfig());
            if (map != null && map.get("adapter") != null) {
                return String.valueOf(map.get("adapter"));
            }
        }
        return null;
    }

    /**
     * 把 config JSON 中未消费的键合并进下发配置（用于维度等自定义字段）。
     */
    private void putExtras(Map<String, Object> target, ModelConfigDO cfg) {
        if (StrUtil.isBlank(cfg.getConfig())) {
            return;
        }
        Map<String, Object> map = JsonUtils.parseMap(cfg.getConfig());
        if (map == null) {
            return;
        }
        for (Map.Entry<String, Object> e : map.entrySet()) {
            Object v = e.getValue();
            // 空值不下发：空串/空白会被 python 侧当成"显式配置"而覆盖 env 兜底，
            // 典型表现是界面清空某个字段后，任务侧报「api_key 缺失」而 env 明明配了 key。
            if (v == null || (v instanceof CharSequence && StrUtil.isBlank((CharSequence) v))) {
                continue;
            }
            if (target.containsKey(e.getKey()) || "adapter".equals(e.getKey())) {
                continue;
            }
            target.put(e.getKey(), v);
        }
    }

    /**
     * 判定 OCR 通道类型：config JSON 显式指定优先，缺省逐图 OCR（deepseek_ocr）。
     */
    private String resolveOcrKind(ModelConfigDO cfg) {
        if (StrUtil.isNotBlank(cfg.getConfig())) {
            Map<String, Object> map = JsonUtils.parseMap(cfg.getConfig());
            if (map != null && map.get("ocr_kind") != null) {
                return String.valueOf(map.get("ocr_kind"));
            }
        }
        return "deepseek_ocr";
    }

    // ==================== 发布（供 python-vector 检索侧使用） ====================

    /**
     * 发布 KB 级配置变更到 python-vector（复用 kb.ingest.rag.config 通道）。
     *
     * <p>推送的是**合并后的完整配置**：chunking / retrieval / rerank / postprocess / model。
     * 其中 model 组由 uid 解析为完整连接信息（embedding/llm/ocr/mineru 的
     * adapter/base_url/api_key/model/vl_supported），python-vector 解析与检索均
     * 只读该缓存，不再依赖任务消息携带模型配置（两级优先级：KB 显式 > 租户默认，已在本端合并）。</p>
     */
    public void publishKbConfig(Long kbId) {
        if (kbId == null) {
            return;
        }
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("chunking", resolveChunkingConfig(kbId, null));
        merged.put("retrieval", resolveRetrievalConfig(kbId));
        merged.put("rerank", resolveRerankConfig(kbId));
        merged.put("postprocess", resolvePostprocessConfig(kbId));
        merged.put("model", resolveModelConfig(kbId));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "kb.process.config.changed");
        payload.put("tenant_id", TenantContextHolder.getTenantId() != null ? TenantContextHolder.getTenantId() : 0);
        payload.put("kb_id", kbId);
        payload.put("config", merged);
        // 异步发布 + 容错：配置变更只是旁路通知，不应阻塞保存接口（RabbitMQ 不可用时 convertAndSend 会长时间挂起）
        CompletableFuture.runAsync(() -> {
            try {
                ragConfigPublisher.publish(payload);
            } catch (Exception ex) {
                log.warn("[publishKbConfig] 发布 RAG 配置变更失败（不影响保存）kbId={}", kbId, ex);
            }
        });
    }

    /**
     * KB 删除后发布空配置：python-vector 消费 event=kb.process.config.changed 且
     * config 为空时删除 Redis kb:process:config:{kb_id}，与 create/update 的
     * publishKbConfig 对称，保证配置缓存生命周期由 Java 端（事实源）闭环管理。
     */
    public void publishKbConfigDelete(Long kbId) {
        if (kbId == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "kb.process.config.changed");
        payload.put("tenant_id", TenantContextHolder.getTenantId() != null ? TenantContextHolder.getTenantId() : 0);
        payload.put("kb_id", kbId);
        payload.put("config", Collections.emptyMap());
        CompletableFuture.runAsync(() -> {
            try {
                ragConfigPublisher.publish(payload);
            } catch (Exception ex) {
                log.warn("[publishKbConfigDelete] 发布 KB 配置删除失败（不影响删除）kbId={}", kbId, ex);
            }
        });
    }

    /**
     * 全量重推所有 KB 的合并配置（启动预热 / 租户默认或模型配置变更后调用）。
     * 在租户忽略上下文执行，遍历库内全部 KB，逐 KB 推送。
     */
    public void publishAllKbProcessConfigs() {
        try {
            // 跨租户收集 (tenant_id, kb_id)：在租户忽略上下文执行，否则裸 SQL 会被租户拦截器改写
            List<Map<String, Object>> rows = TenantUtils.executeIgnore(() -> libraryMapper.selectKbTenantRows());
            if (rows == null || rows.isEmpty()) {
                log.info("[publishAll] 无知识库，跳过 KB 配置预热");
                return;
            }
            int ok = 0;
            for (Map<String, Object> row : rows) {
                Long tenantId = ((Number) row.get("tenantId")).longValue();
                Long kbId = ((Number) row.get("kbId")).longValue();
                try {
                    // 在每个租户上下文内组装并推送该 KB 合并配置（resolve* 依赖当前租户）
                    TenantUtils.execute(tenantId, () -> publishKbConfig(kbId));
                    ok++;
                } catch (Exception ex) {
                    log.warn("[publishAll] KB 配置预热推送失败 kbId={}: {}", kbId, ex.getMessage());
                }
            }
            log.info("[publishAll] KB 级配置全量重推完成: {} 个 KB", ok);
        } catch (Exception ex) {
            log.error("[publishAll] KB 级配置全量重推失败", ex);
        }
    }

    /**
     * 监听 RAG 配置 / 模型配置变更事件：变更后全量重推 KB 合并配置。
     */
    @EventListener
    public void onConfigRefresh(RagConfigRefreshEvent event) {
        publishAllKbProcessConfigs();
    }

    // ==================== 私有辅助 ====================

    /**
     * 按白名单合并 KB 级分组配置到目标 Map（键名与系统配置一致，只收白名单键）。
     */
    private void mergeGroup(Map<String, Object> target, Map<String, Object> kbConfig,
                            String group, Set<String> allowedKeys) {
        Object groupObj = kbConfig.get(group);
        if (!(groupObj instanceof Map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) groupObj).entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (allowedKeys.contains(key) && entry.getValue() != null) {
                target.put(key, entry.getValue());
            }
        }
    }

    /**
     * 合并二级嵌套组（如 postprocess.wiki.{granularity}）：源为 Map 且子对象存在时，
     * 按白名单过滤空值后写入 target[subGroup]；无有效键则不写入（保持缺省回退）。
     */
    private void mergeNestedGroup(Map<String, Object> target, Map<String, Object> kbConfig,
                                  String group, String subGroup, Set<String> allowedKeys) {
        Object groupObj = kbConfig.get(group);
        if (!(groupObj instanceof Map)) {
            return;
        }
        Object subObj = ((Map<?, ?>) groupObj).get(subGroup);
        if (!(subObj instanceof Map)) {
            return;
        }
        Map<String, Object> nested = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) subObj).entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (allowedKeys.contains(key) && value != null && !"".equals(value)) {
                nested.put(key, value);
            }
        }
        if (!nested.isEmpty()) {
            target.put(subGroup, nested);
        }
    }

    /**
     * 读取 kb_chunk_method 默认方法的 default_parameters（租户级切片兜底）。
     */
    private Map<String, Object> resolveDefaultChunkMethodParams() {
        ChunkMethodDO method = chunkMethodMapper.selectDefaultMethod();
        if (method == null || StrUtil.isBlank(method.getDefaultParameters())) {
            return Collections.emptyMap();
        }
        Map<String, Object> params = JsonUtils.parseMap(method.getDefaultParameters());
        return params == null ? Collections.emptyMap() : params;
    }

    /**
     * 内置默认检索配置（键名与 kb_rag_config retrieval 模块一致；对齐 R__rag_config_seed.sql）。
     */
    private Map<String, Object> defaultRetrievalConfig() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("top_k", 30);
        defaults.put("search_type", "unified");
        defaults.put("use_parallel", true);
        defaults.put("score_threshold", 0.5);
        defaults.put("vector_weight", 0.7);
        defaults.put("keyword_weight", 0.3);
        defaults.put("parallel_workers", 5);
        defaults.put("parallel_timeout", 10.0);
        return defaults;
    }

    /**
     * 内置默认重排配置（键名与 kb_rag_config rerank 模块一致）。
     */
    private Map<String, Object> defaultRerankConfig() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("enabled", true);
        defaults.put("timeout", 30);
        defaults.put("top_k", 30);
        return defaults;
    }
}
