package cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

/**
 * 发送消息 Req VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 发送消息 Request VO")
@Data
public class ChatSendReqVO {

    @Schema(description = "用户消息", requiredMode = Schema.RequiredMode.REQUIRED, example = "差旅报销标准是什么？")
    @NotEmpty(message = "消息内容不能为空")
    private String message;

}
