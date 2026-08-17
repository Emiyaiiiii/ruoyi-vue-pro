package cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Schema(description = "管理后台 - 向量处理任务状态回调 Request VO（内部使用）")
@Data
public class VectorTaskCallbackReqVO {

    @Schema(description = "任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "任务ID不能为空")
    private String taskId;

    @Schema(description = "状态：PROCESSING/COMPLETED/FAILED")
    private String status;

    @Schema(description = "进度（0-100）")
    private Integer progress;

    @Schema(description = "当前步骤")
    private String step;

    @Schema(description = "分块数量")
    private Integer chunkCount;

    @Schema(description = "错误信息")
    private String errorMsg;
}
