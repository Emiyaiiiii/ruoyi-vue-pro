package cn.iocoder.yudao.module.kb.service.vectortask;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo.VectorTaskPageReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo.VectorTaskSubmitReqVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.chunkmethod.ChunkMethodDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.vectortask.VectorTaskDO;
import cn.iocoder.yudao.module.kb.dal.mysql.chunkmethod.ChunkMethodMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.document.DocumentMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.vectortask.VectorTaskMapper;
import cn.iocoder.yudao.module.kb.enums.VectorTaskStatusEnum;
import cn.iocoder.yudao.module.kb.enums.ImageStrategyEnum;
import cn.iocoder.yudao.module.kb.framework.config.VectorTaskConfig;
import cn.iocoder.yudao.module.kb.service.kbconfig.KbProcessConfigResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
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
    @Resource
    private VectorTaskPublisher vectorTaskPublisher;
    @Resource
    private ChunkMethodMapper chunkMethodMapper;
    @Resource
    private LibraryMapper libraryMapper;
    @Resource
    private KbProcessConfigResolver kbProcessConfigResolver;
    @Resource
    private cn.iocoder.yudao.module.infra.api.file.FileApi fileApi;

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

        // 5. 发布解析任务到 RabbitMQ（Python 原生 Worker 消费）
        try {
            vectorTaskPublisher.publish(buildMessage(taskId, reqVO), taskId);
        } catch (Exception e) {
            log.error("[submitTask] 发布解析任务失败: taskId={}, error={}", taskId, e.getMessage(), e);
            markTaskSubmitFailed(task.getId(), taskId, reqVO.getDocId(), redisKey,
                    "发布到 RabbitMQ 失败: " + e.getMessage());
            throw exception(VECTOR_TASK_SUBMIT_FAILED);
        }

        return taskId;
    }

    @Override
    public boolean deleteDocumentVectors(Long docId, Long kbId) {
        String url = vectorTaskConfig.getPythonServiceUrl()
                + "/api/v1/documents/" + docId + "?kb_id=" + kbId;
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url, HttpMethod.DELETE, entity, Map.class);
            log.info("[deleteDocumentVectors] 已通知 Python 清理向量: docId={}, kbId={}, resp={}",
                    docId, kbId, resp.getBody());
            return resp.getBody() != null && Integer.valueOf(0).equals(resp.getBody().get("code"));
        } catch (Exception e) {
            log.error("[deleteDocumentVectors] 清理向量失败（不影响文档删除）: docId={}, kbId={}, error={}",
                    docId, kbId, e.getMessage());
            return false;
        }
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

        // 1. 调用 Python 服务取消任务
        try {
            String pythonUrl = vectorTaskConfig.getPythonServiceUrl()
                    + "/api/v1/tasks/" + taskId;
            restTemplate.delete(pythonUrl);
            log.info("[cancelTask] 已通知 Python 取消任务: taskId={}", taskId);
        } catch (Exception e) {
            log.warn("[cancelTask] 通知 Python 取消任务失败（继续取消本地状态）: taskId={}", taskId, e);
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
    public void cancelActiveTaskByDocId(Long docId) {
        VectorTaskDO task = vectorTaskMapper.selectActiveByDocId(docId);
        if (task == null || StrUtil.isBlank(task.getTaskId())) {
            return;
        }
        try {
            cancelTask(task.getTaskId());
            log.info("[cancelActiveTaskByDocId] 已取消文档在途向量任务: docId={}, taskId={}", docId, task.getTaskId());
        } catch (Exception e) {
            log.warn("[cancelActiveTaskByDocId] 取消文档在途任务失败（不影响删除）: docId={}, taskId={}, error={}",
                    docId, task.getTaskId(), e.getMessage());
        }
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
        // 3. 构建提交参数，复用文档的文件信息；file_url 重新签名（7 天），避免复用上传时的旧签名过期
        VectorTaskSubmitReqVO reqVO = new VectorTaskSubmitReqVO();
        reqVO.setDocId(docId);
        reqVO.setKbId(doc.getKbId());
        reqVO.setFileUrl(fileApi.presignGetUrl(doc.getFilePath(), 604800));
        reqVO.setFileType(doc.getFileType());
        // 4. 提交新任务（submitTask 内部会创建新的任务记录并调用 Python）
        log.info("[retryTask] 重试文档向量处理: docId={}, filePath={}", docId, doc.getFilePath());
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
        try {
            updateDO.setChunkCount(chunkCount);
        } catch (Exception e) {
            log.warn("[handleTaskCallback] chunkCount 设置失败", e);
        }
        vectorTaskMapper.updateById(updateDO);
    }

    // ========== 内部方法 ==========

    /**
     * 组装发布到 RabbitMQ 的解析任务消息体（对齐 python-vector ingest.task.process_ingest_task）
     */
    private Map<String, Object> buildMessage(String taskId, VectorTaskSubmitReqVO reqVO) {
        Map<String, Object> message = new HashMap<>();
        message.put("task_id", taskId);
        message.put("doc_id", reqVO.getDocId());
        message.put("kb_id", reqVO.getKbId());
        message.put("file_url", reqVO.getFileUrl());
        message.put("file_type", reqVO.getFileType());
        message.put("tenant_id", requiredContext(TenantContextHolder.getTenantId()));
        message.put("user_id", requiredContext(SecurityFrameworkUtils.getLoginUserId()));
        message.put("document_title", reqVO.getDocumentTitle());
        message.put("chunking_config", buildChunkingConfig(reqVO));
        return message;
    }

    private Object requiredContext(Object value) {
        return value != null ? value : "";
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
        } else if ("CANCELLED".equals(status)) {
            return VectorTaskStatusEnum.CANCELLED.getStatus();
        }
        return null;
    }

    private String buildParamsJson(VectorTaskSubmitReqVO reqVO) {
        return JsonUtils.toJsonString(buildChunkingConfig(reqVO));
    }

    private Map<String, Object> buildChunkingConfig(VectorTaskSubmitReqVO reqVO) {
        // 任务显式传入的切片参数（最高优先）
        Map<String, Object> taskParams = new HashMap<>();
        if (reqVO.getStrategy() != null) {
            taskParams.put("strategy", reqVO.getStrategy());
        }
        if (reqVO.getChunkSize() != null) {
            taskParams.put("chunk_size", reqVO.getChunkSize());
        }
        if (reqVO.getChunkOverlap() != null) {
            taskParams.put("chunk_overlap", reqVO.getChunkOverlap());
        }
        if (reqVO.getMinChunkSize() != null) {
            taskParams.put("min_chunk_size", reqVO.getMinChunkSize());
        }
        if (reqVO.getMaxChunkSize() != null) {
            taskParams.put("max_chunk_size", reqVO.getMaxChunkSize());
        }
        // 三级合并：任务参数 > KB 级 process_config.chunking > kb_chunk_method 默认 > 内置默认
        Map<String, Object> config = new HashMap<>(kbProcessConfigResolver.resolveChunkingConfig(reqVO.getKbId(), taskParams));
        // 模型配置不再随任务消息下发：两级优先级（KB 显式 > 租户默认）由 KbProcessConfigResolver 合并后
        // 经 kb.process.config.changed 推送至 python-vector Redis 缓存（kb:process:config:{kb_id}），
        // 入库/检索均从该缓存读取（KB 无配置时 python 回退 env 默认）。见 KbProcessConfigResolver.publishKbConfig。
        if (reqVO.getIndexGroup() != null) {
            config.put("index_group", reqVO.getIndexGroup());
        }
        if (StrUtil.isNotBlank(reqVO.getParseOptions())) {
            Map<String, Object> parseOptions = JsonUtils.parseMap(reqVO.getParseOptions());
            if (parseOptions != null) {
                config.put("parse_options", parseOptions);
            }
        }
        // 图片处理方案：任务参数 > KB 级 process_config.chunking.image_strategy > kb_library.image_strategy(旧字段)
        // > kb_chunk_method 默认参数；全部缺失时回退空串（python 端按存量布尔推断）。
        // resolver 已处理 KB 级与全局默认，这里仅在未合并到时补旧字段兜底。
        Object mergedImageStrategy = config.get("image_strategy");
        if (mergedImageStrategy == null || StrUtil.isBlank(String.valueOf(mergedImageStrategy))) {
            config.put("image_strategy", resolveLegacyImageStrategy(reqVO.getKbId()));
        }
        return config;
    }

    // ==================== 模型配置解析（请求体优先，读库默认兜底） ====================

    /**
     * 读取 kb_chunk_method 默认方法的 defaultParameters 作为切片兜底参数；
     * 无默认方法时返回空 Map，交由 python-vector 本地缺省值兜底。
     */
    private Map<String, Object> resolveDefaultChunkMethodConfig() {
        ChunkMethodDO method = chunkMethodMapper.selectDefaultMethod();
        if (method == null || StrUtil.isBlank(method.getDefaultParameters())) {
            return new HashMap<>();
        }
        Map<String, Object> params = JsonUtils.parseMap(method.getDefaultParameters());
        return params != null ? params : new HashMap<>();
    }

    /**
     * 解析图片处理方案的旧字段兜底（kb_library.image_strategy 存量字段）。
     *
     * <p>KB 级 process_config.chunking.image_strategy 已由 KbProcessConfigResolver 合并；
     * 此处仅在未合并到时回退旧库字段：kb_library.image_strategy > kb_chunk_method 默认
     * default_parameters.image_strategy > 空字符串（python 端按存量布尔推断）。
     */
    private String resolveLegacyImageStrategy(Long kbId) {
        // 1. 旧库字段：kb_library.image_strategy
        if (kbId != null) {
            LibraryDO lib = libraryMapper.selectById(kbId);
            if (lib != null && StrUtil.isNotBlank(lib.getImageStrategy())
                    && ImageStrategyEnum.getByCode(lib.getImageStrategy()) != null) {
                return lib.getImageStrategy();
            }
        }
        // 2. 全局缺省（kb_chunk_method 默认方法 default_parameters.image_strategy）
        Map<String, Object> def = resolveDefaultChunkMethodConfig();
        Object global = def.get("image_strategy");
        if (global != null && ImageStrategyEnum.getByCode(String.valueOf(global)) != null) {
            return String.valueOf(global);
        }
        // 3. 未显式指定，走存量布尔推断
        return "";
    }
}
