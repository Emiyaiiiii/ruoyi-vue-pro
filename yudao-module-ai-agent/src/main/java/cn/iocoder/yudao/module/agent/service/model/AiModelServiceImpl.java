package cn.iocoder.yudao.module.agent.service.model;

import cn.iocoder.yudao.module.agent.controller.admin.model.vo.*;
import cn.iocoder.yudao.module.agent.framework.config.QwenPawClient;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.*;

/**
 * 模型管理 Service 实现
 *
 * @author 吴皓
 */
@Service
@Validated
@Slf4j
public class AiModelServiceImpl implements AiModelService {

    @Resource
    private QwenPawClient qwenPawClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ProviderRespVO> listProviders() {
        List<Map<String, Object>> providers = qwenPawClient.listProviders();
        List<ProviderRespVO> result = new ArrayList<>();
        for (Map<String, Object> raw : providers) {
            result.add(convertToProviderVO(raw));
        }
        return result;
    }

    @Override
    public ProviderRespVO configureProvider(ProviderConfigReqVO reqVO) {
        Map<String, Object> config = new LinkedHashMap<>();
        if (reqVO.getApiKey() != null) {
            config.put("api_key", reqVO.getApiKey());
        }
        if (reqVO.getBaseUrl() != null) {
            config.put("base_url", reqVO.getBaseUrl());
        }
        if (reqVO.getAuthMode() != null) {
            config.put("auth_mode", reqVO.getAuthMode());
        }
        if (reqVO.getCustomHeaders() != null && !reqVO.getCustomHeaders().isEmpty()) {
            try {
                config.put("custom_headers", objectMapper.readValue(reqVO.getCustomHeaders(), Map.class));
            } catch (Exception e) {
                log.warn("[configureProvider] customHeaders 解析失败: {}", reqVO.getCustomHeaders(), e);
            }
        }
        Map<String, Object> raw = qwenPawClient.configureProvider(reqVO.getProviderId(), config);
        return convertToProviderVO(raw);
    }

    @Override
    public TestConnectionRespVO testProvider(String providerId) {
        Map<String, Object> raw = qwenPawClient.testProvider(providerId);
        return convertToTestRespVO(raw);
    }

    @Override
    public TestConnectionRespVO testModel(String providerId, String modelId) {
        Map<String, Object> raw = qwenPawClient.testModel(providerId, modelId);
        return convertToTestRespVO(raw);
    }

    @Override
    public List<ModelInfoVO> discoverModels(String providerId) {
        List<Map<String, Object>> models = qwenPawClient.discoverModels(providerId);
        List<ModelInfoVO> result = new ArrayList<>();
        for (Map<String, Object> raw : models) {
            result.add(convertToModelInfoVO(raw));
        }
        return result;
    }

    @Override
    public ProviderRespVO addModel(AddModelReqVO reqVO) {
        Map<String, Object> modelInfo = new LinkedHashMap<>();
        modelInfo.put("id", reqVO.getModelId());
        modelInfo.put("name", reqVO.getName());
        if (reqVO.getSupportsMultimodal() != null) {
            modelInfo.put("supports_multimodal", reqVO.getSupportsMultimodal());
        }
        if (reqVO.getSupportsImage() != null) {
            modelInfo.put("supports_image", reqVO.getSupportsImage());
        }
        if (reqVO.getSupportsVideo() != null) {
            modelInfo.put("supports_video", reqVO.getSupportsVideo());
        }
        Map<String, Object> raw = qwenPawClient.addModel(reqVO.getProviderId(), modelInfo);
        return convertToProviderVO(raw);
    }

    @Override
    public void deleteModel(String providerId, String modelId) {
        qwenPawClient.deleteModel(providerId, modelId);
    }

