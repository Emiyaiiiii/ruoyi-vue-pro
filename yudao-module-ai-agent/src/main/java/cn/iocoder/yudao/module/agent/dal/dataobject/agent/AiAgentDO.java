package cn.iocoder.yudao.module.agent.dal.dataobject.agent;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 智能体实例 DO
 *
 * 每行映射到一个 QwenPaw agent（qwenpawAgentId 一对一）
 *
 * @author 吴皓
 */
@TableName("ai_agent")
@KeySequence("ai_agent_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAgentDO extends BaseDO {

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
     * 所属用户ID
     */
    private Long userId;

    /**
     * 智能体名称
     */
    private String name;

    /**
     * 智能体描述
     */
    private String description;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * QwenPaw 侧 agent ID（唯一）
     */
    private String qwenpawAgentId;

    /**
     * QwenPaw workspace 目录
     */
    private String workspaceDir;

    /**
     * 模型供应商
     */
    private String modelProvider;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 是否启用知识库问答工具: 0=关闭, 1=开启
     */
    private Boolean enableKbTool;

    /**
     * 状态: 0=停用, 1=启用
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sortOrder;

}
