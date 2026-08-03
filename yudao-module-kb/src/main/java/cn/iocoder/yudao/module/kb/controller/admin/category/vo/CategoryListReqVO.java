package cn.iocoder.yudao.module.kb.controller.admin.category.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 知识库分类列表 Request VO")
@Data
public class CategoryListReqVO {

    @Schema(description = "分类名称", example = "王五")
    private String name;

    @Schema(description = "关联层级配置ID", example = "6750")
    private Long kbLevelId;

    @Schema(description = "父分类ID: 0=顶级分类", example = "9814")
    private Long parentId;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态: 0=启用, 1=禁用", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}