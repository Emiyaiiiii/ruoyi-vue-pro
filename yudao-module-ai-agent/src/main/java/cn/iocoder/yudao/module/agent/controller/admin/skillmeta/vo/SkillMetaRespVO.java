package cn.iocoder.yudao.module.agent.controller.admin.skillmeta.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 技能商店 Response VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 技能商店 Response VO")
@Data
public class SkillMetaRespVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "QwenPaw 技能名称", example = "pdf")
    private String skillName;

    @Schema(description = "显示名称", example = "PDF 解析")
    private String displayName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "来源: builtin/customized", example = "customized")
    private String source;

    @Schema(description = "版本号", example = "1.0.0")
    private String version;

    @Schema(description = "可见性: 0=个人, 1=公开", example = "1")
    private Integer visibility;

    @Schema(description = "创建者用户ID", example = "225")
    private Long ownerUserId;

    @Schema(description = "标签（JSON 数组字符串）")
    private String tags;

    @Schema(description = "状态: 0=停用, 1=启用", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
