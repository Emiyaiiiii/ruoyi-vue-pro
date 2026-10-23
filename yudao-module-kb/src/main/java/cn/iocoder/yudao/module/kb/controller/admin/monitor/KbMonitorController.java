package cn.iocoder.yudao.module.kb.controller.admin.monitor;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.kb.controller.admin.monitor.vo.MonitorOverviewVO;
import cn.iocoder.yudao.module.kb.service.monitor.WorkerMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 知识库处理监控")
@RestController
@RequestMapping("/kb/monitor")
@Validated
public class KbMonitorController {

    @Resource
    private WorkerMetricsService workerMetricsService;

    @GetMapping("/overview")
    @Operation(summary = "获取处理监控概览")
    @PreAuthorize("@ss.hasPermission('kb:monitor:query')")
    public CommonResult<MonitorOverviewVO> getOverview() {
        return success(workerMetricsService.getOverview());
    }
}
