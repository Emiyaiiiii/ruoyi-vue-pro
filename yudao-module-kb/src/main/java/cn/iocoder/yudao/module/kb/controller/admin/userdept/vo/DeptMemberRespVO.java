package cn.iocoder.yudao.module.kb.controller.admin.userdept.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门成员 Response VO
 * 合并芋道系统用户信息和 kb_user_dept 角色信息
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 部门成员 Response VO")
@Data
public class DeptMemberRespVO {

    @Schema(description = "kb_user_dept 主键ID（无记录时为 null）", example = "1024")
    private Long id;

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long deptId;

    @Schema(description = "用户昵称（来自系统用户表）", example = "张三")
    private String nickname;

    @Schema(description = "角色: 0=成员, 1=管理员", example = "0")
    private Integer role;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
