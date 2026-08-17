package cn.iocoder.yudao.module.agent.controller.admin.agentmcp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 智能体-MCP 绑定创建/更新 Req VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 智能体-MCP 绑定创建/更新 Request VO")
@Data
public class AgentMcpSaveReqVO {

    @Schema(description = "绑定ID（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "智能体ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "智能体ID不能为空")
    private Long agentId;

    @Schema(description = "MCP 商店项ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "MCP 商店项ID不能为空")
    private Long mcpMetaId;

    @Schema(description = "QwenPaw MCP client key（为空则取商店项编码）", example = "kb-mcp")
    private String clientKey;

    @Schema(description = "用户级配置覆盖（JSON，如 url/headers/command）", example = "{\"url\":\"https://kb-mcp.internal/kb/v1/mcp\"}")
    private String configOverride;

    @Schema(description = "工具白名单（JSON 数组）", example = "[\"kb_search\"]")
    private String toolsWhitelist;

    @Schema(description = "是否启用: 0=停用, 1=启用", example = "1")
    private Integer enabled;

    @Schema(description = "排序", example = "0")
    private Integer sortOrder;

}
