package cn.iocoder.yudao.module.agent.dal.dataobject.skillmeta;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 技能商店 DO（QwenPaw 技能池元数据影子表）
 *
 * <p>每条记录对应 QwenPaw 技能池中的一个技能，额外维护 icon、可见性、归属用户等 Java 侧字段。
 *
 * @author 吴皓
 */
@TableName("ai_skill_meta")
@KeySequence("ai_skill_meta_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSkillMetaDO extends BaseDO {

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
     * QwenPaw 技能池中的技能名称（唯一标识，与 QwenPaw pool manifest key 一致）
     */
    private String skillName;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 描述
     */
    private String description;

    /**
     * 图标（emoji 或 URL）
     */
    private String icon;

    /**
     * 来源: builtin / customized（同步自 QwenPaw）
     */
    private String source;

    /**
     * 版本号（同步自 QwenPaw）
     */
    private String version;

    /**
     * 可见性: 0=个人(仅创建者可见), 1=公开(所有用户可见)
     */
    private Integer visibility;

    /**
     * 创建者用户ID
     */
    private Long ownerUserId;

    /**
     * 标签（JSON 数组字符串）
     */
    private String tags;

    /**
     * 状态: 0=停用, 1=启用
     */
    private Integer status;

}
