package cn.iocoder.yudao.module.kb.controller.admin.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 知识库文件新增/修改 Request VO")
@Data
public class DocumentSaveReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "24067")
    private Long id;

    @Schema(description = "所属知识库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "13175")
    @NotNull(message = "所属知识库ID不能为空")
    private Long kbId;

    @Schema(description = "所属文件夹ID: 0=根目录", example = "0")
    private Long folderId;

    @Schema(description = "文件名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "王五")
    @NotEmpty(message = "文件名称不能为空")
    private String fileName;

    @Schema(description = "文件访问URL (芋道文件管理返回)", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://www.iocoder.cn")
    @NotEmpty(message = "文件访问URL (芋道文件管理返回)不能为空")
    private String fileUrl;

    @Schema(description = "文件类型: pdf/docx/xlsx/pptx/jpg/png等", example = "1")
    private String fileType;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "芋道文件配置ID (infra_file_config.id)", example = "25657")
    private Long fileConfigId;

    @Schema(description = "文件存储路径 (芋道文件管理返回)")
    private String filePath;

    @Schema(description = "文件描述", example = "你猜")
    private String description;

    @Schema(description = "标签 (逗号分隔)")
    private String tags;

    @Schema(description = "下载次数", example = "13409")
    private Integer downloadCount;

    @Schema(description = "查看次数", example = "28666")
    private Integer viewCount;

    @Schema(description = "状态: 0=正常, 1=禁用", example = "1")
    private Integer status;

}