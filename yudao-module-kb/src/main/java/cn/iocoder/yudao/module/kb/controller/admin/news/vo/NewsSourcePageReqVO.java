package cn.iocoder.yudao.module.kb.controller.admin.news.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

@Schema(description = "管理后台 - 新闻数据源分页 Request VO")
@Data
public class NewsSourcePageReqVO extends PageParam {

    @Schema(description = "关键字搜索(数据源名称/主机/数据库名)", example = "内部")
    private String search;

    @Schema(description = "是否启用同步: 0=停用, 1=启用", example = "1")
    private Integer syncEnabled;

}
