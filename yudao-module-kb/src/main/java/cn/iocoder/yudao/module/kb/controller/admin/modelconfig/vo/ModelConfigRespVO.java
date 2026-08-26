package cn.iocoder.yudao.module.kb.controller.admin.modelconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;

@Schema(description = "管理后台 - 大模型配置 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ModelConfigRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "模型唯一标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "gpt-4")
    @ExcelProperty("模型UID")
    private String uid;

    @Schema(description = "具体模型名", example = "text-embedding-v4")
    @ExcelProperty("具体模型名")
    private String model;

    @Schema(description = "模型名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "GPT-4")
    @ExcelProperty("模型名称")
    private String name;

    @Schema(description = "API地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://api.openai.com/v1/chat/completions")
    @ExcelProperty("API地址")
    private String url;

    @Schema(description = "API密钥", requiredMode = Schema.RequiredMode.REQUIRED, example = "sk-xxxx")
    @ExcelProperty("API密钥")
    private String appkey;

    @Schema(description = "用途分类: embedding=嵌入/向量模型, llm=大模型, ocr=OCR/多模态模型", example = "llm")
    @ExcelProperty("用途分类")
    private String modelType;

    @Schema(description = "是否启用思考能力", example = "0")
    @ExcelProperty("启用思考")
    private Integer thinkingEnabled;

    @Schema(description = "是否支持多模态(VL)", example = "false")
    @ExcelProperty("支持多模态")
    private Boolean vlSupported;

    @Schema(description = "是否激活", example = "1")
    @ExcelProperty("是否激活")
    private Integer isActive;

    @Schema(description = "模型描述", example = "OpenAI GPT-4模型")
    @ExcelProperty("模型描述")
    private String description;

    @Schema(description = "最大Token数", example = "4096")
    @ExcelProperty("最大Token数")
    private Integer maxTokens;

    @Schema(description = "上下文长度", example = "8192")
    @ExcelProperty("上下文长度")
    private Integer contextLength;

    @Schema(description = "温度参数", example = "0.7")
    @ExcelProperty("温度参数")
    private Double temperature;

    @Schema(description = "Top-P参数", example = "0.9")
    @ExcelProperty("Top-P参数")
    private Double topP;

    @Schema(description = "配置参数(JSON格式)")
    @ExcelProperty("配置参数")
    private String config;

    @Schema(description = "排序顺序", example = "0")
    @ExcelProperty("排序顺序")
    private Integer sortOrder;

    @Schema(description = "是否置顶", example = "0")
    @ExcelProperty("是否置顶")
    private Integer isPinned;

    @Schema(description = "激活时间")
    @ExcelProperty("激活时间")
    private LocalDateTime activatedAt;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
