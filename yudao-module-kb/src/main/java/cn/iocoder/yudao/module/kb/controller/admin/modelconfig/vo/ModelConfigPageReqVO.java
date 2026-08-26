package cn.iocoder.yudao.module.kb.controller.admin.modelconfig.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

@Schema(description = "管理后台 - 大模型配置分页 Request VO")
@Data
public class ModelConfigPageReqVO extends PageParam {

    @Schema(description = "关键字搜索(模型名称/UID/描述)", example = "GPT")
    private String search;

    @Schema(description = "用途分类: embedding/llm/ocr", example = "llm")
    private String modelType;

    @Schema(description = "是否激活: 0=停用, 1=激活", example = "1")
    private Integer isActive;

}
