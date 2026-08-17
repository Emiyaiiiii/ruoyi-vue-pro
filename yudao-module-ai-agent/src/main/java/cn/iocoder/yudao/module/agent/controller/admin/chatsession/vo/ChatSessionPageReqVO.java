package cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 问答会话分页 Req VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 问答会话分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ChatSessionPageReqVO extends PageParam {

    @Schema(description = "智能体ID", example = "1")
    private Long agentId;

    @Schema(description = "用户ID", example = "1024")
    private Long userId;

    @Schema(description = "状态: 0=关闭, 1=进行中", example = "1")
    private Integer status;

    @Schema(description = "关键字：标题", example = "报销")
    private String search;

}
