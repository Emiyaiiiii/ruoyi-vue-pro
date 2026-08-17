package cn.iocoder.yudao.module.agent.dal.mysql.chatmessage;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.agent.dal.dataobject.chatmessage.AiChatMessageDO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatMessagePageReqVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 问答消息 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface AiChatMessageMapper extends BaseMapperX<AiChatMessageDO> {

    /**
     * 分页查询（支持按会话过滤）
     */
    default PageResult<AiChatMessageDO> selectPage(ChatMessagePageReqVO reqVO) {
        LambdaQueryWrapperX<AiChatMessageDO> wrapper = new LambdaQueryWrapperX<AiChatMessageDO>()
                .eqIfPresent(AiChatMessageDO::getSessionId, reqVO.getSessionId())
                .eqIfPresent(AiChatMessageDO::getAgentId, reqVO.getAgentId())
                .eqIfPresent(AiChatMessageDO::getRole, reqVO.getRole());

        wrapper.orderByAsc(AiChatMessageDO::getCreateTime);

        return selectPage(reqVO, wrapper);
    }

    /**
     * 查询某会话的全部消息（按时间升序）
     */
    default List<AiChatMessageDO> selectListBySessionId(Long sessionId) {
        return selectList(new LambdaQueryWrapperX<AiChatMessageDO>()
                .eq(AiChatMessageDO::getSessionId, sessionId)
                .orderByAsc(AiChatMessageDO::getCreateTime));
    }

    /**
     * 统计某会话的消息数
     */
    default Long selectCountBySessionId(Long sessionId) {
        return selectCount(new LambdaQueryWrapperX<AiChatMessageDO>()
                .eq(AiChatMessageDO::getSessionId, sessionId));
    }

    /**
     * 删除某会话的全部消息
     */
    default void deleteBySessionId(Long sessionId) {
        delete(new LambdaQueryWrapperX<AiChatMessageDO>()
                .eq(AiChatMessageDO::getSessionId, sessionId));
    }

}
