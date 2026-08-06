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

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

        int timeoutMinutes = vectorTaskConfig.getTaskTimeoutMinutes();
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);

        List<Integer> checkStatuses = Arrays.asList(
                VectorTaskStatusEnum.PENDING.getStatus(),
                VectorTaskStatusEnum.PROCESSING.getStatus()
        );
        List<VectorTaskDO> timeoutTasks = vectorTaskMapper.selectTimeoutTasks(checkStatuses, threshold);

        for (VectorTaskDO task : timeoutTasks) {
            // 双重确认：检查 Redis 中的最新状态
            String redisKey = vectorTaskConfig.getRedisTaskKeyPrefix() + task.getTaskId();
            Map<Object, Object> redisState = stringRedisTemplate.opsForHash().entries(redisKey);
            String redisStatus = redisState.isEmpty() ? null : String.valueOf(redisState.get("status"));

            // 如果 Redis 中也是未完成状态（或 Redis 无数据），则标记为超时
            if (redisStatus == null || "PENDING".equals(redisStatus) || "PROCESSING".equals(redisStatus)) {
                // 终态校验：再次确认 MySQL 中任务不在终态（防止并发修改）
                VectorTaskDO freshTask = vectorTaskMapper.selectById(task.getId());
                if (freshTask == null || VectorTaskStatusEnum.isTerminal(freshTask.getStatus())) {
                    continue;
                }

                log.warn("[checkTimeoutTasks] 任务超时: taskId={}, status={}, lastUpdate={}",
                        task.getTaskId(), task.getStatus(), task.getUpdateTime());
                VectorTaskDO updateTimeout = new VectorTaskDO();
                updateTimeout.setId(task.getId());
                updateTimeout.setStatus(VectorTaskStatusEnum.TIMEOUT.getStatus());
                updateTimeout.setErrorMsg("任务处理超时（" + timeoutMinutes + "分钟）");
                vectorTaskMapper.updateById(updateTimeout);
                // 同步更新文档状态
                if (task.getDocId() != null && task.getDocId() > 0) {
                    DocumentDO docUpdate = new DocumentDO();
                    docUpdate.setId(task.getDocId());
                    docUpdate.setVectorStatus(VectorTaskStatusEnum.TIMEOUT.getStatus());
                    documentMapper.updateById(docUpdate);
                }
            }
        }
    }
}
