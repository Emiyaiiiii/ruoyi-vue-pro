package cn.iocoder.yudao.module.kb.service.vectortask;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo.VectorTaskPageReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo.VectorTaskSubmitReqVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.vectortask.VectorTaskDO;
import cn.iocoder.yudao.module.kb.dal.mysql.document.DocumentMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.vectortask.VectorTaskMapper;
import cn.iocoder.yudao.module.kb.enums.VectorTaskStatusEnum;
import cn.iocoder.yudao.module.kb.framework.config.VectorTaskConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 向量处理任务 Service 实现类
 */
@Service
@Validated
@Slf4j
public class VectorTaskServiceImpl implements VectorTaskService {

    @Resource
    private VectorTaskMapper vectorTaskMapper;
    @Resource
    private DocumentMapper documentMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private VectorTaskConfig vectorTaskConfig;

    @Override
    public String submitTask(VectorTaskSubmitReqVO reqVO) {
        // 1. 生成唯一任务ID
        String taskId = IdUtil.fastSimpleUUID();

        // 2. 构建处理参数 JSON
        String params = buildParamsJson(reqVO);

        // 3. 创建任务记录（MySQL）
        VectorTaskDO task = new VectorTaskDO();
        task.setTaskId(taskId);
        task.setDocId(reqVO.getDocId());
        task.setKbId(reqVO.getKbId());
        task.setFileUrl(reqVO.getFileUrl());
        task.setFileType(reqVO.getFileType());
        task.setStatus(VectorTaskStatusEnum.PENDING.getStatus());
        task.setProgress(0);
        task.setCurrentStep("");
        task.setChunkCount(0);
        task.setCeleryTaskId("");
        task.setErrorMsg("");
        task.setParams(params);
        vectorTaskMapper.insert(task);

        // 3.5 同步更新文档的向量处理状态
        DocumentDO docUpdate = new DocumentDO();
        docUpdate.setId(reqVO.getDocId());
        docUpdate.setVectorTaskId(taskId);
        docUpdate.setVectorStatus(VectorTaskStatusEnum.PENDING.getStatus());
        documentMapper.updateById(docUpdate);

        // 4. 初始化 Redis 状态（Python 端可读取）
        String redisKey = vectorTaskConfig.getRedisTaskKeyPrefix() + taskId;
        Map<String, String> stateMap = new HashMap<>();
        stateMap.put("task_id", taskId);
        stateMap.put("doc_id", String.valueOf(reqVO.getDocId()));
        stateMap.put("kb_id", String.valueOf(reqVO.getKbId()));
        stateMap.put("status", "PENDING");
        stateMap.put("progress", "0");
        stateMap.put("step", "");
        stateMap.put("error_msg", "");
        stateMap.put("created_at", String.valueOf(System.currentTimeMillis() / 1000));
        // 存储租户ID和用户ID，供事件消费者恢复上下文
        stateMap.put("tenant_id", String.valueOf(TenantContextHolder.getTenantId()));
        stateMap.put("user_id", String.valueOf(SecurityFrameworkUtils.getLoginUserId()));
        stringRedisTemplate.opsForHash().putAll(redisKey, stateMap);
        stringRedisTemplate.expire(redisKey, Duration.ofHours(24));

        // 5. 调用 Python 服务提交任务（带重试）
        String celeryTaskId = callPythonWithRetry(taskId, reqVO);
        if (celeryTaskId == null) {
            // 重试全部失败，标记为提交失败并抛出异常
            markTaskSubmitFailed(task.getId(), taskId, reqVO.getDocId(), redisKey,
                    "调用Python服务失败（已重试" + vectorTaskConfig.getMaxRetries() + "次）");
            throw exception(VECTOR_TASK_SUBMIT_FAILED);
        }

        // 6. 保存 celeryTaskId（用于后续取消任务）
        VectorTaskDO updateCelery = new VectorTaskDO();
        updateCelery.setId(task.getId());
        updateCelery.setCeleryTaskId(celeryTaskId);
        vectorTaskMapper.updateById(updateCelery);

        return taskId;
    }

    @Override
    public void cancelTask(String taskId) {
        VectorTaskDO task = vectorTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            throw exception(VECTOR_TASK_NOT_EXISTS);
        }
        // 只有 PENDING 和 PROCESSING 状态的任务才能取消
        if (VectorTaskStatusEnum.isTerminal(task.getStatus())) {
            throw exception(VECTOR_TASK_ALREADY_FINISHED);
        }

