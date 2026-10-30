package cn.iocoder.yudao.module.kb.controller.admin.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库文档预览信息 Response VO
 *
 * <p>供前端「点击引用来源 → 打开原文并定位」使用：
 * <ul>
 *   <li>{@code url}：短时效预签名地址，可直接在新窗口 / iframe / Office 在线预览中打开</li>
 *   <li>{@code fileConfigId + filePath}：同源字节流地址的组成参数（
 *       {@code /infra/file/{configId}/get/{path}}），供 pdf.js 同源拉取字节，
 *       避免对象存储桶跨域（CORS）限制</li>
 *   <li>{@code pageLocatable}：是否支持页级定位（pdf 才有页码/坐标）</li>
 * </ul>
 */
@Schema(description = "管理后台 - 知识库文档预览信息 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentPreviewRespVO {

    @Schema(description = "文档编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "文件名", example = "技术规范.pdf")
    private String fileName;

    @Schema(description = "文件类型（小写扩展名）", example = "pdf")
    private String fileType;

    @Schema(description = "预览地址（短时效预签名 URL）")
    private String url;

    @Schema(description = "文件配置编号（同源字节流用）")
    private Long fileConfigId;

    @Schema(description = "文件存储路径（同源字节流用）")
    private String filePath;

    @Schema(description = "地址有效期（秒）", example = "1800")
    private Integer expiresIn;

    @Schema(description = "是否支持页级定位（pdf）", example = "true")
    private Boolean pageLocatable;

    @Schema(description = "切片起始页码（传 chunkIndex 时返回）", example = "5")
    private Integer pageStart;

    @Schema(description = "切片结束页码（传 chunkIndex 时返回）", example = "5")
    private Integer pageEnd;

    @Schema(description = "切片页内高亮框：[{p:页码,b:[x0,y0,x1,y1]}]（已归一化 0~1）")
    private java.util.List<java.util.Map<String, Object>> bboxes;

    @Schema(description = "页码来源：mineru/exact/aligned", example = "mineru")
    private String pageSource;

    @Schema(description = "切片正文（截断至 2000 字，供前端在页内做全文定位高亮）")
    private String chunkContent;

    @Schema(description = "解析后的文档全文（includeText=true 时返回，供 Office 类文档文本预览）")
    private String textContent;

}
