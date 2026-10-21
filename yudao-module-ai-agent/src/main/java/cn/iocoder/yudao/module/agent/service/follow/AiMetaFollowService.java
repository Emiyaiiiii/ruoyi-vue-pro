package cn.iocoder.yudao.module.agent.service.follow;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 智囊元数据关注 Service 接口（技能 / MCP / 智能体 统一关注）
 *
 * @author 吴皓
 */
public interface AiMetaFollowService {

    /**
     * 关注（已关注则幂等跳过）
     *
     * @param metaType 元数据类型，见 {@link cn.iocoder.yudao.module.agent.enums.AiMetaTypeEnum}
     * @param metaId   元数据 ID
     * @param userId   关注用户 ID
     * @return 关注记录 ID；已关注时返回 null
     */
    Long follow(Integer metaType, Long metaId, Long userId);

    /**
     * 取消关注（未关注则无副作用）
     */
    void unfollow(Integer metaType, Long metaId, Long userId);

    /**
     * 是否已关注
     */
    boolean isFollowing(Integer metaType, Long metaId, Long userId);

    /**
     * 获取用户关注的某类型元数据 ID 列表
     */
    List<Long> getFollowedIds(Integer metaType, Long userId);

    /**
     * 按元数据批量统计关注数
     *
     * @return metaId → 关注数
     */
    Map<Long, Long> getFollowCountMap(Integer metaType, Collection<Long> metaIds);

}
