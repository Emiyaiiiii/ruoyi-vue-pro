package cn.iocoder.yudao.module.kb.controller.admin.modelconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "管理后台 - 大模型配置精简 Response VO（用于下拉选择）")
@Data
public class ModelConfigSimpleVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "模型唯一标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "gpt-4")
    private String uid;

    @Schema(description = "模型名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "GPT-4")
    private String name;

    @Schema(description = "具体模型名", example = "text-embedding-v4")
    private String model;

    @Schema(description = "用途分类: embedding/llm/ocr", example = "llm")
    private String modelType;

}
