package cn.iocoder.yudao.module.kb.controller.admin.modelconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 大模型配置新增/修改 Request VO")
@Data
public class ModelConfigSaveReqVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "模型唯一标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "gpt-4")
    @NotEmpty(message = "模型UID不能为空")
    @Size(max = 100, message = "模型UID长度不能超过100个字符")
    private String uid;

    @Schema(description = "模型名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "GPT-4")
    @NotEmpty(message = "模型名称不能为空")
    @Size(max = 100, message = "模型名称长度不能超过100个字符")
    private String name;

    @Schema(description = "API地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://api.openai.com/v1/chat/completions")
    @NotEmpty(message = "API地址不能为空")
    private String url;

    @Schema(description = "API密钥", requiredMode = Schema.RequiredMode.REQUIRED, example = "sk-xxxx")
    @NotEmpty(message = "API密钥不能为空")
    @Size(min = 10, message = "API密钥长度至少为10个字符")
    private String appkey;

    @Schema(description = "部署类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "openai")
    @NotEmpty(message = "部署类型不能为空")
    private String deploy;

    @Schema(description = "是否启用思考能力", example = "0")
    private Integer thinkingEnabled;

    @Schema(description = "是否激活", example = "1")
    private Integer isActive;

    @Schema(description = "模型描述", example = "OpenAI GPT-4模型")
    private String description;

    @Schema(description = "最大Token数", example = "4096")
    private Integer maxTokens;

    @Schema(description = "上下文长度", example = "8192")
    private Integer contextLength;

    @Schema(description = "温度参数", example = "0.7")
    private Double temperature;

    @Schema(description = "Top-P参数", example = "0.9")
    private Double topP;

    @Schema(description = "元数据(JSON格式)", example = "{\"provider\":\"openai\"}")
    private String metadata;

    @Schema(description = "配置参数(JSON格式)", example = "{\"stream\":true}")
    private String config;

    @Schema(description = "排序顺序", example = "0")
    private Integer sortOrder;

    @Schema(description = "是否置顶", example = "0")
    private Integer isPinned;

    @Schema(description = "支持平台", example = "both")
    private String platform;

}
