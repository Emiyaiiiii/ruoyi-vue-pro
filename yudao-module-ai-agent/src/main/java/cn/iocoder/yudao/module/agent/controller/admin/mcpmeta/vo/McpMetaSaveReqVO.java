package cn.iocoder.yudao.module.agent.controller.admin.mcpmeta.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

/**
 * MCP 商店创建/更新 Req VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - MCP 商店创建/更新 Request VO")
@Data
public class McpMetaSaveReqVO {

    @Schema(description = "MCP 商店项ID（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "MCP 名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "知识库检索")
    @NotEmpty(message = "MCP 名称不能为空")
    private String name;

    @Schema(description = "MCP 编码（唯一标识）", requiredMode = Schema.RequiredMode.REQUIRED, example = "kb-mcp")
    @NotEmpty(message = "MCP 编码不能为空")
    private String code;

    @Schema(description = "类型: 0=系统级, 1=用户级", example = "0")
    private Integer type;

    @Schema(description = "传输协议: stdio/streamable_http/sse", requiredMode = Schema.RequiredMode.REQUIRED, example = "streamable_http")
    @NotEmpty(message = "传输协议不能为空")
    private String transport;

    @Schema(description = "远程地址（streamable_http/sse 时必填）", example = "https://kb-mcp.internal/kb/v1/mcp")
    private String url;

    @Schema(description = "启动命令（stdio 时必填）", example = "npx -y @modelcontextprotocol/server-filesystem")
    private String command;

    @Schema(description = "启动参数（JSON 数组）", example = "[\"/data\"]")
    private String args;

    @Schema(description = "环境变量（JSON 对象，值可加密）")
    private String env;

    @Schema(description = "请求头（JSON 对象，用于远程鉴权）", example = "{\"Authorization\":\"Bearer xxx\"}")
    private String headers;

    @Schema(description = "工具白名单（JSON 数组，空表示全部）", example = "[\"kb_search\",\"kb_list\"]")
    private String toolsWhitelist;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "状态: 0=停用, 1=启用", example = "1")
    private Integer status;

    @Schema(description = "排序", example = "0")
    private Integer sortOrder;

}
