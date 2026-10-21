package cn.iocoder.yudao.module.agent.dal.mysql.agent;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.agent.dal.dataobject.agent.AiAgentDO;
import cn.iocoder.yudao.module.agent.controller.admin.agent.vo.AgentPageReqVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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
     * 查询「当前用户可见」的启用智能体列表（水利智囊 - 智能体页签）：
     * 公开的（visibility=1，同租户所有用户）+ 我自己创建的（user_id=userId）。
     *
     * <p>租户隔离由 BaseMapperX 的 tenant 拦截器自动注入；
     * 排序：公开优先，再按创建时间倒序（新创建靠前）。
     */
    default List<AiAgentDO> selectVisibleList(Long userId) {
        return selectList(new LambdaQueryWrapperX<AiAgentDO>()
                .eq(AiAgentDO::getStatus, 1)
                .and(w -> w.eq(AiAgentDO::getVisibility, 1)
                        .or().eq(AiAgentDO::getUserId, userId))
                .orderByDesc(AiAgentDO::getVisibility)
                .orderByDesc(AiAgentDO::getCreateTime));
    }

    /**
     * 清除某用户的默认标记，【含已逻辑删除的记录】。
     *
     * <p>逻辑删除的行仍占据 uk_user_default 唯一键（deleted 标记不在索引内），若用户删除过
     * 默认智能体，上面带 deleted=0 过滤的清理无法释放唯一键，后续设置/创建默认必撞
     * Duplicate entry。此处用原生 SQL 绕过逻辑删除过滤，把该用户全部 is_default=1 的行
     * （含已删）重置为 NULL（NULL 不参与唯一性约束）。
     */
    @Update("UPDATE ai_agent SET is_default = NULL WHERE user_id = #{userId} AND is_default = 1")
    int clearDefaultIncludeDeleted(@Param("userId") Long userId);

}
