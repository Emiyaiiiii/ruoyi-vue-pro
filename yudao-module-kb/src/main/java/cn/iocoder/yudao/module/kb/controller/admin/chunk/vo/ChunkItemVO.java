package cn.iocoder.yudao.module.kb.controller.admin.chunk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 切片信息 VO（#7 chunk 编辑，来自 python-vector GET /documents/{id}/chunks）
 */
@Schema(description = "切片信息 VO")
@Data
public class ChunkItemVO {

    @Schema(description = "切片标识(复合: {document_id}_{chunk_index})", requiredMode = Schema.RequiredMode.REQUIRED)
    private String chunkId;

    @Schema(description = "文档ID")
    private Long documentId;

    @Schema(description = "切片序号")
    private Integer chunkIndex;

    @Schema(description = "切片内容")
    private String content;

    @Schema(description = "内容 sha256")
    private String contentHash;

    @Schema(description = "切片角色: text/parent(仅ES上下文块)")
    private String chunkRole;

    @Schema(description = "是否父块")
    private Boolean isParent;

    @Schema(description = "是否子块")
    private Boolean isChild;

    @Schema(description = "父块序号(父子切块时)")
    private Integer parentChunkIndex;

    @Schema(description = "是否图片块")
    private Boolean isImage;

    @Schema(description = "token 数")
    private Integer tokenCount;

    @Schema(description = "字符数")
    private Integer charCount;

    @Schema(description = "标签")
    private List<String> tags;

    @Schema(description = "关键词")
    private List<String> keyPhrases;

    @Schema(description = "召回权重")
    private Double recallWeight;

    @Schema(description = "是否启用")
    private Boolean isActive;

    @Schema(description = "父文件节点ID")
    private Long parentFilenodeId;

    @Schema(description = "文档标题")
    private String documentTitle;

    @Schema(description = "文档类型")
    private String documentType;

    @Schema(description = "标题面包屑(ContextHeader)")
    private String contextHeader;

    @Schema(description = "LLM 生成的问题列表")
    private List<String> questions;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "更新时间")
    private String updatedAt;

    @Schema(description = "起始页码（1 基，PDF 才有）", example = "5")
    private Integer pageStart;

    @Schema(description = "结束页码（1 基）", example = "5")
    private Integer pageEnd;

    @Schema(description = "页内高亮框：[{p:页码, b:[x0,y0,x1,y1]}]，坐标已归一化到 0~1")
    private List<Map<String, Object>> bboxes;

    @Schema(description = "切片在原文中的字符起点")
    private Integer charStart;

    @Schema(description = "切片在原文中的字符终点")
    private Integer charEnd;

    @Schema(description = "页码来源：mineru/exact/aligned（对齐精度）", example = "mineru")
    private String pageSource;

    /** 当前版本号（0=从未编辑）；由 Java 侧按修订表补填，前端乐观锁用 */
    private Integer revision;
}
