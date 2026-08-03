package cn.iocoder.yudao.module.kb.controller.admin.levelconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 知识库层级配置 Response VO")
@Data
@ExcelIgnoreUnannotated
public class LevelConfigRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2769")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "层级编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("层级编码")
    private String levelCode;

    @Schema(description = "层级名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @ExcelProperty("层级名称")
    private String levelName;

    @Schema(description = "可见规则: 1=按所有者, 2=按归属部门, 3=全员, 5=指定部门列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("可见规则: 1=按所有者, 2=按归属部门, 3=全员, 5=指定部门列表")
    private Integer visibilityRule;

    @Schema(description = "归属维度: 0=无, 1=用户, 2=部门")
    @ExcelProperty("归属维度: 0=无, 1=用户, 2=部门")
    private Integer ownerDim;

    @Schema(description = "分类可见部门范围: NULL=全员可见, JSON数组[101,102]=仅指定部门")
    @ExcelProperty("分类可见部门范围: NULL=全员可见, JSON数组[101,102]=仅指定部门")
    private String deptScope;

    @Schema(description = "排序")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "状态: 0=启用, 1=禁用", example = "1")
    @ExcelProperty("状态: 0=启用, 1=禁用")
    private Integer status;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}