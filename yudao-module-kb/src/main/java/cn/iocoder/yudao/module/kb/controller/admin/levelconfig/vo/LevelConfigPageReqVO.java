package cn.iocoder.yudao.module.kb.controller.admin.levelconfig.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 知识库层级配置分页 Request VO")
@Data
public class LevelConfigPageReqVO extends PageParam {

    @Schema(description = "层级编码")
    private String levelCode;

    @Schema(description = "层级名称", example = "李四")
    private String levelName;

    @Schema(description = "可见规则: 1=按所有者, 2=按归属部门, 3=全员, 5=指定部门列表")
    private Integer visibilityRule;

    @Schema(description = "归属维度: 0=无, 1=用户, 2=部门")
    private Integer ownerDim;

    @Schema(description = "分类可见部门范围: NULL=全员可见, JSON数组[101,102]=仅指定部门")
    private String deptScope;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态: 0=启用, 1=禁用", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}