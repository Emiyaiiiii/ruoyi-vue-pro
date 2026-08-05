package cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Set;

@Schema(description = "管理后台 - 批量赋予用户角色 Request VO")
@Data
public class PermissionBatchAssignUserRoleReqVO {

    @Schema(description = "用户编号列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1, 2, 3]")
    @NotEmpty(message = "用户编号列表不能为空")
    @Size(max = 100, message = "单次最多操作100个用户")
    private List<Long> userIds;

    @Schema(description = "角色编号列表", example = "[1, 3, 5]")
    private Set<Long> roleIds;

}
