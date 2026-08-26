package cn.iocoder.yudao.module.kb.controller.admin.chunkmethod.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.List;

@Schema(description = "管理后台 - 切片方法测试 Response VO")
@Data
public class ChunkMethodTestRespVO {

    @Schema(description = "方法ID", example = "1")
    private Long methodId;

    @Schema(description = "方法名称", example = "固定大小分块器")
    private String methodName;

    @Schema(description = "测试文本长度(字符)", example = "500")
    private Integer testTextLength;

    @Schema(description = "分片数量", example = "3")
    private Integer chunkCount;

    @Schema(description = "处理时间(秒)", example = "0.125")
    private Double processingTimeSeconds;

    @Schema(description = "处理速度(字符/秒)", example = "4000.0")
    private Double processingSpeedCharsPerSecond;

    @Schema(description = "平均分片大小(字符)", example = "450")
    private Double avgChunkSize;

    @Schema(description = "实际执行引擎: python=真实服务, local=本地模拟", example = "python")
    private String engine = "local";

    @Schema(description = "分片预览(前3个)", example = "[{\"text\":\"分片内容1\"},{\"text\":\"分片内容2\"}]")
    private List<ChunkPreview> chunksPreview;

    @Data
    @Schema(description = "分片预览")
    public static class ChunkPreview {
        @Schema(description = "分片文本内容")
        private String text;

        @Schema(description = "分片大小(字符)")
        private Integer size;
    }

}
