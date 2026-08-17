package cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发送消息 Resp VO
 *
 * <p>骨架为同步返回；接入 SSE 流式后，改为 Controller 层逐块转发，本 VO 仅用于非流式场景。
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 发送消息 Response VO")
@Data
public class ChatSendRespVO {

    @Schema(description = "本地会话ID", example = "1")
    private Long sessionId;

    @Schema(description = "助手回复内容")
    private String content;

}
