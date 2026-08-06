package cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 向量处理任务分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class VectorTaskPageReqVO extends PageParam {

    @Schema(description = "文档ID", example = "123")
    private Long docId;

    @Schema(description = "知识库ID", example = "456")
    private Long kbId;

    @Schema(description = "状态", example = "1")
    private Integer status;
}
