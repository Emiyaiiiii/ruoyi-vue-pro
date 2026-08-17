package cn.iocoder.yudao.module.kb.controller.admin.modelconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.*;
import java.util.List;

@Schema(description = "管理后台 - 大模型配置批量操作 Request VO")
@Data
public class ModelConfigBatchReqVO {

    @Schema(description = "配置ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "配置ID列表不能为空")
    private List<Long> ids;

    @Schema(description = "操作类型: activate=激活, deactivate=停用, delete=删除", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "操作类型不能为空")
    private String action;

}
