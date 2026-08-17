package cn.iocoder.yudao.module.agent.controller.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

/**
 * Provider 配置请求 VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - Provider 配置请求")
@Data
public class ProviderConfigReqVO {

    @Schema(description = "Provider ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Provider ID 不能为空")
    private String providerId;

    @Schema(description = "API Key")
    private String apiKey;

    @Schema(description = "Base URL")
    private String baseUrl;

    @Schema(description = "自定义请求头")
    private String customHeaders;

    @Schema(description = "认证模式：api_key / auth_token")
    private String authMode;
}
