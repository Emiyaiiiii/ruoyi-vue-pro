package cn.iocoder.yudao.module.agent.dal.mysql.chatsession;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.agent.dal.dataobject.chatsession.AiChatSessionDO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatSessionPageReqVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 问答会话 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface AiChatSessionMapper extends BaseMapperX<AiChatSessionDO> {

    /**
     * 分页查询（支持按智能体、用户、状态过滤）
     */
    default PageResult<AiChatSessionDO> selectPage(ChatSessionPageReqVO reqVO) {
        LambdaQueryWrapperX<AiChatSessionDO> wrapper = new LambdaQueryWrapperX<AiChatSessionDO>()
                .eqIfPresent(AiChatSessionDO::getAgentId, reqVO.getAgentId())
                .eqIfPresent(AiChatSessionDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AiChatSessionDO::getStatus, reqVO.getStatus());

        // 关键字搜索：title
        if (reqVO.getSearch() != null && !reqVO.getSearch().isEmpty()) {
            wrapper.like(AiChatSessionDO::getTitle, reqVO.getSearch());
        }

        wrapper.orderByDesc(AiChatSessionDO::getCreateTime);

        return selectPage(reqVO, wrapper);
    }

}
