package cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答会话 Resp VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 问答会话 Response VO")
@Data
public class ChatSessionRespVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "智能体ID", example = "1")
    private Long agentId;

    @Schema(description = "用户ID", example = "1024")
    private Long userId;

    @Schema(description = "QwenPaw session id")
    private String sessionKey;

    @Schema(description = "会话标题", example = "关于报销制度的咨询")
    private String title;

    @Schema(description = "状态: 0=关闭, 1=进行中", example = "1")
    private Integer status;

    @Schema(description = "智能体名称（冗余展示字段）", example = "知识库助手")
    private String agentName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
