package cn.iocoder.yudao.module.kb.controller.admin.category.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 知识库分类 Response VO")
@Data
@ExcelIgnoreUnannotated
public class CategoryRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "7751")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "王五")
    @ExcelProperty("分类名称")
    private String name;

    @Schema(description = "关联层级配置ID", example = "6750")
    @ExcelProperty("关联层级配置ID")
    private Long kbLevelId;

    @Schema(description = "父分类ID: 0=顶级分类", example = "9814")
    @ExcelProperty("父分类ID: 0=顶级分类")
    private Long parentId;

    @Schema(description = "排序")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "状态: 0=启用, 1=禁用", example = "1")
    @ExcelProperty("状态: 0=启用, 1=禁用")
    private Integer status;

    @Schema(description = "表头配置(JSON): 该分类下文件列表的动态表头")
    private String columnConfig;

    @Schema(description = "是否项目成果库分类: 0=否, 1=是")
    @ExcelProperty("项目成果库分类")
    private Integer isProject;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
