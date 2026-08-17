package cn.iocoder.yudao.module.agent.dal.dataobject.chatmessage;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 问答消息 DO
 *
 * @author 吴皓
 */
@TableName("ai_chat_message")
@KeySequence("ai_chat_message_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessageDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 租户编号
     */
    private Long tenantId;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 智能体ID
     */
    private Long agentId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色: user/assistant/tool/system
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 思考/推理内容
     */
    private String reasoningContent;

    /**
     * 工具调用记录（JSON）
     */
    private String toolCalls;

    /**
     * Token 用量
     */
    private Integer tokens;

}
