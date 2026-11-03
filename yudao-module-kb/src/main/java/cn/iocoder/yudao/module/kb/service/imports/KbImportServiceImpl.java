package cn.iocoder.yudao.module.kb.service.imports;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.FileUploadRespVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.folder.FolderDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.modelconfig.ModelConfigDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.wiki.WikiPageRevisionDO;
import cn.iocoder.yudao.module.kb.dal.mysql.category.CategoryMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.document.DocumentMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.folder.FolderMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.levelconfig.LevelConfigMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.modelconfig.ModelConfigMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.wiki.WikiPageRevisionMapper;
import cn.iocoder.yudao.module.kb.framework.config.VectorTaskConfig;
import cn.iocoder.yudao.module.kb.service.kbconfig.KbProcessConfigResolver;
import cn.iocoder.yudao.module.kb.service.libraryext.LibraryExtService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 知识库完整导入实现
 *
 * <p>流程：</p>
 * <ol>
 *   <li>解压到临时目录（防 zip-slip），定位包根目录（含 manifest.json）；</li>
 *   <li><b>模型/维度预检</b>（调 python `/api/v1/import/precheck`）：维度不一致直接中止，不落任何数据；</li>
 *   <li>新建知识库（新 ID，不改原库），导入自定义字段、文件夹、文档记录（源文件重新上传）；</li>
 *   <li>图片按新 kb/doc id 重写 key 后写回图片桶；</li>
 *   <li>切片/向量/Wiki 逐行改写（document_id、_es_id、id、knowledge_base_id、图片 URL）后流式转发 python 写入；</li>
 *   <li>导入 Wiki 修订快照，更新 docCount，输出报告。</li>
 * </ol>
 */
@Service
@Slf4j
public class KbImportServiceImpl implements KbImportService {

    /** NDJSON 内容类型 */
    private static final String NDJSON = "application/x-ndjson";
    /** 正文图片地址占位符（导出侧生成，导入时替换为新环境 MINIO_IMAGE_BASE_URL） */
    private static final String IMAGE_BASE_PLACEHOLDER = "{{MINIO_IMAGE_BASE_URL}}";
    private static final String DEFAULT_IMAGE_BUCKET = "ckb-images";
    /** 单行 JSON 解析失败/文档映射缺失时计入 skipped */
    private static final int MAX_WARNINGS = 200;
    /** 分片上传会话元数据文件名 */
    private static final String META_FILE = "meta.json";
    /** 合并后的完整导入包文件名 */
    private static final String SOURCE_FILE_NAME = "package.zip";

    @Resource
    private LibraryMapper libraryMapper;
    @Resource
    private FolderMapper folderMapper;
    @Resource
    private DocumentMapper documentMapper;
    @Resource
    private WikiPageRevisionMapper wikiPageRevisionMapper;
    @Resource
    private ModelConfigMapper modelConfigMapper;
    @Resource
    private LevelConfigMapper levelConfigMapper;
    @Resource
    private CategoryMapper categoryMapper;
    @Resource
    private LibraryExtService libraryExtService;
    @Resource
    private FileApi fileApi;
    @Resource
    private KbProcessConfigResolver kbProcessConfigResolver;
    @Resource
    private VectorTaskConfig vectorTaskConfig;
    @Resource(name = "kbExportRestTemplate")
    private RestTemplate kbExportRestTemplate;
    @Resource
    private KbImportTaskReporter importTaskReporter;

