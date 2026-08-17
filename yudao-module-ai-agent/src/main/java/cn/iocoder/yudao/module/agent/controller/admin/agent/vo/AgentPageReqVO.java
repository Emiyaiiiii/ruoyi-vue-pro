package cn.iocoder.yudao.module.agent.controller.admin.agent.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 智能体分页 Req VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 智能体分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AgentPageReqVO extends PageParam {

    @Schema(description = "所属用户ID（为空则查当前用户）", example = "1024")
    private Long userId;

    @Schema(description = "状态: 0=停用, 1=启用", example = "1")
    private Integer status;

    @Schema(description = "关键字：名称/描述", example = "知识库")
    private String search;

}
