package cn.iocoder.yudao.module.kb.controller.admin.modelconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 大模型配置复制 Request VO")
@Data
public class ModelConfigCopyReqVO {

    @Schema(description = "源配置ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "源配置ID不能为空")
    private Long id;

    @Schema(description = "新配置名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "GPT-4 - 副本")
    @NotEmpty(message = "新配置名称不能为空")
    private String newName;

    @Schema(description = "新配置UID", requiredMode = Schema.RequiredMode.REQUIRED, example = "gpt-4-copy")
    @NotEmpty(message = "新配置UID不能为空")
    private String newUid;

    @Schema(description = "初始激活状态", example = "0")
    private Integer isActive;

}
