package cn.iocoder.yudao.module.kb.service.export;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.kb.controller.admin.library.vo.LibraryPageReqVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.category.CategoryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.folder.FolderDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.sharedept.ShareDeptDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.vectortask.VectorTaskDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.wiki.WikiPageRevisionDO;
import cn.iocoder.yudao.module.kb.dal.mysql.category.CategoryMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.document.DocumentMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.folder.FolderMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.levelconfig.LevelConfigMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.sharedept.ShareDeptMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.vectortask.VectorTaskMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.wiki.WikiPageRevisionMapper;
import cn.iocoder.yudao.module.kb.framework.config.VectorTaskConfig;
import cn.iocoder.yudao.module.kb.service.document.DocumentService;
import cn.iocoder.yudao.module.kb.service.kbconfig.KbProcessConfigResolver;
import cn.iocoder.yudao.module.kb.service.library.LibraryService;
import cn.iocoder.yudao.module.kb.service.libraryext.LibraryExtService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 知识库完整导出 Service 实现
 *
 * <p>导出包结构（只读快照，不落地临时文件，全程流式）：</p>
 * <pre>
 * kb-{id}-{名称}/
 *   manifest.json            导出元信息 + 各数据量统计
 *   library.json             知识库配置（库行 + 层级配置 + 分类 + 共享部门 + 自定义字段 + processConfig）
 *   documents.json           文档元数据（kb_document 全字段）
 *   folders.json             文档文件夹树
 *   vector_tasks.json        向量任务记录（含 stages 时间线）
 *   chunks/chunks.jsonl      切片（ES，NDJSON）
 *   vectors/vectors.jsonl    向量（Milvus，NDJSON，含原始数值）
 *   wiki/pages.jsonl         Wiki 页面（ES kb_wiki_all，含正文/双向链接）
 *   wiki/folders.jsonl       Wiki 目录实体（ES）
 *   wiki/issues.jsonl        Wiki 待修复问题（ES）
 *   wiki/revisions.json      Wiki 修订历史（MySQL 薄表）
 *   images/                  图片回图对象（条目路径=图片桶内对象 key，保持相对 key 可迁移）
 *   files/                   原始文档（可选，按 {docId}_{文件名} 命名）
 *   export_report.json       导出结果报告（条数/字节数/告警）
 * </pre>
 *
 * <p>全库导出（{@link #exportAllLibraries}）在根目录下按知识库分子目录，根目录额外输出汇总 manifest 与 report。</p>
 */
@Service
@Validated
@Slf4j
public class KbExportServiceImpl implements KbExportService {

    /** 导出包格式标识/版本（后续做导入或迁移时按此校验） */
    private static final String FORMAT = "ckb-kb-export";
    private static final String FORMAT_VERSION = "1.0";

    private static final String PYTHON_EXPORT_BASE = "/api/v1/export/";
    private static final String NDJSON_MEDIA_TYPE = "application/x-ndjson";
    private static final int COPY_BUFFER_SIZE = 64 * 1024;
    private static final int MAX_FILE_NAME_LENGTH = 120;

    @Resource
    private LibraryService libraryService;
    @Resource
    private LibraryExtService libraryExtService;
    @Resource
    private CategoryMapper categoryMapper;
    @Resource
    private LevelConfigMapper levelConfigMapper;
    @Resource
    private ShareDeptMapper shareDeptMapper;
    @Resource
    private DocumentMapper documentMapper;
    @Resource
    private FolderMapper folderMapper;
    @Resource
    private VectorTaskMapper vectorTaskMapper;
    @Resource
    private WikiPageRevisionMapper wikiPageRevisionMapper;
    @Resource
    private DocumentService documentService;
    @Resource
    private VectorTaskConfig vectorTaskConfig;
    @Resource
    private KbProcessConfigResolver kbProcessConfigResolver;
    @Resource(name = "kbExportRestTemplate")
    private RestTemplate kbExportRestTemplate;

    // ==================== 单库导出 ====================

    @Override
    public void exportLibrary(Long kbId, boolean includeFiles, HttpServletResponse response) throws IOException {
        // 1. 校验存在性 + 可见性（越权导出等同于越权读取全部内容）
        LibraryDO library = requireVisibleLibrary(kbId);
        // 2. 先取 python 数据面统计：不可用时直接失败，避免产出"缺切片/向量"的残缺包
        Map<String, Object> pythonManifest = fetchPythonManifest(kbId);

        String root = exportRootDir(library.getId(), library.getName());
        prepareResponse(response, buildZipFileName(library.getId(), library.getName()));

        List<String> warnings = new ArrayList<>();
        Map<String, Object> packageResult = new LinkedHashMap<>();
        boolean success = true;
        String error = null;

        ZipOutputStream zos = newZipStream(response);
        try {
            packageResult = writeKbPackage(zos, root, library, pythonManifest, includeFiles, warnings);
        } catch (IOException | RuntimeException e) {
            success = false;
            error = e.getMessage();
            log.error("[kb-export] 单库导出中断 kbId={}", kbId, e);
            throw e;
        } finally {
            Map<String, Object> report = buildReport("single", success, error, warnings, packageResult);
            report.put("kbId", kbId);
            writeReportQuietly(zos, root + "/export_report.json", report);
            closeQuietly(zos, kbId);
        }
    }

    @Override
    public void exportAllLibraries(boolean includeFiles, HttpServletResponse response) throws IOException {
        List<LibraryDO> libraries = listVisibleLibraries();
        if (libraries.isEmpty()) {
            throw exception(LIBRARY_NOT_EXISTS);
        }
        String stamp = nowStamp();
        String root = "ckb-export-all-" + stamp;
        prepareResponse(response, root + ".zip");

        List<String> warnings = new ArrayList<>();
        Map<String, Object> perKb = new LinkedHashMap<>();
        Map<String, Object> archiveEntries = new LinkedHashMap<>();
        boolean success = true;
        String error = null;

        ZipOutputStream zos = newZipStream(response);
        try {
            Map<String, Object> archiveManifest = new LinkedHashMap<>();
            archiveManifest.put("format", FORMAT);
            archiveManifest.put("formatVersion", FORMAT_VERSION);
            archiveManifest.put("scope", "all");
            archiveManifest.put("exportedAt", now());
            archiveManifest.put("tenantId", TenantContextHolder.getTenantId());
            archiveManifest.put("kbCount", libraries.size());
            archiveManifest.put("includeFiles", includeFiles);
            archiveManifest.put("readOnly", true);
            archiveManifest.put("kbs", toKbBriefList(libraries));
            archiveManifest.put("layout", "每个知识库一个子目录（kb-{id}-{名称}），子目录结构与单库导出一致");
            writeJsonEntry(zos, root + "/manifest.json", archiveManifest, archiveEntries);

            for (LibraryDO library : libraries) {
                try {
                    Map<String, Object> pythonManifest = tryFetchPythonManifest(library.getId(), warnings);
                    Map<String, Object> result = writeKbPackage(zos, root + "/" + exportRootDir(
                            library.getId(), library.getName()), library, pythonManifest, includeFiles, warnings);
                    perKb.put(String.valueOf(library.getId()), result);
                } catch (IOException e) {
                    // ZIP 写失败（通常是客户端断开）无法继续，直接中断
                    throw e;
                } catch (RuntimeException e) {
                    // 单个库异常不影响其余库导出，记录告警后继续
                    log.warn("[kb-export] 全库导出跳过异常知识库 kbId={}: {}", library.getId(), e.getMessage());
                    warnings.add("知识库 " + library.getId() + "（" + library.getName() + "）导出失败：" + e.getMessage());
                }
            }
            archiveEntries.put("kbPackages", perKb.size());
        } catch (IOException | RuntimeException e) {
            success = false;
            error = e.getMessage();
            log.error("[kb-export] 全库导出中断", e);
            throw e;
        } finally {
            Map<String, Object> report = buildReport("all", success, error, warnings, perKb);
            report.put("kbCount", libraries.size());
            report.put("archiveEntries", archiveEntries);
            writeReportQuietly(zos, root + "/export_report.json", report);
            closeQuietly(zos, null);
        }
    }

    @Override
    public Map<String, Object> getExportManifest(Long kbId) {
        LibraryDO library = requireVisibleLibrary(kbId);
        List<DocumentDO> documents = listDocuments(kbId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kbId", kbId);
        result.put("name", library.getName());
        result.put("documents", documents.size());
        result.put("folders", folderMapper.selectByKbId(kbId).size());
        result.put("vectorTasks", listVectorTasks(kbId).size());
        result.put("wikiRevisions", listWikiRevisions(kbId).size());
        result.put("sourceBytes", documents.stream()
                .filter(d -> d.getFileSize() != null)
                .mapToLong(DocumentDO::getFileSize).sum());
        // 预览不因 python 不可用而失败：仅标记不可用，下载时再严格校验
        Map<String, Object> pythonManifest = Collections.emptyMap();
        try {
            pythonManifest = fetchPythonManifest(kbId);
        } catch (Exception e) {
            log.warn("[kb-export] 预览时 python 导出接口不可用 kbId={}: {}", kbId, e.getMessage());
        }
        result.put("pythonAvailable", !pythonManifest.isEmpty());
        result.put("chunks", pythonCount(pythonManifest, "chunks"));
        result.put("vectors", pythonCount(pythonManifest, "vectors"));
        result.put("wikiPages", pythonCount(pythonManifest, "wiki_pages"));
        result.put("wikiFolders", pythonCount(pythonManifest, "wiki_folders"));
        result.put("wikiIssues", pythonCount(pythonManifest, "wiki_issues"));
        result.put("images", pythonImageCounts(pythonManifest));
        return result;
    }

    // ==================== 单个知识库打包（单库/全库共用） ====================

    /**
     * 把一个知识库的完整快照写入 ZIP 的指定子目录。
     *
     * @return {counts:{...}, entries:{...}}，供上层汇总到 export_report
     */
    private Map<String, Object> writeKbPackage(ZipOutputStream zos, String kbRoot, LibraryDO library,
                                               Map<String, Object> pythonManifest, boolean includeFiles,
                                               List<String> warnings) throws IOException {
        Long kbId = library.getId();
        List<DocumentDO> documents = listDocuments(kbId);
        List<FolderDO> folders = folderMapper.selectByKbId(kbId);
        List<VectorTaskDO> tasks = listVectorTasks(kbId);
        List<WikiPageRevisionDO> revisions = listWikiRevisions(kbId);
        List<Long> shareDeptIds = listShareDeptIds(kbId);
        Map<String, String> extValues = libraryExtService.getExtValues(kbId);

        long sourceBytes = documents.stream()
                .filter(d -> d.getFileSize() != null)
                .mapToLong(DocumentDO::getFileSize).sum();

        Map<String, Object> entryStats = new LinkedHashMap<>();
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("documents", documents.size());
        counts.put("folders", folders.size());
        counts.put("vectorTasks", tasks.size());
        counts.put("wikiRevisions", revisions.size());
        counts.put("sourceBytes", sourceBytes);
        counts.put("python", pythonManifest);

        // 1) 元信息与 MySQL 快照
        writeJsonEntry(zos, kbRoot + "/manifest.json",
                buildManifest(library, includeFiles, pythonManifest), entryStats);
        writeJsonEntry(zos, kbRoot + "/library.json",
                buildLibraryNode(library, shareDeptIds, extValues), entryStats);
        writeJsonEntry(zos, kbRoot + "/documents.json", toDocumentMaps(documents), entryStats);
        writeJsonEntry(zos, kbRoot + "/folders.json", folders, entryStats);
        writeJsonEntry(zos, kbRoot + "/vector_tasks.json", toTaskMaps(tasks), entryStats);
        writeJsonEntry(zos, kbRoot + "/wiki/revisions.json", revisions, entryStats);

        // 2) 数据面（python 流式 NDJSON → 直接写入 ZIP entry）
        //    注意：python 侧路由是 /api/v1/export/{kbId}/...，这里必须拼完整前缀
        String exportBase = PYTHON_EXPORT_BASE + kbId;
        copyPythonStream(zos, kbRoot, "chunks/chunks.jsonl", exportBase + "/chunks", entryStats, warnings);
        copyPythonStream(zos, kbRoot, "vectors/vectors.jsonl", exportBase + "/vectors", entryStats, warnings);
        copyPythonStream(zos, kbRoot, "wiki/pages.jsonl", exportBase + "/wiki?doc_type=page", entryStats, warnings);
        copyPythonStream(zos, kbRoot, "wiki/folders.jsonl", exportBase + "/wiki?doc_type=folder", entryStats, warnings);
        copyPythonStream(zos, kbRoot, "wiki/issues.jsonl", exportBase + "/wiki?doc_type=issue", entryStats, warnings);

        // 3) 图片回图对象（随包导出，导入侧放回图片桶即可按相对 key 在新环境回显）
        copyImageArchive(zos, kbRoot, exportBase + "/images", entryStats, warnings);
        counts.put("images", entryStats.get("images/"));

        // 4) 原始文档（best-effort：单文件失败只记告警）
        if (includeFiles) {
            writeSourceFiles(zos, kbRoot, documents, entryStats, warnings);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", library.getName());
        result.put("counts", counts);
        result.put("entries", entryStats);
        return result;
    }

    // ==================== 元数据组装 ====================

    private LibraryDO requireVisibleLibrary(Long kbId) {
        LibraryDO library = kbId == null ? null : libraryService.getLibrary(kbId);
        if (library == null) {
            throw exception(LIBRARY_NOT_EXISTS);
        }
        if (!libraryService.canSeeKb(kbId)) {
            throw exception(LIBRARY_PERMISSION_DENIED);
        }
        return library;
    }

    /**
     * 当前用户可见的全部知识库（与列表页同一套 VisibilityRuleEngine 过滤）。
     */
    private List<LibraryDO> listVisibleLibraries() {
        LibraryPageReqVO pageReqVO = new LibraryPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<LibraryDO> page = libraryService.getLibraryPage(pageReqVO);
        return page == null || page.getList() == null ? Collections.emptyList() : page.getList();
    }

    private List<Map<String, Object>> toKbBriefList(List<LibraryDO> libraries) {
        List<Map<String, Object>> list = new ArrayList<>(libraries.size());
        for (LibraryDO library : libraries) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", library.getId());
            item.put("name", library.getName());
            item.put("categoryId", library.getCategoryId());
            item.put("kbLevelId", library.getKbLevelId());
            item.put("docCount", library.getDocCount());
            item.put("directory", exportRootDir(library.getId(), library.getName()));
            list.add(item);
        }
        return list;
    }

    private Map<String, Object> buildManifest(LibraryDO library, boolean includeFiles,
                                              Map<String, Object> pythonManifest) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("format", FORMAT);
        manifest.put("formatVersion", FORMAT_VERSION);
        manifest.put("scope", "single");
        manifest.put("exportedAt", now());
        manifest.put("tenantId", TenantContextHolder.getTenantId());
        manifest.put("kbId", library.getId());
        manifest.put("kbName", library.getName());
        manifest.put("kbDescription", library.getDescription());
        manifest.put("docCount", library.getDocCount());
        manifest.put("includeFiles", includeFiles);
        manifest.put("readOnly", true);
        manifest.put("pythonStorage", pythonManifest);
        // embedding 元信息（脱敏）：供导入侧校验「目标模型维度是否一致」，不一致无法写入同一向量集合
        manifest.put("embedding", buildEmbeddingMeta(library, pythonManifest));

        Map<String, String> layout = new LinkedHashMap<>();
        layout.put("library.json", "知识库配置：库行 + 层级配置 + 分类 + 共享部门 + 自定义字段 + processConfig");
        layout.put("documents.json", "文档元数据（kb_document 全字段）");
        layout.put("folders.json", "文档文件夹树（kb_document_folder）");
        layout.put("vector_tasks.json", "向量任务记录（kb_vector_task，含 stages 时间线）");
        layout.put("chunks/chunks.jsonl", "切片（Elasticsearch，每行一条 JSON，含父块/子块/图片块与 metadata）");
        layout.put("vectors/vectors.jsonl", "向量（Milvus，每行一条 JSON，含 vector 原始数值）");
        layout.put("wiki/pages.jsonl", "Wiki 页面（ES kb_wiki_all，含正文与 in/out 双向链接）");
        layout.put("wiki/folders.jsonl", "Wiki 目录实体（ES doc_type=folder）");
        layout.put("wiki/issues.jsonl", "Wiki 待修复问题（ES doc_type=issue）");
        layout.put("wiki/revisions.json", "Wiki 修订历史快照（kb_wiki_revision）");
        layout.put("images/", "图片回图对象（条目路径=图片桶内对象 key；导入侧按同 key 放回图片桶即可访问）");
        layout.put("files/", includeFiles ? "原始文档文件（{docId}_{文件名}，对应 documents.json 的 _export.archivePath）"
                : "本次未包含原始文件");
        layout.put("_export 字段", "chunks/wiki 每行的 _export.images（object_key/archive_path/portable_url）"
                + "与 _export.content_portable（正文图片 URL 的 {{MINIO_IMAGE_BASE_URL}} 占位符版本）");
        layout.put("export_report.json", "导出结果报告：条数/字节数/告警");
        manifest.put("layout", layout);
        return manifest;
    }

    /**
     * embedding 元信息（**不含 api_key**）：adapter/model/base_url/dimension + 向量实际维度。
     *
     * <p>导入前用它和目标环境的 embedding 配置比对：维度不一致直接拒绝导入，
     * 避免把不同维度的向量写进同一 Milvus 集合（会触发集合重建或写入失败）。</p>
     */
    private Map<String, Object> buildEmbeddingMeta(LibraryDO library, Map<String, Object> pythonManifest) {
        Map<String, Object> meta = new LinkedHashMap<>();
        Object vectorDim = pythonManifest.get("vector_dimension");
        if (vectorDim instanceof Number) {
            meta.put("vectorDimension", ((Number) vectorDim).intValue());
        }
        try {
            Map<String, Object> embedding = kbProcessConfigResolver.resolveEmbedding(library.getId());
            meta.put("adapter", embedding.get("adapter"));
            meta.put("model", embedding.get("model"));
            meta.put("base_url", embedding.get("base_url"));
            meta.put("dimension", embedding.get("dimension"));
            meta.put("vlSupported", embedding.get("vl_supported"));
        } catch (Exception e) {
            log.warn("[kb-export] 解析 embedding 配置失败 kbId={}: {}", library.getId(), e.getMessage());
            meta.put("error", e.getMessage());
        }
        return meta;
    }

    /**
     * 知识库配置节点：库行（processConfig 反序列化为对象便于阅读）+ 层级配置 + 分类 + 共享部门 + 自定义字段。
     */
    private Map<String, Object> buildLibraryNode(LibraryDO library, List<Long> shareDeptIds,
                                                 Map<String, String> extValues) {
        Map<String, Object> node = new LinkedHashMap<>();
        Map<String, Object> libraryMap = JsonUtils.parseObject(
                JsonUtils.toJsonString(library), new TypeReference<Map<String, Object>>() {
                });
        if (libraryMap == null) {
            libraryMap = new LinkedHashMap<>();
        }
        // processConfig 在 DO 中是 JSON 字符串，导出时转为对象，便于阅读与后续导入
        if (StrUtil.isNotBlank(library.getProcessConfig())) {
            Map<String, Object> processConfig = JsonUtils.parseMap(library.getProcessConfig());
            libraryMap.put("processConfig", processConfig != null ? processConfig : library.getProcessConfig());
        }
        node.put("library", libraryMap);

        LevelConfigDO levelConfig = library.getKbLevelId() == null
                ? null : levelConfigMapper.selectById(library.getKbLevelId());
        node.put("levelConfig", levelConfig);
        CategoryDO category = library.getCategoryId() == null
                ? null : categoryMapper.selectById(library.getCategoryId());
        node.put("category", category);
        node.put("shareDeptIds", shareDeptIds);
        node.put("extValues", extValues);
        return node;
    }

    /**
     * 文档元数据转 Map：额外写入 `_export`，标明源文件在包内的相对路径
     * （`file_config_id/file_path/file_url` 属于原环境存储引用，导入时需重新上传并回填）。
     */
    private List<Map<String, Object>> toDocumentMaps(List<DocumentDO> documents) {
        List<Map<String, Object>> list = new ArrayList<>(documents.size());
        for (DocumentDO document : documents) {
            Map<String, Object> map = JsonUtils.parseObject(
                    JsonUtils.toJsonString(document), new TypeReference<Map<String, Object>>() {
                    });
            if (map == null) {
                map = new LinkedHashMap<>();
            }
            Map<String, Object> exportMeta = new LinkedHashMap<>();
            exportMeta.put("archivePath", "files/" + sourceFileName(document));
            Map<String, Object> storage = new LinkedHashMap<>();
            storage.put("fileConfigId", document.getFileConfigId());
            storage.put("filePath", document.getFilePath());
            storage.put("fileUrl", document.getFileUrl());
            exportMeta.put("sourceStorage", storage);
            map.put("_export", exportMeta);
            list.add(map);
        }
        return list;
    }

    /**
     * 源文件在包内的文件名：`{docId}_{安全文件名}`（documents.json 的 `_export.archivePath` 与此一致）。
     */
    private String sourceFileName(DocumentDO document) {
        String name = StrUtil.isBlank(document.getFileName())
                ? ("doc-" + document.getId()) : document.getFileName();
        return document.getId() + "_" + safeSegment(name, MAX_FILE_NAME_LENGTH);
    }

    /**
     * 向量任务转 Map：stages / params 为 JSON 字符串，导出时反序列化便于分析（失败保留原文）。
     */
    private List<Map<String, Object>> toTaskMaps(List<VectorTaskDO> tasks) {
        List<Map<String, Object>> list = new ArrayList<>(tasks.size());
        for (VectorTaskDO task : tasks) {
            Map<String, Object> map = JsonUtils.parseObject(
                    JsonUtils.toJsonString(task), new TypeReference<Map<String, Object>>() {
                    });
            if (map == null) {
                map = new LinkedHashMap<>();
            }
            map.put("stages", parseJsonQuietly(task.getStages()));
            map.put("params", parseJsonQuietly(task.getParams()));
            list.add(map);
        }
        return list;
    }

    private Object parseJsonQuietly(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return JsonUtils.parseTree(json);
        } catch (Exception e) {
            return json;
        }
    }

    // ==================== 数据面流式拷贝 ====================

    /**
     * 调用 python 导出接口，把 NDJSON 流原样写入 ZIP entry（不解析、不缓存全量）。
     *
     * <p>这里用 {@link ClientHttpRequest} 手工建连而不是 RestTemplate.execute：
     * execute 会把 extractor 内抛出的 IOException 统一包成 ResourceAccessException，
     * 导致「数据面读取失败」与「外层 ZIP 写失败（客户端断开）」无法区分。</p>
     */
    private void copyPythonStream(ZipOutputStream zos, String root, String entryName, String pythonPath,
                                  Map<String, Object> entryStats, List<String> warnings) throws IOException {
        String entryPath = root + "/" + entryName;
        zos.putNextEntry(new ZipEntry(entryPath));
        try {
            ClientHttpResponse response;
            try {
                response = openPythonGet(pythonPath, MediaType.parseMediaType(NDJSON_MEDIA_TYPE));
            } catch (IOException e) {
                log.warn("[kb-export] 连接 python 失败 {}: {}", entryPath, e.getMessage());
                warnings.add(entryName + " 导出失败：" + e.getMessage());
                entryStats.put(entryName, StreamStat.failed(e.getMessage()).toMap());
                return;
            }
            try (ClientHttpResponse ignored = response) {
                int status = response.getStatusCode().value();
                if (status < 200 || status >= 300) {
                    warnings.add(entryName + " 导出失败：python 返回 HTTP " + status);
                    entryStats.put(entryName, StreamStat.failed("HTTP " + status).toMap());
                    return;
                }
                InputStream in = response.getBody();
                if (in == null) {
                    warnings.add(entryName + " 导出失败：python 返回空响应体");
                    entryStats.put(entryName, StreamStat.failed("empty body").toMap());
                    return;
                }
                StreamStat stat = new StreamStat();
                byte[] buf = new byte[COPY_BUFFER_SIZE];
                while (true) {
                    int n;
                    try {
                        n = in.read(buf);
                    } catch (IOException e) {
                        // 数据面读失败：保留已写内容并告警，不中断整包
                        warnings.add(entryName + " 读取中断：" + e.getMessage());
                        stat.error = e.getMessage();
                        break;
                    }
                    if (n == -1) {
                        break;
                    }
                    zos.write(buf, 0, n); // 外层 ZIP 写失败（客户端断开）继续向上抛
                    stat.bytes += n;
                    for (int i = 0; i < n; i++) {
                        if (buf[i] == '\n') {
                            stat.lines++;
                        }
                    }
                }
                entryStats.put(entryName, stat.toMap());
            }
        } finally {
            closeEntryQuietly(zos, entryPath);
        }
    }

    /**
     * 导出图片回图对象：python 返回「条目名=桶内对象 key」的 ZIP 流，这里解包后
     * 平铺写入外层 ZIP 的 `images/` 目录（导入侧按同 key 放回图片桶即可访问）。
     */
    private void copyImageArchive(ZipOutputStream zos, String root, String pythonPath,
                                  Map<String, Object> entryStats, List<String> warnings) throws IOException {
        long count = 0;
        long totalBytes = 0;
        ClientHttpResponse response;
        try {
            response = openPythonGet(pythonPath, MediaType.parseMediaType("application/zip"));
        } catch (IOException e) {
            log.warn("[kb-export] 连接 python 图片导出失败: {}", e.getMessage());
            warnings.add("images 导出失败：" + e.getMessage());
            entryStats.put("images/", imageStat(0, 0, e.getMessage()));
            return;
        }
        try (ClientHttpResponse ignored = response) {
            int status = response.getStatusCode().value();
            if (status < 200 || status >= 300) {
                warnings.add("images 导出失败：python 返回 HTTP " + status);
                entryStats.put("images/", imageStat(0, 0, "HTTP " + status));
                return;
            }
            InputStream in = response.getBody();
            if (in == null) {
                entryStats.put("images/", imageStat(0, 0, "empty body"));
                return;
            }
            try (ZipInputStream zis = new ZipInputStream(in, StandardCharsets.UTF_8)) {
                while (true) {
                    ZipEntry entry;
                    try {
                        entry = zis.getNextEntry();
                    } catch (IOException e) {
                        // 内层 ZIP 结构损坏/中断：已写出的图片保留，其余跳过
                        warnings.add("images 包读取中断（部分图片可能缺失）：" + e.getMessage());
                        break;
                    }
                    if (entry == null) {
                        break;
                    }
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String name = safeArchivePath(entry.getName());
                    if (name == null) {
                        warnings.add("images 跳过非法路径：" + entry.getName());
                        continue;
                    }
                    String entryPath = root + "/images/" + name;
                    zos.putNextEntry(new ZipEntry(entryPath));
                    long bytes = 0;
                    byte[] buf = new byte[COPY_BUFFER_SIZE];
                    try {
                        while (true) {
                            int n;
                            try {
                                n = zis.read(buf);
                            } catch (IOException e) {
                                warnings.add("图片读取失败：" + name + "（" + e.getMessage() + "）");
                                break;
                            }
                            if (n == -1) {
                                break;
                            }
                            zos.write(buf, 0, n); // 外层写失败继续向上抛
                            bytes += n;
                        }
                    } finally {
                        closeEntryQuietly(zos, entryPath);
                    }
                    count++;
                    totalBytes += bytes;
                }
            }
        } finally {
            entryStats.put("images/", imageStat(count, totalBytes, null));
        }
    }

    private Map<String, Object> imageStat(long count, long bytes, String error) {
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("count", count);
        stat.put("bytes", bytes);
        if (error != null) {
            stat.put("error", error);
        }
        return stat;
    }

    /**
     * 手工建立 python GET 连接（返回未关闭的响应，调用方负责 close()）。
     */
    private ClientHttpResponse openPythonGet(String pythonPath, MediaType accept) throws IOException {
        URI uri = URI.create(pythonUrl(pythonPath));
        ClientHttpRequest request = kbExportRestTemplate.getRequestFactory()
                .createRequest(uri, HttpMethod.GET);
        request.getHeaders().setAccept(Collections.singletonList(accept));
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            request.getHeaders().set("x-tenant-id", String.valueOf(tenantId));
        }
        return request.execute();
    }

    /**
     * 归一化压缩包内路径：去掉前导 `/`、反斜杠，拒绝 `..` 段（防 zip-slip）。
     */
    private String safeArchivePath(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String path = raw.replace('\\', '/').replaceAll("^/+", "");
        for (String segment : path.split("/")) {
            if ("..".equals(segment)) {
                return null;
            }
        }
        return path.isEmpty() ? null : path;
    }

    /**
     * 原始文档写入 files/（best-effort）：单个文件失败只记告警。
     */
    private void writeSourceFiles(ZipOutputStream zos, String root, List<DocumentDO> documents,
                                  Map<String, Object> entryStats, List<String> warnings) throws IOException {
        int ok = 0;
        int failed = 0;
        long totalBytes = 0;
        for (DocumentDO document : documents) {
            String entryPath = root + "/files/" + sourceFileName(document);
            byte[] content;
            try {
                // 读取失败（存储不可用/对象缺失）只告警，不中断整包导出
                content = documentService.getDocumentContent(document);
            } catch (Exception e) {
                failed++;
                log.warn("[kb-export] 读取源文件失败 docId={}: {}", document.getId(), e.getMessage());
                warnings.add("源文件读取失败：" + entryPath + "（" + e.getMessage() + "）");
                continue;
            }
            if (content == null || content.length == 0) {
                failed++;
                warnings.add("源文件缺失或不可读：" + entryPath);
                continue;
            }
            zos.putNextEntry(new ZipEntry(entryPath));
            try {
                zos.write(content);
            } finally {
                closeEntryQuietly(zos, entryPath);
            }
            ok++;
            totalBytes += content.length;
        }
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("count", ok);
        stat.put("failed", failed);
        stat.put("bytes", totalBytes);
        entryStats.put("files/", stat);
    }

    // ==================== python 接口调用 ====================

    private Map<String, Object> fetchPythonManifest(Long kbId) {
        try {
            Map<String, Object> body = requestPythonManifest(kbId);
            Object code = body.get("code");
            if (!(code instanceof Number) || ((Number) code).intValue() != 0) {
                throw new IllegalStateException("python 返回 code=" + code);
            }
            Object data = body.get("data");
            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> manifest = (Map<String, Object>) data;
                return manifest;
            }
            throw new IllegalStateException("python 返回数据为空");
        } catch (Exception e) {
            log.error("[kb-export] 获取 python 导出清单失败 kbId={}: {}", kbId, e.getMessage());
            throw exception(KB_EXPORT_FAILED);
        }
    }

    /**
     * 全库导出时数据面失败不应中断整体：记录告警并继续（该库切片/向量/Wiki 会在流式阶段再报一次）。
     */
    private Map<String, Object> tryFetchPythonManifest(Long kbId, List<String> warnings) {
        try {
            return fetchPythonManifest(kbId);
        } catch (Exception e) {
            warnings.add("知识库 " + kbId + " 数据面统计失败：" + e.getMessage());
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requestPythonManifest(Long kbId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            headers.set("x-tenant-id", String.valueOf(tenantId));
        }
        ResponseEntity<Map> response = kbExportRestTemplate.exchange(
                pythonUrl(PYTHON_EXPORT_BASE + kbId + "/manifest"),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<?, ?> body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("python 返回空响应");
        }
        return (Map<String, Object>) body;
    }

    private String pythonUrl(String path) {
        return vectorTaskConfig.getPythonServiceUrl() + path;
    }

    private int pythonCount(Map<String, Object> manifest, String key) {
        Object counts = manifest.get("counts");
        if (counts instanceof Map) {
            Object value = ((Map<?, ?>) counts).get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        return -1;
    }

    /**
     * python 返回的图片对象统计（counts.images = {count, bytes}）；不可用时给 -1。
     */
    private Map<String, Object> pythonImageCounts(Map<String, Object> manifest) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", -1);
        result.put("bytes", -1);
        Object counts = manifest.get("counts");
        if (!(counts instanceof Map)) {
            return result;
        }
        Object images = ((Map<?, ?>) counts).get("images");
        if (!(images instanceof Map)) {
            return result;
        }
        Map<?, ?> imageMap = (Map<?, ?>) images;
        Object count = imageMap.get("count");
        Object bytes = imageMap.get("bytes");
        if (count instanceof Number) {
            result.put("count", ((Number) count).intValue());
        }
        if (bytes instanceof Number) {
            result.put("bytes", ((Number) bytes).longValue());
        }
        Object error = imageMap.get("error");
        if (error != null) {
            result.put("error", error);
        }
        return result;
    }

    // ==================== MySQL 只读查询 ====================

    private List<DocumentDO> listDocuments(Long kbId) {
        return documentMapper.selectList(new LambdaQueryWrapperX<DocumentDO>()
                .eq(DocumentDO::getKbId, kbId)
                .orderByAsc(DocumentDO::getId));
    }

    private List<VectorTaskDO> listVectorTasks(Long kbId) {
        return vectorTaskMapper.selectList(new LambdaQueryWrapperX<VectorTaskDO>()
                .eq(VectorTaskDO::getKbId, kbId)
                .orderByAsc(VectorTaskDO::getId));
    }

    private List<WikiPageRevisionDO> listWikiRevisions(Long kbId) {
        return wikiPageRevisionMapper.selectList(new LambdaQueryWrapperX<WikiPageRevisionDO>()
                .eq(WikiPageRevisionDO::getKbId, kbId)
                .orderByAsc(WikiPageRevisionDO::getId));
    }

    private List<Long> listShareDeptIds(Long kbId) {
        List<ShareDeptDO> list = shareDeptMapper.selectList(new LambdaQueryWrapperX<ShareDeptDO>()
                .eq(ShareDeptDO::getKbId, kbId));
        List<Long> ids = new ArrayList<>(list.size());
        for (ShareDeptDO item : list) {
            if (item.getDeptId() != null) {
                ids.add(item.getDeptId());
            }
        }
        return ids;
    }

    // ==================== ZIP / HTTP 辅助 ====================

    private ZipOutputStream newZipStream(HttpServletResponse response) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8);
        // 向量 JSON 压缩收益低、CPU 开销大：用最快压缩档位保证吞吐
        zos.setLevel(Deflater.BEST_SPEED);
        return zos;
    }

    private void writeJsonEntry(ZipOutputStream zos, String entryPath, Object data,
                                Map<String, Object> entryStats) throws IOException {
        byte[] bytes = JsonUtils.toJsonPrettyString(data).getBytes(StandardCharsets.UTF_8);
        zos.putNextEntry(new ZipEntry(entryPath));
        try {
            zos.write(bytes);
        } finally {
            closeEntryQuietly(zos, entryPath);
        }
        if (entryStats != null) {
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("bytes", (long) bytes.length);
            entryStats.put(StrUtil.subAfter(entryPath, "/", true), stat);
        }
    }

    private void writeReportQuietly(ZipOutputStream zos, String entryPath, Map<String, Object> report) {
        try {
            writeJsonEntry(zos, entryPath, report, null);
        } catch (Exception e) {
            log.warn("[kb-export] 写入导出报告失败 {}: {}", entryPath, e.getMessage());
        }
    }

    private Map<String, Object> buildReport(String scope, boolean success, String error,
                                            List<String> warnings, Object kbs) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("success", success);
        // partialFailure：整体完成，但存在数据面/源文件级别的失败项（见 warnings）
        report.put("partialFailure", success && !warnings.isEmpty());
        report.put("error", error);
        report.put("format", FORMAT);
        report.put("formatVersion", FORMAT_VERSION);
        report.put("scope", scope);
        report.put("finishedAt", now());
        report.put("kbs", kbs);
        report.put("warnings", warnings);
        return report;
    }

    private void prepareResponse(HttpServletResponse response, String zipName) {
        String asciiFallback = "kb-export.zip";
        String encoded = URLEncoder.encode(zipName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encoded);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        // 让前端能读到文件名（预留）
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }

    private String buildZipFileName(Long kbId, String name) {
        String safeName = safeSegment(name, 40);
        return StrUtil.isBlank(safeName)
                ? "kb-export-" + kbId + "-" + nowStamp() + ".zip"
                : "kb-export-" + kbId + "-" + safeName + "-" + nowStamp() + ".zip";
    }

    private String exportRootDir(Long kbId, String name) {
        String safeName = safeSegment(name, 40);
        return StrUtil.isBlank(safeName) ? "kb-" + kbId : "kb-" + kbId + "-" + safeName;
    }

    /**
     * 文件名安全化：去掉路径分隔符与 Windows 非法字符、控制字符，并限制长度。
     */
    private String safeSegment(String raw, int maxLength) {
        if (StrUtil.isBlank(raw)) {
            return "";
        }
        String cleaned = raw.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_").trim();
        cleaned = cleaned.replaceAll("\\.+$", "");
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        return cleaned;
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private String nowStamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private void closeEntryQuietly(ZipOutputStream zos, String entryPath) {
        try {
            zos.closeEntry();
        } catch (Exception e) {
            log.debug("[kb-export] 关闭 zip entry 失败 {}: {}", entryPath, e.getMessage());
        }
    }

    private void closeQuietly(OutputStream out, Long kbId) {
        try {
            out.close();
        } catch (Exception e) {
            log.debug("[kb-export] 关闭导出流失败 kbId={}: {}", kbId, e.getMessage());
        }
    }

    /**
     * 单条流式导出统计（字节数 + NDJSON 行数）。
     */
    private static class StreamStat {
        private long bytes;
        private long lines;
        private String error;

        static StreamStat failed(String error) {
            StreamStat stat = new StreamStat();
            stat.error = error;
            return stat;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("bytes", bytes);
            map.put("records", lines);
            if (error != null) {
                map.put("error", error);
            }
            return map;
        }
    }
}
