package cn.iocoder.yudao.module.kb.service.follow;

import cn.iocoder.yudao.module.kb.dal.dataobject.follow.FollowDO;
import cn.iocoder.yudao.module.kb.dal.mysql.follow.FollowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 知识库关注 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;

    @Override
    public Long follow(Long kbId, Long userId) {
        // 已关注则跳过
        if (isFollowing(kbId, userId)) {
            return null;
        }
        FollowDO follow = FollowDO.builder()
                .kbId(kbId)
                .userId(userId)
                .build();
        followMapper.insert(follow);
        return follow.getId();
    }

    @Override
    public void unfollow(Long kbId, Long userId) {
        followMapper.delete(new LambdaQueryWrapper<FollowDO>()
                .eq(FollowDO::getKbId, kbId)
                .eq(FollowDO::getUserId, userId));
    }

    @Override
    public boolean isFollowing(Long kbId, Long userId) {
        return followMapper.isFollowing(userId, kbId);
    }

    @Override
    public List<Long> getFollowedKbIds(Long userId) {
        return followMapper.selectFollowedKbIds(userId);
    }
}
