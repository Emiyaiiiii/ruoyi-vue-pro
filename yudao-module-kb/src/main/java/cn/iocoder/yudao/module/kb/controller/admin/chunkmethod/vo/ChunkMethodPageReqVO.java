package cn.iocoder.yudao.module.kb.controller.admin.chunkmethod.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

@Schema(description = "管理后台 - 切片方法分页 Request VO")
@Data
public class ChunkMethodPageReqVO extends PageParam {

    @Schema(description = "关键字搜索(方法名称/代码/描述)", example = "固定大小")
    private String search;

    @Schema(description = "方法类型", example = "fixed_size")
    private String methodType;

    @Schema(description = "是否启用: 0=停用, 1=启用", example = "1")
    private Integer isActive;

}
