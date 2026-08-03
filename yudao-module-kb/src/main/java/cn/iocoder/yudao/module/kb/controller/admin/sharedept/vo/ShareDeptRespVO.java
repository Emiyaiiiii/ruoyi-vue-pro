package cn.iocoder.yudao.module.kb.controller.admin.sharedept.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 知识库共享部门关联 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ShareDeptRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "18181")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "知识库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "27091")
    @ExcelProperty("知识库ID")
    private Long kbId;

    @Schema(description = "共享目标部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "16103")
    @ExcelProperty("共享目标部门ID")
    private Long deptId;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}