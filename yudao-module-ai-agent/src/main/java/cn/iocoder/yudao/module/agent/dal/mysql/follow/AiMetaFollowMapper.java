package cn.iocoder.yudao.module.agent.dal.mysql.follow;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.agent.dal.dataobject.follow.AiMetaFollowDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 智囊元数据关注 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface AiMetaFollowMapper extends BaseMapperX<AiMetaFollowDO> {

    /**
     * 检查用户是否已关注某个元数据
     */
    default boolean isFollowing(Long userId, Integer metaType, Long metaId) {
        return selectCount(new LambdaQueryWrapperX<AiMetaFollowDO>()
                .eq(AiMetaFollowDO::getUserId, userId)
                .eq(AiMetaFollowDO::getMetaType, metaType)
                .eq(AiMetaFollowDO::getMetaId, metaId)) > 0;
    }

    /**
     * 获取用户关注的某类型元数据 ID 列表
     */
    default List<Long> selectFollowedIds(Long userId, Integer metaType) {
        return selectList(new LambdaQueryWrapperX<AiMetaFollowDO>()
                .eq(AiMetaFollowDO::getUserId, userId)
                .eq(AiMetaFollowDO::getMetaType, metaType)).stream()
                .map(AiMetaFollowDO::getMetaId)
                .collect(Collectors.toList());
    }

    /**
     * 按元数据批量统计关注数（前端卡片 followers_count / 热榜排序用）
     *
     * @param metaType 元数据类型
     * @param metaIds  元数据 ID 集合
     * @return metaId → 关注数
     */
    default Map<Long, Long> selectFollowCountMap(Integer metaType, Collection<Long> metaIds) {
        if (metaIds == null || metaIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return selectList(new LambdaQueryWrapperX<AiMetaFollowDO>()
                .eq(AiMetaFollowDO::getMetaType, metaType)
                .in(AiMetaFollowDO::getMetaId, metaIds)).stream()
                .collect(Collectors.groupingBy(AiMetaFollowDO::getMetaId, Collectors.counting()));
    }

    /**
     * 取消关注（不存在则无副作用）
     */
    default void deleteBy(Long userId, Integer metaType, Long metaId) {
        delete(new LambdaQueryWrapperX<AiMetaFollowDO>()
                .eq(AiMetaFollowDO::getUserId, userId)
                .eq(AiMetaFollowDO::getMetaType, metaType)
                .eq(AiMetaFollowDO::getMetaId, metaId));
    }

}
