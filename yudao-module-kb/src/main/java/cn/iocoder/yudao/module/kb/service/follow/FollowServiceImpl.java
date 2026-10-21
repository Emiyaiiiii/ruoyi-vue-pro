package cn.iocoder.yudao.module.kb.service.follow;

import cn.iocoder.yudao.module.kb.dal.dataobject.follow.FollowDO;
import cn.iocoder.yudao.module.kb.dal.mysql.follow.FollowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
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
@Slf4j
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;

    @Override
    public Long follow(Long kbId, Long userId) {
        // 已关注则跳过
        if (isFollowing(kbId, userId)) {
            return null;
        }
        // 先尝试复活被逻辑删除的历史记录：
        // kb_follow 取消关注是软删（deleted=1），该行仍占用 uk_user_kb 唯一键，
        // 而 isFollowing 会自动过滤 deleted=0，导致误判“未关注”。
        // 先修复历史软删行，可避免依赖数据库异常翻译。
        if (followMapper.revive(userId, kbId) > 0) {
            return null;
        }
        FollowDO follow = FollowDO.builder()
                .kbId(kbId)
                .userId(userId)
                .build();
        try {
            followMapper.insert(follow);
            return follow.getId();
        } catch (DuplicateKeyException e) {
            // 并发/重复提交：两个请求同时通过了 isFollowing 检查，后插入者撞唯一键。
            // 此时记录已存在（deleted=0），视为“已关注”幂等返回。
            if (followMapper.revive(userId, kbId) == 0) {
                log.warn("[follow][用户({}) 关注知识库({}) 命中 uk_user_kb，但未找到可复活的记录，可能存在租户或历史数据不一致]",
                        userId, kbId);
            }
            return null;
        }
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
