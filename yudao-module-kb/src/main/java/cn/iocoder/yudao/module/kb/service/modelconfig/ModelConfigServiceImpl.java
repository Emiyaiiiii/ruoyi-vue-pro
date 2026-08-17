package cn.iocoder.yudao.module.kb.service.modelconfig;

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

    // ==================== 部署类型映射 ====================
    private static final Map<String, String> DEPLOY_DISPLAY_MAP = new LinkedHashMap<>();
    static {
        DEPLOY_DISPLAY_MAP.put("doubao", "豆包");
        DEPLOY_DISPLAY_MAP.put("bailian", "百炼");
        DEPLOY_DISPLAY_MAP.put("lite", "LiteLLM");
        DEPLOY_DISPLAY_MAP.put("openai", "OpenAI");
        DEPLOY_DISPLAY_MAP.put("api", "通用API");
        DEPLOY_DISPLAY_MAP.put("xinf", "Xinference");
        DEPLOY_DISPLAY_MAP.put("vllm", "VLLM");
        DEPLOY_DISPLAY_MAP.put("zhipu", "智谱AI");
        DEPLOY_DISPLAY_MAP.put("other", "其他");
    }

    private static final Map<String, String> PLATFORM_DISPLAY_MAP = new LinkedHashMap<>();
    static {
        PLATFORM_DISPLAY_MAP.put("web", "Web端");
        PLATFORM_DISPLAY_MAP.put("app", "App端");
        PLATFORM_DISPLAY_MAP.put("both", "两者都支持");
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
        if (modelConfig.getPlatform() == null) modelConfig.setPlatform("both");
        if (modelConfig.getMetadata() == null) modelConfig.setMetadata("{}");
        if (modelConfig.getConfig() == null) modelConfig.setConfig("{}");
        if (modelConfig.getIsActive() != null && modelConfig.getIsActive() == 1) {
            modelConfig.setActivatedAt(LocalDateTime.now());
        }

        modelConfigMapper.insert(modelConfig);
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
        if (config.getIsActive() != null && config.getIsActive() == 1) {
            throw exception(MODEL_CONFIG_ALREADY_ACTIVE);
        }
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
        modelInfo.setDeploy(config.getDeploy());
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
     * 实际发起测试请求
     */
    private String doTestRequest(ModelConfigDO config, ModelConfigTestReqVO reqVO) throws Exception {
        // 使用 Java 标准 HTTP 请求，避免引入额外依赖
        java.net.URL url = new java.net.URL(config.getUrl());
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + config.getAppkey());

        // 构建请求体
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", config.getUid());
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", reqVO.getTestMessage() != null ? reqVO.getTestMessage() : "你好，请回复\"测试成功\"");
        messages.add(msg);
        payload.put("messages", messages);
        payload.put("temperature", reqVO.getTemperature() != null ? reqVO.getTemperature() : config.getTemperature());
        payload.put("max_tokens", reqVO.getMaxTokens() != null ? reqVO.getMaxTokens() : Math.min(config.getMaxTokens(), 200));

        String jsonBody = OBJECT_MAPPER.writeValueAsString(payload);
        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            try (java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream(), "UTF-8")) {
                String responseBody = scanner.useDelimiter("\\A").next();
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = OBJECT_MAPPER.readValue(responseBody, Map.class);
                // 尝试从各种格式的响应中提取内容
                return extractResponseContent(responseMap, config.getDeploy());
            }
        } else {
            try (java.util.Scanner scanner = new java.util.Scanner(conn.getErrorStream(), "UTF-8")) {
                String errorBody = scanner.useDelimiter("\\A").next();
                throw new RuntimeException("HTTP " + responseCode + ": " + errorBody);
            }
        }
    }

    /**
     * 从不同API响应中提取回答内容
     */
    @SuppressWarnings("unchecked")
    private String extractResponseContent(Map<String, Object> responseData, String deployType) {
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

        // 停用其他所有激活的配置
        List<ModelConfigDO> activeList = modelConfigMapper.selectActiveList();
        for (ModelConfigDO config : activeList) {
            config.setIsActive(0);
            modelConfigMapper.updateById(config);
        }

        // 激活目标配置
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
            item.setDeploy(config.getDeploy());
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
}
