package cn.iocoder.yudao.module.kb.controller.admin.modelconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 大模型配置测试 Request VO")
@Data
public class ModelConfigTestReqVO {

    @Schema(description = "配置ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "配置ID不能为空")
    private Long id;

    @Schema(description = "测试消息", example = "你好，请回复测试成功")
    private String testMessage;

    @Schema(description = "温度参数", example = "0.7")
    private Double temperature;

    @Schema(description = "最大Token数", example = "100")
    private Integer maxTokens;

}
