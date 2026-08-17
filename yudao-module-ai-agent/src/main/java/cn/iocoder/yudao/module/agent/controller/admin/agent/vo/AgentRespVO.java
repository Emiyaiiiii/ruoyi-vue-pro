package cn.iocoder.yudao.module.agent.controller.admin.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体 Resp VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 智能体 Response VO")
@Data
public class AgentRespVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "所属用户ID", example = "1024")
    private Long userId;

    @Schema(description = "智能体名称", example = "知识库助手")
    private String name;

    @Schema(description = "智能体描述")
    private String description;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "QwenPaw 侧 agent ID")
    private String qwenpawAgentId;

    @Schema(description = "QwenPaw workspace 目录")
    private String workspaceDir;

    @Schema(description = "模型供应商", example = "qwen")
    private String modelProvider;

    @Schema(description = "模型名称", example = "qwen3-coder-flash")
    private String modelName;

    @Schema(description = "系统提示词")
    private String systemPrompt;

    @Schema(description = "是否启用知识库问答工具: 0=关闭, 1=开启", example = "true")
    private Boolean enableKbTool;

    @Schema(description = "状态: 0=停用, 1=启用", example = "1")
    private Integer status;

    @Schema(description = "排序", example = "0")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
