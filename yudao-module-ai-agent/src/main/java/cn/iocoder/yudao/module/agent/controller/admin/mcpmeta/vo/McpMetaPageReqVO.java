package cn.iocoder.yudao.module.agent.controller.admin.mcpmeta.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * MCP 商店分页 Req VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - MCP 商店分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class McpMetaPageReqVO extends PageParam {

    @Schema(description = "类型: 0=系统级, 1=用户级", example = "0")
    private Integer type;

    @Schema(description = "传输协议: stdio/streamable_http/sse", example = "streamable_http")
    private String transport;

    @Schema(description = "状态: 0=停用, 1=启用", example = "1")
    private Integer status;

    @Schema(description = "关键字：名称/编码/描述", example = "知识库")
    private String search;

}
