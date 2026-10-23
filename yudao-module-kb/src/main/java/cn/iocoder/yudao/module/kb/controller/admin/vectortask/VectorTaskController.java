package cn.iocoder.yudao.module.kb.controller.admin.vectortask;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.vectortask.VectorTaskDO;
import cn.iocoder.yudao.module.kb.dal.mysql.document.DocumentMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.vectortask.VectorTaskMapper;
import cn.iocoder.yudao.module.kb.enums.VectorTaskStatusEnum;
import cn.iocoder.yudao.module.kb.framework.config.VectorTaskConfig;
import cn.iocoder.yudao.module.kb.service.vectortask.VectorTaskService;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 向量处理任务")
@RestController
@RequestMapping("/kb/vector-task")
@Validated
@Slf4j
public class VectorTaskController {

    @Resource
    private VectorTaskService vectorTaskService;
    @Resource
    private VectorTaskMapper vectorTaskMapper;
    @Resource
    private DocumentMapper documentMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private VectorTaskConfig vectorTaskConfig;

    @PostMapping("/submit")
    @Operation(summary = "提交向量处理任务")
    public CommonResult<String> submitTask(@Valid @RequestBody VectorTaskSubmitReqVO reqVO) {
        return success(vectorTaskService.submitTask(reqVO));
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消向量处理任务")
    @Parameter(name = "taskId", description = "任务ID", required = true)
    public CommonResult<Boolean> cancelTask(@RequestParam("taskId") String taskId) {
        vectorTaskService.cancelTask(taskId);
        return success(true);
    }

    @PostMapping("/retry")
    @Operation(summary = "重试失败文档的向量处理")
    @Parameter(name = "docId", description = "文档ID", required = true)
    public CommonResult<String> retryTask(@RequestParam("docId") Long docId) {
        return success(vectorTaskService.retryTask(docId));
    }

    @GetMapping("/get")
    @Operation(summary = "查询任务详情")
    @Parameter(name = "taskId", description = "任务ID", required = true)
    public CommonResult<VectorTaskRespVO> getTask(@RequestParam("taskId") String taskId) {
        VectorTaskDO task = vectorTaskService.getTaskByTaskId(taskId);
        return success(BeanUtils.toBean(task, VectorTaskRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "查询任务分页")
    public CommonResult<PageResult<VectorTaskRespVO>> getTaskPage(@Valid VectorTaskPageReqVO pageReqVO) {
        PageResult<VectorTaskDO> pageResult = vectorTaskService.getTaskPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, VectorTaskRespVO.class));
    }

    // ========== 超时检测 ==========

    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void checkTimeoutTasks() {
        // 定时任务无 HTTP 上下文，需忽略租户过滤
        TenantUtils.executeIgnore(() -> doCheckTimeoutTasks());
    }

    private void doCheckTimeoutTasks() {
        // 定时任务无 HTTP 上下文，需设置系统用户以供 MyBatis Plus 自动填充 updater 字段
        LoginUser systemUser = new LoginUser();
        systemUser.setId(1L);
        systemUser.setUserType(UserTypeEnum.ADMIN.getValue());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(systemUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ===== 1. 执行超时：只判 PROCESSING，且必须心跳中断 =====
        int execTimeoutMinutes = vectorTaskConfig.getExecTimeoutMinutes();
        LocalDateTime execThreshold = LocalDateTime.now().minusMinutes(execTimeoutMinutes);
        List<VectorTaskDO> processingTimeout = vectorTaskMapper.selectProcessingTimeoutTasks(execThreshold);

        for (VectorTaskDO task : processingTimeout) {
            // Redis 心跳优先：Python 每次 update_state/touch 都会刷新 updated_at
            if (hasFreshRedisHeartbeat(task.getTaskId(), execTimeoutMinutes)) {
                touchHeartbeat(task.getId());
                continue;
            }
            // 终态校验：再次确认 MySQL 中任务不在终态（防止并发修改）
            VectorTaskDO freshTask = vectorTaskMapper.selectById(task.getId());
            if (freshTask == null || VectorTaskStatusEnum.isTerminal(freshTask.getStatus())) {
                continue;
            }
            log.warn("[checkTimeoutTasks] 执行超时: taskId={}, status={}, heartbeatAt={}",
                    task.getTaskId(), task.getStatus(), task.getHeartbeatAt());
            markTimeout(freshTask, "任务执行超时（心跳中断超过 " + execTimeoutMinutes + " 分钟）");
        }

        // ===== 2. 排队超时：PENDING 只告警，不判超时（避免把“排队中”误判为失败） =====
        int queueTimeoutMinutes = vectorTaskConfig.getQueueTimeoutMinutes();
        LocalDateTime queueThreshold = LocalDateTime.now().minusMinutes(queueTimeoutMinutes);
        List<VectorTaskDO> queuedTimeout = vectorTaskMapper.selectQueuedTimeoutTasks(queueThreshold);
        if (!queuedTimeout.isEmpty()) {
            log.warn("[checkTimeoutTasks] 发现 {} 个排队超过 {} 分钟的任务（保持 PENDING，不判超时）: taskIds={}",
                    queuedTimeout.size(), queueTimeoutMinutes,
                    queuedTimeout.stream().map(VectorTaskDO::getTaskId).limit(20).toList());
        }
    }

    /** Redis updated_at 是否仍在执行超时窗口内（Python 侧心跳优先） */
    private boolean hasFreshRedisHeartbeat(String taskId, int execTimeoutMinutes) {
        String redisKey = vectorTaskConfig.getRedisTaskKeyPrefix() + taskId;
        Object updatedAt = stringRedisTemplate.opsForHash().get(redisKey, "updated_at");
        if (updatedAt == null) {
            return false;
        }
        try {
            long ts = Long.parseLong(String.valueOf(updatedAt));
            return System.currentTimeMillis() / 1000 - ts < execTimeoutMinutes * 60L;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** 把 MySQL heartbeat_at 追平，避免下一轮重复扫到 */
    private void touchHeartbeat(Long taskPkId) {
        VectorTaskDO update = new VectorTaskDO();
        update.setId(taskPkId);
        update.setHeartbeatAt(LocalDateTime.now());
        vectorTaskMapper.updateById(update);
    }

    /** 标记执行超时：MySQL + Redis + Document 三端同步 */
    private void markTimeout(VectorTaskDO task, String errorMsg) {
        VectorTaskDO update = new VectorTaskDO();
        update.setId(task.getId());
        update.setStatus(VectorTaskStatusEnum.TIMEOUT.getStatus());
        update.setErrorMsg(errorMsg);
        update.setHeartbeatAt(LocalDateTime.now());
        vectorTaskMapper.updateById(update);

        String redisKey = vectorTaskConfig.getRedisTaskKeyPrefix() + task.getTaskId();
        stringRedisTemplate.opsForHash().put(redisKey, "status", "TIMEOUT");
        stringRedisTemplate.opsForHash().put(redisKey, "error_msg", errorMsg);

        if (task.getDocId() != null && task.getDocId() > 0) {
            DocumentDO docUpdate = new DocumentDO();
            docUpdate.setId(task.getDocId());
            docUpdate.setVectorStatus(VectorTaskStatusEnum.TIMEOUT.getStatus());
            documentMapper.updateById(docUpdate);
        }
    }
}
