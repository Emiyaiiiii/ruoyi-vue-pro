package cn.iocoder.yudao.module.kb.service.follow;

import cn.iocoder.yudao.module.kb.dal.dataobject.follow.FollowDO;

import java.util.List;

/**
 * 知识库关注 Service 接口
 *
 * @author 吴皓
 */
public interface FollowService {

    /**
     * 关注知识库
     */
    Long follow(Long kbId, Long userId);

    /**
     * 取消关注
     */
    void unfollow(Long kbId, Long userId);

    /**
     * 是否已关注
     */
    boolean isFollowing(Long kbId, Long userId);

    /**
     * 获取用户关注的知识库ID列表
     */
    List<Long> getFollowedKbIds(Long userId);
}
