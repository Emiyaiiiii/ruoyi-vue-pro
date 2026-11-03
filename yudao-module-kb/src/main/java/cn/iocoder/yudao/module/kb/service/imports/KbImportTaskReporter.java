package cn.iocoder.yudao.module.kb.service.imports;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 知识库导入任务进度上报（Redis Hash，TTL 24h）。
 *
 * <p>导入是长耗时操作（分片合并 + 文档/图片/切片/向量写入，reembed 可能几十分钟），
 * 因此 `complete` 只负责启动后台任务并立即返回 taskId，前端轮询
 * `GET /kb/library/import-package/task/{taskId}` 获取阶段与百分比。</p>
 *
 * <p>字段：status(running/done/failed)、stage、progress(0-100)、message、kbId、error、
 * result(导入报告 JSON)、startedAt、updatedAt。</p>
 */
@Component
@Slf4j
public class KbImportTaskReporter {

    private static final String KEY_PREFIX = "kb:import:task:";
    private static final Duration TTL = Duration.ofHours(24);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 进度写节流：同一 taskId 相同百分比不重复写 */
    private final Map<String, Integer> lastProgress = new ConcurrentHashMap<>();

    public void start(String taskId) {
        Map<String, String> data = new HashMap<>();
        data.put("status", "running");
        data.put("stage", "准备中");
        data.put("progress", "0");
        data.put("message", "开始导入");
        data.put("startedAt", String.valueOf(System.currentTimeMillis()));
        data.put("updatedAt", String.valueOf(System.currentTimeMillis()));
        write(taskId, data);
    }

    /**
     * 更新阶段进度（百分比与上次相同则跳过，避免高频写 Redis）。
     */
    public void progress(String taskId, String stage, int progress, String message) {
        if (taskId == null) {
            return;
        }
        int normalized = Math.max(0, Math.min(100, progress));
        Integer last = lastProgress.get(taskId);
        if (last != null && last == normalized) {
            return;
        }
        lastProgress.put(taskId, normalized);
        Map<String, String> data = new HashMap<>();
        data.put("status", "running");
        data.put("stage", stage == null ? "" : stage);
        data.put("progress", String.valueOf(normalized));
        data.put("message", message == null ? "" : message);
        data.put("updatedAt", String.valueOf(System.currentTimeMillis()));
        write(taskId, data);
    }

    public void finish(String taskId, Map<String, Object> result) {
        if (taskId == null) {
            return;
        }
        lastProgress.remove(taskId);
        Map<String, String> data = new HashMap<>();
        data.put("status", "done");
        data.put("stage", "完成");
        data.put("progress", "100");
        data.put("message", "导入完成");
        data.put("result", JsonUtils.toJsonString(result));
        data.put("updatedAt", String.valueOf(System.currentTimeMillis()));
        write(taskId, data);
    }

    /**
     * @param partial 失败前的部分结果（可能已创建知识库），便于前端提示
     */
    public void fail(String taskId, String error, Map<String, Object> partial) {
        if (taskId == null) {
            return;
        }
        lastProgress.remove(taskId);
        Map<String, String> data = new HashMap<>();
        data.put("status", "failed");
        data.put("stage", "失败");
        data.put("message", error == null ? "导入失败" : error);
        data.put("error", error == null ? "" : error);
        if (partial != null && !partial.isEmpty()) {
            data.put("result", JsonUtils.toJsonString(partial));
        }
        data.put("updatedAt", String.valueOf(System.currentTimeMillis()));
        write(taskId, data);
    }

    /**
     * 查询任务进度；任务不存在/已过期返回空 Map。
     */
    public Map<String, Object> get(String taskId) {
        if (taskId == null) {
            return Collections.emptyMap();
        }
        Map<Object, Object> raw;
        try {
            raw = stringRedisTemplate.opsForHash().entries(KEY_PREFIX + taskId);
        } catch (Exception e) {
            log.warn("[kb-import] 读取导入进度失败 task={}: {}", taskId, e.getMessage());
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
                case "progress" -> result.put(k, parseInt(v));
                case "kbId" -> result.put(k, parseLong(v));
                case "startedAt", "updatedAt" -> result.put(k, parseLong(v));
                case "result" -> result.put(k, parseJson(v));
                default -> result.put(k, v);
            }
        });
        result.put("taskId", taskId);
        return result;
    }

    private void write(String taskId, Map<String, String> data) {
        try {
            String key = KEY_PREFIX + taskId;
            stringRedisTemplate.opsForHash().putAll(key, data);
            stringRedisTemplate.expire(key, TTL);
        } catch (Exception e) {
            // 进度上报失败不影响导入本身
            log.warn("[kb-import] 写入导入进度失败 task={}: {}", taskId, e.getMessage());
        }
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return null;
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
