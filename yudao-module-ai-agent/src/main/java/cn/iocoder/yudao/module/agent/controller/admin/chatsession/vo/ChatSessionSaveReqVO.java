package cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 问答会话创建 Req VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 问答会话创建 Request VO")
@Data
public class ChatSessionSaveReqVO {

    @Schema(description = "智能体ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "智能体ID不能为空")
    private Long agentId;

    @Schema(description = "用户ID，不传则默认当前登录用户", example = "1024")
    private Long userId;

    @Schema(description = "会话标题", example = "关于报销制度的咨询")
    private String title;

}
