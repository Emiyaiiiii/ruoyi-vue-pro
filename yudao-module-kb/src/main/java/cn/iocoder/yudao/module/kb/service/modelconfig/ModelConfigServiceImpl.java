package cn.iocoder.yudao.module.kb.service.modelconfig;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.kb.controller.admin.modelconfig.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.modelconfig.ModelConfigDO;
import cn.iocoder.yudao.module.kb.dal.mysql.modelconfig.ModelConfigMapper;
import cn.iocoder.yudao.module.kb.service.kbconfig.RagConfigRefreshEvent;
import cn.iocoder.yudao.module.kb.service.modelcatalog.ModelCatalogService;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 大模型配置 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
@Slf4j
public class ModelConfigServiceImpl implements ModelConfigService {

    @Resource
    private ModelConfigMapper modelConfigMapper;

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Resource
    private ModelCatalogService modelCatalogService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 模型配置变更：全量重推 KB 合并配置（KB 显式 uid 引用的模型连接信息可能变化） */
    private void publishKbConfigRefresh() {
        try {
            applicationEventPublisher.publishEvent(new RagConfigRefreshEvent("model"));
        } catch (Exception ex) {
            log.warn("[model] 发布 KB 配置刷新事件失败（不影响保存）", ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createModelConfig(ModelConfigSaveReqVO createReqVO) {
        // 校验UID唯一性
        validateUidUnique(createReqVO.getUid(), null);

        // 插入
        ModelConfigDO modelConfig = BeanUtils.toBean(createReqVO, ModelConfigDO.class);
        // 设置默认值
        if (modelConfig.getMaxTokens() == null) modelConfig.setMaxTokens(4096);
        if (modelConfig.getContextLength() == null) modelConfig.setContextLength(8192);
        if (modelConfig.getTemperature() == null) modelConfig.setTemperature(0.7);
        if (modelConfig.getTopP() == null) modelConfig.setTopP(0.9);
        if (modelConfig.getSortOrder() == null) modelConfig.setSortOrder(0);
        if (modelConfig.getIsPinned() == null) modelConfig.setIsPinned(0);
        if (modelConfig.getModelType() == null) modelConfig.setModelType("llm");
        if (modelConfig.getConfig() == null) modelConfig.setConfig("{}");
        if (modelConfig.getIsActive() != null && modelConfig.getIsActive() == 1) {
            modelConfig.setActivatedAt(LocalDateTime.now());
        }

        modelConfigMapper.insert(modelConfig);
        // 同类唯一激活：新建即激活时，停用同类其它激活配置
        if (modelConfig.getIsActive() != null && modelConfig.getIsActive() == 1) {
            deactivateSameTypeExcept(modelConfig.getModelType(), modelConfig.getId());
        }
        publishKbConfigRefresh();
        return modelConfig.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateModelConfig(ModelConfigSaveReqVO updateReqVO) {
        // 校验存在
        ModelConfigDO existing = validateExists(updateReqVO.getId());

        // 校验UID唯一性
        validateUidUnique(updateReqVO.getUid(), updateReqVO.getId());

        // 更新
        ModelConfigDO updateObj = BeanUtils.toBean(updateReqVO, ModelConfigDO.class);
        // 检测激活状态变更
        boolean wasActive = existing.getIsActive() != null && existing.getIsActive() == 1;
        boolean willBeActive = updateObj.getIsActive() != null && updateObj.getIsActive() == 1;
        if (!wasActive && willBeActive) {
            updateObj.setActivatedAt(LocalDateTime.now());
        }

        modelConfigMapper.updateById(updateObj);
        // 同类唯一激活：编辑后变为激活时，停用同类其它激活配置
        if (willBeActive) {
            deactivateSameTypeExcept(updateObj.getModelType(), updateObj.getId());
        }
        publishKbConfigRefresh();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModelConfig(Long id) {
        validateExists(id);
        modelConfigMapper.deleteById(id);
        publishKbConfigRefresh();
    }

    @Override
    public ModelConfigDO getModelConfig(Long id) {
        return validateExists(id);
    }

    @Override
    public PageResult<ModelConfigDO> getModelConfigPage(ModelConfigPageReqVO pageReqVO) {
        return modelConfigMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateModelConfig(Long id) {
        ModelConfigDO config = validateExists(id);
        assertValidType(config.getModelType());
        if (config.getIsActive() != null && config.getIsActive() == 1) {
            throw exception(MODEL_CONFIG_ALREADY_ACTIVE);
        }
        // 同类内唯一激活：先停用同类其他激活配置，再激活目标
        deactivateSameTypeExcept(config.getModelType(), config.getId());
        config.setIsActive(1);
        config.setActivatedAt(LocalDateTime.now());
        modelConfigMapper.updateById(config);
        publishKbConfigRefresh();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateModelConfig(Long id) {
        ModelConfigDO config = validateExists(id);
        if (config.getIsActive() == null || config.getIsActive() == 0) {
            throw exception(MODEL_CONFIG_ALREADY_INACTIVE);
        }
        config.setIsActive(0);
        modelConfigMapper.updateById(config);
        publishKbConfigRefresh();
    }

    @Override
    public ModelConfigTestRespVO testModelConfig(ModelConfigTestReqVO reqVO) {
        ModelConfigDO config = validateExists(reqVO.getId());

        long startTime = System.currentTimeMillis();
        ModelConfigTestRespVO result = new ModelConfigTestRespVO();
        result.setConfigId(config.getId());
        result.setName(config.getName());
        result.setTestMessage(reqVO.getTestMessage() != null ? reqVO.getTestMessage() : "你好，请回复\"测试成功\"");

        // 构建模型信息
        ModelConfigTestRespVO.ModelInfo modelInfo = new ModelConfigTestRespVO.ModelInfo();
        modelInfo.setName(config.getName());
        modelInfo.setUid(config.getUid());
        modelInfo.setUrl(config.getUrl());
        modelInfo.setMaxTokens(config.getMaxTokens());
        modelInfo.setTemperature(config.getTemperature());
        modelInfo.setThinkingSupported(config.getThinkingEnabled());
        modelInfo.setIsActive(config.getIsActive());
        result.setModelInfo(modelInfo);

        try {
            // 尝试发起 HTTP 请求测试模型连通性
            String responseStr = doTestRequest(config, reqVO);
            long elapsed = System.currentTimeMillis() - startTime;
            result.setSuccess(true);
            result.setResponseTime(elapsed / 1000.0);
            result.setResponse(responseStr);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            result.setSuccess(false);
            result.setResponseTime(elapsed / 1000.0);
            result.setError(e.getMessage());
            log.warn("[模型测试] 配置ID={}, 名称={}, 测试失败: {}", config.getId(), config.getName(), e.getMessage());
        }

        return result;
    }

    @Override
    public ModelConfigDetectRespVO detectModelParams(ModelConfigDetectReqVO reqVO) {
        ModelConfigDetectRespVO result = new ModelConfigDetectRespVO();
        result.setSuccess(false);
        String modelType = StrUtil.isBlank(reqVO.getModelType()) ? "llm" : reqVO.getModelType();
        // 1. 解析基础地址（去掉 /chat/completions、/embeddings、/rerank 等操作路径后缀）
        String baseUrl = resolveDetectBaseUrl(reqVO.getUrl());
        if (StrUtil.isBlank(baseUrl)) {
            result.setMessage("API地址无效，无法探测模型参数");
            return result;
        }
        String model = StrUtil.trim(reqVO.getModel());
        // 1.5 内置模型目录查表（第①层：零网络请求，命中任一参数即返回）
        Map<String, Object> catalogHit = modelCatalogService.lookup(model);
        if (catalogHit != null) {
            Integer catContext = firstIntField(catalogHit, "max_input_tokens", "max_tokens");
            Integer catMax = firstIntField(catalogHit, "max_output_tokens", "max_tokens");
            Boolean catVl = firstBoolField(catalogHit, "supports_vision");
            Boolean catThinking = firstBoolField(catalogHit, "supports_reasoning");
            // embedding 类：内置目录不含向量维度，需实测 /embeddings 取维度
            Integer catDim = null;
            if ("embedding".equals(modelType)) {
                catDim = probeEmbeddingDimension(reqVO.getUrl(), reqVO.getAppkey(), model);
            }
            List<String> catDetected = new ArrayList<>();
            if (catContext != null) catDetected.add("上下文长度=" + catContext);
            if (catMax != null) catDetected.add("最大Token=" + catMax);
            if (catDim != null) catDetected.add("向量维度=" + catDim);
            if (catVl != null) catDetected.add("多模态(VL)=" + (catVl ? "是" : "否"));
            if (catThinking != null) catDetected.add("思考能力=" + (catThinking ? "是" : "否"));
            if (!catDetected.isEmpty()) {
                result.setContextLength(catContext);
                result.setMaxTokens(catMax);
                result.setDimensions(catDim);
                result.setVlSupported(catVl);
                result.setThinkingEnabled(catThinking);
                result.setSuccess(true);
                result.setMessage("已识别模型参数（内置目录）：" + String.join("，", catDetected));
                log.info("[模型参数探测] 完成: model={}, modelType={}, 来源=内置目录, 识别参数=[{}]",
                        model, modelType, String.join("，", catDetected));
                return result;
            }
        }
        // 2. 优先 GET {base}/models/{model}（兼容未带 /v1 的根地址，追加 /v1 候选）
        Map<String, Object> meta = null;
        List<String> singleCandidates = new ArrayList<>();
        singleCandidates.add(baseUrl + "/models/" + urlEncodePath(model));
        if (!baseUrl.endsWith("/v1")) {
            singleCandidates.add(baseUrl + "/v1/models/" + urlEncodePath(model));
        }
        for (String candidate : singleCandidates) {
            meta = fetchModelMeta(candidate, reqVO.getAppkey());
            if (meta != null && !meta.isEmpty()) {
                break;
            }
        }
        if (meta == null || meta.isEmpty()) {
            // 3. 回退 GET {base}/models，按 id 匹配
            List<String> listCandidates = new ArrayList<>();
            listCandidates.add(baseUrl + "/models");
            if (!baseUrl.endsWith("/v1")) {
                listCandidates.add(baseUrl + "/v1/models");
            }
            for (String candidate : listCandidates) {
                meta = fetchModelMetaFromList(candidate, reqVO.getAppkey(), model);
                if (meta != null && !meta.isEmpty()) {
                    break;
                }
            }
        }
        // 4. 从 /models 元数据解析（如可用）
        Integer contextLength = null;
        Integer maxTokens = null;
        Integer dimensions = null;
        Boolean vl = null;
        Boolean thinking = null;
        if (meta != null && !meta.isEmpty()) {
            // 向量维度（仅 embedding 类模型展示）：常见键如 dimensions / embedding_length / dim / vector_dim
            if ("embedding".equals(modelType)) {
                dimensions = firstIntField(meta, "dimensions", "dimension", "embedding_length",
                        "embedding_size", "vector_dim", "dim", "output_dim");
            }
            contextLength = firstIntField(meta, "context_length", "context_len", "context_window",
                    "max_context_length", "max_model_len", "max_model_length", "n_ctx",
                    "max_input_tokens", "input_tokens_limit", "max_position_embeddings", "model_max_length");
            maxTokens = firstIntField(meta, "max_tokens", "max_output_tokens", "output_tokens_limit");
            vl = firstBoolField(meta, "support_vision", "supports_vision", "vision",
                    "multimodal", "support_vl", "supports_vl", "vl_supported");
            if (vl == null) {
                vl = hasModalityImage(meta);
            }
            if (vl == null) {
                vl = resolveModelTypeVl(meta);
            }
            thinking = firstBoolField(meta, "support_thinking", "supports_thinking",
                    "thinking", "reasoning", "supports_reasoning");
        }
        // 5. chat 类模型补充探测：/models 不可用或参数缺失时，用 chat/completions 轻量请求推断
        //    （多模态：发 1x1 图请求被接受即支持；Token 上限：超限请求解析报错中的上限值）
        boolean isChatType = "llm".equals(modelType) || "ocr".equals(modelType);
        boolean chatProbed = false;
        boolean needProbe = meta == null || meta.isEmpty()
                || vl == null || (contextLength == null && maxTokens == null);
        if (isChatType && needProbe) {
            String chatUrl = resolveEndpoint(reqVO.getUrl(), "/chat/completions");
            if (StrUtil.isNotBlank(chatUrl)) {
                chatProbed = true;
                if (vl == null) {
                    Boolean probedVl = probeVlSupport(chatUrl, reqVO.getAppkey(), model);
                    if (probedVl != null) {
                        vl = probedVl;
                    }
                }
                if (contextLength == null && maxTokens == null) {
                    Map<String, Integer> limits = probeTokenLimits(chatUrl, reqVO.getAppkey(), model);
                    if (limits != null) {
                        if (contextLength == null && limits.get("context") != null) {
                            contextLength = limits.get("context");
                        }
                        if (maxTokens == null && limits.get("max") != null) {
                            maxTokens = limits.get("max");
                        }
                    }
                }
            }
        }
        // 5.5 embedding 类模型：元数据未给出维度时，实测一次 embeddings 取返回向量维度
        boolean embeddingProbed = false;
        if ("embedding".equals(modelType) && dimensions == null) {
            Integer probedDim = probeEmbeddingDimension(reqVO.getUrl(), reqVO.getAppkey(), model);
            if (probedDim != null) {
                dimensions = probedDim;
                embeddingProbed = true;
            }
        }
        // 6. 组装结果
        result.setContextLength(contextLength);
        result.setMaxTokens(maxTokens);
        result.setVlSupported(vl);
        result.setThinkingEnabled(thinking);
        result.setDimensions(dimensions);
        List<String> detected = new ArrayList<>();
        if (contextLength != null) detected.add("上下文长度=" + contextLength);
        if (maxTokens != null) detected.add("最大Token=" + maxTokens);
        if (dimensions != null) detected.add("向量维度=" + dimensions);
        if (vl != null) detected.add("多模态(VL)=" + (vl ? "是" : "否"));
        if (thinking != null) detected.add("思考能力=" + (thinking ? "是" : "否"));
        result.setSuccess(!detected.isEmpty());
        if (!detected.isEmpty()) {
            result.setMessage("已识别模型参数：" + String.join("，", detected));
        } else if (meta == null || meta.isEmpty()) {
            result.setMessage(chatProbed
                    ? "接口不可达或未返回可识别的模型参数，请检查 API 地址 / 密钥 / 模型名，或手动填写"
                    : "未能从接口获取到模型信息，请检查 API 地址 / 密钥 / 模型名，或手动填写参数");
        } else {
            result.setMessage("接口可达，但未返回可识别的模型参数（可手动填写或展开进阶配置补充）");
        }
        log.info("[模型参数探测] 完成: model={}, modelType={}, models列表={}, chat探测={}, 识别参数=[{}]",
                model, modelType,
                (meta != null && !meta.isEmpty()) ? "命中" : "未命中",
                chatProbed ? "已执行" : "未执行",
                detected.isEmpty() ? "无" : String.join("，", detected));
        return result;
    }

    /**
     * 实际发起测试请求：按模型类型分发不同协议
     */
    private String doTestRequest(ModelConfigDO config, ModelConfigTestReqVO reqVO) throws Exception {
        String modelType = config.getModelType() != null ? config.getModelType() : "llm";
        switch (modelType) {
            case "embedding":
                return testEmbedding(config);
            case "rerank":
                return testRerank(config);
            case "ocr":
                return testOcr(config);
            case "mineru":
                return testMineru(config);
            case "llm":
            default:
                return testLlm(config, reqVO);
        }
    }

    /**
     * 发起 HTTP POST 请求，返回响应体
     */
    private String httpPostJson(String url, String apiKey, String jsonBody) throws Exception {
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        if (apiKey != null && !apiKey.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        try (java.util.Scanner scanner = new java.util.Scanner(
                conn.getInputStream(), "UTF-8").useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        } catch (java.io.IOException e) {
            try (java.util.Scanner scanner = new java.util.Scanner(
                    conn.getErrorStream(), "UTF-8").useDelimiter("\\A")) {
                String errorBody = scanner.hasNext() ? scanner.next() : "";
                throw new RuntimeException("HTTP " + conn.getResponseCode() + ": " + errorBody);
            }
        }
    }

    /**
     * 拼接请求 URL：base 上补充操作路径，避免重复拼接
     */
    private String resolveEndpoint(String baseUrl, String opPath) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return "";
        }
        if (baseUrl.endsWith(opPath)) {
            return baseUrl;
        }
        return baseUrl.replaceAll("/+$", "") + opPath;
    }

    /**
     * 测试 LLM（chat completions 协议）
     */
    private String testLlm(ModelConfigDO config, ModelConfigTestReqVO reqVO) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        // model: 优先 model 字段（真实模型名/部署点），uid 仅是业务唯一标识
        payload.put("model", StrUtil.isNotBlank(config.getModel()) ? config.getModel() : config.getUid());
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", reqVO.getTestMessage() != null ? reqVO.getTestMessage() : "你好，请回复\"测试成功\"");
        messages.add(msg);
        payload.put("messages", messages);
        payload.put("temperature", reqVO.getTemperature() != null ? reqVO.getTemperature() : config.getTemperature());
        if (reqVO.getMaxTokens() != null) {
            payload.put("max_tokens", reqVO.getMaxTokens());
        } else if (config.getMaxTokens() != null && config.getMaxTokens() > 0) {
            payload.put("max_tokens", Math.min(config.getMaxTokens(), 4096));
        }
        String endpoint = resolveEndpoint(config.getUrl(), "/chat/completions");
        String responseBody = httpPostJson(endpoint, config.getAppkey(), OBJECT_MAPPER.writeValueAsString(payload));
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = OBJECT_MAPPER.readValue(responseBody, Map.class);
        return extractResponseContent(responseMap);
    }

    /**
     * 实测 Embedding 维度：对 /embeddings 发一次请求，取返回向量长度（无法识别返回 null）
     */
    private Integer probeEmbeddingDimension(String url, String appkey, String model) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", StrUtil.isNotBlank(model) ? model : "");
            payload.put("input", "你好");
            String endpoint = resolveEndpoint(url, "/embeddings");
            String responseBody = httpPostJson(endpoint, appkey, OBJECT_MAPPER.writeValueAsString(payload));
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = OBJECT_MAPPER.readValue(responseBody, Map.class);
            Object data = responseMap.get("data");
            if (data instanceof List) {
                List<?> list = (List<?>) data;
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    Object emb = ((Map<?, ?>) list.get(0)).get("embedding");
                    if (emb instanceof List) {
                        return ((List<?>) emb).size();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[模型参数探测] embedding 维度实测失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 测试 Embedding（embeddings 协议）：若模型配置了维度（config.dimensions），随请求传入，
     * 使返回维度落在配置值上（支持维度裁剪的模型生效）。
     */
    private String testEmbedding(ModelConfigDO config) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", StrUtil.isNotBlank(config.getModel()) ? config.getModel() : config.getUid());
        payload.put("input", "你好");
        // 从 config JSON 读向量维度（前端保存的 dimensions），支持裁剪的模型按此输出
        if (StrUtil.isNotBlank(config.getConfig())) {
            Map<String, String> cfg = parseConfigMap(config.getConfig());
            String dimStr = cfg.containsKey("dimensions") ? cfg.get("dimensions")
                    : cfg.getOrDefault("dimension", null);
            if (StrUtil.isNotBlank(dimStr)) {
                try {
                    payload.put("dimensions", Integer.parseInt(dimStr.trim()));
                } catch (NumberFormatException ignored) {
                    // 维度非数字则忽略，使用模型默认
                }
            }
        }
        String endpoint = resolveEndpoint(config.getUrl(), "/embeddings");
        String responseBody = httpPostJson(endpoint, config.getAppkey(), OBJECT_MAPPER.writeValueAsString(payload));
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = OBJECT_MAPPER.readValue(responseBody, Map.class);
        Object data = responseMap.get("data");
        if (data instanceof List) {
            List<?> list = (List<?>) data;
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                Object emb = ((Map<?, ?>) list.get(0)).get("embedding");
                if (emb instanceof List) {
                    return "嵌入成功，维度=" + ((List<?>) emb).size();
                }
            }
        }
        return "嵌入测试完成，返回结构未见向量: " + responseBody;
    }

    /**
     * 测试 Rerank（rerank 协议）
     */
    private String testRerank(ModelConfigDO config) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", StrUtil.isNotBlank(config.getModel()) ? config.getModel() : config.getUid());
        payload.put("query", "什么是向量检索");
        payload.put("documents", new String[]{"向量检索是常用的召回手段。", "今天天气不错。"});
        payload.put("top_n", 1);
        String endpoint = resolveEndpoint(config.getUrl(), "/rerank");
        String responseBody = httpPostJson(endpoint, config.getAppkey(), OBJECT_MAPPER.writeValueAsString(payload));
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = OBJECT_MAPPER.readValue(responseBody, Map.class);
        if (responseMap.containsKey("results")) {
            return "重排成功，返回结果条数=" + String.valueOf(responseMap.get("results"));
        }
        if (responseMap.containsKey("data")) {
            return "重排成功: " + responseBody;
        }
        // 兼容返回 relevance_score / score 字段
        if (responseMap.containsKey("score") || responseMap.containsKey("relevance_score")) {
            return "重排成功: " + responseBody;
        }
        return "重排完成，返回结构未见评分: " + responseBody;
    }

    /**
     * 测试 OCR（多模态 chat，发送 1x1 纯白图 + 提示词）
     */
    private String testOcr(ModelConfigDO config) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", StrUtil.isNotBlank(config.getModel()) ? config.getModel() : config.getUid());
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "user");
        List<Map<String, Object>> content = new ArrayList<>();
        // 1x1 透明 PNG（极小 base64），仅用于连通性验证
        content.add(Map.of("type", "text", "text", "请识别图中的文字"));
        content.add(Map.of("type", "image_url", "image_url",
                Map.of("url", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==")));
        msg.put("content", content);
        messages.add(msg);
        payload.put("messages", messages);
        if (config.getMaxTokens() != null && config.getMaxTokens() > 0) {
            payload.put("max_tokens", Math.min(config.getMaxTokens(), 8192));
        }
        String endpoint = resolveEndpoint(config.getUrl(), "/chat/completions");
        String responseBody = httpPostJson(endpoint, config.getAppkey(), OBJECT_MAPPER.writeValueAsString(payload));
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = OBJECT_MAPPER.readValue(responseBody, Map.class);
        return extractResponseContent(responseMap);
    }

    /**
     * 测试 MinerU 文档解析通道连通性：对服务根地址发一个 GET（V1 可打 /v1/usage，云 v4 打根路径）。
     * 仅验证可达，不真正消耗解析额度。
     */
    private String testMineru(ModelConfigDO config) throws Exception {
        Map<String, String> cfg = config.getConfig() == null ? new HashMap<>() : parseConfigMap(config.getConfig());
        if ("v1".equalsIgnoreCase(String.valueOf(cfg.getOrDefault("mineru_mode", "v4"))) && StrUtil.isNotBlank(config.getUrl())) {
            int code = httpGetCode(resolveEndpoint(config.getUrl(), "/v1/usage"), config.getAppkey());
            return "MinerU(自部署V1) 连接成功，HTTP " + code;
        }
        if (StrUtil.isBlank(config.getUrl())) {
            return "MinerU 未配置服务地址";
        }
        int code = httpGetCode(config.getUrl(), config.getAppkey());
        return "MinerU 连接成功，HTTP " + code;
    }

    private Map<String, String> parseConfigMap(String configJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = OBJECT_MAPPER.readValue(configJson, Map.class);
            Map<String, String> out = new HashMap<>();
            map.forEach((k, v) -> out.put(k, String.valueOf(v)));
            return out;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * 发起 GET 请求，返回 HTTP 状态码（仅连通性探测，忽略响应体）。
     */
    private int httpGetCode(String url, String apiKey) {
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            return conn.getResponseCode();
        } catch (Exception e) {
            throw new RuntimeException("MinerU 连接失败: " + e.getMessage());
        }
    }

    /**
     * 解析模型参数探测基础地址：去掉常见操作路径后缀，保留 /v1 前缀。
     */
    private String resolveDetectBaseUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return "";
        }
        String base = url.trim();
        // 防御：清理误粘贴的反引号/引号（如 markdown 代码片段复制残留）
        while (base.startsWith("`") || base.startsWith("'") || base.startsWith("\"")) {
            base = base.substring(1);
        }
        while (base.endsWith("`") || base.endsWith("'") || base.endsWith("\"")) {
            base = base.substring(0, base.length() - 1);
        }
        base = base.trim();
        String[] opSuffixes = {"/chat/completions", "/completions", "/embeddings", "/rerank", "/models"};
        for (String suffix : opSuffixes) {
            if (base.endsWith(suffix)) {
                base = base.substring(0, base.length() - suffix.length());
                break;
            }
        }
        return base.replaceAll("/+$", "");
    }

    private String urlEncodePath(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * GET 请求并解析 JSON 响应；失败返回 null。
     */
    private Map<String, Object> httpGetJson(String url, String apiKey) {
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new RuntimeException("HTTP " + code);
            }
            try (java.util.Scanner scanner = new java.util.Scanner(
                    conn.getInputStream(), "UTF-8").useDelimiter("\\A")) {
                String body = scanner.hasNext() ? scanner.next() : "";
                if (StrUtil.isBlank(body)) {
                    return null;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> map = OBJECT_MAPPER.readValue(body, Map.class);
                return map;
            }
        } catch (Exception e) {
            log.warn("[模型参数探测] GET {} 失败: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * 获取单个模型元数据：兼容 {data:{...}} 包裹与直接返回两种结构。
     */
    private Map<String, Object> fetchModelMeta(String url, String apiKey) {
        Map<String, Object> root = httpGetJson(url, apiKey);
        if (root == null) {
            return null;
        }
        Object data = root.get("data");
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        return root;
    }

    /**
     * 从模型列表 {data:[...]} 中按 id 匹配模型元数据。
     * 匹配时归一化点/横线/下划线（如 doubao-seed-2.0-mini 可命中 doubao-seed-2-0-mini-260428）。
     */
    private Map<String, Object> fetchModelMetaFromList(String url, String apiKey, String modelId) {
        Map<String, Object> root = httpGetJson(url, apiKey);
        if (root == null) {
            return null;
        }
        Object data = root.get("data");
        if (!(data instanceof List)) {
            return null;
        }
        String target = normalizeModelId(modelId);
        for (Object item : (List<?>) data) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<String, Object> m = (Map<String, Object>) item;
            Object id = m.get("id");
            if (id == null) {
                continue;
            }
            String idStr = normalizeModelId(String.valueOf(id));
            if (idStr.equals(target) || (!target.isEmpty() && (idStr.contains(target) || target.contains(idStr)))) {
                return m;
            }
        }
        return null;
    }

    /**
     * 归一化模型 ID 用于模糊匹配：去分隔符、转小写。
     */
    private String normalizeModelId(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("[._\\-]", "");
    }

    /**
     * 按字段名依次读取整数（兼容 Number 与数字字符串）。
     */
    private Integer firstIntField(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v == null) {
                continue;
            }
            if (v instanceof Number) {
                return ((Number) v).intValue();
            }
            String s = String.valueOf(v).trim();
            if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
                continue;
            }
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("-?\\d+").matcher(s);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group());
            }
        }
        return null;
    }

    /**
     * 按字段名依次读取布尔（兼容 Boolean / "true" / 1 / enabled 等）。
     */
    private Boolean firstBoolField(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v == null) {
                continue;
            }
            if (v instanceof Boolean) {
                return (Boolean) v;
            }
            if (v instanceof Number) {
                return ((Number) v).intValue() != 0;
            }
            String s = String.valueOf(v).trim().toLowerCase();
            if ("true".equals(s) || "1".equals(s) || "yes".equals(s)
                    || "enable".equals(s) || "enabled".equals(s)
                    || "support".equals(s) || "supported".equals(s)) {
                return true;
            }
            if ("false".equals(s) || "0".equals(s) || "no".equals(s)) {
                return false;
            }
        }
        return null;
    }

    /**
     * 兼容 modalities 数组（如 ["text","image"]）判定多模态。
     */
    private Boolean hasModalityImage(Map<String, Object> map) {
        Object v = map.get("modalities");
        if (v instanceof List) {
            for (Object item : (List<?>) v) {
                if (item != null && String.valueOf(item).toLowerCase().contains("image")) {
                    return true;
                }
            }
        }
        return null;
    }

    /**
     * 兼容 model_type 字段（火山方舟等：text / multimodal / image2text / vision）判定多模态。
     */
    private Boolean resolveModelTypeVl(Map<String, Object> map) {
        Object v = map.get("model_type");
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim().toLowerCase();
        if (s.contains("multimodal") || s.contains("vision") || s.contains("image")
                || s.contains("多模态") || s.contains("视觉")) {
            return true;
        }
        if ("text".equals(s) || s.contains("text-only") || s.contains("纯文本")) {
            return false;
        }
        return null;
    }

    /**
     * chat/completions 轻量探测结果（HTTP 状态码 + 响应体）。
     */
    private static class ChatProbeResult {
        final int code;
        final String body;
        ChatProbeResult(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }

    /**
     * 发送 chat/completions 探测请求（可选携带 1x1 图 / 指定 max_tokens），不解析正常响应。
     */
    private ChatProbeResult postChatProbe(String url, String apiKey, String model,
                                          Integer maxTokens, boolean withImage) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", "user");
            if (withImage) {
                List<Map<String, Object>> content = new ArrayList<>();
                content.add(Map.of("type", "text", "text", "hi"));
                content.add(Map.of("type", "image_url", "image_url",
                        Map.of("url", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==")));
                msg.put("content", content);
            } else {
                msg.put("content", "hi");
            }
            messages.add(msg);
            payload.put("messages", messages);
            if (maxTokens != null) {
                payload.put("max_tokens", maxTokens);
            }
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(OBJECT_MAPPER.writeValueAsBytes(payload));
            }
            int code = conn.getResponseCode();
            String body;
            try (java.util.Scanner scanner = new java.util.Scanner(
                    conn.getInputStream(), "UTF-8").useDelimiter("\\A")) {
                body = scanner.hasNext() ? scanner.next() : "";
            } catch (java.io.IOException e) {
                try (java.util.Scanner scanner = new java.util.Scanner(
                        conn.getErrorStream(), "UTF-8").useDelimiter("\\A")) {
                    body = scanner.hasNext() ? scanner.next() : "";
                }
            }
            log.debug("[模型参数探测] chat 探测({}) {} -> HTTP {}", withImage ? "VL" : "限额", url, code);
            if (code >= 400 && StrUtil.isNotBlank(body)) {
                log.debug("[模型参数探测] chat 探测响应体: {}", StrUtil.maxLength(body, 300));
            }
            return new ChatProbeResult(code, body);
        } catch (Exception e) {
            log.warn("[模型参数探测] chat 探测失败 {}: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * 多模态探测：向 chat/completions 发送 1x1 图片，请求被接受即支持 VL；
     * 明确拒绝图片输入则判定不支持；其余错误无法判断返回 null。
     */
    private Boolean probeVlSupport(String url, String apiKey, String model) {
        ChatProbeResult r = postChatProbe(url, apiKey, model, 1, true);
        if (r == null) {
            return null;
        }
        if (r.code >= 200 && r.code < 300) {
            return true;
        }
        if (r.code == 401 || r.code == 403) {
            return null;
        }
        String body = r.body == null ? "" : r.body.toLowerCase();
        String[] noVlHints = {
                "does not support image", "not support image", "unsupported image", "image is not supported",
                "image input is not supported", "only support text", "only text", "image is not allowed",
                "image not allowed", "vision is not supported", "does not support vision", "not support vision",
                "does not have vision", "no vision", "不支持图片", "不支持多模态", "仅支持文本", "只支持文本", "图片功能未开通"
        };
        for (String hint : noVlHints) {
            if (body.contains(hint)) {
                return false;
            }
        }
        return null;
    }

    /**
     * Token 上限探测：发送远超任何现有模型上限的 max_tokens 请求（Integer.MAX_VALUE），
     * 请求必被拒绝（不产生生成计费），从报错信息解析上下文长度/最大输出上限；
     * 兼容 1M+ 上下文窗口的大模型。
     */
    private Map<String, Integer> probeTokenLimits(String url, String apiKey, String model) {
        Map<String, Integer> limits = new HashMap<>();
        ChatProbeResult r = postChatProbe(url, apiKey, model, Integer.MAX_VALUE, false);
        if (r == null || (r.code >= 200 && r.code < 300)) {
            return limits;
        }
        String body = r.body == null ? "" : r.body;
        Integer ctx = parseLimitFromError(body, true);
        if (ctx != null) {
            limits.put("context", ctx);
        }
        Integer mx = parseLimitFromError(body, false);
        if (mx != null) {
            limits.put("max", mx);
        }
        return limits;
    }

    /**
     * 从报错文本解析 token 上限（context=true 解析上下文长度，否则解析最大输出）。
     * 兼容 OpenAI / vLLM / 火山方舟 / 智谱等常见报错措辞，尽力而为。
     */
    private Integer parseLimitFromError(String errorBody, boolean context) {
        if (StrUtil.isBlank(errorBody)) {
            return null;
        }
        // 剔除"请求值"形式（max_tokens: 1000000 / max_tokens (2147483647)），避免误取请求值而非上限
        String text = errorBody.toLowerCase()
                .replaceAll("max[_\\s-]?tokens?\\s*(?:[:=]|\\(\\s*)\\s*\\d{1,10}", "max_tokens: X");
        // 优先级1：明确的 "<= N" / "maximum context length is N" / "limit of N" 等
        String[] priPatterns = context
                ? new String[]{"maximum context length is\\D{0,24}(\\d{3,9})",
                        "context length\\D{0,24}(\\d{3,9})", "context_window\\D{0,24}(\\d{3,9})",
                        "context window\\D{0,24}(\\d{3,9})", "上下文长度\\D{0,24}(\\d{3,9})",
                        "最大上下文\\D{0,24}(\\d{3,9})"}
                : new String[]{"less than or equal to\\D{0,12}(\\d{3,9})",
                        "exceeds?(?: the)?(?: model's)?(?: maximum)?(?: token)?\\s?limit(?: of)?\\D{0,12}(\\d{3,9})",
                        "max[_\\s-]?tokens?(?: is| should be| must be)?\\D{0,24}(\\d{3,9})",
                        "最大(?:输出|回答)tokens?\\D{0,24}(\\d{3,9})", "最大为\\D{0,12}(\\d{3,9})",
                        "上限为\\D{0,12}(\\d{3,9})"};
        for (String pattern : priPatterns) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(text);
            if (m.find()) {
                Integer v = clampTokenNumber(m.group(1));
                if (v != null) {
                    return v;
                }
            }
        }
        // 优先级2：按关键词窗口取第一个大数
        String[] keys = context
                ? new String[]{"context length", "context_window", "context window", "context_length",
                        "maximum context", "上下文", "max context"}
                : new String[]{"max_tokens", "max tokens", "max_token", "output tokens", "maximum output", "max_tokens参数"};
        for (String key : keys) {
            int idx = text.indexOf(key);
            if (idx < 0) {
                continue;
            }
            int from = Math.max(0, idx - 40);
            int to = Math.min(text.length(), idx + 80);
            String window = text.substring(from, to);
            java.util.regex.Matcher wm = java.util.regex.Pattern.compile("\\d{3,9}").matcher(window);
            while (wm.find()) {
                Integer v = clampTokenNumber(wm.group());
                if (v != null) {
                    return v;
                }
            }
        }
        return null;
    }

    /**
     * 归一化 token 数值：仅接受合理区间（1024 ~ 1000 万），避免误取时间戳/ID 等数字。
     */
    private Integer clampTokenNumber(String s) {
        try {
            int v = Integer.parseInt(s);
            return (v >= 1024 && v <= 10000000) ? v : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从不同API响应中提取回答内容
     */
    @SuppressWarnings("unchecked")
    private String extractResponseContent(Map<String, Object> responseData) {
        try {
            // OpenAI 格式: {"choices": [{"message": {"content": "..."}}]}
            if (responseData.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseData.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    if (choice.containsKey("message") && choice.get("message") instanceof Map) {
                        Map<String, String> message = (Map<String, String>) choice.get("message");
                        if (message.containsKey("content")) {
                            return message.get("content");
                        }
                    }
                    if (choice.containsKey("text")) {
                        return String.valueOf(choice.get("text"));
                    }
                    if (choice.containsKey("content")) {
                        return String.valueOf(choice.get("content"));
                    }
                }
            }
            // 智谱格式: {"data": {"choices": [{"content": "..."}]}}
            if (responseData.containsKey("data") && responseData.get("data") instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) responseData.get("data");
                if (data.containsKey("choices")) {
                    List<Map<String, String>> choices = (List<Map<String, String>>) data.get("choices");
                    if (choices != null && !choices.isEmpty() && choices.get(0).containsKey("content")) {
                        return choices.get(0).get("content");
                    }
                }
            }
            // 其他常见格式
            if (responseData.containsKey("text")) return String.valueOf(responseData.get("text"));
            if (responseData.containsKey("response")) return String.valueOf(responseData.get("response"));
            if (responseData.containsKey("result")) return String.valueOf(responseData.get("result"));
            if (responseData.containsKey("content")) return String.valueOf(responseData.get("content"));
            // 无法提取，返回整个响应
            return OBJECT_MAPPER.writeValueAsString(responseData);
        } catch (Exception e) {
            log.warn("提取响应内容失败: {}", e.getMessage());
            return String.valueOf(responseData);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyModelConfig(ModelConfigCopyReqVO reqVO) {
        ModelConfigDO source = validateExists(reqVO.getId());

        // 校验新UID唯一性
        validateUidUnique(reqVO.getNewUid(), null);

        ModelConfigDO copy = BeanUtils.toBean(source, ModelConfigDO.class);
        copy.setId(null);
        copy.setUid(reqVO.getNewUid());
        copy.setName(reqVO.getNewName());
        copy.setIsActive(reqVO.getIsActive() != null ? reqVO.getIsActive() : 0);
        copy.setActivatedAt(reqVO.getIsActive() != null && reqVO.getIsActive() == 1 ? LocalDateTime.now() : null);
        copy.setCreator(null);
        copy.setUpdater(null);
        copy.setCreateTime(null);
        copy.setUpdateTime(null);

        modelConfigMapper.insert(copy);
        return copy.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultModelConfig(Long id) {
        ModelConfigDO target = validateExists(id);
        assertValidType(target.getModelType());
        // 同类内唯一激活：仅停用同用途分类的其他配置，不影响其他类
        deactivateSameTypeExcept(target.getModelType(), target.getId());
        target.setIsActive(1);
        target.setActivatedAt(LocalDateTime.now());
        modelConfigMapper.updateById(target);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchOperation(ModelConfigBatchReqVO reqVO) {
        int count = 0;
        String action = reqVO.getAction();

        for (Long id : reqVO.getIds()) {
            ModelConfigDO config = modelConfigMapper.selectById(id);
            if (config == null) continue;

            switch (action) {
                case "activate":
                    if (config.getIsActive() != null && config.getIsActive() == 1) continue;
                    config.setIsActive(1);
                    config.setActivatedAt(LocalDateTime.now());
                    modelConfigMapper.updateById(config);
                    // 同类唯一激活：激活时停用同类其它激活配置
                    deactivateSameTypeExcept(config.getModelType(), config.getId());
                    count++;
                    break;
                case "deactivate":
                    if (config.getIsActive() == null || config.getIsActive() == 0) continue;
                    config.setIsActive(0);
                    modelConfigMapper.updateById(config);
                    count++;
                    break;
                case "delete":
                    modelConfigMapper.deleteById(id);
                    count++;
                    break;
                default:
                    throw exception(MODEL_CONFIG_BATCH_ACTION_INVALID);
            }
        }

        return count;
    }

    @Override
    public ModelConfigStatisticsRespVO getStatistics() {
        // 获取所有配置
        List<ModelConfigDO> allConfigs = modelConfigMapper.selectList();

        ModelConfigStatisticsRespVO result = new ModelConfigStatisticsRespVO();
        result.setTotalConfigs((long) allConfigs.size());
        result.setActiveConfigs(allConfigs.stream()
                .filter(c -> c.getIsActive() != null && c.getIsActive() == 1)
                .count());
        result.setTotalUsage(0L); // 使用次数需要从聊天记录中统计，此处先为0

        // 构建统计项列表
        List<ModelConfigStatisticsRespVO.ModelConfigStatItem> statItems = new ArrayList<>();
        for (ModelConfigDO config : allConfigs) {
            ModelConfigStatisticsRespVO.ModelConfigStatItem item = new ModelConfigStatisticsRespVO.ModelConfigStatItem();
            item.setConfigId(config.getId());
            item.setName(config.getName());
            item.setModelType(config.getModelType());
            item.setIsActive(config.getIsActive());
            item.setUsageCount(0L);
            item.setTotalSessions(0L);
            item.setLastUsed(null);
            item.setCreateTime(config.getCreateTime() != null ? config.getCreateTime().toString() : null);
            item.setDescription(config.getDescription());
            statItems.add(item);
        }
        // 按配置ID排序
        statItems.sort(Comparator.comparing(ModelConfigStatisticsRespVO.ModelConfigStatItem::getConfigId));

        result.setStatistics(statItems);
        return result;
    }

    @Override
    public List<ModelConfigSimpleVO> getSimpleModelConfigList() {
        List<ModelConfigDO> list = modelConfigMapper.selectActiveList();
        return BeanUtils.toBean(list, ModelConfigSimpleVO.class);
    }

    // ==================== 私有辅助方法 ====================

    private ModelConfigDO validateExists(Long id) {
        ModelConfigDO config = modelConfigMapper.selectById(id);
        if (config == null) {
            throw exception(MODEL_CONFIG_NOT_EXISTS);
        }
        return config;
    }

    private void validateUidUnique(String uid, Long excludeId) {
        ModelConfigDO existing = modelConfigMapper.selectByUid(uid);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw exception(MODEL_CONFIG_UID_EXISTS);
        }
    }

    /**
     * 校验用途分类合法性（embedding / llm / ocr / rerank / mineru；空视为 llm）
     * mineru 为文档文本解析通道（独立分类，与 ocr 图片处理无关）。
     */
    private void assertValidType(String modelType) {
        String type = (modelType == null || modelType.isEmpty()) ? "llm" : modelType;
        if (!"embedding".equals(type) && !"llm".equals(type)
                && !"ocr".equals(type) && !"rerank".equals(type)
                && !"mineru".equals(type)) {
            throw exception(MODEL_CONFIG_TYPE_INVALID);
        }
    }

    /**
     * 停用指定用途分类下除 excludeId 外的所有激活配置（实现"每类各有一个默认"）
     */
    private void deactivateSameTypeExcept(String modelType, Long excludeId) {
        String type = (modelType == null || modelType.isEmpty()) ? "llm" : modelType;
        List<ModelConfigDO> activeList = modelConfigMapper.selectActiveByType(type);
        for (ModelConfigDO config : activeList) {
            if (config.getId().equals(excludeId)) {
                continue;
            }
            config.setIsActive(0);
            modelConfigMapper.updateById(config);
        }
    }
}
