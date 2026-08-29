package cn.iocoder.yudao.module.agent.controller.admin.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 工具审批操作（允许/拒绝）Request VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 工具审批操作 Request VO")
@Data
public class ApprovalActionReqVO {

    @Schema(description = "审批请求ID（QwenPaw 下发）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "审批请求ID不能为空")
    private String requestId;

    @Schema(description = "会话ID（QwenPaw root session_id，与对话时一致）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "会话ID不能为空")
    private String sessionId;

    @Schema(description = "审批范围（允许时可选）：exact=仅本次, similar=同类工具后续自动放行, all=该会话全部放行", example = "exact")
    private String scope;

    @Schema(description = "拒绝原因（拒绝时可选）", example = "信息不足")
    private String reason;

}