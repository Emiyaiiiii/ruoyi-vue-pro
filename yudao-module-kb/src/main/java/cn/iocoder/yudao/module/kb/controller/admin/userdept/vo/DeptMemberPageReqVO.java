package cn.iocoder.yudao.module.kb.controller.admin.userdept.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 部门成员分页 Request VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 部门成员分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DeptMemberPageReqVO extends PageParam {

    @Schema(description = "部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long deptId;

    @Schema(description = "是否包含子部门用户", example = "false")
    private Boolean includeChildren = false;

}
