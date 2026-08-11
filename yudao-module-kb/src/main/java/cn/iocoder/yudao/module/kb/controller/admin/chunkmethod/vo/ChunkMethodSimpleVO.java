package cn.iocoder.yudao.module.kb.controller.admin.chunkmethod.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "管理后台 - 切片方法精简 VO（用于下拉选择等）")
@Data
public class ChunkMethodSimpleVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "方法名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "固定大小分块器")
    private String name;

    @Schema(description = "方法代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "fixed_size")
    private String code;

    @Schema(description = "方法类型", example = "fixed_size")
    private String methodType;

}
