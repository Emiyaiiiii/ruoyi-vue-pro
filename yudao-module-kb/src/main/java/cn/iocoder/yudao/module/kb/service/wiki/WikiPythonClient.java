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

    /** 收尾重建（对齐 WeKnora rebuild-links）：死链清理 + 交叉链接 + 入链重建 + index 兜底。
     *  python 侧为纯算法、不调 LLM，返回各步统计（dead_removed / cross_added / relinked）。 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> rebuildLinks(Long kbId) {
        try {
            ResponseEntity<Map> r = restTemplate.exchange(
                    base() + "/" + kbId + "/rebuild-links", HttpMethod.POST,
                    authEntity(), Map.class);
            Object d = unwrap(r);
            return d instanceof Map ? (Map<String, Object>) d : Map.of();
        } catch (Exception ex) {
            log.warn("[WikiPython] 重建链接失败 kb={}: {}", kbId, ex.getMessage());
            return Map.of();
        }
    }

    /** Wiki 健康统计：页数（按类型）/ 链接数 / 孤立页 / 死链数 / 待处理 issue 数 */
    public Map<String, Object> stats(Long kbId) {
        return getMap(base() + "/" + kbId + "/stats");
    }

    /** 最近一次 Wiki 攒批状态（阶段② WikiScheduler：批处理进度 + 待处理文档数） */
    public Map<String, Object> batchLatest(Long kbId) {
        return getMap(base() + "/" + kbId + "/batch/latest");
    }

    /** Wiki 质量检查（对齐 WeKnora /lint）：孤立页/死链/空内容/失效来源/缺失交叉链接 + 健康分 */
    public Map<String, Object> lint(Long kbId) {
        return getMap(base() + "/" + kbId + "/lint");
    }

    /** 自动修复可修项（对齐 WeKnora /auto-fix）：死链拆文本 / 空内容归档 / 失效来源清理 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> autoFix(Long kbId) {
        try {
            ResponseEntity<Map> r = restTemplate.exchange(
                    base() + "/" + kbId + "/auto-fix", HttpMethod.POST,
                    authEntity(), Map.class);
            Object d = unwrap(r);
            return d instanceof Map ? (Map<String, Object>) d : Map.of();
        } catch (Exception ex) {
            log.warn("[WikiPython] 自动修复失败 kb={}: {}", kbId, ex.getMessage());
            return Map.of();
        }
    }

    // ---- 独立目录实体（P1-1，对齐 WeKnora wiki_folders） ----

    /** 目录实体树（含每目录直接挂载页数） */
    public List<Map<String, Object>> folderTree(Long kbId) {
        return getList(base() + "/" + kbId + "/folder-tree");
    }

    /** 创建目录 */
    public Map<String, Object> createFolder(Long kbId, String name, String parentId) {
        return exchangeJson(base() + "/" + kbId + "/folders", HttpMethod.POST,
                Map.of("name", name, "parent_id", parentId == null ? "" : parentId));
    }

    /** 重命名目录（级联更新子树与页面派生路径） */
    public Map<String, Object> renameFolder(Long kbId, String folderId, String name) {
        return exchangeJson(base() + "/" + kbId + "/folders", HttpMethod.PUT,
                Map.of("folder_id", folderId, "name", name));
    }

    /** 移动目录到新父级（含子树重算） */
    public Map<String, Object> moveFolder(Long kbId, String folderId, String parentId) {
        return exchangeJson(base() + "/" + kbId + "/folders/move", HttpMethod.PUT,
                Map.of("folder_id", folderId, "parent_id", parentId == null ? "" : parentId));
    }

    /** 删除目录及整棵子树（子树内页面归位到根） */
    @SuppressWarnings("unchecked")
    public Map<String, Object> deleteFolder(Long kbId, String folderId) {
        try {
            ResponseEntity<Map> r = restTemplate.exchange(
                    base() + "/" + kbId + "/folders/" + folderId, HttpMethod.DELETE,
                    authEntity(), Map.class);
            Object d = unwrap(r);
            return d instanceof Map ? (Map<String, Object>) d : Map.of();
        } catch (Exception ex) {
            log.warn("[WikiPython] 删除目录失败 kb={} folder={}: {}", kbId, folderId, ex.getMessage());
            return Map.of();
        }
    }

    /** 移动页面到指定目录（folderId 为空表示根） */
    public Map<String, Object> movePage(Long kbId, String slug, String folderId) {
        return exchangeJson(base() + "/" + kbId + "/pages/move", HttpMethod.PUT,
                Map.of("slug", slug, "folder_id", folderId == null ? "" : folderId));
    }

    /** JSON 请求通用实现（POST/PUT），返回 python {"code":0,"data":...} 的 data */
    @SuppressWarnings("unchecked")
    private Map<String, Object> exchangeJson(String url, HttpMethod method, Map<String, Object> body) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            Long tid = TenantContextHolder.getTenantId();
            if (tid != null) {
                h.set("x-tenant-id", String.valueOf(tid));
            }
            ResponseEntity<Map> r = restTemplate.exchange(url, method,
                    new HttpEntity<>(body, h), Map.class);
            Object d = unwrap(r);
            return d instanceof Map ? (Map<String, Object>) d : Map.of();
        } catch (Exception ex) {
            log.warn("[WikiPython] 请求失败 url={}: {}", url, ex.getMessage());
            return Map.of();
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