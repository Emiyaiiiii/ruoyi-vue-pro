package cn.iocoder.yudao.module.agent.service.model;

import cn.iocoder.yudao.module.agent.controller.admin.model.vo.*;

import java.util.List;
import java.util.Map;

/**
 * 模型管理 Service 接口
 *
 * <p>封装 QwenPaw /models 系列接口，供 Controller 调用。
 * 模型数据由 QwenPaw 管理，芋道侧只做透传与 VO 转换。
 *
 * @author 吴皓
 */
public interface AiModelService {

    /**
     * 列出所有 Provider（含其下模型列表）
     */
    List<ProviderRespVO> listProviders();

    /**
     * 配置 Provider（API key、base_url 等）
     */
    ProviderRespVO configureProvider(ProviderConfigReqVO reqVO);

    /**
     * 测试 Provider 连接
     */
    TestConnectionRespVO testProvider(String providerId);

    /**
     * 测试特定模型连接
     */
    TestConnectionRespVO testModel(String providerId, String modelId);

    /**
     * 从 Provider API 发现可用模型
     */
    List<ModelInfoVO> discoverModels(String providerId);

    /**
     * 向 Provider 添加模型
     */
    ProviderRespVO addModel(AddModelReqVO reqVO);

    /**
     * 从 Provider 删除模型
     */
    void deleteModel(String providerId, String modelId);

    /**
     * 配置模型参数
     */
    ProviderRespVO configureModel(ModelConfigReqVO reqVO);

    /**
     * 获取当前激活的模型
     */
    ActiveModelRespVO getActiveModel(String scope, String agentId);

    /**
     * 设置激活模型
     */
    ActiveModelRespVO setActiveModel(SetActiveModelReqVO reqVO);

    /**
     * 创建自定义 Provider
     */
    ProviderRespVO createCustomProvider(Map<String, Object> providerInfo);

    /**
     * 删除自定义 Provider
     */
    void deleteCustomProvider(String providerId);

    /**
     * 获取所有可用模型（扁平列表，供智能体创建时下拉选择）
     */
    List<Map<String, Object>> listAllModels();
}
