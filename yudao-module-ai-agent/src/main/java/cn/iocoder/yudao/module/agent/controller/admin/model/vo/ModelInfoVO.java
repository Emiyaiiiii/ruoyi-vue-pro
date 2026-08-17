package cn.iocoder.yudao.module.agent.controller.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 模型信息 VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 模型信息")
@Data
public class ModelInfoVO {

    @Schema(description = "模型 ID")
    private String id;

    @Schema(description = "模型名称")
    private String name;

    @Schema(description = "是否支持多模态")
    private Boolean supportsMultimodal;

    @Schema(description = "是否支持图片")
    private Boolean supportsImage;

    @Schema(description = "是否支持视频")
    private Boolean supportsVideo;

    @Schema(description = "最大输出 token 数")
    private Integer maxTokens;

    @Schema(description = "最大输入长度")
    private Integer maxInputLength;

    @Schema(description = "生成参数（temperature 等）")
    private Map<String, Object> generateKwargs;

    @Schema(description = "是否启用思考模式")
    private Boolean thinkingEnabled;

    @Schema(description = "思考预算")
    private Integer thinkingBudget;

    @Schema(description = "推理力度")
    private String reasoningEffort;
}
