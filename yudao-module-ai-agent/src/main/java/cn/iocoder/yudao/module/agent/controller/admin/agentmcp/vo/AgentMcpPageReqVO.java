package cn.iocoder.yudao.module.agent.controller.admin.agentmcp.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 智能体-MCP 绑定分页 Req VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 智能体-MCP 绑定分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AgentMcpPageReqVO extends PageParam {

    @Schema(description = "智能体ID", example = "1")
    private Long agentId;

    @Schema(description = "MCP 商店项ID", example = "1")
    private Long mcpMetaId;

    @Schema(description = "是否启用: 0=停用, 1=启用", example = "1")
    private Integer enabled;

}