        // 1. 调用 Python 服务取消 Celery 任务
        if (task.getCeleryTaskId() != null && !task.getCeleryTaskId().isEmpty()) {
            try {
                String pythonUrl = vectorTaskConfig.getPythonServiceUrl()
                        + "/api/v1/tasks/" + taskId;
                restTemplate.delete(pythonUrl);
                log.info("[cancelTask] 已通知 Python 取消任务: taskId={}, celeryTaskId={}",
                        taskId, task.getCeleryTaskId());
            } catch (Exception e) {
                log.warn("[cancelTask] 通知 Python 取消任务失败（继续取消本地状态）: taskId={}", taskId, e);
            }
        }

        // 2. 更新 MySQL 任务状态
        VectorTaskDO updateCancel = new VectorTaskDO();
        updateCancel.setId(task.getId());
        updateCancel.setStatus(VectorTaskStatusEnum.CANCELLED.getStatus());
        updateCancel.setErrorMsg("用户取消任务");
        vectorTaskMapper.updateById(updateCancel);

        // 3. 同步更新文档状态
        if (task.getDocId() != null && task.getDocId() > 0) {
            DocumentDO docUpdate = new DocumentDO();
            docUpdate.setId(task.getDocId());
            docUpdate.setVectorStatus(VectorTaskStatusEnum.CANCELLED.getStatus());
            documentMapper.updateById(docUpdate);
        }

