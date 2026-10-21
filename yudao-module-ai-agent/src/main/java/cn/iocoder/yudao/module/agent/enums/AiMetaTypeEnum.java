package cn.iocoder.yudao.module.agent.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 智囊元数据类型枚举（关注表区分被关注对象所在表）
 *
 * @author 吴皓
 */
@Getter
@AllArgsConstructor
public enum AiMetaTypeEnum {

    SKILL(1, "技能"),
    MCP(2, "MCP"),
    AGENT(3, "智能体");

    /**
     * 类型
     */
    private final Integer type;

    /**
     * 名称
     */
    private final String name;

}
