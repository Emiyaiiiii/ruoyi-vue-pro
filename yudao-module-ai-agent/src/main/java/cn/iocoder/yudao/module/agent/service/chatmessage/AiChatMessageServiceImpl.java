package cn.iocoder.yudao.module.agent.service.chatmessage;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatMessagePageReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.chatmessage.AiChatMessageDO;
import cn.iocoder.yudao.module.agent.dal.mysql.chatmessage.AiChatMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 问答消息 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
@Slf4j
public class AiChatMessageServiceImpl implements AiChatMessageService {

    @Resource
    private AiChatMessageMapper chatMessageMapper;

    @Override
    public Long createMessage(AiChatMessageDO message) {
        if (message.getTokens() == null) {
            message.setTokens(0);
        }
        if (message.getToolCalls() == null) {
            message.setToolCalls("");
        }
        chatMessageMapper.insert(message);
        return message.getId();
    }

    @Override
    public PageResult<AiChatMessageDO> getMessagePage(ChatMessagePageReqVO pageReqVO) {
        return chatMessageMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AiChatMessageDO> getMessageListBySessionId(Long sessionId) {
        return chatMessageMapper.selectListBySessionId(sessionId);
    }

    @Override
    public void deleteBySessionId(Long sessionId) {
        chatMessageMapper.deleteBySessionId(sessionId);
    }

}
