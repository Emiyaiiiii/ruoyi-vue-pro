package cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 向量处理任务 Response VO")
@Data
public class VectorTaskRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "知识库ID")
    private Long kbId;

    @Schema(description = "文件下载地址")
    private String fileUrl;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "状态：0-待提交 1-处理中 2-已完成 3-失败 4-提交失败 5-超时")
    private Integer status;

    @Schema(description = "进度（0-100）")
    private Integer progress;

    @Schema(description = "当前处理步骤")
    private String currentStep;

    @Schema(description = "分块数量")
    private Integer chunkCount;

    @Schema(description = "Celery任务ID")
    private String celeryTaskId;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
