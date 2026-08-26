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
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModelConfig(Long id) {
        validateExists(id);
        modelConfigMapper.deleteById(id);
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
     * 测试 Embedding（embeddings 协议）
     */
    private String testEmbedding(ModelConfigDO config) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", StrUtil.isNotBlank(config.getModel()) ? config.getModel() : config.getUid());
        payload.put("input", "你好");
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
     * 校验用途分类合法性（embedding / llm / ocr / rerank；空视为 llm）
     */
    private void assertValidType(String modelType) {
        String type = (modelType == null || modelType.isEmpty()) ? "llm" : modelType;
        if (!"embedding".equals(type) && !"llm".equals(type)
                && !"ocr".equals(type) && !"rerank".equals(type)) {
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
