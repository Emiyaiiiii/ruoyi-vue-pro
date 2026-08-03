package cn.iocoder.yudao.module.kb.controller.admin.sharedept.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 知识库共享部门关联新增/修改 Request VO")
@Data
public class ShareDeptSaveReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "18181")
    private Long id;

    @Schema(description = "知识库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "27091")
    @NotNull(message = "知识库ID不能为空")
    private Long kbId;

    @Schema(description = "共享目标部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "16103")
    @NotNull(message = "共享目标部门ID不能为空")
    private Long deptId;

}