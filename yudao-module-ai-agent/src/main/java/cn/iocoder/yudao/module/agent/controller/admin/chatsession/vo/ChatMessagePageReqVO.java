package cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 问答消息分页 Req VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 问答消息分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ChatMessagePageReqVO extends PageParam {

    @Schema(description = "会话ID", example = "1")
    private Long sessionId;

    @Schema(description = "智能体ID", example = "1")
    private Long agentId;

    @Schema(description = "角色: user/assistant/tool/system", example = "user")
    private String role;

}
