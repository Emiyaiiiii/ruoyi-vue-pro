package cn.iocoder.yudao.module.kb.controller.admin.tag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 标签 Response VO")
@Data
public class TagRespVO {

    @Schema(description = "标签编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "标签名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "标签颜色", example = "#007bff")
    private String color;

    @Schema(description = "标签类型", example = "document")
    private String type;

    @Schema(description = "归属用户ID，null=全局标签")
    private Long ownerId;

    @Schema(description = "归属人昵称（全局标签为空）")
    private String ownerNickname;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}