    /**
     * 导入后台执行器：导入耗时可能几十分钟（尤其 reembed），不能占用 HTTP 请求线程。
     */
    private final ExecutorService importExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "kb-import-worker");
        thread.setDaemon(true);
        return thread;
    });

    /** 同一分片会话只允许一个导入任务在跑 */
    private final Set<String> runningUploads = ConcurrentHashMap.newKeySet();

    // ==================== 主流程 ====================

    @Override
    public Map<String, Object> importLibrary(MultipartFile file, String vectorMode) throws IOException {
        if (file == null || file.isEmpty()) {
            throw exception(KB_IMPORT_PACKAGE_INVALID);
        }
        Path work = Files.createTempDirectory("kb-import-");
        try (InputStream input = file.getInputStream()) {
            unzip(input, work);
            // 直传接口保持同步返回（脚本/小包场景），不产生可轮询任务
            return doImport(work, normalizeVectorMode(vectorMode), null);
        } finally {
            deleteQuietly(work);
        }
    }

    /**
     * 解压已完成后的导入主流程（临时目录由调用方负责清理）。
     *
     * @param taskId 异步任务 ID（null 表示同步直传，不上报进度）
     */
    private Map<String, Object> doImport(Path work, String vectorMode, String taskId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        result.put("success", false);
        result.put("vectorMode", vectorMode);
        result.put("warnings", warnings);
        Long newKbId = null;
        try {
            report(taskId, "解析导入包", 5, "读取包内元数据");
            Path kbRoot = locatePackageRoot(work);
            Map<String, Object> manifest = readJsonMap(kbRoot.resolve("manifest.json"));
            Map<String, Object> libraryNode = readJsonMap(kbRoot.resolve("library.json"));
            if (libraryNode.isEmpty()) {
                throw exception(KB_IMPORT_PACKAGE_INVALID);
            }
            List<Map<String, Object>> documents = readJsonList(kbRoot.resolve("documents.json"));
            List<Map<String, Object>> folders = readJsonList(kbRoot.resolve("folders.json"));
            List<Map<String, Object>> revisions = readJsonList(kbRoot.resolve("wiki/revisions.json"));

            // 1. 预检（模型 / 向量维度）：不一致时中止；若包内有切片，可提示改用 reembed 重试
            report(taskId, "模型校验", 10, "校验 embedding 模型与向量维度");
            Map<String, Object> precheck = callPrecheck(manifest, kbRoot, vectorMode);
            result.put("precheck", precheck);
            if (!Boolean.TRUE.equals(precheck.get("ok"))) {
                result.put("message", joinMessages(precheck.get("errors")));
                // 包内存在切片 → 可用「重跑向量」模式重试（分片上传会话会被保留）
                result.put("retryable", VECTOR_MODE_REUSE.equals(vectorMode) && hasChunks(kbRoot));
                return result;
            }
            warnings.addAll(stringList(precheck.get("warnings")));

            Map<String, Object> imageMeta = asMap(precheck.get("image"));
            String bucket = StrUtil.blankToDefault(str(imageMeta.get("bucket")), DEFAULT_IMAGE_BUCKET);
            String imageBase = StrUtil.blankToDefault(str(imageMeta.get("base_url")), "");

            // 图片地址可访问性校验：python 侧 MINIO_IMAGE_BASE_URL 若是容器/本机地址，
            // 导入会把 localhost 写进切片正文，浏览器必然打不开；此时直接中止并提示配置
            if (hasImages(kbRoot) && isUnreachableImageBase(imageBase)) {
                result.put("message", "当前环境的 MINIO_IMAGE_BASE_URL 不可用于浏览器访问（" + imageBase
                        + "），导入会把图片地址写成本机地址，已中止；"
                        + "请先在 python-vector 配置对外可访问的 MinIO 地址（如 http://<服务器IP>:8080/minio）后重试");
                result.put("retryable", false);
                return result;
            }

            // 2. 新建知识库（不覆盖原库）
            report(taskId, "创建知识库", 15, "创建新知识库");
            LibraryDO library = createLibrary(libraryNode, precheck, warnings);
            newKbId = library.getId();
            result.put("kbId", newKbId);
            result.put("kbName", library.getName());
            report(taskId, "创建知识库", 18, "知识库 #" + newKbId + " 已创建");

            // 3. 文件夹 / 文档（源文件重新上传）
            Map<Long, Long> folderMap = importFolders(newKbId, folders);
            Map<Long, DocumentDO> docEntities = new LinkedHashMap<>();
            Map<Long, Long> docMap = importDocuments(newKbId, documents, folderMap, kbRoot,
                    docEntities, VECTOR_MODE_SKIP.equals(vectorMode) ? 0 : 2, taskId, warnings);

            // 4. 图片对象（按新 kb/doc id 重写 key）
            int imageCount = importImages(newKbId, docMap, kbRoot, taskId, warnings);

            // 5. 切片 / Wiki（ES 数据直接复用，无需重新解析）
            Map<String, Object> chunkStat = importChunks(kbRoot, newKbId, docMap, docEntities,
                    bucket, imageBase, taskId);
            Map<String, Object> wikiStat = importWiki(kbRoot, newKbId, docMap, bucket, imageBase, taskId);
            report(taskId, "写入修订", 88, "写入 Wiki 修订快照");
            int revisionCount = importWikiRevisions(newKbId, revisions, warnings);

            // 6. 向量：沿用包内 / 用切片重跑 / 跳过
            report(taskId, "向量", 90, VECTOR_MODE_REEMBED.equals(vectorMode)
                    ? "开始按切片重新向量化" : "开始写入向量");
            Map<String, Object> vectorStat;
            if (VECTOR_MODE_SKIP.equals(vectorMode)) {
                vectorStat = new LinkedHashMap<>();
                vectorStat.put("mode", VECTOR_MODE_SKIP);
                vectorStat.put("note", "未导入向量，可稍后在文档详情触发重建");
            } else if (VECTOR_MODE_REEMBED.equals(vectorMode)) {
                vectorStat = reembedVectors(newKbId, precheck, kbRoot, taskId);
                Object status = vectorStat.get("status");
                if (!"done".equals(status)) {
                    warnings.add("向量重建未完成（status=" + status + "）："
                            + StrUtil.blankToDefault(str(vectorStat.get("error")),
                            StrUtil.blankToDefault(str(vectorStat.get("note")), "可稍后重试")));
                }
            } else {
                vectorStat = importVectors(kbRoot, newKbId, docMap, docEntities, bucket, imageBase, taskId);
                vectorStat.put("mode", VECTOR_MODE_REUSE);
            }

            // 7. docCount 回填
            LibraryDO update = new LibraryDO();
            update.setId(newKbId);
            update.setDocCount(docMap.size());
            libraryMapper.updateById(update);

            report(taskId, "完成", 100, "导入完成");
            result.put("success", true);
            result.put("message", "导入完成");
            result.put("documents", docMap.size());
            result.put("folders", folderMap.size());
            result.put("images", imageCount);
            result.put("chunks", chunkStat);
            result.put("vectors", vectorStat);
            result.put("wiki", wikiStat);
            result.put("wikiRevisions", revisionCount);
            return result;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[kb-import] 导入失败", e);
            String message = "导入失败：" + e.getMessage();
            if (newKbId != null) {
                message += "（已创建知识库 #" + newKbId + "，可在列表中删除后重试）";
            }
            result.put("message", message);
            return result;
        }
    }

    private boolean hasChunks(Path kbRoot) {
        Path file = kbRoot.resolve("chunks/chunks.jsonl");
        try {
            return Files.isRegularFile(file) && Files.size(file) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 包内是否包含图片对象（有图片才需要校验回图地址可访问性）。
     */
    private boolean hasImages(Path kbRoot) {
        Path dir = kbRoot.resolve("images");
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.anyMatch(Files::isRegularFile);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 回图 base 是否为容器内/本机地址（浏览器访问不到）。
     */
    private boolean isUnreachableImageBase(String baseUrl) {
        if (StrUtil.isBlank(baseUrl)) {
            return true;
        }
        String lower = baseUrl.toLowerCase();
        return lower.contains("localhost") || lower.contains("127.0.0.1") || lower.contains("0.0.0.0");
    }

    /**
     * 上报导入进度（taskId 为空表示同步直传，不上报）。
     */
    private void report(String taskId, String stage, int progress, String message) {
        if (taskId == null) {
            return;
        }
        importTaskReporter.progress(taskId, stage, progress, message);
    }

    private String normalizeVectorMode(String vectorMode) {
        String mode = StrUtil.blankToDefault(vectorMode, VECTOR_MODE_REUSE).trim().toLowerCase();
        if (VECTOR_MODE_REEMBED.equals(mode) || VECTOR_MODE_SKIP.equals(mode)) {
            return mode;
        }
        return VECTOR_MODE_REUSE;
    }

    // ==================== 预检 ====================

    /**
     * 调 python 预检：包内向量维度 vs 目标 embedding 配置 / 目标 Milvus 集合维度。
     *
     * <p>reembed 模式下包内向量被丢弃，改为校验「目标模型维度 vs 目标集合维度」。</p>
     */
    private Map<String, Object> callPrecheck(Map<String, Object> manifest, Path kbRoot, String vectorMode) {
        Map<String, Object> packageEmbedding = asMap(manifest.get("embedding"));
        Integer dimension = intOrNull(packageEmbedding.get("vectorDimension"));
        if (dimension == null || dimension <= 0) {
            dimension = intOrNull(packageEmbedding.get("dimension"));
        }
        if (dimension == null || dimension <= 0) {
            dimension = readFirstVectorDimension(kbRoot);
        }

        Map<String, Object> target = new LinkedHashMap<>();
        try {
            Map<String, Object> embedding = kbProcessConfigResolver.resolveEmbedding(null);
            target.put("adapter", embedding.get("adapter"));
            target.put("model", embedding.get("model"));
            target.put("dimension", embedding.get("dimension"));
        } catch (Exception e) {
            log.warn("[kb-import] 解析目标 embedding 配置失败: {}", e.getMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("kb_id", null);
        body.put("dimension", dimension);
        body.put("model", str(packageEmbedding.get("model")));
        body.put("adapter", str(packageEmbedding.get("adapter")));
        body.put("embedding", target);
        body.put("vector_mode", vectorMode);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> response = kbExportRestTemplate.exchange(
                    pythonUrl("/api/v1/import/precheck"), HttpMethod.POST,
                    new HttpEntity<>(body, headers), Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !(responseBody.get("data") instanceof Map)) {
                return precheckFailure("python 预检返回为空");
            }
            return asMap(responseBody.get("data"));
        } catch (Exception e) {
            log.error("[kb-import] 预检调用失败: {}", e.getMessage());
            return precheckFailure("python-vector 预检接口不可用：" + e.getMessage());
        }
    }

    private Map<String, Object> precheckFailure(String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ok", false);
        data.put("errors", Collections.singletonList(message));
        data.put("warnings", Collections.emptyList());
        return data;
    }

    /**
     * 包内向量维度兜底：读 vectors.jsonl 第一行的 vector 长度。
     */
    private Integer readFirstVectorDimension(Path kbRoot) {
        Path vectors = kbRoot.resolve("vectors/vectors.jsonl");
        if (!Files.isRegularFile(vectors)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(vectors, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            if (StrUtil.isBlank(line)) {
                return null;
            }
            JsonNode node = JsonUtils.parseTree(line);
            return node == null ? null : node.path("vector").size();
        } catch (Exception e) {
            log.warn("[kb-import] 读取向量维度失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 建库 / 文件夹 / 文档 ====================

    private LibraryDO createLibrary(Map<String, Object> libraryNode, Map<String, Object> precheck,
                                    List<String> warnings) {
        Map<String, Object> source = asMap(libraryNode.get("library"));
        LibraryDO library = new LibraryDO();
        library.setName(uniqueLibraryName(str(source.get("name"))));
        library.setDescription(str(source.get("description")));
        library.setCoverUrl(str(source.get("coverUrl")));
        library.setStatus(0);
        // 导入库默认不公开，避免原库的公开状态在新环境误暴露
        library.setIsPublic(0);
        library.setIsProject(intOrNull(source.get("isProject")));
        library.setOwnerId(SecurityFrameworkUtils.getLoginUserId());
        library.setDocCount(0);

        Long levelId = longOrNull(source.get("kbLevelId"));
        if (levelId != null && levelConfigMapper.selectById(levelId) != null) {
            library.setKbLevelId(levelId);
        } else if (levelId != null) {
            warnings.add("原库层级配置 #" + levelId + " 在当前环境不存在，已置空");
        }
        Long categoryId = longOrNull(source.get("categoryId"));
        if (categoryId != null && categoryMapper.selectById(categoryId) != null) {
            library.setCategoryId(categoryId);
        } else if (categoryId != null) {
            warnings.add("原库分类 #" + categoryId + " 在当前环境不存在，已置空");
        }

        Integer packageDim = intOrNull(asMap(precheck.get("package")).get("dimension"));
        library.setProcessConfig(sanitizeProcessConfig(source.get("processConfig"), packageDim, warnings));
        libraryMapper.insert(library);
        // 推送 KB 级配置（python 侧检索/入库按 kb_id 读取）
        kbProcessConfigResolver.publishKbConfig(library.getId());

        Map<String, String> extValues = asStringMap(libraryNode.get("extValues"));
        if (!extValues.isEmpty()) {
            libraryExtService.replaceExtValues(library.getId(), extValues);
        }
        // 共享部门不在包里恢复：跨环境部门 ID 不可移植，避免绑定到错误部门
        if (libraryNode.get("shareDeptIds") instanceof List<?> deptIds && !deptIds.isEmpty()) {
            warnings.add("原库共享部门未导入（部门 ID 不可跨环境复用），请在新环境重新配置可见范围");
        }
        return library;
    }

    private String uniqueLibraryName(String sourceName) {
        String base = StrUtil.blankToDefault(sourceName, "导入知识库");
        String candidate = base + "（导入）";
        int index = 2;
        while (existsLibraryName(candidate)) {
            candidate = base + "（导入" + index + "）";
            index++;
        }
        return candidate;
    }

    private boolean existsLibraryName(String name) {
        Long count = libraryMapper.selectCount(new LambdaQueryWrapperX<LibraryDO>()
                .eq(LibraryDO::getName, name));
        return count != null && count > 0;
    }

    /**
     * 保留原库 processConfig，但剔除此环境不存在的模型 UID（含维度不一致），避免入库/检索走错模型。
     */
    @SuppressWarnings("unchecked")
    private String sanitizeProcessConfig(Object raw, Integer packageDim, List<String> warnings) {
        if (!(raw instanceof Map)) {
            return null;
        }
        Map<String, Object> config = new LinkedHashMap<>((Map<String, Object>) raw);
        Object modelValue = config.get("model");
        if (modelValue instanceof Map) {
            Map<String, Object> model = new LinkedHashMap<>((Map<String, Object>) modelValue);
            Object uidValue = model.get("embedding_model_uid");
            if (uidValue instanceof String uid && StrUtil.isNotBlank(uid)) {
                ModelConfigDO modelConfig = modelConfigMapper.selectByUid(uid);
                if (modelConfig == null) {
                    model.remove("embedding_model_uid");
                    warnings.add("原库 embedding 模型 " + uid + " 在当前环境不存在，已回退租户默认模型");
                } else {
                    Integer modelDim = extractModelDimension(modelConfig);
                    if (packageDim != null && modelDim != null && !packageDim.equals(modelDim)) {
                        model.remove("embedding_model_uid");
                        warnings.add("原库 embedding 模型 " + uid + " 维度 " + modelDim
                                + " 与包内向量 " + packageDim + " 维不一致，已回退租户默认模型");
                    }
                }
            }
            if (model.isEmpty()) {
                config.remove("model");
            } else {
                config.put("model", model);
            }
        }
        return config.isEmpty() ? null : JsonUtils.toJsonString(config);
    }

    private Integer extractModelDimension(ModelConfigDO modelConfig) {
        if (StrUtil.isBlank(modelConfig.getConfig())) {
            return null;
        }
        try {
            Map<String, Object> config = JsonUtils.parseMap(modelConfig.getConfig());
            return config == null ? null : intOrNull(config.get("dimension"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 文件夹两轮导入：先建（parentId=0）拿到新 ID，再回填父子关系（避免顺序依赖）。
     */
    private Map<Long, Long> importFolders(Long kbId, List<Map<String, Object>> folders) {
        Map<Long, Long> folderMap = new LinkedHashMap<>();
        for (Map<String, Object> source : folders) {
            Long oldId = longOrNull(source.get("id"));
            FolderDO folder = new FolderDO();
            folder.setKbId(kbId);
            folder.setName(StrUtil.blankToDefault(str(source.get("name")), "未命名文件夹"));
            folder.setParentId(FolderDO.PARENT_ID_ROOT);
            folder.setSort(intOrNull(source.get("sort")));
            folderMapper.insert(folder);
            if (oldId != null) {
                folderMap.put(oldId, folder.getId());
            }
        }
        for (Map<String, Object> source : folders) {
            Long oldId = longOrNull(source.get("id"));
            Long newId = oldId == null ? null : folderMap.get(oldId);
            Long oldParent = longOrNull(source.get("parentId"));
            if (newId == null || oldParent == null || !folderMap.containsKey(oldParent)) {
                continue;
            }
            FolderDO update = new FolderDO();
            update.setId(newId);
            update.setParentId(folderMap.get(oldParent));
            folderMapper.updateById(update);
        }
        return folderMap;
    }

    /**
     * 文档导入：源文件字节重新上传到当前环境存储，建立 oldDocId → newDocId 映射。
     * 源文件缺失不影响文档记录创建（切片/向量已随包导入，源文件只用于预览/下载）。
     */
    private Map<Long, Long> importDocuments(Long kbId, List<Map<String, Object>> documents,
                                            Map<Long, Long> folderMap, Path kbRoot,
                                            Map<Long, DocumentDO> docEntities,
                                            int vectorStatus, String taskId, List<String> warnings) {
        Map<Long, Long> docMap = new LinkedHashMap<>();
        int total = documents.size();
        int processed = 0;
        for (Map<String, Object> source : documents) {
            processed++;
            if (total > 0 && (processed == total || processed % 5 == 0)) {
                report(taskId, "导入文档", 20 + (int) (processed * 25.0 / total),
                        "文档 " + processed + "/" + total);
            }
            Long oldId = longOrNull(source.get("id"));
            if (oldId == null) {
                continue;
            }
            String fileName = StrUtil.blankToDefault(str(source.get("fileName")), "doc-" + oldId);
            DocumentDO document = new DocumentDO();
            document.setKbId(kbId);
            Long oldFolderId = longOrNull(source.get("folderId"));
            document.setFolderId(oldFolderId == null ? FolderDO.PARENT_ID_ROOT
                    : folderMap.getOrDefault(oldFolderId, FolderDO.PARENT_ID_ROOT));
            document.setFileName(fileName);
            document.setFileType(str(source.get("fileType")));
            document.setFileSize(longOrNull(source.get("fileSize")));
            document.setDescription(str(source.get("description")));
            document.setTags(str(source.get("tags")));
            document.setDownloadCount(0);
            document.setViewCount(0);
            document.setStatus(0);
            // 向量状态：沿用/重跑 → 已完成；skip 模式 → 未处理（后续可手动重建）
            document.setVectorStatus(vectorStatus);

            String archivePath = str(asMap(source.get("_export")).get("archivePath"));
            byte[] content = readArchive(kbRoot, archivePath);
            if (content != null && content.length > 0) {
                try {
                    FileUploadRespVO upload = fileApi.createFileDetail(
                            content, fileName, "kb", guessContentType(fileName));
                    document.setFileUrl(upload.getUrl());
                    document.setFilePath(upload.getPath());
                    document.setFileConfigId(upload.getConfigId());
                } catch (Exception e) {
                    warnings.add("源文件上传失败 " + fileName + "：" + e.getMessage());
                }
            } else if (StrUtil.isNotBlank(archivePath)) {
                warnings.add("源文件缺失：" + archivePath + "（文档记录仍创建，仅无法预览/下载源文件）");
            }
            documentMapper.insert(document);
            docMap.put(oldId, document.getId());
            docEntities.put(oldId, document);
        }
        return docMap;
    }

    // ==================== 图片 / 数据面写入 ====================

    private int importImages(Long kbId, Map<Long, Long> docMap, Path kbRoot,
                             String taskId, List<String> warnings) {
        Path imagesDir = kbRoot.resolve("images");
        if (!Files.isDirectory(imagesDir)) {
            return 0;
        }
        List<Path> files;
        try (Stream<Path> stream = Files.walk(imagesDir)) {
            files = stream.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            warnings.add("读取 images 目录失败：" + e.getMessage());
            return 0;
        }
        int total = files.size();
        int ok = 0;
        int index = 0;
        for (Path path : files) {
            index++;
            if (total > 0 && (index == total || index % 10 == 0)) {
                report(taskId, "导入图片", 45 + (int) (index * 13.0 / total),
                        "图片 " + index + "/" + total);
            }
            String relative = imagesDir.relativize(path).toString().replace('\\', '/');
            String newKey = remapImageKey(relative, kbId, docMap);
            try {
                postImage(newKey, Files.readAllBytes(path));
                ok++;
            } catch (Exception e) {
                addWarning(warnings, "图片导入失败 " + relative + "：" + e.getMessage());
            }
        }
        return ok;
    }

    private void postImage(String objectKey, byte[] data) throws IOException {
        URI uri = URI.create(pythonUrl("/api/v1/import/image?object_key="
                + URLEncoder.encode(objectKey, StandardCharsets.UTF_8)));
        ClientHttpRequest request = kbExportRestTemplate.getRequestFactory().createRequest(uri, HttpMethod.POST);
        request.getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM);
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            request.getHeaders().set("x-tenant-id", String.valueOf(tenantId));
        }
        try (OutputStream body = request.getBody()) {
            body.write(data);
        }
        try (ClientHttpResponse response = request.execute()) {
            int status = response.getStatusCode().value();
            byte[] body = StreamUtils.copyToByteArray(response.getBody());
            if (status < 200 || status >= 300) {
                throw new IOException("python 返回 HTTP " + status);
            }
            Map<String, Object> parsed = parsePythonResponse(body);
            Object code = parsed.get("code");
            if (code instanceof Number && ((Number) code).intValue() != 0) {
                throw new IOException("python 返回失败: " + parsed.get("message"));
            }
        }
    }

    private Map<String, Object> importChunks(Path kbRoot, Long kbId, Map<Long, Long> docMap,
                                             Map<Long, DocumentDO> docEntities,
                                             String bucket, String imageBase, String taskId) throws IOException {
        Path file = kbRoot.resolve("chunks/chunks.jsonl");
        return postRewrittenNdjson("/api/v1/import/" + kbId + "/chunks", file,
                58, 78, "导入切片", taskId, node -> {
            Long oldDocId = node.path("document_id").asLong();
            Long newDocId = docMap.get(oldDocId);
            if (newDocId == null) {
                return null;
            }
            ObjectNode obj = (ObjectNode) node;
            obj.put("document_id", newDocId);
            obj.put("knowledge_base_id", kbId);
            obj.put("_es_id", newDocId + "_" + obj.path("chunk_index").asInt());
            rewriteMedia(obj, kbId, docMap, bucket, imageBase);
            rewriteSourceFile(obj, docEntities.get(oldDocId));
            obj.remove("_export");
            return obj;
        });
    }

    private Map<String, Object> importVectors(Path kbRoot, Long kbId, Map<Long, Long> docMap,
                                              Map<Long, DocumentDO> docEntities,
                                              String bucket, String imageBase, String taskId) throws IOException {
        Path file = kbRoot.resolve("vectors/vectors.jsonl");
        return postRewrittenNdjson("/api/v1/import/" + kbId + "/vectors", file,
                90, 99, "写入向量", taskId, node -> {
            Long oldDocId = node.path("document_id").asLong();
            Long newDocId = docMap.get(oldDocId);
            if (newDocId == null) {
                return null;
            }
            ObjectNode obj = (ObjectNode) node;
            obj.put("id", newDocId + "_" + obj.path("chunk_id").asInt());
            obj.put("document_id", newDocId);
            obj.put("knowledge_base_id", kbId);
            rewriteMedia(obj, kbId, docMap, bucket, imageBase);
            rewriteSourceFile(obj, docEntities.get(oldDocId));
            return obj;
        });
    }

    // ==================== 按切片重跑向量 ====================

    /**
     * 用已导入 ES 的切片 content 重新向量化（不重新解析文档、不跑 LLM）。
     *
     * <p>python 侧异步执行（embedding 调用较慢），这里轮询直到完成或超时；
     * 超时后返回 taskId，任务在 python 侧继续，可稍后查看。</p>
     */
    private Map<String, Object> reembedVectors(Long kbId, Map<String, Object> precheck, Path kbRoot,
                                               String importTaskId) {
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("mode", VECTOR_MODE_REEMBED);
        Integer dimension = intOrNull(asMap(precheck.get("target")).get("dimension"));
        if (dimension == null || dimension <= 0) {
            dimension = intOrNull(asMap(precheck.get("package")).get("dimension"));
        }
        if (dimension == null || dimension <= 0) {
            dimension = readFirstVectorDimension(kbRoot);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("include_images", false);
        body.put("batch_size", 32);
        body.put("dimension", dimension);
        String reembedTaskId;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> response = kbExportRestTemplate.exchange(
                    pythonUrl("/api/v1/import/" + kbId + "/reembed"), HttpMethod.POST,
                    new HttpEntity<>(body, headers), Map.class);
            reembedTaskId = str(asMap(response.getBody() == null ? null : response.getBody().get("data"))
                    .get("task_id"));
            if (StrUtil.isBlank(reembedTaskId)) {
                throw new IllegalStateException("python 未返回 task_id");
            }
        } catch (Exception e) {
            log.error("[kb-import] 启动重跑向量失败 kbId={}: {}", kbId, e.getMessage());
            stat.put("status", "failed");
            stat.put("error", e.getMessage());
            markVectorStatusFailed(kbId);
            return stat;
        }
        stat.put("taskId", reembedTaskId);

        long timeout = (vectorTaskConfig.getImportReembedTimeoutMs() == null
                ? 1800000L : vectorTaskConfig.getImportReembedTimeoutMs());
        long deadline = System.currentTimeMillis() + timeout;
        Map<String, Object> statusData = Collections.emptyMap();
        while (System.currentTimeMillis() < deadline) {
            statusData = normalizeReembedStatus(getReembedStatus(reembedTaskId));
            String status = str(statusData.get("status"));
            if ("done".equals(status) || "failed".equals(status)) {
                stat.putAll(statusData);
                if ("failed".equals(status)) {
                    // 向量重建失败：文档向量状态回写为 3（失败），避免前端仍显示「已完成」
                    markVectorStatusFailed(kbId);
                }
                report(importTaskId, "重跑向量", 99,
                        "向量重建" + ("failed".equals(status) ? "失败" : "完成"));
                return stat;
            }
            report(importTaskId, "重跑向量", reembedPercent(statusData), reembedMessage(statusData));
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        stat.putAll(statusData);
        stat.put("status", StrUtil.blankToDefault(str(statusData.get("status")), "running"));
        stat.put("note", "向量重建仍在后台执行，可稍后刷新查看（taskId=" + reembedTaskId + "）");
        report(importTaskId, "重跑向量", reembedPercent(statusData),
                reembedMessage(statusData) + "（后台继续执行）");
        return stat;
    }

    /**
     * 重跑向量阶段映射到总进度 90-99。
     */
    private int reembedPercent(Map<String, Object> statusData) {
        Integer total = intOrNull(statusData.get("total"));
        Integer done = intOrNull(statusData.get("done"));
        if (total == null || total <= 0 || done == null) {
            return 95;
        }
        return Math.min(99, 90 + (int) (done * 9.0 / total));
    }

    private String reembedMessage(Map<String, Object> statusData) {
        Integer total = intOrNull(statusData.get("total"));
        Integer done = intOrNull(statusData.get("done"));
        return total == null || total <= 0
                ? "向量重建中"
                : "向量重建 " + (done == null ? 0 : done) + "/" + total;
    }

    /**
     * 向量重建失败：把该库文档的 vector_status 回写为 3（失败）。
     *
     * <p>导入时文档默认标记为已完成（2），若不回写，前端文档列表会误显示「已完成」。</p>
     */
    private void markVectorStatusFailed(Long kbId) {
        try {
            int updated = documentMapper.updateVectorStatusByKbId(kbId, 3);
            log.warn("[kb-import] 向量重建失败，文档向量状态已置为失败 kbId={} docs={}", kbId, updated);
        } catch (Exception e) {
            log.warn("[kb-import] 回写文档向量状态失败 kbId={}: {}", kbId, e.getMessage());
        }
    }

    /**
     * python 进度存 Redis 字符串，这里把计数字段转成数值，便于前端直接展示。
     */
    private Map<String, Object> normalizeReembedStatus(Map<String, Object> status) {
        Map<String, Object> result = new LinkedHashMap<>(status);
        for (String key : List.of("done", "failed", "total", "vector_dimension")) {
            Object value = result.get(key);
            if (value != null) {
                Integer number = intOrNull(value);
                result.put(key, number == null ? value : number);
            }
        }
        return result;
    }

    private Map<String, Object> getReembedStatus(String taskId) {
        try {
            ResponseEntity<Map> response = kbExportRestTemplate.exchange(
                    pythonUrl("/api/v1/import/reembed/" + taskId), HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()), Map.class);
            return asMap(response.getBody() == null ? null : response.getBody().get("data"));
        } catch (Exception e) {
            log.warn("[kb-import] 查询重跑向量进度失败 task={}: {}", taskId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 切片/向量 metadata 里的源文件地址改为新环境上传后的地址（旧 file_url 带旧域名/签名会失效）。
     */
    private void rewriteSourceFile(ObjectNode obj, DocumentDO document) {
        if (document == null) {
            return;
        }
        JsonNode metadata = obj.get("metadata");
        if (metadata instanceof ObjectNode metaNode) {
            if (StrUtil.isNotBlank(document.getFileUrl())) {
                metaNode.put("file_url", document.getFileUrl());
            }
            if (StrUtil.isNotBlank(document.getFilePath())) {
                metaNode.put("file_path", document.getFilePath());
            }
        }
    }

    private Map<String, Object> importWiki(Path kbRoot, Long kbId, Map<Long, Long> docMap,
                                           String bucket, String imageBase, String taskId) throws IOException {
        Path file = kbRoot.resolve("wiki/pages.jsonl");
        Map<String, Object> pageStat = postRewrittenNdjson("/api/v1/import/" + kbId + "/wiki", file,
                78, 84, "导入 Wiki", taskId, node -> {
            ObjectNode obj = (ObjectNode) node;
            String docType = obj.path("doc_type").asText("page");
            obj.put("kb_id", String.valueOf(kbId));
            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                obj.put("tenant_id", String.valueOf(tenantId));
            }
            if ("folder".equals(docType)) {
                obj.put("_es_id", kbId + ":folder:" + obj.path("folder_id").asText(""));
            } else if ("issue".equals(docType)) {
                obj.put("_es_id", kbId + ":" + obj.path("slug").asText("")
                        + ":" + obj.path("issue_type").asText(""));
            } else {
                obj.put("_es_id", kbId + ":" + obj.path("slug").asText(""));
            }
            rewriteWikiRefs(obj, docMap);
            rewriteMedia(obj, kbId, docMap, bucket, imageBase);
            obj.remove("_export");
            return obj;
        });
        // wiki 目录/issue 单独条目文件（导出为独立 jsonl）
        Map<String, Object> folderStat = postRewrittenNdjson("/api/v1/import/" + kbId + "/wiki",
                kbRoot.resolve("wiki/folders.jsonl"), 84, 86, "导入 Wiki 目录", taskId, node -> {
                    ObjectNode obj = (ObjectNode) node;
                    obj.put("kb_id", String.valueOf(kbId));
                    Long tenantId = TenantContextHolder.getTenantId();
                    if (tenantId != null) {
                        obj.put("tenant_id", String.valueOf(tenantId));
                    }
                    obj.put("_es_id", kbId + ":folder:" + obj.path("folder_id").asText(""));
                    obj.remove("_export");
                    return obj;
                });
        Map<String, Object> issueStat = postRewrittenNdjson("/api/v1/import/" + kbId + "/wiki",
                kbRoot.resolve("wiki/issues.jsonl"), 86, 88, "导入 Wiki 问题", taskId, node -> {
                    ObjectNode obj = (ObjectNode) node;
                    obj.put("kb_id", String.valueOf(kbId));
                    Long tenantId = TenantContextHolder.getTenantId();
                    if (tenantId != null) {
                        obj.put("tenant_id", String.valueOf(tenantId));
                    }
                    obj.put("_es_id", kbId + ":" + obj.path("slug").asText("")
                            + ":" + obj.path("issue_type").asText(""));
                    obj.remove("_export");
                    return obj;
                });
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("pages", pageStat);
        stat.put("folders", folderStat);
        stat.put("issues", issueStat);
        return stat;
    }

    private void rewriteWikiRefs(ObjectNode obj, Map<Long, Long> docMap) {
        JsonNode sourceRefs = obj.get("source_refs");
        if (sourceRefs != null && sourceRefs.isArray()) {
            ArrayNode rebuilt = obj.putArray("source_refs");
            for (JsonNode ref : sourceRefs) {
                String raw = ref.asText("");
                String idPart = raw.split("\\|", 2)[0].trim();
                Long oldDocId = longOrNull(idPart);
                Long newDocId = oldDocId == null ? null : docMap.get(oldDocId);
                if (newDocId == null) {
                    continue;
                }
                rebuilt.add(newDocId + raw.substring(idPart.length()));
            }
        }
        JsonNode chunkRefs = obj.get("chunk_refs");
        if (chunkRefs != null && chunkRefs.isArray()) {
            ArrayNode rebuilt = obj.putArray("chunk_refs");
            for (JsonNode ref : chunkRefs) {
                String raw = ref.asText("");
                int index = raw.lastIndexOf('_');
                if (index <= 0) {
                    continue;
                }
                Long oldDocId = longOrNull(raw.substring(0, index));
                Long newDocId = oldDocId == null ? null : docMap.get(oldDocId);
                if (newDocId == null) {
                    continue;
                }
                rebuilt.add(newDocId + raw.substring(index));
            }
        }
    }

    /**
     * 正文/元数据里的图片引用改写：
     * - 优先使用导出的 `_export.content_portable`（占位符地址），替换成新环境 base + 新 key；
     * - 无 `_export` 的行（向量）按 `metadata.image_url` 的旧 key 反查 URL 并替换；
     * - `metadata.image_url` 统一改写为新相对 key（`{bucket}/kb/{newKb}/{newDoc}/...`）。
     */
    private void rewriteMedia(ObjectNode obj, Long newKbId, Map<Long, Long> docMap,
                              String bucket, String imageBase) {
        JsonNode export = obj.path("_export");
        boolean portable = export.hasNonNull("content_portable");
        String content = portable
                ? export.get("content_portable").asText("").replace(IMAGE_BASE_PLACEHOLDER, imageBase)
                : obj.path("content").asText("");

        JsonNode images = export.path("images");
        if (images.isArray()) {
            for (JsonNode ref : images) {
                String oldKey = ref.path("object_key").asText("");
                if (StrUtil.isBlank(oldKey)) {
                    continue;
                }
                String newKey = remapImageKey(oldKey, newKbId, docMap);
                String newUrl = imageBase + "/" + bucket + "/" + newKey;
                if (portable) {
                    // 导出环境桶名可能与本环境不同（历史 kb-images / 当前 ckb-images），统一改写为目标桶
                    content = content.replace(bucket + "/" + oldKey, bucket + "/" + newKey);
                    content = content.replace("kb-images/" + oldKey, bucket + "/" + newKey);
                    content = content.replace("ckb-images/" + oldKey, bucket + "/" + newKey);
                } else {
                    content = replaceImageUrl(content, bucket, oldKey, newUrl);
                }
                String sourceUrl = ref.path("source_url").asText("");
                if (StrUtil.isNotBlank(sourceUrl)) {
                    content = content.replace(sourceUrl, newUrl);
                }
            }
        }

        JsonNode metadata = obj.get("metadata");
        if (metadata instanceof ObjectNode metaNode) {
            String oldImage = metaNode.path("image_url").asText("");
            if (StrUtil.isNotBlank(oldImage)) {
                String oldKey = extractImageKey(oldImage);
                if (oldKey != null) {
                    String newKey = remapImageKey(oldKey, newKbId, docMap);
                    if (!portable) {
                        content = replaceImageUrl(content, bucket, oldKey,
                                imageBase + "/" + bucket + "/" + newKey);
                    }
                    metaNode.put("image_url", bucket + "/" + newKey);
                }
            }
        }
        obj.put("content", content);
    }

    /**
     * 把正文中指向该图片对象的绝对 URL 替换为新地址（不依赖旧域名，按桶名 + 对象 key 定位）。
     */
    private String replaceImageUrl(String content, String bucket, String oldKey, String newUrl) {
        if (StrUtil.isBlank(content) || StrUtil.isBlank(oldKey)) {
            return content;
        }
        String regex = "https?://[^\\s)\"'<>]*?/" + Pattern.quote(bucket) + "/" + Pattern.quote(oldKey);
        return content.replaceAll(regex, Matcher.quoteReplacement(newUrl));
    }

    private String extractImageKey(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String text = value.trim();
        // data url 自带图片内容，不属于对象引用，跳过
        if (text.startsWith("data:")) {
            return null;
        }
        int index = text.indexOf("/kb/");
        if (index >= 0) {
            return text.substring(index + 1);
        }
        return text.startsWith("kb/") ? text : null;
    }

    /**
     * 图片对象 key 重映射：`kb/{oldKb}/{oldDoc}/file` → `kb/{newKb}/{newDoc}/file`。
     */
    private String remapImageKey(String objectKey, Long newKbId, Map<Long, Long> docMap) {
        if (StrUtil.isBlank(objectKey)) {
            return objectKey;
        }
        String[] parts = objectKey.split("/");
        if (parts.length < 4 || !"kb".equals(parts[0])) {
            return objectKey;
        }
        Long oldDocId = longOrNull(parts[2]);
        Long newDocId = oldDocId == null ? null : docMap.get(oldDocId);
        StringBuilder builder = new StringBuilder("kb/").append(newKbId).append('/')
                .append(newDocId != null ? newDocId : parts[2]).append('/');
        for (int i = 3; i < parts.length; i++) {
            if (i > 3) {
                builder.append('/');
            }
            builder.append(parts[i]);
        }
        return builder.toString();
    }

    /**
     * 逐行读 NDJSON、按 rewriter 改写后流式 POST 给 python（不整包入内存）。
     * rewriter 返回 null 表示跳过该行（如文档映射缺失）。
     *
     * @param startPercent 该阶段在总进度中的起点
     * @param endPercent   该阶段在总进度中的终点
     * @param stage        阶段名（进度提示用）
     * @param taskId       导入任务 ID（为空则不上报进度）
     */
    private Map<String, Object> postRewrittenNdjson(String pythonPath, Path jsonlFile,
                                                    int startPercent, int endPercent, String stage,
                                                    String taskId,
                                                    Function<JsonNode, ObjectNode> rewriter) throws IOException {
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("written", 0);
        stat.put("skipped", 0);
        stat.put("result", Collections.emptyMap());
        if (jsonlFile == null || !Files.isRegularFile(jsonlFile)) {
            return stat;
        }
        long totalBytes = 0;
        try {
            totalBytes = Files.size(jsonlFile);
        } catch (IOException ignored) {
            // 拿不到大小则只在阶段结束时报进度
        }
        URI uri = URI.create(pythonUrl(pythonPath));
        ClientHttpRequest request = kbExportRestTemplate.getRequestFactory().createRequest(uri, HttpMethod.POST);
        request.getHeaders().setContentType(MediaType.parseMediaType(NDJSON));
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            request.getHeaders().set("x-tenant-id", String.valueOf(tenantId));
        }

        long written = 0;
        long skipped = 0;
        long readBytes = 0;
        try (BufferedReader reader = Files.newBufferedReader(jsonlFile, StandardCharsets.UTF_8);
             OutputStream body = request.getBody()) {
            String line;
            while ((line = reader.readLine()) != null) {
                readBytes += line.getBytes(StandardCharsets.UTF_8).length + 1;
                if (StrUtil.isBlank(line)) {
                    continue;
                }
                ObjectNode rewritten;
                try {
                    JsonNode node = JsonUtils.parseTree(line);
                    if (node == null || !node.isObject()) {
                        skipped++;
                        continue;
                    }
                    rewritten = rewriter.apply(node);
                } catch (Exception e) {
                    skipped++;
                    continue;
                }
                if (rewritten == null) {
                    skipped++;
                    continue;
                }
                body.write(JsonUtils.toJsonString(rewritten).getBytes(StandardCharsets.UTF_8));
                body.write('\n');
                written++;
                // 每 500 行按已读字节比例上报一次（避免每行都写 Redis）
                if (taskId != null && totalBytes > 0 && written % 500 == 0) {
                    int percent = startPercent + (int) (Math.min(readBytes, totalBytes)
                            * (endPercent - startPercent) / totalBytes);
                    report(taskId, stage, percent, stage + " " + written + " 条");
                }
            }
        }
        stat.put("written", written);
        stat.put("skipped", skipped);
        report(taskId, stage, endPercent, stage + "完成，共 " + written + " 条");

        try (ClientHttpResponse response = request.execute()) {
            int status = response.getStatusCode().value();
            byte[] payload = StreamUtils.copyToByteArray(response.getBody());
            if (status < 200 || status >= 300) {
                throw new IOException("python 返回 HTTP " + status + "：" + new String(payload, StandardCharsets.UTF_8));
            }
            Map<String, Object> parsed = parsePythonResponse(payload);
            Object code = parsed.get("code");
            if (code instanceof Number && ((Number) code).intValue() != 0) {
                throw new IOException("python 返回失败：" + parsed.get("message"));
            }
            stat.put("result", asMap(parsed.get("data")));
        }
        return stat;
    }

    // ==================== Wiki 修订 ====================

    private int importWikiRevisions(Long kbId, List<Map<String, Object>> revisions, List<String> warnings) {
        int ok = 0;
        for (Map<String, Object> source : revisions) {
            String slug = str(source.get("slug"));
            Integer revision = intOrNull(source.get("revision"));
            if (StrUtil.isBlank(slug) || revision == null) {
                continue;
            }
            WikiPageRevisionDO entity = new WikiPageRevisionDO();
            entity.setKbId(kbId);
            entity.setSlug(slug);
            // 页面在 ES 后 page_id 仅兼容保留（避免非空约束），沿用原值；缺失时写 0
            Long pageId = longOrNull(source.get("pageId"));
            entity.setPageId(pageId == null ? 0L : pageId);
            entity.setRevision(revision);
            entity.setTitle(str(source.get("title")));
            entity.setContent(str(source.get("content")));
            entity.setSummary(str(source.get("summary")));
            entity.setAliases(str(source.get("aliases")));
            entity.setPageType(str(source.get("pageType")));
            entity.setStatus(str(source.get("status")));
            entity.setEditSource(str(source.get("editSource")));
            entity.setOperatorId(longOrNull(source.get("operatorId")));
            entity.setOperatorName(str(source.get("operatorName")));
            try {
                wikiPageRevisionMapper.insert(entity);
                ok++;
            } catch (Exception e) {
                addWarning(warnings, "Wiki 修订导入失败 " + slug + "#" + revision + "：" + e.getMessage());
            }
        }
        return ok;
    }

    // ==================== 分片上传（大包导入） ====================

    @Override
    public Map<String, Object> initUpload(String fileName, long totalSize) {
        try {
            Path root = uploadRoot();
            Files.createDirectories(root);
            cleanupExpiredUploads(root);
            String uploadId = IdUtil.fastSimpleUUID();
            Path dir = root.resolve(uploadId);
            Files.createDirectories(dir);
            int chunkSize = chunkSizeBytes();
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("fileName", fileName);
            meta.put("totalSize", totalSize);
            meta.put("chunkSize", chunkSize);
            meta.put("createdAt", System.currentTimeMillis());
            Files.writeString(dir.resolve(META_FILE), JsonUtils.toJsonString(meta), StandardCharsets.UTF_8);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("uploadId", uploadId);
            result.put("chunkSize", chunkSize);
            result.put("totalChunks", (int) Math.max(1, Math.ceil(totalSize / (double) chunkSize)));
            return result;
        } catch (IOException e) {
            log.error("[kb-import] 初始化分片上传失败", e);
            throw exception(KB_IMPORT_FAILED);
        }
    }

    @Override
    public Map<String, Object> uploadChunk(String uploadId, int index, byte[] data) throws IOException {
        if (index < 0) {
            throw exception(KB_IMPORT_PACKAGE_INVALID);
        }
        Path dir = requireUploadDir(uploadId);
        byte[] payload = data == null ? new byte[0] : data;
        Files.write(dir.resolve(index + ".part"), payload);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("uploadId", uploadId);
        result.put("index", index);
        result.put("size", payload.length);
        return result;
    }

    @Override
    public Map<String, Object> getUploadStatus(String uploadId) {
        Path dir = requireUploadDir(uploadId);
        Map<String, Object> meta = readUploadMeta(dir);
        List<Integer> uploaded = listUploadedIndexes(dir);
        int totalChunks = uploadTotalChunks(meta);
        Map<String, Object> result = new LinkedHashMap<>(meta);
        result.put("uploadId", uploadId);
        result.put("uploaded", uploaded);
        result.put("totalChunks", totalChunks);
        result.put("complete", uploaded.size() >= totalChunks);
        return result;
    }

    @Override
    public Map<String, Object> completeUpload(String uploadId, String vectorMode) throws IOException {
        Path dir = requireUploadDir(uploadId);
        if (!runningUploads.add(uploadId)) {
            throw new ServiceException(KB_IMPORT_FAILED.getCode(), "该导入包正在处理中，请等待当前任务结束");
        }
        boolean submitted = false;
        try {
            Map<String, Object> meta = readUploadMeta(dir);
            int totalChunks = uploadTotalChunks(meta);
            for (int i = 0; i < totalChunks; i++) {
                if (!Files.isRegularFile(dir.resolve(i + ".part"))) {
                    throw exception(KB_IMPORT_PACKAGE_INVALID);
                }
            }
            // 按序合并分片为完整 ZIP（毫秒级，可在请求线程完成）
            Path zip = dir.resolve(SOURCE_FILE_NAME);
            try (OutputStream out = Files.newOutputStream(zip, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                for (int i = 0; i < totalChunks; i++) {
                    Files.copy(dir.resolve(i + ".part"), out);
                }
            }

            // 导入（含 reembed）可能几十分钟：转异步执行，立即返回 taskId 供前端轮询
            String taskId = IdUtil.fastSimpleUUID();
            String mode = normalizeVectorMode(vectorMode);
            importTaskReporter.start(taskId);
            try {
                importExecutor.submit(() -> runImportTask(taskId, uploadId, dir, zip, mode));
            } catch (RuntimeException e) {
                // 线程池拒绝等异常：标记任务失败，避免留下永远 running 的僵尸任务
                importTaskReporter.fail(taskId, "导入任务提交失败：" + e.getMessage(), null);
                throw e;
            }
            submitted = true;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskId", taskId);
            result.put("uploadId", uploadId);
            result.put("status", "running");
            return result;
        } finally {
            if (!submitted) {
                runningUploads.remove(uploadId);
            }
        }
    }

    /**
     * 后台执行导入：写进度、落结果、按需保留分片会话（预检失败可换 reembed 重试）。
     */
    private void runImportTask(String taskId, String uploadId, Path sessionDir, Path zip, String mode) {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean keepSession = false;
        Path work = null;
        try {
            work = Files.createTempDirectory("kb-import-");
            try (InputStream input = Files.newInputStream(zip)) {
                unzip(input, work);
            }
            importTaskReporter.progress(taskId, "解析导入包", 3, "解压完成，开始导入");
            result = doImport(work, mode, taskId);
            boolean success = Boolean.TRUE.equals(result.get("success"));
            if (success) {
                importTaskReporter.finish(taskId, result);
            } else {
                // 预检失败且包内有切片：保留会话，前端可换 reembed 重试
                keepSession = Boolean.TRUE.equals(result.get("retryable"));
                if (keepSession) {
                    result.put("uploadId", uploadId);
                }
                importTaskReporter.fail(taskId, str(result.get("message")), result);
            }
        } catch (Exception e) {
            log.error("[kb-import] 异步导入失败 task={} upload={}", taskId, uploadId, e);
            importTaskReporter.fail(taskId, "导入失败：" + e.getMessage(), result);
        } finally {
            runningUploads.remove(uploadId);
            if (work != null) {
                deleteQuietly(work);
            }
            if (!keepSession) {
                deleteQuietly(sessionDir);
            }
        }
    }

    @Override
    public Map<String, Object> getTaskProgress(String taskId) {
        Map<String, Object> progress = importTaskReporter.get(taskId);
        if (progress.isEmpty()) {
            // 任务不存在/已过期（Redis TTL 24h）：明确告知前端，避免无限轮询
            Map<String, Object> expired = new LinkedHashMap<>();
            expired.put("taskId", taskId);
            expired.put("status", "expired");
            expired.put("message", "任务不存在或已过期（进度仅保留 24 小时）");
            return expired;
        }
        return progress;
    }

    /**
     * 分片上传根目录（系统临时目录下，按会话分子目录）。
     */
    private Path uploadRoot() {
        return Paths.get(System.getProperty("java.io.tmpdir"), "ckb-kb-import-uploads");
    }

    private int chunkSizeBytes() {
        Integer mb = vectorTaskConfig.getImportChunkSizeMb();
        int size = (mb == null || mb <= 0 ? 64 : mb) * 1024 * 1024;
        return Math.max(1024 * 1024, size);
    }

    private Path requireUploadDir(String uploadId) {
        if (StrUtil.isBlank(uploadId)) {
            throw exception(KB_IMPORT_PACKAGE_INVALID);
        }
        Path root = uploadRoot().normalize();
        Path dir = root.resolve(uploadId).normalize();
        if (!dir.startsWith(root) || !Files.isDirectory(dir)) {
            throw exception(KB_IMPORT_PACKAGE_INVALID);
        }
        return dir;
    }

    private Map<String, Object> readUploadMeta(Path dir) {
        Path meta = dir.resolve(META_FILE);
        if (!Files.isRegularFile(meta)) {
            return new LinkedHashMap<>();
        }
        try {
            return readJsonMap(meta);
        } catch (IOException e) {
            return new LinkedHashMap<>();
        }
    }

    private int uploadTotalChunks(Map<String, Object> meta) {
        long totalSize = meta.get("totalSize") instanceof Number number ? number.longValue() : 0L;
        int chunkSize = meta.get("chunkSize") instanceof Number number
                ? number.intValue() : chunkSizeBytes();
        if (chunkSize <= 0) {
            chunkSize = chunkSizeBytes();
        }
        return (int) Math.max(1, Math.ceil(totalSize / (double) chunkSize));
    }

    private List<Integer> listUploadedIndexes(Path dir) {
        List<Integer> indexes = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".part")).forEach(path -> {
                String name = path.getFileName().toString();
                try {
                    indexes.add(Integer.parseInt(name.substring(0, name.length() - 5)));
                } catch (NumberFormatException ignored) {
                    // 非分片文件忽略
                }
            });
        } catch (IOException e) {
            log.warn("[kb-import] 列出分片失败: {}", e.getMessage());
        }
        indexes.sort(Integer::compareTo);
        return indexes;
    }

    /**
     * 定时清理过期的分片上传会话（默认 24h），避免临时目录堆积。
     */
    @Scheduled(fixedDelay = 3600000L)
    public void cleanupExpiredUploadSessions() {
        try {
            cleanupExpiredUploads(uploadRoot());
        } catch (Exception e) {
            log.warn("[kb-import] 清理分片上传会话失败: {}", e.getMessage());
        }
    }

    private void cleanupExpiredUploads(Path root) {
        if (!Files.isDirectory(root)) {
            return;
        }
        Integer ttlHours = vectorTaskConfig.getImportUploadTtlHours();
        long ttl = Math.max(1, ttlHours == null ? 24 : ttlHours) * 3600_000L;
        long now = System.currentTimeMillis();
        try (Stream<Path> stream = Files.list(root)) {
            List<Path> dirs = stream.filter(Files::isDirectory).collect(Collectors.toList());
            for (Path dir : dirs) {
                try {
                    Map<String, Object> meta = readUploadMeta(dir);
                    long createdAt = meta.get("createdAt") instanceof Number number
                            ? number.longValue() : 0L;
                    if (createdAt > 0 && now - createdAt > ttl) {
                        deleteQuietly(dir);
                    }
                } catch (Exception ignored) {
                    // 单目录清理失败不影响其它会话
                }
            }
        } catch (IOException e) {
            log.debug("[kb-import] 扫描分片上传目录失败: {}", e.getMessage());
        }
    }

    // ==================== 包解析 / 通用工具 ====================

    private void unzip(InputStream input, Path dest) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(input, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                Path target = dest.resolve(entry.getName().replace('\\', '/')).normalize();
                if (!target.startsWith(dest)) {
                    throw new IOException("导入包含非法路径: " + entry.getName());
                }
                Files.createDirectories(target.getParent());
                Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private Path locatePackageRoot(Path work) throws IOException {
        try (Stream<Path> stream = Files.walk(work, 3)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> "manifest.json".equals(path.getFileName().toString()))
                    .map(Path::getParent)
                    .findFirst()
                    .orElseThrow(() -> exception(KB_IMPORT_PACKAGE_INVALID));
        }
    }

    private Map<String, Object> readJsonMap(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return new LinkedHashMap<>();
        }
        String text = Files.readString(path, StandardCharsets.UTF_8);
        Map<String, Object> map = JsonUtils.parseObject(text, new TypeReference<Map<String, Object>>() {
        });
        return map == null ? new LinkedHashMap<>() : map;
    }

    private List<Map<String, Object>> readJsonList(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return Collections.emptyList();
        }
        String text = Files.readString(path, StandardCharsets.UTF_8);
        List<Map<String, Object>> list = JsonUtils.parseObject(
                text, new TypeReference<List<Map<String, Object>>>() {
                });
        return list == null ? Collections.emptyList() : list;
    }

    private byte[] readArchive(Path kbRoot, String archivePath) {
        if (StrUtil.isBlank(archivePath)) {
            return null;
        }
        String relative = archivePath.replace('\\', '/').replaceAll("^/+", "");
        Path target = kbRoot.resolve(relative).normalize();
        if (!target.startsWith(kbRoot) || !Files.isRegularFile(target)) {
            return null;
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            return null;
        }
    }

    private void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // 临时目录清理失败不影响导入结果
                }
            });
        } catch (IOException e) {
            log.debug("[kb-import] 清理临时目录失败 {}: {}", dir, e.getMessage());
        }
    }

    private void addWarning(List<String> warnings, String message) {
        if (warnings.size() < MAX_WARNINGS) {
            warnings.add(message);
        }
    }

    private String pythonUrl(String path) {
        return vectorTaskConfig.getPythonServiceUrl() + path;
    }

    private Map<String, Object> parsePythonResponse(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> map = JsonUtils.parseObject(new String(payload, StandardCharsets.UTF_8),
                    new TypeReference<Map<String, Object>>() {
                    });
            return map == null ? Collections.emptyMap() : map;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String guessContentType(String fileName) {
        String type = URLConnection.guessContentTypeFromName(fileName == null ? "" : fileName);
        return StrUtil.isBlank(type) ? MediaType.APPLICATION_OCTET_STREAM_VALUE : type;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
    }

    private Map<String, String> asStringMap(Object value) {
        Map<String, Object> raw = asMap(value);
        if (raw.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        raw.forEach((key, item) -> {
            if (key != null && item != null) {
                result.put(key, String.valueOf(item));
            }
        });
        return result;
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private String joinMessages(Object value) {
        List<String> messages = stringList(value);
        return messages.isEmpty() ? "预检未通过" : String.join("；", messages);
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longOrNull(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StrUtil.isNotBlank(text)) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer intOrNull(Object value) {
        Long number = longOrNull(value);
        return number == null ? null : number.intValue();
    }
}
