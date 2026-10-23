package cn.iocoder.yudao.module.kb.controller.admin.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 监控告警")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitorAlertVO {

    @Schema(description = "级别：INFO / WARN / CRITICAL")
    private String level;

    @Schema(description = "告警码")
    private String code;

    @Schema(description = "告警文案")
    private String message;
}
