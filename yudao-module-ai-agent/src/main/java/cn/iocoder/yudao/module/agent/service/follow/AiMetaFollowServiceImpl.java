package cn.iocoder.yudao.module.agent.service.follow;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import cn.iocoder.yudao.module.agent.dal.dataobject.follow.AiMetaFollowDO;
import cn.iocoder.yudao.module.agent.dal.mysql.follow.AiMetaFollowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * 智囊元数据关注 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
@RequiredArgsConstructor
public class AiMetaFollowServiceImpl implements AiMetaFollowService {

    private final AiMetaFollowMapper followMapper;

    @Override
    public Long follow(Integer metaType, Long metaId, Long userId) {
        // 已关注则跳过，保证重复点击幂等
        if (followMapper.isFollowing(userId, metaType, metaId)) {
            return null;
        }
        AiMetaFollowDO follow = AiMetaFollowDO.builder()
                .metaType(metaType)
                .metaId(metaId)
                .userId(userId)
                .build();
        followMapper.insert(follow);
        return follow.getId();
    }

    @Override
    public void unfollow(Integer metaType, Long metaId, Long userId) {
        followMapper.deleteBy(userId, metaType, metaId);
    }

    @Override
    public boolean isFollowing(Integer metaType, Long metaId, Long userId) {
        return followMapper.isFollowing(userId, metaType, metaId);
    }

    @Override
    public List<Long> getFollowedIds(Integer metaType, Long userId) {
        return followMapper.selectFollowedIds(userId, metaType);
    }

    @Override
    public Map<Long, Long> getFollowCountMap(Integer metaType, Collection<Long> metaIds) {
        return followMapper.selectFollowCountMap(metaType, metaIds);
    }

}
