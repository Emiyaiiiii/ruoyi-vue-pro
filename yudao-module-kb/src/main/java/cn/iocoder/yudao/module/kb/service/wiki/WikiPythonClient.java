package cn.iocoder.yudao.module.kb.service.wiki;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.kb.framework.config.VectorTaskConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Wiki 数据面桥接：wiki 只读/编辑转发 python-vector（python 直落 ES kb_wiki_all），Java 只做转发。
 *
 * <p>python 接口：{{base}}/{kbId}/list|index|folders|issues|pages/{slug}|graph
 *   + PUT {kbId}/issues/status、PUT {kbId}/pages </p>
 */
@Slf4j
@Component
public class WikiPythonClient {

    @Resource
    private RestTemplate restTemplate;
    @Resource
    private VectorTaskConfig vectorTaskConfig;

    private HttpEntity<Void> authEntity() {
        HttpHeaders h = new HttpHeaders();
        Long tid = TenantContextHolder.getTenantId();
        if (tid != null) {
            h.set("x-tenant-id", String.valueOf(tid));
        }
        return new HttpEntity<>(h);
    }

    private String base() {
        return vectorTaskConfig.getPythonServiceUrl() + "/api/v1/wiki";
    }

    public List<Map<String, Object>> listPages(Long kbId) {
        return getList(base() + "/" + kbId + "/list");
    }

    public Map<String, Object> index(Long kbId) {
        return getMap(base() + "/" + kbId + "/index");
    }

    public List<Map<String, Object>> folders(Long kbId) {
        return getList(base() + "/" + kbId + "/folders");
    }

    public List<Map<String, Object>> issues(Long kbId, String slug, String status) {
        StringBuilder u = new StringBuilder(base()).append("/").append(kbId).append("/issues?");
        if (slug != null) {
            u.append("slug=").append(slug).append("&");
        }
        if (status != null) {
            u.append("status=").append(status);
        }
        return getList(u.toString());
    }

    /** 空 Map ⇒ 页面不存在 */
    public Map<String, Object> page(Long kbId, String slug) {
        return getMap(base() + "/" + kbId + "/pages/" + slug);
    }

    public Map<String, Object> graph(Long kbId, String mode, String center, Integer limit,
                                     Integer depth, String types) {
        StringBuilder u = new StringBuilder(base()).append("/").append(kbId).append("/graph?mode=").append(mode);
        if (center != null) {
            u.append("&center_slug=").append(center);
        }
        if (depth != null) {
            u.append("&depth=").append(depth);
        }
        if (types != null && !types.trim().isEmpty()) {
            u.append("&types=").append(types.trim());
        }
        if (limit != null) {
            u.append("&limit=").append(limit);
        }
        return getMap(u.toString());
    }

    public boolean savePage(Long kbId, Map<String, Object> page) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            Long tid = TenantContextHolder.getTenantId();
            if (tid != null) {
                h.set("x-tenant-id", String.valueOf(tid));
            }
            HttpEntity<Map<String, Object>> e = new HttpEntity<>(page, h);
            ResponseEntity<Map> r = restTemplate.exchange(
                    base() + "/" + kbId + "/pages", HttpMethod.PUT, e, Map.class);
            return unwrap(r) != null;
        } catch (Exception ex) {
            log.warn("[WikiPython] 保存页面失败 kb={} slug={}: {}", kbId, page.get("slug"), ex.getMessage());
            return false;
        }
    }

    public boolean updateIssueStatus(Long kbId, String slug, String issueType, String status) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            Long tid = TenantContextHolder.getTenantId();
            if (tid != null) {
                h.set("x-tenant-id", String.valueOf(tid));
            }
            HttpEntity<Map<String, Object>> e = new HttpEntity<>(
                    Map.of("slug", slug, "issue_type", issueType, "status", status), h);
            ResponseEntity<Map> r = restTemplate.exchange(
                    base() + "/" + kbId + "/issues/status", HttpMethod.PUT, e, Map.class);
            return unwrap(r) != null;
        } catch (Exception ex) {
            log.warn("[WikiPython] 更新 issue 状态失败 kb={} slug={} type={}: {}", kbId, slug, issueType, ex.getMessage());
            return false;
        }
    }

    /** 文档删除联动：剥除/删除 source_refs 引用该文档的 wiki 页面（python 拥有 ES 归属） */
    public boolean unlinkDocument(Long kbId, Long docId) {
        try {
            ResponseEntity<Map> r = restTemplate.exchange(
                    base() + "/" + kbId + "/document/" + docId, HttpMethod.DELETE,
                    authEntity(), Map.class);
            return unwrap(r) != null;
        } catch (Exception ex) {
            log.warn("[WikiPython] unlink 文档失败 kb={} doc={}: {}", kbId, docId, ex.getMessage());
            return false;
        }
    }

    /** 删库统一清理：清 ES 切片 + ES wiki 页面/issue + Milvus 向量（python 一站式） */
    public boolean deleteKbStorage(Long kbId) {
        try {
            ResponseEntity<Map> r = restTemplate.exchange(
                    vectorTaskConfig.getPythonServiceUrl() + "/api/v1/kb/" + kbId,
                    HttpMethod.DELETE, authEntity(), Map.class);
            return unwrap(r) != null;
        } catch (Exception ex) {
            log.warn("[WikiPython] 删库清理失败 kb={}: {}", kbId, ex.getMessage());
            return false;
        }
    }

    // ---- 内部：解析 python {"code":0,"data":...} ----

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(String url) {
        try {
            Object d = unwrap(restTemplate.exchange(url, HttpMethod.GET, authEntity(), Map.class));
            return d instanceof List ? (List<Map<String, Object>>) d : List.of();
        } catch (Exception e) {
            log.warn("[WikiPython] GET 失败 url={}: {}", url, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(String url) {
        try {
            Object d = unwrap(restTemplate.exchange(url, HttpMethod.GET, authEntity(), Map.class));
            return d instanceof Map ? (Map<String, Object>) d : Map.of();
        } catch (Exception e) {
            log.warn("[WikiPython] GET 失败 url={}: {}", url, e.getMessage());
            return Map.of();
        }
    }

    private Object unwrap(ResponseEntity<Map> r) {
        Map<?, ?> body = r.getBody();
        if (body == null) {
            return null;
        }
        Object code = body.get("code");
        if (!(code instanceof Number) || ((Number) code).intValue() != 0) {
            throw new IllegalStateException("python code=" + code);
        }
        return body.get("data");
    }
}