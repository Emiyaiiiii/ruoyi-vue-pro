package cn.iocoder.yudao.module.agent.controller.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

/**
 * 添加模型请求 VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 添加模型请求")
@Data
public class AddModelReqVO {

    @Schema(description = "Provider ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Provider ID 不能为空")
    private String providerId;

    @Schema(description = "模型 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "模型 ID 不能为空")
    private String modelId;

    @Schema(description = "模型名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "模型名称不能为空")
    private String name;

    @Schema(description = "是否支持多模态")
    private Boolean supportsMultimodal;

    @Schema(description = "是否支持图片")
    private Boolean supportsImage;

    @Schema(description = "是否支持视频")
    private Boolean supportsVideo;
}
