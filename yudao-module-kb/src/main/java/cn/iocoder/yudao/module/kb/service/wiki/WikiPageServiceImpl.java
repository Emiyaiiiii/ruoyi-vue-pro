package cn.iocoder.yudao.module.kb.service.wiki;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.wiki.WikiPageRevisionDO;
import cn.iocoder.yudao.module.kb.dal.mysql.document.DocumentMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.wiki.WikiPageRevisionMapper;
import cn.iocoder.yudao.module.kb.service.library.LibraryService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * Wiki 页面 Service 实现（python 数据面 + 修订薄表载体）
 *
 * <p>页面正文/目录/双向链接/issues 由 python-vector 直落 ES（kb_wiki_all），本服务仅：
 *   - 只读/issue/图谱：转发 WikiPythonClient（Java 只做权限 + 转发）；
 *   - 人工编辑 save / 回滚 revert / 修订历史 listRevisions：
 *     ES 正文（python PUT /pages）+ MySQL kb_wiki_revision 薄表承载修订快照（(kb_id, slug, revision) 键控）。
 * 不再写 kb_wiki_page 主表，也不接收蒸馏落库（蒸馏已由 python 直落 ES）。</p>
 */
@Service
@Validated
@Slf4j
public class WikiPageServiceImpl implements WikiPageService {

    @Resource
    private LibraryService libraryService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private WikiPageRevisionMapper wikiPageRevisionMapper;
    @Resource
    private WikiPythonClient wikiPythonClient;
    @Resource
    private DocumentMapper documentMapper;
    /** 来源文档标题缓存（doc_id → 文件名），避免列表页逐条查询 */
    private final Map<Long, String> docTitleCache = new java.util.concurrent.ConcurrentHashMap<>();

    // ==================== 只读：转发 python ====================

    @Override
    public List<WikiPageRespVO> listByKb(Long kbId) {
        validateKbView(kbId);
        return wikiPythonClient.listPages(kbId).stream().map(this::toResp).collect(Collectors.toList());
    }

