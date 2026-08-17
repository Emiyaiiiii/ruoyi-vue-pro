package cn.iocoder.yudao.module.agent.dal.dataobject.chatsession;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 问答会话 DO
 *
 * @author 吴皓
 */
@TableName("ai_chat_session")
@KeySequence("ai_chat_session_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatSessionDO extends BaseDO {

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
     * 智能体ID
     */
    private Long agentId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * QwenPaw session id
     */
    private String sessionKey;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 状态: 0=关闭, 1=进行中
     */
    private Integer status;

}
