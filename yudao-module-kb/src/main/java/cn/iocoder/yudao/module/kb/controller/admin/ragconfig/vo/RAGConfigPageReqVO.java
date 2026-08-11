package cn.iocoder.yudao.module.kb.controller.admin.ragconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

@Schema(description = "管理后台 - RAG配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class RAGConfigPageReqVO extends PageParam {

    @Schema(description = "所属模块", example = "retrieval")
    private String module;

    @Schema(description = "是否启用", example = "1")
    private Integer isActive;

    @Schema(description = "搜索关键字(匹配键名和描述)", example = "top_k")
    private String search;

}
