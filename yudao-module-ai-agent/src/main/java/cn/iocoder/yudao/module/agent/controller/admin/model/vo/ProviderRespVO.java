package cn.iocoder.yudao.module.agent.controller.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Provider 响应 VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - Provider 响应")
@Data
public class ProviderRespVO {

    @Schema(description = "Provider ID")
    private String id;

    @Schema(description = "Provider 名称")
    private String name;

    @Schema(description = "API Key 前缀（脱敏展示）")
    private String apiKeyPrefix;

    @Schema(description = "当前聊天模型")
    private String chatModel;

    @Schema(description = "base_url")
    private String baseUrl;

    @Schema(description = "是否已配置 API Key")
    private Boolean configured;

    @Schema(description = "是否必须配置 API Key（false 表示免费/本地，无需 key）")
    private Boolean requireApiKey;

    @Schema(description = "是否为自定义 Provider")
    private Boolean isCustom;

    @Schema(description = "是否为本地 Provider")
    private Boolean isLocal;

    @Schema(description = "是否支持模型发现")
    private Boolean supportModelDiscovery;

    @Schema(description = "内置模型列表")
    private List<ModelInfoVO> models;

    @Schema(description = "用户添加的模型列表")
    private List<ModelInfoVO> extraModels;

    @Schema(description = "原始数据（透传 QwenPaw 返回的完整字段）")
    private Map<String, Object> raw;
}
