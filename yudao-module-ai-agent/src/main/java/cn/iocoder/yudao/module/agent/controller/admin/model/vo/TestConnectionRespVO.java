package cn.iocoder.yudao.module.agent.controller.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 连接测试响应 VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 连接测试响应")
@Data
public class TestConnectionRespVO {

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "错误信息")
    private String error;

    @Schema(description = "延迟（毫秒）")
    private Long latencyMs;
}
