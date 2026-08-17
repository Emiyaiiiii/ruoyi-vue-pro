package cn.iocoder.yudao.module.kb.controller.admin.folder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 文档文件夹新增/修改 Request VO")
@Data
public class FolderSaveReqVO {

    @Schema(description = "主键ID", example = "24067")
    private Long id;

    @Schema(description = "所属知识库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "13175")
    @NotNull(message = "所属知识库ID不能为空")
    private Long kbId;

    @Schema(description = "文件夹名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "项目文档")
    @NotEmpty(message = "文件夹名称不能为空")
    private String name;

    @Schema(description = "父文件夹ID: 0=根目录", example = "0")
    private Long parentId;

    @Schema(description = "排序")
    private Integer sort;

}