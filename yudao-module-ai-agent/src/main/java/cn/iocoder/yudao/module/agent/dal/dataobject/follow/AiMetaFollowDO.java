package cn.iocoder.yudao.module.agent.dal.dataobject.follow;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 智囊元数据关注 DO（技能 / MCP / 智能体 统一关注表）
 *
 * @author 吴皓
 */
@TableName("ai_meta_follow")
@KeySequence("ai_meta_follow_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMetaFollowDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 租户编号
     */
    private Long tenantId;

    /**
     * 被关注对象类型：见 {@link cn.iocoder.yudao.module.agent.enums.AiMetaTypeEnum}
     */
    private Integer metaType;

    /**
     * 被关注对象ID（ai_skill_meta.id / ai_mcp_meta.id / ai_agent.id）
     */
    private Long metaId;

    /**
     * 关注用户ID
     */
    private Long userId;

}
