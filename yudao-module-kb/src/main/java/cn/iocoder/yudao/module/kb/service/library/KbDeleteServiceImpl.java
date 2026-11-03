package cn.iocoder.yudao.module.kb.service.library;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.sharedept.ShareDeptDO;
import cn.iocoder.yudao.module.kb.dal.mysql.document.DocumentMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.folder.FolderMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.sharedept.ShareDeptMapper;
import cn.iocoder.yudao.module.kb.service.document.DocumentService;
import cn.iocoder.yudao.module.kb.service.kbconfig.KbProcessConfigResolver;
import cn.iocoder.yudao.module.kb.service.libraryext.LibraryExtService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 知识库异步删除实现
 *
 * <p>流程：权限校验 → 逐文档删除（每篇清理向量/切片/图片/源文件/Wiki 引用）→
 * 收尾清理（派生数据、修订、文件夹、共享部门、自定义字段）→ 删库 → 发布配置删除。</p>
 */
@Service
@Slf4j
public class KbDeleteServiceImpl implements KbDeleteService {

    private static final String KEY_PREFIX = "kb:delete:task:";
    private static final Duration TTL = Duration.ofHours(24);

    @Resource
    private LibraryService libraryService;
    @Resource
    private DocumentService documentService;
    @Resource
    private DocumentMapper documentMapper;
    @Resource
    private FolderMapper folderMapper;
    @Resource
    private ShareDeptMapper shareDeptMapper;
    @Resource
    private LibraryMapper libraryMapper;
    @Resource
    private LibraryExtService libraryExtService;
    @Resource
    private KbProcessConfigResolver kbProcessConfigResolver;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 删除任务执行器：删除是重 IO 操作，串行执行即可，避免同时压垮 python-vector / 对象存储。
     */
    private final ExecutorService deleteExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "kb-delete-worker");
        thread.setDaemon(true);
        return thread;
    });

    /** 同一知识库只允许一个删除任务，防止重复点击 */
    private final Set<Long> runningKbIds = ConcurrentHashMap.newKeySet();

    // ==================== 对外接口 ====================

    @Override
    public Map<String, Object> deleteLibraryAsync(Long kbId) {
        validateManagePermission(kbId);
        if (!runningKbIds.add(kbId)) {
            throw new ServiceException(KB_DELETE_FAILED.getCode(), "该知识库正在删除中，请稍候");
        }
        String taskId = IdUtil.fastSimpleUUID();
        start(taskId, 1);
        try {
            deleteExecutor.submit(() -> runDelete(taskId, Collections.singletonList(kbId)));
        } catch (RuntimeException e) {
            runningKbIds.remove(kbId);
            fail(taskId, "删除任务提交失败：" + e.getMessage(), null);
            throw e;
        }
        return taskResult(taskId);
    }

    @Override
    public Map<String, Object> deleteLibraryListAsync(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw exception(KB_DELETE_FAILED);
        }
        // 先全部校验，避免删一半才发现无权限
        for (Long id : ids) {
            validateManagePermission(id);
        }
        List<Long> locked = new ArrayList<>();
        for (Long id : ids) {
            if (!runningKbIds.add(id)) {
                runningKbIds.removeAll(locked);
                throw new ServiceException(KB_DELETE_FAILED.getCode(),
                        "知识库 #" + id + " 正在删除中，请稍候");
            }
            locked.add(id);
        }
        String taskId = IdUtil.fastSimpleUUID();
        start(taskId, ids.size());
        try {
            deleteExecutor.submit(() -> runDelete(taskId, ids));
        } catch (RuntimeException e) {
            runningKbIds.removeAll(ids);
            fail(taskId, "删除任务提交失败：" + e.getMessage(), null);
            throw e;
        }
        return taskResult(taskId);
    }

    @Override
    public Map<String, Object> getTaskProgress(String taskId) {
        Map<String, Object> progress = readProgress(taskId);
        if (progress.isEmpty()) {
            Map<String, Object> expired = new LinkedHashMap<>();
            expired.put("taskId", taskId);
            expired.put("status", "expired");
            expired.put("message", "任务不存在或已过期（进度仅保留 24 小时）");
            return expired;
        }
        return progress;
    }

    // ==================== 删除执行 ====================

    private void runDelete(String taskId, List<Long> kbIds) {
        int total = kbIds.size();
        int done = 0;
        List<String> warnings = new ArrayList<>();
        try {
            for (Long kbId : kbIds) {
                int base = 5 + (int) (done * 90.0 / total);
                report(taskId, "删除知识库", base, done, total, "开始删除知识库 #" + kbId);
                deleteOne(taskId, kbId, done, total, warnings);
                done++;
                report(taskId, "删除知识库", Math.min(98, 5 + (int) (done * 90.0 / total)),
                        done, total, "已删除知识库 #" + kbId);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("deleted", done);
            result.put("warnings", warnings);
            finish(taskId, result);
            log.info("[kb-delete] 删除完成 task={} kbIds={}", taskId, kbIds);
        } catch (Exception e) {
            log.error("[kb-delete] 删除失败 task={} kbIds={}", taskId, kbIds, e);
            Map<String, Object> partial = new LinkedHashMap<>();
            partial.put("deleted", done);
            partial.put("warnings", warnings);
            fail(taskId, "删除失败：" + e.getMessage(), partial);
        } finally {
            for (Long kbId : kbIds) {
                runningKbIds.remove(kbId);
            }
        }
    }

    /**
     * 删除单个知识库：逐文档清理（可上报进度）→ 收尾清理 → 删库记录。
     */
    private void deleteOne(String taskId, Long kbId, int doneKbs, int totalKbs,
                           List<String> warnings) {
        List<DocumentDO> documents = documentMapper.selectList(
                new LambdaQueryWrapperX<DocumentDO>().eq(DocumentDO::getKbId, kbId));
        int totalDocs = documents.size();
        int index = 0;
        for (DocumentDO document : documents) {
            index++;
            int percent = 5 + (int) ((doneKbs + (double) index / Math.max(totalDocs, 1)) * 90.0 / totalKbs);
            report(taskId, "删除文档", Math.min(95, percent), doneKbs, totalKbs,
                    "知识库 #" + kbId + "：文档 " + index + "/" + totalDocs);
            try {
                documentService.deleteDocument(document.getId());
            } catch (Exception e) {
                log.warn("[kb-delete] 删除文档失败 docId={}: {}", document.getId(), e.getMessage());
                if (warnings.size() < 200) {
                    warnings.add("删除文档 #" + document.getId() + " 失败：" + e.getMessage());
                }
            }
        }

        // 收尾：此时文档已删空，deleteByKbId 只做派生数据/修订/python 清理
        try {
            documentService.deleteByKbId(kbId);
        } catch (Exception e) {
            log.warn("[kb-delete] 库级清理失败 kbId={}: {}", kbId, e.getMessage());
            if (warnings.size() < 200) {
                warnings.add("库级数据清理失败：" + e.getMessage());
            }
        }
        try {
            shareDeptMapper.delete(new LambdaQueryWrapperX<ShareDeptDO>()
                    .eq(ShareDeptDO::getKbId, kbId));
            libraryExtService.removeAllByKbId(kbId);
            folderMapper.deleteByKbId(kbId);
            libraryMapper.deleteById(kbId);
        } catch (Exception e) {
            throw new ServiceException(KB_DELETE_FAILED.getCode(),
                    "删除知识库 #" + kbId + " 记录失败：" + e.getMessage());
        }
        try {
            kbProcessConfigResolver.publishKbConfigDelete(kbId);
        } catch (Exception e) {
            log.warn("[kb-delete] 发布配置删除失败 kbId={}: {}", kbId, e.getMessage());
        }
    }

    private void validateManagePermission(Long kbId) {
        LibraryDO library = libraryService.getLibrary(kbId);
        if (library == null) {
            throw exception(LIBRARY_NOT_EXISTS);
        }
        if (!libraryService.canManage(kbId)) {
            throw exception(LIBRARY_PERMISSION_DENIED);
        }
    }

    private Map<String, Object> taskResult(String taskId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("status", "running");
        return result;
    }

    // ==================== 进度读写（Redis Hash，TTL 24h） ====================

    private void start(String taskId, int total) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("status", "running");
        data.put("stage", "准备中");
        data.put("progress", "0");
        data.put("message", "开始删除");
        data.put("total", String.valueOf(total));
        data.put("current", "0");
        data.put("updatedAt", String.valueOf(System.currentTimeMillis()));
        write(taskId, data);
    }

    private void report(String taskId, String stage, int progress, int current, int total,
                        String message) {
        int normalized = Math.max(0, Math.min(99, progress));
        Map<String, String> data = new LinkedHashMap<>();
        data.put("status", "running");
        data.put("stage", stage == null ? "" : stage);
        data.put("progress", String.valueOf(normalized));
        data.put("message", message == null ? "" : message);
        data.put("current", String.valueOf(current));
        data.put("total", String.valueOf(total));
        data.put("updatedAt", String.valueOf(System.currentTimeMillis()));
        write(taskId, data);
    }

    private void finish(String taskId, Map<String, Object> result) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("status", "done");
        data.put("stage", "完成");
        data.put("progress", "100");
        data.put("message", "删除完成");
        data.put("result", JsonUtils.toJsonString(result));
        data.put("updatedAt", String.valueOf(System.currentTimeMillis()));
        write(taskId, data);
    }

    private void fail(String taskId, String error, Map<String, Object> partial) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("status", "failed");
        data.put("stage", "失败");
        data.put("message", StrUtil.blankToDefault(error, "删除失败"));
        data.put("error", StrUtil.blankToDefault(error, ""));
        if (partial != null && !partial.isEmpty()) {
            data.put("result", JsonUtils.toJsonString(partial));
        }
        data.put("updatedAt", String.valueOf(System.currentTimeMillis()));
        write(taskId, data);
    }

    private void write(String taskId, Map<String, String> data) {
        try {
            String key = KEY_PREFIX + taskId;
            stringRedisTemplate.opsForHash().putAll(key, data);
            stringRedisTemplate.expire(key, TTL);
        } catch (Exception e) {
            log.warn("[kb-delete] 写入删除进度失败 task={}: {}", taskId, e.getMessage());
        }
    }

    private Map<String, Object> readProgress(String taskId) {
        Map<Object, Object> raw;
        try {
            raw = stringRedisTemplate.opsForHash().entries(KEY_PREFIX + taskId);
        } catch (Exception e) {
            log.warn("[kb-delete] 读取删除进度失败 task={}: {}", taskId, e.getMessage());
            return Collections.emptyMap();
        }
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, value) -> {
            String k = String.valueOf(key);
            String v = value == null ? "" : String.valueOf(value);
            switch (k) {
                case "progress", "current", "total" -> result.put(k, parseInt(v));
                case "result" -> result.put(k, parseJson(v));
                default -> result.put(k, v);
            }
        });
        result.put("taskId", taskId);
        return result;
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private Object parseJson(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> parsed = JsonUtils.parseObject(value, new TypeReference<Map<String, Object>>() {
            });
            return parsed == null ? Collections.emptyMap() : parsed;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
