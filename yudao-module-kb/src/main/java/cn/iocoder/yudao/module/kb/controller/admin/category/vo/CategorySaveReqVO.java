package cn.iocoder.yudao.module.kb.controller.admin.category.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 知识库分类新增/修改 Request VO")
@Data
public class CategorySaveReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "7751")
    private Long id;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "王五")
    @NotEmpty(message = "分类名称不能为空")
    private String name;

    @Schema(description = "关联层级配置ID", example = "6750")
    private Long kbLevelId;

    @Schema(description = "父分类ID: 0=顶级分类", example = "9814")
    private Long parentId;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态: 0=启用, 1=禁用", example = "1")
    private Integer status;

    @Schema(description = "表头配置(JSON): 该分类下文件列表的动态表头")
    private String columnConfig;

    @Schema(description = "是否项目成果库分类: 0=否, 1=是")
    private Integer isProject;

}