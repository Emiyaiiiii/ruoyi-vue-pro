package cn.iocoder.yudao.module.kb.controller.admin.levelconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "管理后台 - 知识库层级配置精简信息 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LevelConfigSimpleVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2769")
    private Long id;

    @Schema(description = "层级编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String levelCode;

    @Schema(description = "层级名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "个人知识库")
    private String levelName;

    @Schema(description = "可见规则: 1=按所有者, 2=按归属部门, 3=全员, 5=指定部门列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer visibilityRule;

    @Schema(description = "归属维度: 0=无, 1=用户, 2=部门")
    private Integer ownerDim;
}