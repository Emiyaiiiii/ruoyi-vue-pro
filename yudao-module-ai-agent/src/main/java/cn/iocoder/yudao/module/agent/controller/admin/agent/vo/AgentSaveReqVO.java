package cn.iocoder.yudao.module.agent.controller.admin.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 智能体创建/更新 Req VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 智能体创建/更新 Request VO")
@Data
public class AgentSaveReqVO {

    @Schema(description = "智能体ID（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "所属用户ID（为空则取当前登录用户）", example = "1024")
    private Long userId;

    @Schema(description = "智能体名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "知识库助手")
    @NotEmpty(message = "智能体名称不能为空")
    private String name;

    @Schema(description = "智能体描述", example = "基于企业内部知识库的问答助手")
    private String description;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "模型供应商", example = "qwen")
    private String modelProvider;

    @Schema(description = "模型名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "qwen3-coder-flash")
    @NotEmpty(message = "模型名称不能为空")
    private String modelName;

    @Schema(description = "系统提示词")
    private String systemPrompt;

    @Schema(description = "是否启用知识库问答工具: 0=关闭, 1=开启", example = "true")
    private Boolean enableKbTool;

    @Schema(description = "状态: 0=停用, 1=启用", example = "1")
    private Integer status;

    @Schema(description = "排序", example = "0")
    private Integer sortOrder;

    @Schema(description = "初始技能（仅创建时生效，从 QwenPaw 技能池按 name 安装）", example = "[\"QA_source_index\",\"pdf\"]")
    private List<String> initialSkills;

}
