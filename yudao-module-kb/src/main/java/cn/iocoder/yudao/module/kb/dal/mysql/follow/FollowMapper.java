package cn.iocoder.yudao.module.kb.dal.mysql.follow;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.kb.dal.dataobject.follow.FollowDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FollowMapper extends BaseMapperX<FollowDO> {

    default PageResult<FollowDO> selectPage(Long userId, Integer pageNo, Integer pageSize) {
        return selectPage(new cn.iocoder.yudao.framework.common.pojo.PageParam() {{
            setPageNo(pageNo);
            setPageSize(pageSize);
        }}, new LambdaQueryWrapperX<FollowDO>()
                .eq(FollowDO::getUserId, userId)
                .orderByDesc(FollowDO::getId));
    }

    /**
     * 复活被逻辑删除的关注记录（重新关注时避免撞 uk_user_kb 唯一键）。
     *
     * <p>kb_follow 为逻辑删除：取消关注后记录 deleted=1 仍占用 (user_id, kb_id) 唯一键，
     * 而 isFollowing 会过滤 deleted=0 导致误判"未关注"，再次 insert 即 DuplicateKey。
     * 此处直接置 deleted=0（跨过逻辑删除条件），幂等。租户条件由租户插件自动追加。
     */
    @Update("UPDATE kb_follow SET deleted = 0, update_time = NOW() WHERE user_id = #{userId} AND kb_id = #{kbId}")
    int revive(@Param("userId") Long userId, @Param("kbId") Long kbId);

    /**
     * 检查用户是否已关注某个知识库
     */
    default boolean isFollowing(Long userId, Long kbId) {
        return selectCount(new LambdaQueryWrapperX<FollowDO>()
                .eq(FollowDO::getUserId, userId)
                .eq(FollowDO::getKbId, kbId)) > 0;
    }

    /**
     * 获取用户关注的知识库ID列表
     */
    default java.util.List<Long> selectFollowedKbIds(Long userId) {
        return selectList(new LambdaQueryWrapperX<FollowDO>()
                .eq(FollowDO::getUserId, userId)).stream()
                .map(FollowDO::getKbId)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 按知识库批量统计关注数（前端卡片 followers_count 用）
     *
     * @param kbIds 知识库ID集合
     * @return kbId → 关注数
     */
    default java.util.Map<Long, Long> selectFollowCountMap(java.util.Collection<Long> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return selectList(new LambdaQueryWrapperX<FollowDO>()
                .in(FollowDO::getKbId, kbIds)).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        FollowDO::getKbId, java.util.stream.Collectors.counting()));
    }
}