        // 4. 更新 Redis 状态
        String redisKey = vectorTaskConfig.getRedisTaskKeyPrefix() + taskId;
        stringRedisTemplate.opsForHash().put(redisKey, "status", "CANCELLED");
        stringRedisTemplate.opsForHash().put(redisKey, "error_msg", "用户取消任务");
    }

    @Override
    public String retryTask(Long docId) {
        // 1. 查询文档信息
        DocumentDO doc = documentMapper.selectById(docId);
        if (doc == null) {
            throw exception(VECTOR_TASK_NOT_EXISTS);
        }
        // 2. 校验：只有失败类终态才允许重试（FAILED / SUBMIT_FAILED / TIMEOUT / CANCELLED）
        //    已完成(COMPLETED)不允许重试，正在处理中/待处理也不允许
        Integer vectorStatus = doc.getVectorStatus();
        if (vectorStatus == null
                || vectorStatus.equals(VectorTaskStatusEnum.COMPLETED.getStatus())
                || !VectorTaskStatusEnum.isTerminal(vectorStatus)) {
            log.warn("[retryTask] 文档向量任务不允许重试: docId={}, vectorStatus={}", docId, vectorStatus);
            throw exception(VECTOR_TASK_ALREADY_FINISHED);
        }
        // 3. 构建提交参数，复用文档的文件信息
        VectorTaskSubmitReqVO reqVO = new VectorTaskSubmitReqVO();
        reqVO.setDocId(docId);
        reqVO.setKbId(doc.getKbId());
        reqVO.setFileUrl(doc.getFileUrl());
        reqVO.setFileType(doc.getFileType());
        // 4. 提交新任务（submitTask 内部会创建新的任务记录并调用 Python）
        log.info("[retryTask] 重试文档向量处理: docId={}, fileUrl={}", docId, doc.getFileUrl());
        return submitTask(reqVO);
    }

    @Override
    public VectorTaskDO getTaskByTaskId(String taskId) {
        return vectorTaskMapper.selectByTaskId(taskId);
    }

    @Override
    public PageResult<VectorTaskDO> getTaskPage(VectorTaskPageReqVO pageReqVO) {
        return vectorTaskMapper.selectPage(pageReqVO);
    }

    @Override
    public void handleTaskCallback(String taskId, String status, Integer progress,
                                   String step, Integer chunkCount, String errorMsg) {
        VectorTaskDO task = vectorTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            log.warn("[handleTaskCallback] 任务不存在: taskId={}", taskId);
            return;
        }
        // 终态校验：已结束的任务不接受回调
        if (VectorTaskStatusEnum.isTerminal(task.getStatus())) {
            log.warn("[handleTaskCallback] 任务已处于终态，忽略回调: taskId={}, currentStatus={}, callbackStatus={}",
                    taskId, task.getStatus(), status);
            return;
        }

        VectorTaskDO updateDO = new VectorTaskDO();
        updateDO.setId(task.getId());
        updateDO.setStatus(mapStatus(status));
        if (progress != null) {
            updateDO.setProgress(progress);
        }
        if (step != null) {
            updateDO.setCurrentStep(step);
        }
        if (chunkCount != null) {
            updateDO.setChunkCount(chunkCount);
        }
        if (errorMsg != null) {
            updateDO.setErrorMsg(errorMsg);
        }
        vectorTaskMapper.updateById(updateDO);
    }

    // ========== 内部方法 ==========

    /**
     * 带重试的 Python 服务调用
     *
     * @return celeryTaskId，全部重试失败返回 null
     */
    private String callPythonWithRetry(String taskId, VectorTaskSubmitReqVO reqVO) {
        String pythonUrl = vectorTaskConfig.getPythonServiceUrl() + "/api/v1/tasks/submit";
        int maxRetries = vectorTaskConfig.getMaxRetries();
        int retryDelayMs = vectorTaskConfig.getRetryDelayMs();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("task_id", taskId);
                requestBody.put("doc_id", reqVO.getDocId());
                requestBody.put("kb_id", reqVO.getKbId());
                requestBody.put("file_url", reqVO.getFileUrl());
                requestBody.put("file_type", reqVO.getFileType());
                requestBody.put("params", buildPythonParams(reqVO));

                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.postForObject(pythonUrl, requestBody, Map.class);
                log.debug("[submitTask] Python 服务返回: taskId={}, attempt={}, response={}", taskId, attempt, response);

                // 校验 Python 返回的业务状态码
                if (response == null) {
                    log.warn("[submitTask] Python 服务返回空响应: taskId={}, attempt={}", taskId, attempt);
                } else {
                    Object code = response.get("code");
                    if (code != null && Integer.parseInt(String.valueOf(code)) != 0) {
                        String message = String.valueOf(response.getOrDefault("message", "unknown error"));
                        log.error("[submitTask] Python 服务返回业务错误: taskId={}, code={}, message={}", taskId, code, message);
                        // 业务错误不重试（参数问题等），直接返回 null
                        return null;
                    }
                    // 提取 celeryTaskId
                    Object celeryId = response.get("celery_task_id");
                    if (celeryId != null) {
                        return String.valueOf(celeryId);
                    }
                    log.warn("[submitTask] Python 响应中缺少 celery_task_id: taskId={}", taskId);
                }
            } catch (Exception e) {
                log.warn("[submitTask] 调用 Python 服务失败: taskId={}, attempt={}/{}, error={}",
                        taskId, attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 标记任务提交失败（MySQL + Redis + Document 三端同步）
     */
    private void markTaskSubmitFailed(Long taskPkId, String taskId, Long docId,
                                      String redisKey, String errorMsg) {
        log.error("[submitTask] 任务提交最终失败: taskId={}, error={}", taskId, errorMsg);
        VectorTaskDO updateFail = new VectorTaskDO();
        updateFail.setId(taskPkId);
        updateFail.setStatus(VectorTaskStatusEnum.SUBMIT_FAILED.getStatus());
        updateFail.setErrorMsg(errorMsg);
        vectorTaskMapper.updateById(updateFail);
        stringRedisTemplate.opsForHash().put(redisKey, "status", "SUBMIT_FAILED");
        stringRedisTemplate.opsForHash().put(redisKey, "error_msg", errorMsg);
        DocumentDO docFailUpdate = new DocumentDO();
        docFailUpdate.setId(docId);
        docFailUpdate.setVectorStatus(VectorTaskStatusEnum.SUBMIT_FAILED.getStatus());
        documentMapper.updateById(docFailUpdate);
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

    private String buildParamsJson(VectorTaskSubmitReqVO reqVO) {
        StringBuilder sb = new StringBuilder("{");
        if (reqVO.getChunkSize() != null) {
            sb.append("\"chunk_size\":").append(reqVO.getChunkSize());
        }
        if (reqVO.getChunkOverlap() != null) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"chunk_overlap\":").append(reqVO.getChunkOverlap());
        }
        if (reqVO.getEmbeddingModel() != null) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"embedding_model\":\"").append(reqVO.getEmbeddingModel()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private Map<String, Object> buildPythonParams(VectorTaskSubmitReqVO reqVO) {
        Map<String, Object> params = new HashMap<>();
        if (reqVO.getChunkSize() != null) {
            params.put("chunk_size", reqVO.getChunkSize());
        }
        if (reqVO.getChunkOverlap() != null) {
            params.put("chunk_overlap", reqVO.getChunkOverlap());
        }
        if (reqVO.getEmbeddingModel() != null) {
            params.put("embedding_model", reqVO.getEmbeddingModel());
        }
        return params;
    }
}
