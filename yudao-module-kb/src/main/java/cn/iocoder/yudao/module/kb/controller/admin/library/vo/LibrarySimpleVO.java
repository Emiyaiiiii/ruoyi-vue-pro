package cn.iocoder.yudao.module.kb.controller.admin.library.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "管理后台 - 知识库精简信息 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LibrarySimpleVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "18531")
    private Long id;

    @Schema(description = "知识库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "项目知识库")
    private String name;

    @Schema(description = "是否项目成果库: 0=否, 1=是", example = "0")
    private Integer isProject;

    @Schema(description = "所属分类ID（用于按分类树分组展示）", example = "1024")
    private Long categoryId;
}