package cn.iocoder.yudao.module.kb.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 知识库处理监控配置（P1）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "kb.monitor")
public class KbMonitorProperties {

    /**
     * 是否启用监控汇总
     */
    private Boolean enabled = true;

    /**
     * 汇总间隔（毫秒）
     */
    private Integer collectIntervalMs = 10000;

    /**
     * 队列积压告警阈值（消息数）
     */
    private Integer queueDepthThreshold = 500;

    /**
     * 抓取 worker /metrics 的连接超时（毫秒）
     */
    private Integer scrapeConnectTimeoutMs = 1000;

    /**
     * 抓取 worker /metrics 的读取超时（毫秒）
     */
    private Integer scrapeReadTimeoutMs = 2000;

    /**
     * 排队超过该时长（分钟）视为异常积压
     */
    private Integer pendingStuckMinutes = 1440;

    /**
     * 近 24h 超时率告警阈值（百分比）
     */
    private Double timeoutRateThreshold = 5.0D;
}
