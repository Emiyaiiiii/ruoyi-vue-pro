package cn.iocoder.yudao.module.kb.controller.admin.levelconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 知识库层级配置新增/修改 Request VO")
@Data
public class LevelConfigSaveReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2769")
    private Long id;

    @Schema(description = "层级编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "层级编码不能为空")
    private String levelCode;

    @Schema(description = "层级名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotEmpty(message = "层级名称不能为空")
    private String levelName;

    @Schema(description = "可见规则: 1=按所有者, 2=按归属部门, 3=全员, 5=指定部门列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "可见规则: 1=按所有者, 2=按归属部门, 3=全员, 5=指定部门列表不能为空")
    private Integer visibilityRule;

    @Schema(description = "归属维度: 0=无, 1=用户, 2=部门")
    private Integer ownerDim;

    @Schema(description = "分类可见部门范围: NULL=全员可见, JSON数组[101,102]=仅指定部门")
    private String deptScope;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态: 0=启用, 1=禁用", example = "1")
    private Integer status;

}