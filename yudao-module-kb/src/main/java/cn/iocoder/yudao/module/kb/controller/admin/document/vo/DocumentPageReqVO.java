package cn.iocoder.yudao.module.kb.controller.admin.document.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 知识库文件分页 Request VO")
@Data
public class DocumentPageReqVO extends PageParam {

    @Schema(description = "所属知识库ID", example = "13175")
    private Long kbId;

    @Schema(description = "所属文件夹ID: 0=根目录", example = "0")
    private Long folderId;

    @Schema(description = "文件名称", example = "王五")
    private String fileName;

    @Schema(description = "文件访问URL (芋道文件管理返回)", example = "https://www.iocoder.cn")
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

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}