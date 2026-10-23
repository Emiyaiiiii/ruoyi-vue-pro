package cn.iocoder.yudao.module.kb.service.monitor;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.kb.controller.admin.monitor.vo.MonitorAlertVO;
import cn.iocoder.yudao.module.kb.controller.admin.monitor.vo.MonitorOverviewVO;
import cn.iocoder.yudao.module.kb.controller.admin.monitor.vo.QueueMetricsVO;
import cn.iocoder.yudao.module.kb.controller.admin.monitor.vo.TaskStatsVO;
import cn.iocoder.yudao.module.kb.controller.admin.monitor.vo.WorkerMetricsVO;
import cn.iocoder.yudao.module.kb.dal.mysql.vectortask.VectorTaskMapper;
import cn.iocoder.yudao.module.kb.framework.config.KbMonitorProperties;
import cn.iocoder.yudao.module.kb.framework.config.VectorTaskConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rabbitmq.client.AMQP;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 知识库处理监控汇总：
 * - 从 Redis 读取 worker 注册表 + 心跳
 * - 抓取每个 worker 的 /metrics（prometheus_client）
 * - 汇总 RabbitMQ 队列深度与 MySQL 任务统计
 */
@Service
@Slf4j
public class WorkerMetricsServiceImpl implements WorkerMetricsService {

    private static final String REGISTRY_KEY = "kb:worker:registry";
    private static final String HB_KEY_PREFIX = "kb:worker:";
    private static final String HB_ZSET_KEY = "kb:worker:heartbeat";
    private static final String SNAPSHOT_KEY_PREFIX = "kb:worker:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private VectorTaskMapper vectorTaskMapper;
    @Resource
    private VectorTaskConfig vectorTaskConfig;
    @Resource
    private KbMonitorProperties monitorProperties;

    private final AtomicReference<MonitorOverviewVO> cache = new AtomicReference<>();

