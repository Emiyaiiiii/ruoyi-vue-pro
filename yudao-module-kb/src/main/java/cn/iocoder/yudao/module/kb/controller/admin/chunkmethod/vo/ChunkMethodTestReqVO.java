package cn.iocoder.yudao.module.kb.controller.admin.chunkmethod.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 切片方法测试 Request VO")
@Data
public class ChunkMethodTestReqVO {

    @Schema(description = "切片方法ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "切片方法ID不能为空")
    private Long id;

    @Schema(description = "测试文本", requiredMode = Schema.RequiredMode.REQUIRED, example = "这是一段测试文本，用于测试切片方法的效果。")
    @NotEmpty(message = "测试文本不能为空")
    private String testText;

}
