package cn.iocoder.yudao.module.agent.controller.admin.agentmcp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体-MCP 绑定 Resp VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 智能体-MCP 绑定 Response VO")
@Data
public class AgentMcpRespVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "智能体ID", example = "1")
    private Long agentId;

    @Schema(description = "MCP 商店项ID", example = "1")
    private Long mcpMetaId;

    @Schema(description = "QwenPaw MCP client key", example = "kb-mcp")
    private String clientKey;

    @Schema(description = "用户级配置覆盖（JSON）")
    private String configOverride;

    @Schema(description = "工具白名单（JSON 数组）")
    private String toolsWhitelist;

    @Schema(description = "是否启用: 0=停用, 1=启用", example = "1")
    private Integer enabled;

    @Schema(description = "排序", example = "0")
    private Integer sortOrder;

    @Schema(description = "MCP 商店项名称（冗余展示字段）", example = "知识库 MCP")
    private String mcpName;

    @Schema(description = "MCP 商店项编码（冗余展示字段）", example = "kb-mcp")
    private String mcpCode;

    @Schema(description = "MCP 传输协议（冗余展示字段）", example = "streamable_http")
    private String transport;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
