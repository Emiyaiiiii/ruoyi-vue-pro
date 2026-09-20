package cn.iocoder.yudao.module.kb.service.kbconfig;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.kb.dal.dataobject.chunkmethod.ChunkMethodDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.mysql.chunkmethod.ChunkMethodMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import cn.iocoder.yudao.module.kb.service.ragconfig.RAGSystemConfigService;
import cn.iocoder.yudao.module.kb.service.vectortask.RagConfigPublisher;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
     * 注意：getConfigByModule("rerank") 会附带 kb_model_config 的账户信息（endpoint/api_key/default_model），
     * 保留用于 python-vector 检索侧覆盖。
     */
    public Map<String, Object> resolveRerankConfig(Long kbId) {
        Map<String, Object> result = new LinkedHashMap<>(defaultRerankConfig());
        result.putAll(ragSystemConfigService.getConfigByModule("rerank"));
        mergeGroup(result, getKbProcessConfig(kbId), "rerank", RERANK_KEYS);
        return result;
    }

    /**
     * 合并后处理配置（#10）：KB 级 postprocess > 全部默认 false。
     * 返回布尔开关映射：{question_enabled, summary_enabled, graph_enabled, extract_enabled, wiki_enabled}。
     * 供入库后处理阶段（python-vector）按开关执行；缺模型时 python 侧静默跳过。
     */
    public Map<String, Object> resolvePostprocessConfig(Long kbId) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : POSTPROCESS_KEYS) {
            result.put(key, false);
        }
        mergeGroup(result, getKbProcessConfig(kbId), "postprocess", POSTPROCESS_KEYS);
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

    // ==================== 发布（供 python-vector 检索侧使用） ====================

    /**
     * 发布 KB 级配置变更到 python-vector（复用 kb.ingest.rag.config 通道）。
     * python-vector 消费 event=kb.process.config.changed 写入 Redis kb:process:config:{kb_id}，
     * 检索时按 knowledge_base_ids 读取实现 KB 级覆盖。
     */
    public void publishKbConfig(Long kbId) {
        if (kbId == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "kb.process.config.changed");
        payload.put("tenant_id", TenantContextHolder.getTenantId() != null ? TenantContextHolder.getTenantId() : 0);
        payload.put("kb_id", kbId);
        payload.put("config", getKbProcessConfig(kbId));
        // 异步发布 + 容错：配置变更只是旁路通知，不应阻塞保存接口（RabbitMQ 不可用时 convertAndSend 会长时间挂起）
        CompletableFuture.runAsync(() -> {
            try {
                ragConfigPublisher.publish(payload);
            } catch (Exception ex) {
                log.warn("[publishKbConfig] 发布 RAG 配置变更失败（不影响保存）kbId={}", kbId, ex);
            }
        });
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
