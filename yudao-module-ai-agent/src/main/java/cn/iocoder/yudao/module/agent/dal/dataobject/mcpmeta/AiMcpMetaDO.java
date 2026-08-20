package cn.iocoder.yudao.module.agent.dal.dataobject.mcpmeta;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 系统级 MCP 商店 DO
 *
 * @author 吴皓
 */
@TableName("ai_mcp_meta")
@KeySequence("ai_mcp_meta_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMcpMetaDO extends BaseDO {

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
     * MCP 名称
     */
    private String name;

    /**
     * MCP 编码（唯一标识）
     */
    private String code;

    /**
     * 类型: 0=系统级, 1=用户级
     */
    private Integer type;

    /**
     * 传输协议: stdio / streamable_http / sse
     */
    private String transport;

    /**
     * 远程地址（streamable_http/sse 时必填）
     */
    private String url;

    /**
     * 启动命令（stdio 时必填）
     */
    private String command;

    /**
     * 启动参数（JSON 数组）
     */
    private String args;

    /**
     * 环境变量（JSON 对象，值可加密）
     */
    private String env;

    /**
     * 请求头（JSON 对象，用于远程鉴权）
     */
    private String headers;

    /**
     * 工具白名单（JSON 数组，空表示全部）
     */
    private String toolsWhitelist;

    /**
     * 描述
     */
    private String description;

    /**
     * 图标
     */
    private String icon;

    /**
     * 状态: 0=停用, 1=启用
     */
    private Integer status;

    /**
     * 归属用户ID（type=1 个人 MCP 时，仅创建者可见）
     */
    private Long ownerUserId;

    /**
     * 排序
     */
    private Integer sortOrder;

}
