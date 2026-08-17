package cn.iocoder.yudao.module.agent.dal.dataobject.agentmcp;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 智能体-MCP 绑定 DO
 *
 * @author 吴皓
 */
@TableName("ai_agent_mcp")
@KeySequence("ai_agent_mcp_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAgentMcpDO extends BaseDO {

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
     * 智能体ID
     */
    private Long agentId;

    /**
     * MCP 商店项ID
     */
    private Long mcpMetaId;

    /**
     * QwenPaw MCP client key
     */
    private String clientKey;

    /**
     * 用户级配置覆盖（JSON，如 url/headers/command）
     */
    private String configOverride;

    /**
     * 工具白名单（JSON 数组）
     */
    private String toolsWhitelist;

    /**
     * 是否启用: 0=停用, 1=启用
     */
    private Integer enabled;

    /**
     * 排序
     */
    private Integer sortOrder;

}