    private RestTemplate metricsRestTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(monitorProperties.getScrapeConnectTimeoutMs());
        factory.setReadTimeout(monitorProperties.getScrapeReadTimeoutMs());
        this.metricsRestTemplate = new RestTemplate(factory);
    }

    @Override
    public MonitorOverviewVO getOverview() {
        MonitorOverviewVO overview = cache.get();
        // 定时任务可能被其他 @Scheduled 阻塞/延迟，页面读取时发现缓存过期就主动重查，
        // 避免任务清单已失败、监控页仍展示旧的成功/0 统计。
        if (isCacheStale(overview)) {
            overview = collect();
        }
        return overview;
    }

    private boolean isCacheStale(MonitorOverviewVO overview) {
        if (overview == null || overview.getCollectedAt() == null) {
            return true;
        }
        Integer interval = monitorProperties.getCollectIntervalMs();
        long staleMillis = interval == null || interval <= 0 ? 10000L : interval;
        return Duration.between(overview.getCollectedAt(), LocalDateTime.now()).toMillis() >= staleMillis;
    }

    @Scheduled(fixedDelayString = "${kb.monitor.collect-interval-ms:10000}")
    public void scheduledCollect() {
        if (Boolean.FALSE.equals(monitorProperties.getEnabled())) {
            return;
        }
        try {
            collect();
        } catch (Exception e) {
            log.warn("[scheduledCollect] 监控汇总失败", e);
        }
    }

    @Override
    public MonitorOverviewVO collect() {
        MonitorOverviewVO overview = new MonitorOverviewVO();
        List<WorkerMetricsVO> workers = new ArrayList<>();
        int online = 0;
        try {
            Map<Object, Object> registry = stringRedisTemplate.opsForHash().entries(REGISTRY_KEY);
            for (Map.Entry<Object, Object> entry : registry.entrySet()) {
                WorkerRegistration registration = parseRegistration(String.valueOf(entry.getValue()));
                if (registration == null) {
                    continue;
                }
                String workerId = String.valueOf(entry.getKey());
                WorkerMetricsVO worker = new WorkerMetricsVO();
                worker.setWorkerId(workerId);
                worker.setHost(registration.getHost());
                worker.setPort(registration.getPort());
                worker.setConcurrency(registration.getConcurrency());

                String heartbeat = stringRedisTemplate.opsForValue().get(HB_KEY_PREFIX + workerId + ":hb");
                if (heartbeat == null) {
                    // 心跳消失即下线：不再展示离线实例，并从注册表清理
                    stringRedisTemplate.opsForHash().delete(REGISTRY_KEY, workerId);
                    stringRedisTemplate.opsForZSet().remove(HB_ZSET_KEY, workerId);
                    log.info("[collect] Worker 心跳缺失，已下线: {}", workerId);
                    continue;
                }
                worker.setStatus("ONLINE");
                worker.setLastHeartbeat(parseEpochSecond(heartbeat));
                online++;
                try {
                    fillMetrics(worker, scrapeHttp(registration));
                    worker.setSource("HTTP");
                } catch (Exception httpError) {
                    // 容器网络不通时降级读 Redis 快照（worker 每 15s 推送一次）
                    WorkerSnapshot snapshot = readSnapshot(workerId);
                    if (snapshot != null) {
                        fillFromSnapshot(worker, snapshot);
                        worker.setSource("REDIS");
                    } else {
                        worker.setError(truncate(httpError.getMessage(), 300));
                        log.warn("[collect] 抓取 worker 指标失败且无快照: workerId={}, error={}",
                                workerId, httpError.getMessage());
                    }
                }
                workers.add(worker);
            }
        } catch (Exception e) {
            log.warn("[collect] 读取 worker 注册表失败", e);
        }

        workers.sort(Comparator.comparing(WorkerMetricsVO::getWorkerId,
                Comparator.nullsLast(String::compareTo)));
        overview.setWorkers(workers);
        overview.setWorkerCount(workers.size());
        overview.setOnlineWorkerCount(online);
        overview.setQueue(buildQueueMetrics());
        overview.setTasks(buildTaskStats());
        overview.setAlerts(buildAlerts(overview));
        overview.setCollectedAt(LocalDateTime.now());
        cache.set(overview);
        return overview;
    }

    private WorkerRegistration parseRegistration(String json) {
        try {
            return JsonUtils.parseObject(json, WorkerRegistration.class);
        } catch (Exception e) {
            log.debug("[collect] 解析 worker 注册信息失败: {}", json);
            return null;
        }
    }

    /**
     * 依次尝试注册的 hostname 与 IP：
     * Docker 容器默认 hostname 是容器 ID，跨网络/宿主机访问时可能无法解析；
     * 失败后回退到容器 IP，仍失败则抛出异常，由调用方降级读 Redis 快照。
     */
    private Map<String, Double> scrapeHttp(WorkerRegistration registration) {
        List<String> candidates = new ArrayList<>();
        if (registration.getHost() != null && !registration.getHost().isEmpty()) {
            candidates.add(registration.getHost());
        }
        if (registration.getIp() != null && !registration.getIp().isEmpty()
                && !candidates.contains(registration.getIp())) {
            candidates.add(registration.getIp());
        }
        if (candidates.isEmpty() || registration.getPort() == null || registration.getPort() <= 0) {
            return Collections.emptyMap();
        }
        RuntimeException lastError = null;
        for (String candidate : candidates) {
            try {
                return scrape(candidate, registration.getPort());
            } catch (RuntimeException e) {
                lastError = e;
                log.debug("[collect] 抓取失败，尝试下一个地址: {}:{}, error={}",
                        candidate, registration.getPort(), e.getMessage());
            }
        }
        throw new IllegalStateException("无法连接 Worker " + String.join(" / ", candidates)
                + ":" + registration.getPort()
                + "（可设置 WORKER_ADVERTISE_HOST 为 Java 可访问的地址）", lastError);
    }

    private Map<String, Double> scrape(String host, Integer port) {
        if (host == null || host.isEmpty() || port == null || port <= 0) {
            return Collections.emptyMap();
        }
        String url = "http://" + host + ":" + port + "/metrics";
        String body = metricsRestTemplate.getForObject(url, String.class);
        return parsePrometheus(body);
    }

    /**
     * HTTP 不可达时的兜底：读取 worker 每 15s 写入 Redis 的指标快照。
     */
    private WorkerSnapshot readSnapshot(String workerId) {
        try {
            String json = stringRedisTemplate.opsForValue()
                    .get(SNAPSHOT_KEY_PREFIX + workerId + ":metrics");
            return json == null ? null : JsonUtils.parseObject(json, WorkerSnapshot.class);
        } catch (Exception e) {
            log.debug("[collect] 读取 worker 指标快照失败: workerId={}, error={}", workerId, e.getMessage());
            return null;
        }
    }

    private void fillFromSnapshot(WorkerMetricsVO worker, WorkerSnapshot snapshot) {
        worker.setInflight(snapshot.getInflight());
        worker.setCompleted(snapshot.getCompleted());
        worker.setFailed(snapshot.getFailed());
        worker.setCancelled(snapshot.getCancelled());
        worker.setAvgTaskSeconds(snapshot.getAvgTaskSeconds());
        worker.setEmbeddingRequests(snapshot.getEmbeddingSuccess());
        worker.setEmbeddingErrors(snapshot.getEmbeddingError());
    }

    /**
     * 解析 Prometheus 文本格式：metric{labels} value
     */
    private Map<String, Double> parsePrometheus(String text) {
        Map<String, Double> result = new HashMap<>();
        if (text == null || text.isEmpty()) {
            return result;
        }
        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int splitIndex = line.lastIndexOf(' ');
            if (splitIndex <= 0) {
                continue;
            }
            String key = line.substring(0, splitIndex).trim();
            String value = line.substring(splitIndex + 1).trim();
            try {
                result.put(key, Double.parseDouble(value));
            } catch (NumberFormatException ignored) {
                // 忽略 NaN/Inf 等无法解析的值
            }
        }
        return result;
    }

    private void fillMetrics(WorkerMetricsVO worker, Map<String, Double> metrics) {
        worker.setInflight((int) sumByName(metrics, "kb_worker_inflight"));
        worker.setCompleted((long) sumByLabel(metrics, "kb_worker_tasks_total", "status", "COMPLETED"));
        worker.setFailed((long) sumByLabel(metrics, "kb_worker_tasks_total", "status", "FAILED"));
        worker.setCancelled((long) sumByLabel(metrics, "kb_worker_tasks_total", "status", "CANCELLED"));
        double durationSum = sumByName(metrics, "kb_worker_task_duration_seconds_sum");
        double durationCount = sumByName(metrics, "kb_worker_task_duration_seconds_count");
        worker.setAvgTaskSeconds(durationCount > 0
                ? Math.round(durationSum / durationCount * 10) / 10.0 : 0.0);
        worker.setEmbeddingRequests((long) sumByLabel(metrics,
                "kb_worker_embedding_requests_total", "status", "success"));
        worker.setEmbeddingErrors((long) sumByLabel(metrics,
                "kb_worker_embedding_requests_total", "status", "error"));
    }

    private double sumByName(Map<String, Double> metrics, String name) {
        double sum = 0;
        for (Map.Entry<String, Double> entry : metrics.entrySet()) {
            String key = entry.getKey();
            if (key.equals(name) || key.startsWith(name + "{")) {
                sum += entry.getValue();
            }
        }
        return sum;
    }

    private double sumByLabel(Map<String, Double> metrics, String name, String label, String value) {
        double sum = 0;
        String labelMatch = label + "=\"" + value + "\"";
        for (Map.Entry<String, Double> entry : metrics.entrySet()) {
            String key = entry.getKey();
            if ((key.startsWith(name + "{") || key.equals(name)) && key.contains(labelMatch)) {
                sum += entry.getValue();
            }
        }
        return sum;
    }

    private QueueMetricsVO buildQueueMetrics() {
        QueueMetricsVO queue = new QueueMetricsVO();
        String queueName = vectorTaskConfig.getIngestQueue();
        queue.setName(queueName);
        try {
            Integer[] info = rabbitTemplate.execute(channel -> {
                AMQP.Queue.DeclareOk ok = channel.queueDeclarePassive(queueName);
                return new Integer[]{ok.getMessageCount(), ok.getConsumerCount()};
            });
            if (info != null) {
                queue.setDepth(info[0]);
                queue.setConsumers(info[1]);
            }
        } catch (Exception e) {
            log.debug("[collect] 查询队列失败: queue={}, error={}", queueName, e.getMessage());
            queue.setDepth(-1);
            queue.setConsumers(-1);
        }
        return queue;
    }

    private TaskStatsVO buildTaskStats() {
        TaskStatsVO stats = new TaskStatsVO();
        stats.setTotal(0L);
        stats.setPending(0L);
        stats.setProcessing(0L);
        stats.setCompleted(0L);
        stats.setFailed(0L);
        stats.setSubmitFailed(0L);
        stats.setTimeout(0L);
        stats.setCancelled(0L);
        stats.setAvgDurationSeconds(0.0);
        stats.setTimeoutRate24h(0.0);
        stats.setOldestPendingMinutes(0L);

        try {
            List<Map<String, Object>> rows = TenantUtils.executeIgnore(
                    () -> vectorTaskMapper.selectStatusCounts());
            long total = 0;
            for (Map<String, Object> row : rows) {
                int status = ((Number) row.get("status")).intValue();
                long count = ((Number) row.get("cnt")).longValue();
                total += count;
                switch (status) {
                    case 0 -> stats.setPending(count);
                    case 1 -> stats.setProcessing(count);
                    case 2 -> stats.setCompleted(count);
                    case 3 -> stats.setFailed(count);
                    case 4 -> stats.setSubmitFailed(count);
                    case 5 -> stats.setTimeout(count);
                    case 6 -> stats.setCancelled(count);
                    default -> { }
                }
            }
            stats.setTotal(total);
        } catch (Exception e) {
            log.warn("[collect] 查询任务状态统计失败", e);
        }

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        try {
            Double avg = TenantUtils.executeIgnore(
                    () -> vectorTaskMapper.selectAvgCompletedDurationSeconds(since));
            stats.setAvgDurationSeconds(avg == null ? 0.0 : Math.round(avg * 10) / 10.0);
            Long timeoutCount = TenantUtils.executeIgnore(
                    () -> vectorTaskMapper.selectTimeoutCountSince(since));
            Long finishedCount = TenantUtils.executeIgnore(
                    () -> vectorTaskMapper.selectFinishedCountSince(since));
            if (finishedCount != null && finishedCount > 0 && timeoutCount != null) {
                stats.setTimeoutRate24h(Math.round(timeoutCount * 1000.0 / finishedCount) / 10.0);
            }
        } catch (Exception e) {
            log.warn("[collect] 查询任务耗时/超时率失败", e);
        }

        try {
            LocalDateTime oldest = TenantUtils.executeIgnore(
                    () -> vectorTaskMapper.selectOldestPendingQueuedAt());
            if (oldest != null) {
                stats.setOldestPendingMinutes(Math.max(0,
                        Duration.between(oldest, LocalDateTime.now()).toMinutes()));
            }
        } catch (Exception e) {
            log.warn("[collect] 查询最早排队任务失败", e);
        }
        return stats;
    }

    private List<MonitorAlertVO> buildAlerts(MonitorOverviewVO overview) {
        List<MonitorAlertVO> alerts = new ArrayList<>();
        int workerCount = overview.getWorkerCount() == null ? 0 : overview.getWorkerCount();
        if (workerCount == 0) {
            alerts.add(new MonitorAlertVO("WARN", "NO_WORKER",
                    "没有在线的处理 Worker，任务将堆积在队列中（python-vector 可能未启动）"));
        }

        QueueMetricsVO queue = overview.getQueue();
        if (queue != null && queue.getDepth() != null
                && queue.getDepth() > monitorProperties.getQueueDepthThreshold()) {
            alerts.add(new MonitorAlertVO("WARN", "QUEUE_BACKLOG",
                    "队列积压 " + queue.getDepth() + " 条，超过阈值 "
                            + monitorProperties.getQueueDepthThreshold()));
        }

        TaskStatsVO tasks = overview.getTasks();
        if (tasks != null && tasks.getOldestPendingMinutes() != null
                && tasks.getOldestPendingMinutes() > monitorProperties.getPendingStuckMinutes()) {
            alerts.add(new MonitorAlertVO("WARN", "PENDING_STUCK",
                    "最早排队任务已等待 " + tasks.getOldestPendingMinutes() + " 分钟"));
        }
        if (tasks != null && tasks.getTimeoutRate24h() != null
                && tasks.getTimeoutRate24h() > monitorProperties.getTimeoutRateThreshold()) {
            alerts.add(new MonitorAlertVO("WARN", "TIMEOUT_RATE_HIGH",
                    "近 24h 超时率 " + tasks.getTimeoutRate24h() + "%，超过阈值 "
                            + monitorProperties.getTimeoutRateThreshold() + "%"));
        }
        return alerts;
    }

    private LocalDateTime parseEpochSecond(String value) {
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(value)),
                    ZoneId.systemDefault());
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    @Data
    public static class WorkerRegistration {
        private String workerId;
        private String host;
        /** 容器/主机 IP，hostname 无法解析时使用 */
        private String ip;
        private Integer port;
        private Integer concurrency;
        private String version;
        private Long startedAt;
    }

    /**
     * Redis 指标快照（Python publish_snapshot 写入，snake_case）
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkerSnapshot {
        @JsonProperty("inflight")
        private Integer inflight;
        @JsonProperty("completed")
        private Long completed;
        @JsonProperty("failed")
        private Long failed;
        @JsonProperty("cancelled")
        private Long cancelled;
        @JsonProperty("avg_task_seconds")
        private Double avgTaskSeconds;
        @JsonProperty("embedding_success")
        private Long embeddingSuccess;
        @JsonProperty("embedding_error")
        private Long embeddingError;
        @JsonProperty("updated_at")
        private Long updatedAt;
    }
}
