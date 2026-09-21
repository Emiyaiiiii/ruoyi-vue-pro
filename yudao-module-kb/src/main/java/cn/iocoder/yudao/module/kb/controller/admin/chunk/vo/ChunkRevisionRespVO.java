package cn.iocoder.yudao.module.kb.controller.admin.chunk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 切片修订记录 VO（#7 修订历史）
 */
@Schema(description = "切片修订记录 VO")
@Data
public class ChunkRevisionRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "切片标识(复合: {document_id}_{chunk_index})")
    private String chunkId;

    @Schema(description = "版本号")
    private Integer revision;

    @Schema(description = "该版本快照内容(变更前)")
    private String content;

    @Schema(description = "内容 sha256")
    private String contentHash;

    @Schema(description = "编辑者用户ID")
    private Long editorId;

    @Schema(description = "编辑者昵称")
    private String editorName;

    @Schema(description = "变更类型: edit/revert")
    private String changeType;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
