package cn.iocoder.yudao.module.kb.controller.admin.news.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

@Schema(description = "管理后台 - 新闻同步日志分页 Request VO")
@Data
public class NewsSyncLogPageReqVO extends PageParam {

    @Schema(description = "数据源ID", example = "1")
    private Long sourceId;

    @Schema(description = "同步类型: full=全量, incremental=增量, manual=手动", example = "incremental")
    private String syncType;

    @Schema(description = "状态: started=已开始, running=运行中, completed=已完成, failed=失败", example = "completed")
    private String status;

}