    @Override
    public ProviderRespVO configureModel(ModelConfigReqVO reqVO) {
        Map<String, Object> config = new LinkedHashMap<>();
        if (reqVO.getMaxTokens() != null) {
            config.put("max_tokens", reqVO.getMaxTokens());
        }
        if (reqVO.getMaxInputLength() != null) {
            config.put("max_input_length", reqVO.getMaxInputLength());
        }
        if (reqVO.getGenerateKwargs() != null && !reqVO.getGenerateKwargs().isEmpty()) {
            try {
                config.put("generate_kwargs", objectMapper.readValue(reqVO.getGenerateKwargs(), Map.class));
            } catch (Exception e) {
                log.warn("[configureModel] generateKwargs 解析失败: {}", reqVO.getGenerateKwargs(), e);
            }
        }
        if (reqVO.getThinkingEnabled() != null) {
            config.put("thinking_enabled", reqVO.getThinkingEnabled());
        }
        if (reqVO.getThinkingBudget() != null) {
            config.put("thinking_budget", reqVO.getThinkingBudget());
        }
        if (reqVO.getReasoningEffort() != null) {
            config.put("reasoning_effort", reqVO.getReasoningEffort());
        }
        Map<String, Object> raw = qwenPawClient.configureModel(reqVO.getProviderId(), reqVO.getModelId(), config);
        return convertToProviderVO(raw);
    }

    @Override
    public ActiveModelRespVO getActiveModel(String scope, String agentId) {
        Map<String, Object> raw = qwenPawClient.getActiveModel(scope, agentId);
        ActiveModelRespVO vo = convertToActiveModelVO(raw);
        if (vo.getScope() == null) {
            vo.setScope(scope != null && !scope.isEmpty() ? scope : "effective");
        }
        return vo;
    }

    @Override
    public ActiveModelRespVO setActiveModel(SetActiveModelReqVO reqVO) {
        Map<String, Object> raw = qwenPawClient.setActiveModel(
                reqVO.getScope(), reqVO.getProviderId(), reqVO.getModel(), reqVO.getAgentId());
        return convertToActiveModelVO(raw);
    }

    @Override
    public ProviderRespVO createCustomProvider(Map<String, Object> providerInfo) {
        Map<String, Object> raw = qwenPawClient.createCustomProvider(providerInfo);
        return convertToProviderVO(raw);
    }

    @Override
    public void deleteCustomProvider(String providerId) {
        qwenPawClient.deleteCustomProvider(providerId);
    }

