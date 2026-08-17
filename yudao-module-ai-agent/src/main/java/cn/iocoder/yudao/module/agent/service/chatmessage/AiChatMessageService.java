package cn.iocoder.yudao.module.agent.service.chatmessage;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatMessagePageReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.chatmessage.AiChatMessageDO;

import java.util.List;

/**
 * 问答消息 Service 接口
 *
 * @author 吴皓
 */
public interface AiChatMessageService {

    /**
     * 保存消息
     */
    Long createMessage(AiChatMessageDO message);

    /**
     * 分页查询消息
     */
    PageResult<AiChatMessageDO> getMessagePage(ChatMessagePageReqVO pageReqVO);

    /**
     * 查询某会话的全部消息（按时间升序）
     */
    List<AiChatMessageDO> getMessageListBySessionId(Long sessionId);

    /**
     * 删除某会话的全部消息
     */
    void deleteBySessionId(Long sessionId);

}
