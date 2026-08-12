package cn.iocoder.yudao.module.kb.controller.admin.news.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

@Schema(description = "管理后台 - 新闻记录分页 Request VO")
@Data
public class NewsRecordPageReqVO extends PageParam {

    @Schema(description = "关键字搜索(标题/内容/外部ID)", example = "年度总结")
    private String search;

    @Schema(description = "数据源ID", example = "1")
    private Long sourceId;

    @Schema(description = "状态: pending=待处理, completed=已完成, failed=失败, skipped=已跳过", example = "pending")
    private String status;

    @Schema(description = "频道", example = "公司要闻")
    private String externalChannel;

}
