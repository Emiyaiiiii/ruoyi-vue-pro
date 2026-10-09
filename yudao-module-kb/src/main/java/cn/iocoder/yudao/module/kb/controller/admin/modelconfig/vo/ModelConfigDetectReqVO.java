package cn.iocoder.yudao.module.kb.controller.admin.modelconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 大模型配置参数探测 Request VO（OpenAI 兼容接口自动获取模型参数）")
@Data
public class ModelConfigDetectReqVO {

    @Schema(description = "API地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://api.example.com/v1/chat/completions")
    @NotEmpty(message = "API地址不能为空")
    private String url;

    @Schema(description = "API密钥", example = "sk-xxxx")
    private String appkey;

    @Schema(description = "具体模型名", requiredMode = Schema.RequiredMode.REQUIRED, example = "deepseek-chat")
    @NotEmpty(message = "具体模型名不能为空")
    private String model;

    @Schema(description = "用途分类: llm/embedding/ocr/rerank", example = "llm")
    private String modelType;

}
