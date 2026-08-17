package cn.iocoder.yudao.module.agent.controller.admin.skillmeta.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

/**
 * 技能商店 创建/更新 Request VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 技能商店创建/更新 Request VO")
@Data
public class SkillMetaSaveReqVO {

    @Schema(description = "主键ID（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "QwenPaw 技能池中的技能名称（唯一标识）", requiredMode = Schema.RequiredMode.REQUIRED, example = "pdf")
    @NotEmpty(message = "技能名称不能为空")
    private String skillName;

    @Schema(description = "显示名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "PDF 解析")
    @NotEmpty(message = "显示名称不能为空")
    private String displayName;

    @Schema(description = "描述", example = "解析 PDF 文件内容")
    private String description;

    @Schema(description = "图标（emoji 或 URL）", example = "📄")
    private String icon;

    @Schema(description = "可见性: 0=个人, 1=公开", example = "1")
    private Integer visibility;

    @Schema(description = "标签（JSON 数组字符串）", example = "[\"pdf\",\"文档\"]")
    private String tags;

    @Schema(description = "状态: 0=停用, 1=启用", example = "1")
    private Integer status;

}
