package cn.iocoder.yudao.module.agent.controller.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

/**
 * 模型配置请求 VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 模型配置请求")
@Data
public class ModelConfigReqVO {

    @Schema(description = "Provider ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Provider ID 不能为空")
    private String providerId;

    @Schema(description = "模型 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "模型 ID 不能为空")
    private String modelId;

    @Schema(description = "最大输出 token 数")
    private Integer maxTokens;

    @Schema(description = "最大输入长度")
    private Integer maxInputLength;

    @Schema(description = "生成参数 JSON（temperature、top_p 等）")
    private String generateKwargs;

    @Schema(description = "是否启用思考模式")
    private Boolean thinkingEnabled;

    @Schema(description = "思考预算")
    private Integer thinkingBudget;

    @Schema(description = "推理力度")
    private String reasoningEffort;
}
