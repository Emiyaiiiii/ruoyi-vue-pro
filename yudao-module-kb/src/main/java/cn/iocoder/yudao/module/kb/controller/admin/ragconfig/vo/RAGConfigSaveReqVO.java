package cn.iocoder.yudao.module.kb.controller.admin.ragconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - RAG配置新增/修改 Request VO")
@Data
public class RAGConfigSaveReqVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "所属模块", example = "retrieval")
    private String module;

    @Schema(description = "配置键名", example = "top_k")
    @Size(max = 100, message = "配置键名长度不能超过100个字符")
    private String key;

    @Schema(description = "配置值", example = "30")
    private String value;

    @Schema(description = "值类型", example = "int")
    private String valueType;

    @Schema(description = "配置说明", example = "最终返回给 LLM 的文档片段数量")
    private String description;

    @Schema(description = "是否启用", example = "1")
    private Integer isActive;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

}