    @Override
    public List<Map<String, Object>> listAllModels() {
        List<Map<String, Object>> providers = qwenPawClient.listProviders();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> provider : providers) {
            String providerId = getString(provider, "id");
            String providerName = getString(provider, "name");
            // 合并 models 和 extra_models
            List<Map<String, Object>> allModels = new ArrayList<>();
            allModels.addAll(toMapList(provider.get("models")));
            allModels.addAll(toMapList(provider.get("extra_models")));
            for (Map<String, Object> model : allModels) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("providerId", providerId);
                item.put("providerName", providerName);
                item.put("modelId", getString(model, "id"));
                item.put("modelName", getString(model, "name"));
                item.put("supportsMultimodal", model.get("supports_multimodal"));
                item.put("maxTokens", model.get("max_tokens"));
                result.add(item);
            }
        }
        return result;
    }

    // ==================== 转换方法 ====================

    @SuppressWarnings("unchecked")
    private ProviderRespVO convertToProviderVO(Map<String, Object> raw) {
        if (raw == null) {
            return null;
        }
        ProviderRespVO vo = new ProviderRespVO();
        vo.setId(getString(raw, "id"));
        vo.setName(getString(raw, "name"));
        vo.setApiKeyPrefix(getString(raw, "api_key_prefix"));
        vo.setChatModel(getString(raw, "chat_model"));
        vo.setBaseUrl(getString(raw, "base_url"));
        vo.setIsCustom(getBool(raw, "is_custom"));
        vo.setIsLocal(getBool(raw, "is_local"));
        vo.setRequireApiKey(getBool(raw, "require_api_key"));
        vo.setSupportModelDiscovery(getBool(raw, "support_model_discovery"));
        // 判断是否已配置：与 QwenPaw 前端 getIsConfigured() 完全一致
        //   - 内嵌本地(qwenpaw-local/copaw-local)恒为已配置
        //   - 自定义 provider 配置了 base_url 即视为已配置
        //   - require_api_key===false（免费/本地服务商，如 OpenCode/Kilo）无需 key 即视为已配置
        //   - require_api_key===true 时以实际配置的 api_key 非空为准（不能用 api_key_prefix，
        //     因为内置服务商往往预置了默认 prefix 但并未配置 key）
        String providerId = getString(raw, "id");
        String apiKey = getString(raw, "api_key");
        Boolean requireApiKey = vo.getRequireApiKey();
        Boolean isCustom = vo.getIsCustom();
        String baseUrl = getString(raw, "base_url");
        // require_api_key 缺失时按 true 处理（QwenPaw pydantic 默认值即为 true）
        boolean needsKey = requireApiKey == null || requireApiKey;
        boolean configured;
        if ("qwenpaw-local".equals(providerId) || "copaw-local".equals(providerId)) {
            configured = true;
        } else if (Boolean.TRUE.equals(isCustom) && !StrUtil.isBlank(baseUrl)) {
            configured = true;
        } else if (!needsKey) {
            configured = true;
        } else if (!StrUtil.isBlank(apiKey)) {
            configured = true;
        } else {
            configured = false;
        }
        vo.setConfigured(configured);
        // 模型列表
        vo.setModels(toModelInfoVOList(toMapList(raw.get("models"))));
        vo.setExtraModels(toModelInfoVOList(toMapList(raw.get("extra_models"))));
        vo.setRaw(raw);
        return vo;
    }

    private List<ModelInfoVO> toModelInfoVOList(List<Map<String, Object>> rawList) {
        List<ModelInfoVO> result = new ArrayList<>();
        if (rawList == null) {
            return result;
        }
        for (Map<String, Object> raw : rawList) {
            result.add(convertToModelInfoVO(raw));
        }
        return result;
    }

    private ModelInfoVO convertToModelInfoVO(Map<String, Object> raw) {
        ModelInfoVO vo = new ModelInfoVO();
        vo.setId(getString(raw, "id"));
        vo.setName(getString(raw, "name"));
        vo.setSupportsMultimodal(getBool(raw, "supports_multimodal"));
        vo.setSupportsImage(getBool(raw, "supports_image"));
        vo.setSupportsVideo(getBool(raw, "supports_video"));
        vo.setMaxTokens(getInt(raw, "max_tokens"));
        vo.setMaxInputLength(getInt(raw, "max_input_length"));
        vo.setThinkingEnabled(getBool(raw, "thinking_enabled"));
        vo.setThinkingBudget(getInt(raw, "thinking_budget"));
        vo.setReasoningEffort(getString(raw, "reasoning_effort"));
        Object kwargs = raw.get("generate_kwargs");
        if (kwargs instanceof Map) {
            vo.setGenerateKwargs((Map<String, Object>) kwargs);
        }
        return vo;
    }

    private TestConnectionRespVO convertToTestRespVO(Map<String, Object> raw) {
        TestConnectionRespVO vo = new TestConnectionRespVO();
        if (raw == null) {
            vo.setSuccess(false);
            vo.setError("无响应");
            return vo;
        }
        vo.setSuccess(getBool(raw, "success"));
        vo.setError(getString(raw, "error"));
        Object latency = raw.get("latency_ms");
        if (latency instanceof Number) {
            vo.setLatencyMs(((Number) latency).longValue());
        }
        return vo;
    }

    private ActiveModelRespVO convertToActiveModelVO(Map<String, Object> raw) {
        ActiveModelRespVO vo = new ActiveModelRespVO();
        if (raw == null) {
            return vo;
        }
        // QwenPaw 的 /models/active 返回 ActiveModelsInfo，槽位嵌套在 active_llm 下：
        //   { "active_llm": { "provider_id": "...", "model": "..." }, ... }
        Object activeObj = raw.get("active_llm");
        if (activeObj instanceof Map) {
            Map<String, Object> active = (Map<String, Object>) activeObj;
            vo.setProviderId(getString(active, "provider_id"));
            vo.setModel(getString(active, "model"));
        }
        vo.setScope(getString(raw, "scope"));
        vo.setAgentId(getString(raw, "agent_id"));
        return vo;
    }

    // ==================== 工具方法 ====================

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private Boolean getBool(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toMapList(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                }
            }
        }
        return result;
    }
}
