package cn.iocoder.yudao.module.kb.controller.admin.modelconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "管理后台 - 大模型配置参数探测 Response VO")
@Data
public class ModelConfigDetectRespVO {

    @Schema(description = "是否探测到可识别的模型参数（false 表示接口不可达或未返回可识别参数）")
    private Boolean success;

    @Schema(description = "提示信息（探测到的参数汇总或失败原因）")
    private String message;

    @Schema(description = "最大Token数", example = "8192")
    private Integer maxTokens;

    @Schema(description = "上下文长度", example = "32768")
    private Integer contextLength;

    @Schema(description = "是否支持多模态(VL)", example = "true")
    private Boolean vlSupported;

    @Schema(description = "是否启用思考能力", example = "true")
    private Boolean thinkingEnabled;

    @Schema(description = "向量维度（仅 embedding/embedding 类模型有效）", example = "4096")
    private Integer dimensions;

}
