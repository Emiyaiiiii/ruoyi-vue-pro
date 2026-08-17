package cn.iocoder.yudao.module.kb.service.vectortask;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.infra.api.websocket.WebSocketSenderApi;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.vectortask.VectorTaskDO;
import cn.iocoder.yudao.module.kb.dal.mysql.document.DocumentMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.vectortask.VectorTaskMapper;
import cn.iocoder.yudao.module.kb.enums.VectorTaskStatusEnum;
import cn.iocoder.yudao.module.kb.framework.config.VectorTaskConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream 事件消费者
 *
 * 消费 Python 端写入的 kb:task:events 事件流，
 * 更新 MySQL 任务状态，并通过 WebSocket 推送给前端
 *
 * 消费模式：简单 Stream 读取（非 Consumer Group），通过手动 lastId 追踪消费位点。
 * 选择简单模式的原因：
 *   1. 当前只有一个 Java 消费者实例，不需要 Consumer Group 的多消费者分发能力
 *   2. 避免了 Consumer Group 内部位点与手动 lastId 冲突导致的消息丢失/重复问题
 *   3. 事件内容为状态更新，天然幂等，即使短暂重复也不影响数据正确性
 */
@Component
@Slf4j
public class VectorTaskRedisEventConsumer {

    private static final String LAST_ID_KEY = "kb:vector:consumer:java:last_id";

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private VectorTaskConfig vectorTaskConfig;
    @Resource
    private VectorTaskMapper vectorTaskMapper;
    @Resource
    private DocumentMapper documentMapper;
    @Resource
    private WebSocketSenderApi webSocketSenderApi;

    private volatile boolean running = true;
    private Thread consumerThread;

