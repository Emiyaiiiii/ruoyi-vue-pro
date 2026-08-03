package cn.iocoder.yudao.module.kb.controller.admin.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 知识库文件 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DocumentRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "24067")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "所属知识库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "13175")
    @ExcelProperty("所属知识库ID")
    private Long kbId;

    @Schema(description = "文件名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "王五")
    @ExcelProperty("文件名称")
    private String fileName;

    @Schema(description = "文件访问URL (芋道文件管理返回)", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://www.iocoder.cn")
    @ExcelProperty("文件访问URL (芋道文件管理返回)")
    private String fileUrl;

    @Schema(description = "文件类型: pdf/docx/xlsx/pptx/jpg/png等", example = "1")
    @ExcelProperty("文件类型: pdf/docx/xlsx/pptx/jpg/png等")
    private String fileType;

    @Schema(description = "文件大小(字节)")
    @ExcelProperty("文件大小(字节)")
    private Long fileSize;

    @Schema(description = "芋道文件配置ID (infra_file_config.id)", example = "25657")
    @ExcelProperty("芋道文件配置ID (infra_file_config.id)")
    private Long fileConfigId;

    @Schema(description = "文件存储路径 (芋道文件管理返回)")
    @ExcelProperty("文件存储路径 (芋道文件管理返回)")
    private String filePath;

    @Schema(description = "文件描述", example = "你猜")
    @ExcelProperty("文件描述")
    private String description;

    @Schema(description = "标签 (逗号分隔)")
    @ExcelProperty("标签 (逗号分隔)")
    private String tags;

    @Schema(description = "下载次数", example = "13409")
    @ExcelProperty("下载次数")
    private Integer downloadCount;

    @Schema(description = "查看次数", example = "28666")
    @ExcelProperty("查看次数")
    private Integer viewCount;

    @Schema(description = "状态: 0=正常, 1=禁用", example = "1")
    @ExcelProperty("状态: 0=正常, 1=禁用")
    private Integer status;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}