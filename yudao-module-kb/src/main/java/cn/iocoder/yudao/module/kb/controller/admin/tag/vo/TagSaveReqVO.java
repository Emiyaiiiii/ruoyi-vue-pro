package cn.iocoder.yudao.module.kb.controller.admin.tag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 标签创建/更新 Request VO")
@Data
public class TagSaveReqVO {

    @Schema(description = "标签编号", example = "1024")
    private Long id;

    @Schema(description = "标签名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "标签名称不能为空")
    @Size(max = 100, message = "标签名称长度不能超过 100 个字符")
    private String name;

    @Schema(description = "标签颜色", example = "#007bff")
    private String color;

    @Schema(description = "标签类型", example = "document")
    private String type;

    @Schema(description = "是否全局可见（仅管理员可设置为全局，非管理员该值被忽略）", example = "false")
    private Boolean isGlobal;

}