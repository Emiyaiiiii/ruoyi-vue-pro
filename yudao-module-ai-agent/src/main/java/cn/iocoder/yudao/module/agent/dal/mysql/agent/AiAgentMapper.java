package cn.iocoder.yudao.module.agent.dal.mysql.agent;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.agent.dal.dataobject.agent.AiAgentDO;
import cn.iocoder.yudao.module.agent.controller.admin.agent.vo.AgentPageReqVO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 智能体实例 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface AiAgentMapper extends BaseMapperX<AiAgentDO> {

    /**
     * 分页查询（支持用户、状态过滤和关键字搜索）
     */
    default PageResult<AiAgentDO> selectPage(AgentPageReqVO reqVO) {
        LambdaQueryWrapperX<AiAgentDO> wrapper = new LambdaQueryWrapperX<AiAgentDO>()
                .eqIfPresent(AiAgentDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AiAgentDO::getStatus, reqVO.getStatus());

        // 关键字搜索：name / description 任一匹配（OR 关系）
        if (reqVO.getSearch() != null && !reqVO.getSearch().isEmpty()) {
            wrapper.and(w -> w
                    .like(AiAgentDO::getName, reqVO.getSearch())
                    .or()
                    .like(AiAgentDO::getDescription, reqVO.getSearch()));
        }

        wrapper.orderByAsc(AiAgentDO::getSortOrder)
               .orderByDesc(AiAgentDO::getCreateTime);

        return selectPage(reqVO, wrapper);
    }

    /**
     * 按用户 + 名称查询智能体（用于重名校验）
     */
    default AiAgentDO selectByUserIdAndName(Long userId, String name) {
        return selectOne(new LambdaQueryWrapperX<AiAgentDO>()
                .eq(AiAgentDO::getUserId, userId)
                .eq(AiAgentDO::getName, name));
    }

    /**
     * 按 QwenPaw agent id 查询
     */
    default AiAgentDO selectByQwenpawAgentId(String qwenpawAgentId) {
        return selectOne(new LambdaQueryWrapperX<AiAgentDO>()
                .eq(AiAgentDO::getQwenpawAgentId, qwenpawAgentId));
    }

    /**
     * 查询某用户的启用智能体列表
     */
    default List<AiAgentDO> selectEnabledByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<AiAgentDO>()
                .eq(AiAgentDO::getUserId, userId)
                .eq(AiAgentDO::getStatus, 1)
                .orderByAsc(AiAgentDO::getSortOrder));
    }

    /**
     * 统计某用户的智能体总数（含停用），用于判断用户是否已有任何智能体
     */
    default Long selectCountByUserId(Long userId) {
        return selectCount(new LambdaQueryWrapperX<AiAgentDO>()
                .eq(AiAgentDO::getUserId, userId));
    }

    /**
     * 查询某用户的默认智能体（is_default = 1）。每人最多 1 条，故用 selectOne。
     */
    default AiAgentDO selectDefaultByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapperX<AiAgentDO>()
                .eq(AiAgentDO::getUserId, userId)
                .eq(AiAgentDO::getIsDefault, 1));
    }

    /**
     * 清除某用户的默认智能体标记（即将 is_default 重置为 NULL）。
     *
     * <p>多租户下 user_id 全局唯一、默认 agent 每人最多 1 条，故重置该用户全部记录为 NULL 是安全的。
     */
    default void clearDefaultByUserId(Long userId) {
        update(null, new LambdaUpdateWrapper<AiAgentDO>()
                .eq(AiAgentDO::getUserId, userId)
                .set(AiAgentDO::getIsDefault, null));
    }

}
