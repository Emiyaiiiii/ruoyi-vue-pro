package cn.iocoder.yudao.module.kb.controller.admin.library.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 知识库 Response VO")
@Data
@ExcelIgnoreUnannotated
public class LibraryRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "18531")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "知识库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @ExcelProperty("知识库名称")
    private String name;

    @Schema(description = "分类ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "26803")
    @ExcelProperty("分类ID")
    private Long categoryId;

    @Schema(description = "关联层级配置ID", example = "19733")
    @ExcelProperty("关联层级配置ID")
    private Long kbLevelId;

    @Schema(description = "所有者ID: 用户或部门, 取决于层级配置的owner_dim", example = "9877")
    @ExcelProperty("所有者ID: 用户或部门, 取决于层级配置的owner_dim")
    private Long ownerId;

    @Schema(description = "描述", example = "随便")
    @ExcelProperty("描述")
    private String description;

    @Schema(description = "封面图片URL", example = "https://www.iocoder.cn")
    @ExcelProperty("封面图片URL")
    private String coverUrl;

    @Schema(description = "文档数量", example = "1563")
    @ExcelProperty("文档数量")
    private Integer docCount;

    @Schema(description = "状态: 0=启用, 1=禁用", example = "1")
    @ExcelProperty("状态: 0=启用, 1=禁用")
    private Integer status;

    @Schema(description = "是否公开到广场: 0=否, 1=是", example = "1")
    @ExcelProperty("是否公开到广场")
    private Integer isPublic;

    @Schema(description = "是否项目成果库: 0=否, 1=是", example = "0")
    @ExcelProperty("是否项目成果库")
    private Integer isProject;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "共享部门ID列表")
    private List<Long> shareDeptIds;

}