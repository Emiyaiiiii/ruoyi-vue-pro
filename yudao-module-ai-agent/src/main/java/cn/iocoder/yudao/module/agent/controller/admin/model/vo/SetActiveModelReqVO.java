package cn.iocoder.yudao.module.agent.controller.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

/**
 * 设置激活模型请求 VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 设置激活模型请求")
@Data
public class SetActiveModelReqVO {

    @Schema(description = "作用域：global / agent", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "作用域不能为空")
    private String scope;

    @Schema(description = "Provider ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Provider ID 不能为空")
    private String providerId;

    @Schema(description = "模型 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "模型 ID 不能为空")
    private String model;

    @Schema(description = "智能体 ID（scope=agent 时必填）")
    private String agentId;
}