    @Override
    public List<WikiFolderRespVO> listFolders(Long kbId) {
        validateKbView(kbId);
        return wikiPythonClient.folders(kbId).stream().map(f -> {
            WikiFolderRespVO vo = new WikiFolderRespVO();
            vo.setKbId(kbId);
            List<String> segs = new ArrayList<>();
            Object pv = f.get("path");
            if (pv instanceof List) {
                for (Object o : (List<?>) pv) {
                    if (o != null) segs.add(String.valueOf(o));
                }
            } else if (pv != null) {
                segs.add(String.valueOf(pv));
            }
            vo.setDepth(segs.size());
            vo.setName(segs.isEmpty() ? StrUtil.toString(f.get("name")) : segs.get(segs.size() - 1));
            vo.setPath(segs.isEmpty() ? "" : String.join("/", segs));
            vo.setPageCount(toLong(f.get("count")));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public WikiIndexRespVO getIndex(Long kbId) {
        validateKbView(kbId);
        Map<String, Object> data = wikiPythonClient.index(kbId);
        WikiIndexRespVO resp = new WikiIndexRespVO();
        resp.setIntro(StrUtil.toString(data.get("intro")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pages = (List<Map<String, Object>>) data.get("pages");
        if (pages == null) {
            return resp;
        }
        Map<String, List<Map<String, Object>>> byType = pages.stream().collect(Collectors.groupingBy(
                p -> StrUtil.blankToDefault(StrUtil.toString(p.get("page_type")), "entity"),
                LinkedHashMap::new, Collectors.toList()));
        resp.setGroups(byType.entrySet().stream().map(e -> {
            WikiIndexRespVO.Group g = new WikiIndexRespVO.Group();
            g.setType(e.getKey());
            g.setTotal((long) e.getValue().size());
            g.setItems(e.getValue().stream().map(p -> {
                WikiIndexRespVO.Entry en = new WikiIndexRespVO.Entry();
                en.setSlug(StrUtil.toString(p.get("slug")));
                en.setTitle(StrUtil.toString(p.get("title")));
                en.setSummary(StrUtil.toString(p.get("summary")));
                en.setCategoryPath(JsonUtils.toJsonString(p.get("folder_path")));
                return en;
            }).collect(Collectors.toList()));
            return g;
        }).collect(Collectors.toList()));
        return resp;
    }

    @Override
    public List<WikiIssueRespVO> listIssues(Long kbId, String slug, String status) {
        validateKbView(kbId);
        return wikiPythonClient.issues(kbId, StrUtil.blankToDefault(slug, null),
                StrUtil.blankToDefault(status, null)).stream().map(it -> {
            WikiIssueRespVO vo = new WikiIssueRespVO();
            vo.setKbId(kbId);
            vo.setSlug(StrUtil.toString(it.get("slug")));
            vo.setIssueType(StrUtil.toString(it.get("issue_type")));
            vo.setDescription(StrUtil.toString(it.get("description")));
            vo.setStatus(StrUtil.toString(it.get("status")));
            vo.setReportedBy(StrUtil.toString(it.get("reported_by")));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void updateIssueStatus(Long kbId, String slug, String issueType, String status) {
        validateKbView(kbId);
        if (!"pending".equals(status) && !"ignored".equals(status) && !"resolved".equals(status)) {
            throw exception(WIKI_ISSUE_STATUS_INVALID);
        }
        if (!wikiPythonClient.updateIssueStatus(kbId, slug, issueType, status)) {
            throw exception(WIKI_PAGE_NOT_EXISTS);
        }
    }

    @Override
    public Long countPendingIssues(Long kbId) {
        return (long) wikiPythonClient.issues(kbId, null, "pending").size();
    }

    @Override
    public WikiPageRespVO getPage(Long kbId, String slug) {
        validateKbView(kbId);
        Map<String, Object> page = wikiPythonClient.page(kbId, slug);
        if (page.isEmpty()) {
            throw exception(WIKI_PAGE_NOT_EXISTS);
        }
        return toResp(page);
    }

    // ==================== 编辑：ES 正文 + 修订薄表载体 ====================

    @Override
    public void savePage(WikiPageSaveReqVO reqVO) {
        validateKbManage(reqVO.getKbId());
        String slug = reqVO.getSlug().trim();
        if (StrUtil.isBlank(slug)) {
            throw exception(WIKI_PAGE_CONFLICT);
        }
        Map<String, Object> existing = wikiPythonClient.page(reqVO.getKbId(), slug);
        if (!existing.isEmpty()) {
            // 更新：乐观锁
            if (toInt(existing.get("version")) != reqVO.getExpectedRevision()) {
                throw exception(WIKI_PAGE_CONFLICT);
            }
            boolean changed = contentChanged(existing, reqVO);
            if (changed) {
                insertRevisionSnapshot(reqVO.getKbId(), slug, existing);
            }
        }
        wikiPythonClient.savePage(reqVO.getKbId(), pageBody(reqVO));
    }

    @Override
    public Integer revertPage(WikiRevertReqVO reqVO) {
        validateKbManage(reqVO.getKbId());
        Map<String, Object> page = wikiPythonClient.page(reqVO.getKbId(), reqVO.getSlug());
        if (page.isEmpty()) {
            throw exception(WIKI_PAGE_NOT_EXISTS);
        }
        if (toInt(page.get("version")) != reqVO.getExpectedRevision()) {
            throw exception(WIKI_PAGE_CONFLICT);
        }
        Integer currentRevision = toInt(page.get("revision"));
        if (currentRevision.equals(reqVO.getTargetRevision())) {
            throw exception(WIKI_REVERT_TO_CURRENT);
        }
        WikiPageRevisionDO target = wikiPageRevisionMapper.selectByKbSlugRevision(
                reqVO.getKbId(), reqVO.getSlug(), reqVO.getTargetRevision());
        if (target == null) {
            throw exception(WIKI_REVISION_NOT_EXISTS);
        }
        // 当前状态也作为一条旧快照保留
        insertRevisionSnapshot(reqVO.getKbId(), reqVO.getSlug(), page);
        // 用目标快照覆盖正文（python 落 ES 并维护双向链接/版本）
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("slug", reqVO.getSlug());
        body.put("title", target.getTitle());
        body.put("content", target.getContent());
        body.put("summary", target.getSummary());
        body.put("aliases", parseAliases(target.getAliases()));
        body.put("page_type", target.getPageType());
        body.put("folder_path", page.get("folder_path") != null ? page.get("folder_path") : new ArrayList<String>());
        wikiPythonClient.savePage(reqVO.getKbId(), body);
        return currentRevision + 1;
    }

    @Override
    public List<WikiRevisionRespVO> listRevisions(Long kbId, String slug) {
        validateKbView(kbId);
        if (wikiPythonClient.page(kbId, slug).isEmpty()) {
            throw exception(WIKI_PAGE_NOT_EXISTS);
        }
        return wikiPageRevisionMapper.selectListByKbSlug(kbId, slug).stream().map(rev -> {
            WikiRevisionRespVO vo = new WikiRevisionRespVO();
            vo.setId(rev.getId());
            vo.setRevision(rev.getRevision());
            vo.setTitle(rev.getTitle());
            vo.setContent(rev.getContent());
            vo.setSummary(rev.getSummary());
            vo.setPageType(rev.getPageType());
            vo.setEditSource(rev.getEditSource());
            vo.setOperatorName(rev.getOperatorName());
            vo.setCreateTime(rev.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== 图谱：转发 python ====================

    @Override
    public WikiGraphRespVO graph(Long kbId, String mode, String centerSlug, Integer depth,
                                 String types, Integer limit) {
        validateKbView(kbId);
        Map<String, Object> data = wikiPythonClient.graph(kbId, mode, centerSlug, limit, depth, types);
        WikiGraphRespVO resp = new WikiGraphRespVO();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) data.get("nodes");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> edges = (List<Map<String, Object>>) data.get("edges");
        resp.setNodes(nodes == null ? new ArrayList<>() : nodes.stream().map(n -> {
            WikiGraphRespVO.Node node = new WikiGraphRespVO.Node();
            node.setSlug(StrUtil.toString(n.get("slug")));
            node.setTitle(StrUtil.toString(n.get("title")));
            node.setPageType(StrUtil.toString(n.get("page_type")));
            node.setLinkCount(toInt(n.get("link_count")));
            return node;
        }).collect(Collectors.toList()));
        resp.setEdges(edges == null ? new ArrayList<>() : edges.stream().map(e -> {
            WikiGraphRespVO.Edge edge = new WikiGraphRespVO.Edge();
            edge.setSource(StrUtil.toString(e.get("source")));
            edge.setTarget(StrUtil.toString(e.get("target")));
            return edge;
        }).collect(Collectors.toList()));
        // meta（对齐 WeKnora WikiGraphMeta）：mode/total/returned/truncated/center/depth
        Object metaRaw = data.get("meta");
        if (metaRaw instanceof Map) {
            Map<?, ?> meta = (Map<?, ?>) metaRaw;
            WikiGraphRespVO.Meta m = new WikiGraphRespVO.Meta();
            m.setMode(StrUtil.toString(meta.get("mode")));
            m.setTotal(toLong(meta.get("total")));
            m.setReturned(toLong(meta.get("returned")));
            m.setTruncated(Boolean.TRUE.equals(meta.get("truncated")));
            m.setCenter(StrUtil.toString(meta.get("center")));
            m.setDepth(toInt(meta.get("depth")));
            resp.setMeta(m);
        }
        return resp;
    }

    // ==================== 私有辅助 ====================

    private void validateKbView(Long kbId) {
        LibraryDO library = libraryService.getLibrary(kbId);
        if (library != null && Integer.valueOf(1).equals(library.getIsProject())
                && !libraryService.canOpenProjectLibrary(kbId)) {
            throw exception(WIKI_PERMISSION_DENIED);
        }
    }

    private void validateKbManage(Long kbId) {
        if (!libraryService.canManage(kbId)) {
            throw exception(WIKI_PERMISSION_DENIED);
        }
    }

    /** 修订薄表快照写入：记录「被替换的旧状态」，(kb_id, slug, revision) 唯一幂等。 */
    private void insertRevisionSnapshot(Long kbId, String slug, Map<String, Object> page) {
        WikiPageRevisionDO rev = new WikiPageRevisionDO();
        rev.setKbId(kbId);
        rev.setSlug(slug);
        rev.setRevision(toInt(page.get("revision")));
        rev.setTitle(StrUtil.toString(page.get("title")));
        rev.setContent(StrUtil.toString(page.get("content")));
        rev.setSummary(StrUtil.toString(page.get("summary")));
        rev.setAliases(JsonUtils.toJsonString(page.get("aliases")));
        rev.setPageType(StrUtil.toString(page.get("page_type")));
        rev.setStatus(StrUtil.toString(page.get("status")));
        rev.setEditSource("user");
        rev.setOperatorId(operatorId());
        rev.setOperatorName(editorName());
        try {
            wikiPageRevisionMapper.insert(rev);
        } catch (DuplicateKeyException e) {
            log.debug("[insertRevisionSnapshot] 修订快照重复（并发），忽略 kb={} slug={} rev={}", kbId, slug, rev.getRevision());
        }
    }

    private Map<String, Object> pageBody(WikiPageSaveReqVO reqVO) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("slug", reqVO.getSlug().trim());
        body.put("title", reqVO.getTitle());
        body.put("content", reqVO.getContent());
        body.put("summary", reqVO.getSummary());
        body.put("aliases", parseAliases(reqVO.getAliases()));
        body.put("page_type", StrUtil.isBlank(reqVO.getPageType()) ? "manual" : reqVO.getPageType());
        body.put("folder_path", new ArrayList<String>());
        return body;
    }

    private boolean contentChanged(Map<String, Object> page, WikiPageSaveReqVO reqVO) {
        return !eq(StrUtil.toString(page.get("title")), reqVO.getTitle())
                || !eq(StrUtil.toString(page.get("content")), reqVO.getContent())
                || !eq(StrUtil.toString(page.get("summary")), reqVO.getSummary());
    }

    private WikiPageRespVO toResp(Map<String, Object> p) {
        WikiPageRespVO vo = new WikiPageRespVO();
        vo.setKbId(toLong(p.get("kb_id")));
        vo.setSlug(StrUtil.toString(p.get("slug")));
        vo.setTitle(StrUtil.toString(p.get("title")));
        vo.setContent(StrUtil.toString(p.get("content")));
        vo.setSummary(StrUtil.toString(p.get("summary")));
        vo.setAliases(JsonUtils.toJsonString(p.get("aliases")));
        vo.setPageType(StrUtil.toString(p.get("page_type")));
        vo.setCategoryPath(JsonUtils.toJsonString(p.get("folder_path")));
        vo.setDepth(toInt(p.get("depth")));
        // 链接/来源直接透传数组（对齐 WeKnora：in_links/out_links/source_refs 为字符串数组，前端按数组消费）
        vo.setSourceRefs(resolveSourceRefs(toStringList(p.get("source_refs"))));
        vo.setChunkRefs(toStringList(p.get("chunk_refs")));
        vo.setInLinks(toStringList(p.get("in_links")));
        vo.setOutLinks(toStringList(p.get("out_links")));
        vo.setStatus(StrUtil.toString(p.get("status")));
        vo.setRevision(toInt(p.get("revision")));
        vo.setVersion(toInt(p.get("version")));
        vo.setLastEditSource(StrUtil.toString(p.get("last_edit_source")));
        return vo;
    }

    private List<String> parseAliases(Object aliases) {
        if (aliases instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) aliases;
            return list;
        }
        String json = StrUtil.toString(aliases);
        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            List<String> list = JsonUtils.parseArray(json, String.class);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** 兼容 List / JSON 字符串 → 字符串数组（python 直落 ES 的数组字段透传给前端）。 */
    private List<String> toStringList(Object o) {
        if (o instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) o;
            return list == null ? new ArrayList<>() : list;
        }
        String json = StrUtil.toString(o);
        if (StrUtil.isBlank(json) || "null".equals(json)) {
            return new ArrayList<>();
        }
        try {
            List<String> list = JsonUtils.parseArray(json, String.class);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** 来源文档标题解析：裸 doc_id → `id|文件名`（对齐 WeKnora parseSourceRefEntry，
     *  前端直接显示标题；已带 | 的条目原样透传）。文件名走缓存，避免列表页逐条查库。 */
    private List<String> resolveSourceRefs(List<String> refs) {
        if (refs == null || refs.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>(refs.size());
        for (String ref : refs) {
            String r = StrUtil.trimToEmpty(ref);
            if (r.isEmpty()) {
                continue;
            }
            if (r.indexOf('|') > 0) {
                out.add(r);
                continue;
            }
            Long docId = null;
            try {
                docId = Long.valueOf(r);
            } catch (NumberFormatException ignored) {
                out.add(r);
                continue;
            }
            String title = docTitleCache.get(docId);
            if (title == null) {
                DocumentDO doc = documentMapper.selectById(docId);
                title = doc == null ? null : doc.getFileName();
                if (title != null) {
                    docTitleCache.put(docId, title);
                }
            }
            out.add(title == null ? r : r + "|" + title);
        }
        return out;
    }

    private Long operatorId() {
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        return user == null ? 0L : user.getId();
    }

    private String editorName() {
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        if (user == null) {
            return "";
        }
        try {
            AdminUserRespDTO sysUser = adminUserApi.getUser(user.getId());
            return sysUser != null ? sysUser.getNickname() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean eq(String a, String b) {
        return Objects.equals(a, b);
    }

    private static long toLong(Object o) {
        // python 端 ES 文档数值字段（如 kb_id）可能以字符串存储，需兼容解析
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        if (o != null) {
            try {
                return Long.parseLong(String.valueOf(o).trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return 0L;
    }

    private static int toInt(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        if (o != null) {
            try {
                return Integer.parseInt(String.valueOf(o).trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return 0;
    }
}