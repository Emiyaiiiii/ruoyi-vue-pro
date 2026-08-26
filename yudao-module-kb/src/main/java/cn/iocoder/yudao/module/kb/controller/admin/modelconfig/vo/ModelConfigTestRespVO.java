package cn.iocoder.yudao.module.kb.controller.admin.modelconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 大模型配置测试结果 Response VO")
@Data
public class ModelConfigTestRespVO {

    @Schema(description = "配置ID")
    private Long configId;

    @Schema(description = "模型名称")
    private String name;

    @Schema(description = "测试消息")
    private String testMessage;

    @Schema(description = "测试是否成功")
    private Boolean success;

    @Schema(description = "响应时间(秒)")
    private Double responseTime;

    @Schema(description = "模型回复内容")
    private String response;

    @Schema(description = "错误信息")
    private String error;

    @Schema(description = "模型信息")
    private ModelInfo modelInfo;

    @Data
    @Schema(description = "模型信息")
    public static class ModelInfo {
        @Schema(description = "模型名称")
        private String name;
        @Schema(description = "模型UID")
        private String uid;
        @Schema(description = "API地址")
        private String url;
        @Schema(description = "最大Token数")
        private Integer maxTokens;
        @Schema(description = "温度参数")
        private Double temperature;
        @Schema(description = "是否支持思考")
        private Integer thinkingSupported;
        @Schema(description = "是否激活")
        private Integer isActive;
    }

}
