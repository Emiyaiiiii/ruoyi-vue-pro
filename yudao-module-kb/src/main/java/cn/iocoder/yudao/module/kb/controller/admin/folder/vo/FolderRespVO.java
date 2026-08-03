package cn.iocoder.yudao.module.kb.controller.admin.folder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 文档文件夹 Response VO")
@Data
public class FolderRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "24067")
    private Long id;

    @Schema(description = "所属知识库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "13175")
    private Long kbId;

    @Schema(description = "文件夹名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "项目文档")
    private String name;

    @Schema(description = "父文件夹ID: 0=根目录", example = "0")
    private Long parentId;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "子文件夹列表")
    private List<FolderRespVO> children;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}