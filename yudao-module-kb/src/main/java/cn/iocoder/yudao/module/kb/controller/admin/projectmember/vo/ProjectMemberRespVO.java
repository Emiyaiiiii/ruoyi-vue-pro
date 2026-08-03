package cn.iocoder.yudao.module.kb.controller.admin.projectmember.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目成员 Response VO
 * 合并系统用户昵称信息
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 项目成员 Response VO")
@Data
public class ProjectMemberRespVO {

    @Schema(description = "主键ID", example = "1024")
    private Long id;

    @Schema(description = "知识库ID（项目）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long kbId;

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "用户昵称（来自系统用户表）", example = "张三")
    private String nickname;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
