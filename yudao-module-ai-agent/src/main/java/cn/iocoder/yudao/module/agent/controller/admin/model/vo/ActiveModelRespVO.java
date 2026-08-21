package cn.iocoder.yudao.module.agent.controller.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 激活模型响应 VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 激活模型响应")
@Data
public class ActiveModelRespVO {

    @Schema(description = "Provider ID")
    private String providerId;

    @Schema(description = "模型 ID ")
    private String model;

    @Schema(description = "作用域：global / agent")
    private String scope;

    @Schema(description = "智能体 ID（scope=agent 时有值）")
    private String agentId;
}
