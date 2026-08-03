package cn.iocoder.yudao.module.kb.dal.mysql.follow;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.kb.dal.dataobject.follow.FollowDO;
import org.apache.ibatis.annotations.Mapper;

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
}