    @PostConstruct
    public void start() {
        consumerThread = new Thread(this::consumeLoop, "vector-task-event-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
        log.info("[VectorTaskRedisEventConsumer] 事件消费者已启动, streamKey={}",
                vectorTaskConfig.getRedisStreamKey());
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
        log.info("[VectorTaskRedisEventConsumer] 事件消费者已停止");
    }

    private void consumeLoop() {
        // 等待 Redis 就绪
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        String streamKey = vectorTaskConfig.getRedisStreamKey();

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                String lastId = getLastId();

                // 简单模式读取：从 lastId 之后读取新事件，阻塞 2 秒
                // 不使用 Consumer Group，避免位点冲突
                List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                        StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                        StreamOffset.create(streamKey, ReadOffset.from(lastId))
                );

                if (records != null && !records.isEmpty()) {
                    for (MapRecord<String, Object, Object> record : records) {
                        processEvent(record);
                        // 更新消费位点（处理成功后才更新，保证至少处理一次）
                        saveLastId(record.getId().getValue());
                    }
                }
            } catch (Exception e) {
                log.error("[VectorTaskRedisEventConsumer] 消费异常", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 处理单条事件：从 Redis Hash 读取 tenant_id，恢复租户上下文后更新 MySQL + 推送 WebSocket
     */
    private void processEvent(MapRecord<String, Object, Object> record) {
        Map<Object, Object> fields = record.getValue();
        String taskId = String.valueOf(fields.get("task_id"));
        String status = String.valueOf(fields.get("status"));
        String progressStr = String.valueOf(fields.get("progress"));
        String step = String.valueOf(fields.get("step"));
        String chunkCountStr = String.valueOf(fields.get("chunk_count"));
        String errorMsg = String.valueOf(fields.get("error_msg"));

        log.debug("[VectorTaskRedisEventConsumer] 收到事件: taskId={}, status={}, progress={}", taskId, status, progressStr);

        int progressParsed = 0;
        try {
            progressParsed = Integer.parseInt(progressStr);
        } catch (NumberFormatException ignored) {
        }
        Integer chunkCountParsed = null;
        try {
            chunkCountParsed = Integer.parseInt(chunkCountStr);
        } catch (NumberFormatException ignored) {
        }
        // 创建 final 引用供 lambda 使用
        final int progress = progressParsed;
        final Integer chunkCount = chunkCountParsed;

        // 从 Redis Hash 读取租户ID和用户ID，恢复上下文
        String redisKey = vectorTaskConfig.getRedisTaskKeyPrefix() + taskId;
        Object tenantIdObj = stringRedisTemplate.opsForHash().get(redisKey, "tenant_id");
        Object userIdObj = stringRedisTemplate.opsForHash().get(redisKey, "user_id");
        Long tenantId = null;
        Long userId = null;
        if (tenantIdObj != null && !"null".equals(String.valueOf(tenantIdObj))) {
            try {
                tenantId = Long.parseLong(String.valueOf(tenantIdObj));
            } catch (NumberFormatException e) {
                log.warn("[VectorTaskRedisEventConsumer] tenant_id 解析失败: taskId={}, tenantId={}", taskId, tenantIdObj);
            }
        }
        if (userIdObj != null && !"null".equals(String.valueOf(userIdObj))) {
            try {
                userId = Long.parseLong(String.valueOf(userIdObj));
            } catch (NumberFormatException e) {
                log.warn("[VectorTaskRedisEventConsumer] user_id 解析失败: taskId={}, userId={}", taskId, userIdObj);
            }
        }

        // 创建 final 引用供 lambda 使用
        final Long finalTenantId = tenantId;
        final Long finalUserId = userId;

        if (finalTenantId != null && finalUserId != null) {
            // 有租户ID和用户ID：恢复租户和用户上下文
            TenantUtils.execute(finalTenantId, () -> {
                // 创建 LoginUser 对象并直接设置到 SecurityContextHolder（绕过 HttpServletRequest）
                LoginUser loginUser = new LoginUser();
                loginUser.setId(finalUserId);
                loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
                setSecurityContext(loginUser);
                // 处理事件
                doProcessEvent(taskId, status, progress, step, chunkCount, errorMsg);
            });
        } else if (finalTenantId != null) {
            // 仅有租户ID：恢复租户上下文，使用系统用户
            log.warn("[VectorTaskRedisEventConsumer] 任务缺少 user_id，使用系统用户: taskId={}", taskId);
            TenantUtils.execute(finalTenantId, () -> {
                LoginUser systemUser = new LoginUser();
                systemUser.setId(1L); // 使用系统管理员ID
                systemUser.setUserType(UserTypeEnum.ADMIN.getValue());
                setSecurityContext(systemUser);
                doProcessEvent(taskId, status, progress, step, chunkCount, errorMsg);
            });
        } else {
            // 无租户ID（历史数据或异常情况）：绕过租户过滤作为兜底
            log.warn("[VectorTaskRedisEventConsumer] 任务无 tenant_id，绕过租户过滤: taskId={}", taskId);
            TenantUtils.executeIgnore(() -> {
                LoginUser systemUser = new LoginUser();
                systemUser.setId(1L); // 使用系统管理员ID
                systemUser.setUserType(UserTypeEnum.ADMIN.getValue());
                setSecurityContext(systemUser);
                doProcessEvent(taskId, status, progress, step, chunkCount, errorMsg);
            });
        }
    }

    /**
     * 实际处理事件逻辑（在忽略租户过滤的上下文中执行）
     */
    private void doProcessEvent(String taskId, String status, int progress,
                                 String step, Integer chunkCount, String errorMsg) {
        // 0. 查询当前任务状态
        VectorTaskDO task = vectorTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            log.warn("[VectorTaskRedisEventConsumer] 任务不存在: taskId={}", taskId);
            return;
        }

        // 1. 状态机校验：终态不可被覆盖
        if (VectorTaskStatusEnum.isTerminal(task.getStatus())) {
            log.debug("[VectorTaskRedisEventConsumer] 任务已处于终态，跳过事件: taskId={}, currentStatus={}, eventStatus={}",
                    taskId, task.getStatus(), status);
            return;
        }

        // 2. 更新 MySQL
        Integer newStatus = mapStatus(status);
        VectorTaskDO updateDO = new VectorTaskDO();
        updateDO.setId(task.getId());
        updateDO.setStatus(newStatus);
        updateDO.setProgress(progress);
        updateDO.setCurrentStep(step);
        if (chunkCount != null) {
            updateDO.setChunkCount(chunkCount);
        }
        if (errorMsg != null && !"null".equals(errorMsg) && !errorMsg.isEmpty()) {
            updateDO.setErrorMsg(errorMsg);
        }
        vectorTaskMapper.updateById(updateDO);

        // 3. 同步更新文档的向量处理状态
        if (task.getDocId() != null && task.getDocId() > 0 && newStatus != null) {
            DocumentDO docUpdate = new DocumentDO();
            docUpdate.setId(task.getDocId());
            docUpdate.setVectorStatus(newStatus);
            documentMapper.updateById(docUpdate);
        }

        // 4. 推送 WebSocket（所有状态变更都推送，前端根据 taskId 过滤）
        Map<String, Object> wsMessage = new HashMap<>();
        wsMessage.put("taskId", taskId);
        wsMessage.put("status", status);
        wsMessage.put("progress", progress);
        wsMessage.put("step", step);
        wsMessage.put("chunkCount", chunkCount);
        wsMessage.put("errorMsg", errorMsg);

        webSocketSenderApi.sendObject(UserTypeEnum.ADMIN.getValue(),
                "vector-task-status", wsMessage);
    }

    /**
     * 将 Python 端状态字符串映射为 Java 枚举值
     */
    private Integer mapStatus(String status) {
        if ("PROCESSING".equals(status)) {
            return VectorTaskStatusEnum.PROCESSING.getStatus();
        } else if ("COMPLETED".equals(status)) {
            return VectorTaskStatusEnum.COMPLETED.getStatus();
        } else if ("FAILED".equals(status)) {
            return VectorTaskStatusEnum.FAILED.getStatus();
        }
        return null;
    }

    private String getLastId() {
        String lastId = stringRedisTemplate.opsForValue().get(LAST_ID_KEY);
        return (lastId != null && !lastId.isEmpty()) ? lastId : "0";
    }

    private void saveLastId(String id) {
        stringRedisTemplate.opsForValue().set(LAST_ID_KEY, id);
    }

    /**
     * 在后台线程中设置 SecurityContext（不依赖 HttpServletRequest）
     *
     * 直接创建 UsernamePasswordAuthenticationToken 并设置到 SecurityContextHolder，
     * 避免 WebAuthenticationDetailsSource 尝试访问 null request 导致 NPE
     */
    private void setSecurityContext(LoginUser loginUser) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
