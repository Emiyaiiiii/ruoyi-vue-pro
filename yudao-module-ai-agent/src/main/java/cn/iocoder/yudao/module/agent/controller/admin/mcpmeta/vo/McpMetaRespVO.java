package cn.iocoder.yudao.module.agent.controller.admin.mcpmeta.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP 商店 Resp VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - MCP 商店 Response VO")
@Data
public class McpMetaRespVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "MCP 名称", example = "知识库检索")
    private String name;

    @Schema(description = "MCP 编码", example = "kb-mcp")
    private String code;

    @Schema(description = "类型: 0=系统级, 1=用户级", example = "0")
    private Integer type;

    @Schema(description = "传输协议: stdio/streamable_http/sse", example = "streamable_http")
    private String transport;

    @Schema(description = "远程地址", example = "https://kb-mcp.internal/kb/v1/mcp")
    private String url;

    @Schema(description = "启动命令", example = "npx -y ...")
    private String command;

    @Schema(description = "启动参数（JSON 数组）")
    private String args;

    @Schema(description = "环境变量（JSON 对象）")
    private String env;

    @Schema(description = "请求头（JSON 对象）")
    private String headers;

    @Schema(description = "工具白名单（JSON 数组）")
    private String toolsWhitelist;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "状态: 0=停用, 1=启用", example = "1")
    private Integer status;

    @Schema(description = "排序", example = "0")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
