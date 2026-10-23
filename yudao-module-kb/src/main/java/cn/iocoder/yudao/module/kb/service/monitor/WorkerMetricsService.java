package cn.iocoder.yudao.module.kb.service.monitor;

import cn.iocoder.yudao.module.kb.controller.admin.monitor.vo.MonitorOverviewVO;

/**
 * 知识库处理监控 Service
 */
public interface WorkerMetricsService {

    /**
     * 获取最近一次汇总结果（缓存为空时同步汇总一次）
     */
    MonitorOverviewVO getOverview();

    /**
     * 立即汇总一次
     */
    MonitorOverviewVO collect();
}
