package cn.iocoder.yudao.module.kb.service.modelcatalog;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import cn.iocoder.yudao.module.kb.framework.config.ModelCatalogConfig;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 模型能力目录服务（LiteLLM model_prices_and_context_window.json）。
 *
 * <p>作为模型参数探测的第①层：内置快照（离线兜底）+ 远程定时刷新（保持新鲜），
 * 命中即返回，零网络请求；未命中由调用方继续走 /models 与 chat 探测。</p>
 */
@Service
@Slf4j
public class ModelCatalogService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** sample_spec 是字段说明伪数据，加载时必须剔除 */
    private static final String SAMPLE_SPEC_KEY = "sample_spec";

    @Resource
    private ModelCatalogConfig modelCatalogConfig;

    @Resource
    private RestTemplate modelCatalogRestTemplate;

    /** 归一化模型 id -> 模型能力条目 */
    private volatile Map<String, Map<String, Object>> catalog = Collections.emptyMap();

    /** 上次成功加载/刷新时间戳 */
    private volatile long lastRefreshAt = 0L;

    /** 是否正在刷新（防并发重复拉取） */
    private volatile boolean refreshing = false;

    @PostConstruct
    public void init() {
        if (!isEnabled()) {
            log.info("[模型目录] 未启用（kb.model-catalog.enabled=false）");
            return;
        }
        loadFromClasspath();
        triggerAsyncRefresh();
    }

    /**
     * 按模型 id 查表（归一化精确命中 -> 归一化包含匹配），未命中返回 null。
     *
     * @param modelId 具体模型名（如 doubao-seed-2.0-mini / deepseek-chat）
     * @return 模型能力条目（max_input_tokens / max_output_tokens / supports_vision / supports_reasoning 等）
     */
    public Map<String, Object> lookup(String modelId) {
        maybeRefreshIfStale();
        String key = normalize(modelId);
        if (StrUtil.isBlank(key)) {
            return null;
        }
        // 1) 归一化精确命中
        Map<String, Object> hit = catalog.get(key);
        if (hit != null) {
            return hit;
        }
        // 2) 包含匹配：key 太短容易误命中（如 gpt-4 -> gpt-4o），要求足够长度
        if (key.length() < 6) {
            return null;
        }
        for (Map.Entry<String, Map<String, Object>> entry : catalog.entrySet()) {
            String k = entry.getKey();
            if (k.contains(key) || key.contains(k)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 当前目录条目数（诊断用）。
     */
    public int size() {
        return catalog.size();
    }

    // ==================== 加载 ====================

    private void loadFromClasspath() {
        String path = modelCatalogConfig.getClasspathFile();
        if (StrUtil.isBlank(path)) {
            return;
        }
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            log.info("[模型目录] 未找到 classpath 内置快照({})，跳过（依赖远程刷新或 /models+chat 探测）", path);
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = OBJECT_MAPPER.readValue(in, Map.class);
            replaceCatalog(raw);
            lastRefreshAt = System.currentTimeMillis();
            log.info("[模型目录] classpath 快照加载成功: {} 条", catalog.size());
        } catch (Exception e) {
            log.warn("[模型目录] classpath 快照加载失败({}): {}", path, e.getMessage());
        }
    }

    private void loadFromRemote() {
        String url = modelCatalogConfig.getRemoteUrl();
        if (StrUtil.isBlank(url)) {
            return;
        }
        try {
            ResponseEntity<String> resp = modelCatalogRestTemplate.getForEntity(url, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || StrUtil.isBlank(resp.getBody())) {
                throw new RuntimeException("HTTP " + resp.getStatusCode().value());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = OBJECT_MAPPER.readValue(resp.getBody(), Map.class);
            replaceCatalog(raw);
            lastRefreshAt = System.currentTimeMillis();
            log.info("[模型目录] 远程清单刷新成功: {} 条", catalog.size());
        } catch (Exception e) {
            log.warn("[模型目录] 远程清单刷新失败: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void replaceCatalog(Map<String, Object> raw) {
        if (raw == null) {
            return;
        }
        Map<String, Map<String, Object>> next = new HashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            if (SAMPLE_SPEC_KEY.equals(e.getKey())) {
                continue;
            }
            if (!(e.getValue() instanceof Map)) {
                continue;
            }
            next.put(normalize(e.getKey()), (Map<String, Object>) e.getValue());
        }
        catalog = next;
    }

    // ==================== 刷新控制 ====================

    private boolean isEnabled() {
        return Boolean.TRUE.equals(modelCatalogConfig.getEnabled());
    }

    private void maybeRefreshIfStale() {
        if (!isEnabled()) {
            return;
        }
        long ttlMs = modelCatalogConfig.getRefreshHours() * 3600_000L;
        if (System.currentTimeMillis() - lastRefreshAt < ttlMs) {
            return;
        }
        triggerAsyncRefresh();
    }

    private void triggerAsyncRefresh() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        CompletableFuture.runAsync(() -> {
            try {
                loadFromRemote();
            } finally {
                refreshing = false;
            }
        });
    }

    /**
     * 归一化模型 id：去分隔符（点/横线/下划线）、转小写。
     */
    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("[._\\-]", "");
    }
}
