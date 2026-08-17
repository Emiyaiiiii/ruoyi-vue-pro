package cn.iocoder.yudao.module.kb.controller.admin.library.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 知识库分页 Request VO")
@Data
public class LibraryPageReqVO extends PageParam {

    @Schema(description = "知识库名称", example = "张三")
    private String name;

    @Schema(description = "分类ID", example = "26803")
    private Long categoryId;

    @Schema(description = "关联层级配置ID", example = "19733")
    private Long kbLevelId;

    @Schema(description = "所有者ID: 用户或部门, 取决于层级配置的owner_dim", example = "9877")
    private Long ownerId;

    @Schema(description = "描述", example = "随便")
    private String description;

    @Schema(description = "封面图片URL", example = "https://www.iocoder.cn")
    private String coverUrl;

    @Schema(description = "文档数量", example = "1563")
    private Integer docCount;

    @Schema(description = "状态: 0=启用, 1=禁用", example = "1")
    private Integer status;

    @Schema(description = "是否公开到广场: 0=否, 1=是", example = "1")
    private Integer isPublic;

    @Schema(description = "是否项目成果库: 0=否, 1=是", example = "0")
    private Integer isProject;

    @Schema(description = "排序字段：name / docCount / createTime", example = "createTime")
    private String sortField;

    @Schema(description = "排序方式：ascending=升序, descending=降序", example = "descending")
    private String sortOrder;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}