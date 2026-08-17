package cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 问答会话重命名 Req VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 问答会话重命名 Request VO")
@Data
public class ChatSessionUpdateTitleReqVO {

    @Schema(description = "会话ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "会话ID不能为空")
    private Long id;

    @Schema(description = "新标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "关于报销制度的咨询")
    @NotEmpty(message = "会话标题不能为空")
    private String title;

}
