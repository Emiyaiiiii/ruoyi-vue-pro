package cn.iocoder.yudao.module.kb.service.document;

import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.FileUploadRespVO;
import cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo.VectorTaskSubmitReqVO;
import cn.iocoder.yudao.module.kb.service.vectortask.VectorTaskService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;
import cn.iocoder.yudao.module.kb.controller.admin.document.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;

import cn.iocoder.yudao.module.kb.dal.mysql.document.DocumentMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.vectortask.VectorTaskMapper;
import cn.iocoder.yudao.module.kb.service.library.rule.VisibilityContext;
import cn.iocoder.yudao.module.kb.service.library.rule.VisibilityRuleEngine;
import cn.iocoder.yudao.module.kb.service.folder.FolderService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 知识库文件 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    /** 预览预签名地址默认有效期（秒） */
    private static final int PREVIEW_EXPIRES_DEFAULT = 1800;
    private static final int PREVIEW_EXPIRES_MIN = 60;
    private static final int PREVIEW_EXPIRES_MAX = 86400;

    @Resource
    private DocumentMapper documentMapper;

    @Resource
    private LibraryMapper libraryMapper;

    @Resource
    private FileApi fileApi;

    @Resource
    private VectorTaskService vectorTaskService;

    @Resource
    private VectorTaskMapper vectorTaskMapper;

    @Resource
    private VisibilityRuleEngine ruleEngine;

    @Resource
    private FolderService folderService;

    @Resource
    private cn.iocoder.yudao.module.kb.dal.mysql.chunk.KbChunkRevisionMapper kbChunkRevisionMapper;
    @Resource
    private cn.iocoder.yudao.module.kb.dal.mysql.wiki.WikiPageRevisionMapper wikiPageRevisionMapper;
    @Resource
    private cn.iocoder.yudao.module.kb.service.wiki.WikiPythonClient wikiPythonClient;

    @Override
    public Long createDocument(DocumentSaveReqVO createReqVO) {
        // 1. 校验知识库存在
        LibraryDO library = libraryMapper.selectById(createReqVO.getKbId());
        if (library == null) {
            throw exception(LIBRARY_NOT_EXISTS);
        }

        // 2. 写入文件记录
        DocumentDO document = BeanUtils.toBean(createReqVO, DocumentDO.class);
        document.setDownloadCount(0);
        document.setViewCount(0);
        documentMapper.insert(document);

        // 3. 更新知识库的文档数量
        libraryMapper.updateDocCount(createReqVO.getKbId(), 1);

        return document.getId();
    }

    @Override
    public void updateDocument(DocumentSaveReqVO updateReqVO) {
        // 校验存在
        validateDocumentExists(updateReqVO.getId());
        // 更新
        DocumentDO updateObj = BeanUtils.toBean(updateReqVO, DocumentDO.class);
        documentMapper.updateById(updateObj);
    }

    @Override
    public void deleteDocument(Long id) {
        // 校验存在
        DocumentDO document = validateDocumentExists(id);
        // 删除 → 联动清理向量分片 + 源文件对象 + 向量任务记录
        cleanupDocumentResources(document);
        // 删除
        documentMapper.deleteById(id);
        // 更新知识库的文档数量
        libraryMapper.updateDocCount(document.getKbId(), -1);
    }

    @Override
    public void deleteDocumentListByIds(List<Long> ids) {
        for (Long id : ids) {
            DocumentDO document = documentMapper.selectById(id);
            if (document != null) {
                // 联动清理向量分片 + 源文件对象 + 向量任务记录
                cleanupDocumentResources(document);
                libraryMapper.updateDocCount(document.getKbId(), -1);
            }
        }
        // 删除
        documentMapper.deleteByIds(ids);
    }

    @Override
    public void deleteByKbId(Long kbId) {
        // 查询该库下全部文档
        List<DocumentDO> documents = documentMapper.selectList(
                new LambdaQueryWrapperX<DocumentDO>().eq(DocumentDO::getKbId, kbId));
        for (DocumentDO document : documents) {
            // 逐个联动清理向量分片 + 源文件对象（图片清理已随向量分片一并处理）
            cleanupDocumentResources(document);
        }
        // 删除该库下所有文档记录与向量任务记录
        documentMapper.delete(new LambdaQueryWrapperX<DocumentDO>().eq(DocumentDO::getKbId, kbId));
        vectorTaskMapper.deleteByKbId(kbId);
        // 删除该库下所有切片修订历史（#7）
        try {
            kbChunkRevisionMapper.deleteByKbId(kbId);
        } catch (Exception e) {
            log.warn("[deleteByKbId] 删除切片修订历史失败: kbId={}, error={}", kbId, e.getMessage());
        }
        // 删库统一清理（python 一站式：ES 切片 + wiki 页面/issue + Milvus 向量），避免遗漏
        try {
            wikiPythonClient.deleteKbStorage(kbId);
        } catch (Exception e) {
            log.warn("[deleteByKbId] python 删库清理失败: kbId={}, error={}", kbId, e.getMessage());
        }
        // 修订历史薄表载体（MySQL）随库删除
        try {
            wikiPageRevisionMapper.deletePhysicallyByKbId(kbId);
        } catch (Exception e) {
            log.warn("[deleteByKbId] 删除修订历史失败: kbId={}, error={}", kbId, e.getMessage());
        }
        // 重置知识库的文档数量
        libraryMapper.updateDocCount(kbId, -documents.size());
    }

    /**
     * 联动清理单个文档的关联资源（best-effort，任一步失败均不阻断文档记录删除）：
     * 1. 向量分片（Milvus/ES + 图片回链对象，经 Python 文档删除接口）；
     * 2. 源文件对象（MinIO 等，按文件配置ID + 存储路径物理删除）；
     * 3. 向量任务记录。
     */
    private void cleanupDocumentResources(DocumentDO document) {
        // 0. 先取消该文档仍在运行的向量任务（Python 端），避免删除后任务继续写入产生脏数据
        try {
            vectorTaskService.cancelActiveTaskByDocId(document.getId());
        } catch (Exception e) {
            log.warn("[cleanupDocumentResources] 取消在途向量任务失败: docId={}, error={}", document.getId(), e.getMessage());
        }
        // 0.5 Wiki 解链（对齐 WeKnora：先清理 wiki 页、再删 chunk，因剥源需依赖页面归属）：
        //     反查引用该文档的页面 → 仅被它引用的整页删+删修订；多文档共享页仅剥掉该来源。
        try {
            wikiUnlinkOnDocumentDelete(document.getKbId(), document.getId());
        } catch (Exception e) {
            log.warn("[cleanupDocumentResources] Wiki 解链失败: docId={}, error={}", document.getId(), e.getMessage());
        }
        // 1. 清理向量分片（含图片回链对象）
        try {
            vectorTaskService.deleteDocumentVectors(document.getId(), document.getKbId());
        } catch (Exception e) {
            log.warn("[cleanupDocumentResources] 清理向量失败: docId={}, error={}", document.getId(), e.getMessage());
        }
        // 2. 物理删除源文件对象
        if (document.getFileConfigId() != null && StrUtil.isNotBlank(document.getFilePath())) {
            try {
                fileApi.deleteFile(document.getFileConfigId(), document.getFilePath());
            } catch (Exception e) {
                log.warn("[cleanupDocumentResources] 删除源文件对象失败: docId={}, error={}", document.getId(), e.getMessage());
            }
        }
        // 3. 删除向量任务记录
        try {
            vectorTaskMapper.deleteByDocId(document.getId());
        } catch (Exception e) {
            log.warn("[cleanupDocumentResources] 删除向量任务记录失败: docId={}, error={}", document.getId(), e.getMessage());
        }
        // 4. 删除切片修订历史（#7：文档删除时同步清理，避免孤儿修订数据）
        try {
            kbChunkRevisionMapper.deleteByDocumentId(document.getId());
        } catch (Exception e) {
            log.warn("[cleanupDocumentResources] 删除切片修订历史失败: docId={}, error={}", document.getId(), e.getMessage());
        }
    }

    /**
     * Wiki 文档删除联动：剥除/删除 source_refs 引用该文档的 wiki 页面。
     * 页面已迁 ES（python 拥有 source_refs 归属），由 python 负责，Java 仅转发。
     */
    private void wikiUnlinkOnDocumentDelete(Long kbId, Long docId) {
        try {
            wikiPythonClient.unlinkDocument(kbId, docId);
        } catch (Exception e) {
            log.warn("[wiki-unlink] 文档删除联动失败 kbId={} docId={}: {}", kbId, docId, e.getMessage());
        }
    }


    private DocumentDO validateDocumentExists(Long id) {
        DocumentDO document = documentMapper.selectById(id);
        if (document == null) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
        return document;
    }

    @Override
    public DocumentDO getDocument(Long id) {
        return documentMapper.selectById(id);
    }

    @Override
    public byte[] getDocumentContent(DocumentDO document) {
        // 1) 优先按 fileConfigId + filePath 直读存储（不经过 HTTP，最稳）
        if (document.getFileConfigId() != null && StrUtil.isNotBlank(document.getFilePath())) {
            try {
                byte[] content = fileApi.getFileContent(document.getFileConfigId(), document.getFilePath());
                if (content != null && content.length > 0) {
                    return content;
                }
            } catch (Exception e) {
                log.warn("[getDocumentContent] 按存储路径读取失败 docId={} configId={} path={}: {}",
                        document.getId(), document.getFileConfigId(), document.getFilePath(), e.getMessage());
            }
        }
        // 2) 兜底：历史数据可能只有 fileUrl（共享/预签名地址）→ 直接 HTTP 拉取
        String url = StrUtil.isNotBlank(document.getFileUrl()) ? document.getFileUrl() : presignQuietly(document);
        if (StrUtil.isBlank(url)) {
            return null;
        }
        try {
            byte[] content = cn.hutool.http.HttpRequest.get(url).timeout(30000).execute().bodyBytes();
            return (content != null && content.length > 0) ? content : null;
        } catch (Exception e) {
            log.warn("[getDocumentContent] 回退按 URL 拉取失败 docId={}: {}", document.getId(), e.getMessage());
            return null;
        }
    }

    /** 生成预签名地址（失败返回 null，不抛异常） */
    private String presignQuietly(DocumentDO document) {
        if (StrUtil.isBlank(document.getFilePath())) {
            return null;
        }
        try {
            return fileApi.presignGetUrl(document.getFilePath(), PREVIEW_EXPIRES_DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public DocumentPreviewRespVO getDocumentPreview(DocumentDO document, Integer expiresIn) {
        // 有效期收敛：默认 30 分钟（够一次阅读，且不会长期泄露）
        int ttl = expiresIn == null || expiresIn <= 0 ? PREVIEW_EXPIRES_DEFAULT : expiresIn;
        ttl = Math.max(PREVIEW_EXPIRES_MIN, Math.min(PREVIEW_EXPIRES_MAX, ttl));

        String fileName = document.getFileName();
        String fileType = StrUtil.isNotBlank(document.getFileType())
                ? document.getFileType() : FileUtil.extName(fileName);
        fileType = StrUtil.blankToDefault(fileType, "").toLowerCase(Locale.ROOT);

        // 预签名按对象路径生成；历史数据可能只有共享 URL（fileUrl）兜底
        String path = StrUtil.isNotBlank(document.getFilePath())
                ? document.getFilePath() : document.getFileUrl();
        String url = null;
        if (StrUtil.isNotBlank(path)) {
            try {
                url = fileApi.presignGetUrl(path, ttl);
            } catch (Exception e) {
                log.warn("[getDocumentPreview] 预签名失败 docId={}: {}", document.getId(), e.getMessage());
            }
        }
        if (StrUtil.isBlank(url)) {
            url = document.getFileUrl();
        }

        return DocumentPreviewRespVO.builder()
                .id(document.getId())
                .fileName(fileName)
                .fileType(fileType)
                .url(url)
                .fileConfigId(document.getFileConfigId())
                .filePath(document.getFilePath())
                .expiresIn(ttl)
                // 页级定位（跳页 + bbox 高亮）目前仅 PDF 具备页码/坐标
                .pageLocatable("pdf".equals(fileType))
                .build();
    }

    @Override
    public PageResult<DocumentDO> getDocumentPage(DocumentPageReqVO pageReqVO) {
        // 未指定知识库时，按当前用户的"可见知识库"收敛文档，避免列出其无权访问的知识库文档
        if (pageReqVO.getKbId() == null) {
            Long userId = SecurityFrameworkUtils.getLoginUserId();
            VisibilityContext ctx = ruleEngine.buildContext(userId);
            if (!ctx.isSuperAdmin()) {
                List<Long> visibleKbIds = libraryMapper.selectSimpleList(null).stream()
                        .filter(lib -> ruleEngine.canSee(ctx, lib))
                        .map(LibraryDO::getId)
                        .collect(Collectors.toList());
                if (CollUtil.isEmpty(visibleKbIds)) {
                    // 无任何可见知识库 → 直接返回空，避免 inIfPresent 空集合退化为全量
                    return new PageResult<>(Collections.emptyList(), 0L);
                }
                pageReqVO.setKbIds(visibleKbIds);
            }
            // 超管短路：可见集合为其可见的全部知识库（此处不做裁剪，等同全量）
        }
        return documentMapper.selectPage(pageReqVO);
    }

    @Override
    public Long uploadAndCreate(MultipartFile file, Long kbId, String description, String tags) {
        return uploadAndCreate(file, kbId, null, description, tags);
    }

    @Override
    public Long uploadAndCreate(MultipartFile file, Long kbId, Long folderId, String description, String tags) {
        return uploadAndCreate(file, kbId, folderId, null, description, tags);
    }

    @Override
    public Long uploadAndCreate(MultipartFile file, Long kbId, Long folderId, String relPath, String description, String tags) {
        // 1. 校验知识库存在
        LibraryDO library = libraryMapper.selectById(kbId);
        if (library == null) {
            throw exception(LIBRARY_NOT_EXISTS);
        }

        // 1.1 文件夹层级上传：按相对路径逐级获取/创建子文件夹，定位叶子文件夹
        Long targetFolderId = folderId != null ? folderId : 0L;
        if (StrUtil.isNotBlank(relPath)) {
            targetFolderId = folderService.getOrCreateFolderByPath(kbId, targetFolderId, relPath);
        }

        // 2. 上传文件到芋道文件存储
        byte[] content;
        try {
            content = file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("读取文件内容失败", e);
        }
        String fileName = file.getOriginalFilename();
        // 文件夹/批量上传时，浏览器可能把原始文件名带上路径前缀（如 C:\fakepath\... 或 webkitRelativePath）。
        // 归一到纯文件名，避免 FilePathUtils.validateFileName 因含路径分隔符拒绝上传。
        String rawName = fileName;
        if (StrUtil.isNotBlank(fileName) && !StrUtil.equals(fileName, FileUtil.getName(fileName))) {
            fileName = FileUtil.getName(fileName);
            log.warn("[uploadAndCreate] 文件名含路径前缀，已归一化: [{}] -> [{}]", rawName, fileName);
        }
        // 使用 createFileDetail 获取完整文件信息（含 URL、路径、配置编号）
        FileUploadRespVO uploadResp = fileApi.createFileDetail(content, fileName, "kb", file.getContentType());

        // 3. 创建文档记录
        DocumentDO document = new DocumentDO();
        document.setKbId(kbId);
        document.setFolderId(targetFolderId);
        document.setFileName(fileName);
        document.setFileUrl(uploadResp.getUrl());
        document.setFilePath(uploadResp.getPath());
        document.setFileConfigId(uploadResp.getConfigId());
        document.setFileType(StrUtil.isNotBlank(fileName) ? FileUtil.extName(fileName) : null);
        document.setFileSize((long) content.length);
        document.setDescription(description);
        document.setTags(tags);
        document.setDownloadCount(0);
        document.setViewCount(0);
        document.setStatus(0);
        documentMapper.insert(document);

        // 4. 更新知识库的文档数量
        libraryMapper.updateDocCount(kbId, 1);

        // 5. 自动触发向量处理任务
        try {
            VectorTaskSubmitReqVO taskReqVO = new VectorTaskSubmitReqVO();
            taskReqVO.setDocId(document.getId());
            taskReqVO.setKbId(kbId);
            // 下发时重新生成签名 URL（7 天，host=文件配置 endpoint，python-vector 容器内网可达）：
            // 不复用 uploadResp.getUrl()（上传时签名，可能因队列延迟/重试过期）
            taskReqVO.setFileUrl(fileApi.presignGetUrl(uploadResp.getPath(), 604800));
            taskReqVO.setFileType(document.getFileType());
            taskReqVO.setDocumentTitle(fileName);
            vectorTaskService.submitTask(taskReqVO);
            log.info("[uploadAndCreate] 已触发向量处理任务: docId={}, taskId={}", document.getId());
        } catch (Exception e) {
            log.error("[uploadAndCreate] 触发向量处理任务失败: docId={}", document.getId(), e);
            // 不抛出异常，避免影响文件上传主流程
        }

        return document.getId();
    }

}
