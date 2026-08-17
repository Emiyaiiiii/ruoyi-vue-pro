package cn.iocoder.yudao.module.kb.controller.admin.library.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 知识库新增/修改 Request VO")
@Data
public class LibrarySaveReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "18531")
    private Long id;

    @Schema(description = "知识库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotEmpty(message = "知识库名称不能为空")
    private String name;

    @Schema(description = "分类ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "26803")
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @Schema(description = "关联层级配置ID", example = "19733")
    private Long kbLevelId;

    @Schema(description = "所有者ID: 用户或部门, 取决于层级配置的owner_dim", example = "9877")
    private Long ownerId;

    @Schema(description = "描述", example = "随便")
    private String description;

    @Schema(description = "封面图片URL", example = "https://www.iocoder.cn")
    private String coverUrl;

    @Schema(description = "文档数量", example = "1563")
    private Integer docCount;

    @Schema(description = "状态: 0=启用, 1=禁用", example = "1")
    private Integer status;

    @Schema(description = "共享部门ID列表")
    private List<Long> shareDeptIds;

    @Schema(description = "是否公开到广场: 0=否, 1=是")
    private Integer isPublic;

    @Schema(description = "是否项目成果库: 0=否, 1=是")
    private Integer isProject;

}