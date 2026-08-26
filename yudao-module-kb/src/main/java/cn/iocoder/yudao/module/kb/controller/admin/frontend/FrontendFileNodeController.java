package cn.iocoder.yudao.module.kb.controller.admin.frontend;

import cn.iocoder.yudao.module.kb.controller.admin.document.vo.DocumentSaveReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.folder.vo.FolderSaveReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendFileNodeBatchDeleteReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendFileNodeBatchMoveReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendFileNodeFolderCreateReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendFileNodePageVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendFileNodeRenameReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendFileNodeVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendFolderTreeNodeVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendResult;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.folder.FolderDO;
import cn.iocoder.yudao.module.kb.dal.mysql.document.DocumentMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.folder.FolderMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import cn.iocoder.yudao.module.kb.service.document.DocumentService;
import cn.iocoder.yudao.module.kb.service.folder.FolderService;
import cn.iocoder.yudao.module.kb.service.library.LibraryService;
import cn.iocoder.yudao.module.kb.service.vectortask.VectorTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 前端 C 端 - 文件节点（文件夹/文档）兼容层
 *
 * <p>将 Python 端 {@code /knowledge/bases/{kbId}/file-nodes/**} 接口映射到 Java kb 模块的
 * Folder/Document Service，返回前端约定的扁平结构。覆盖知识库详情页（knowledge-folder-detail）
 * 所需的列表 / 目录树 / 新建 / 重命名 / 删除 / 上传 / 批量删除 / 批量移动 / 重建向量。
 *
 * <p>说明：笔记的在线编辑（create-document / content GET/PUT）依赖独立的文本内容存储，Java 端
 * kb_document 仅存文件引用，暂未在本次兼容层覆盖。
 *
 * @author 吴皓
 */
@Tag(name = "前端 C 端 - 文件节点兼容层")
@RestController
@RequestMapping("/knowledge/bases/{kbId}/file-nodes")
public class FrontendFileNodeController {

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private FolderService folderService;

    @Resource
    private DocumentService documentService;

    @Resource
    private VectorTaskService vectorTaskService;

    @Resource
    private FolderMapper folderMapper;

    @Resource
    private DocumentMapper documentMapper;

    @Resource
    private LibraryMapper libraryMapper;

    @Resource
    private LibraryService libraryService;

    // ---------- 查询 ----------

    /**
     * 分页查询指定目录下的文件夹 + 文档（对应 /file-nodes/items-paginated/）
     */
    @GetMapping({"/items-paginated", "/items-paginated/"})
    @Operation(summary = "前端文件节点分页列表")
    public FrontendResult<FrontendFileNodePageVO> itemsPaginated(
            @PathVariable("kbId") Long kbId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "parent_id", required = false) Long parentId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "tags", required = false) String tags) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long parent = parentId != null ? parentId : FolderDO.PARENT_ID_ROOT;

        // 文件夹（按 sort 升序）
        List<FolderDO> folders = folderMapper.selectByKbId(kbId).stream()
                .filter(f -> Objects.equals(f.getParentId(), parent))
                .filter(f -> matchesSearch(f.getName(), search))
                .sorted(Comparator.comparing(f -> f.getSort() == null ? 0 : f.getSort()))
                .collect(Collectors.toList());

        // 文档（按 id 倒序，新的在前）
        Set<String> tagSet = parseTags(tags);
        List<DocumentDO> documents = documentMapper.selectList(DocumentDO::getKbId, kbId).stream()
                .filter(d -> Objects.equals(d.getFolderId(), parent))
                .filter(d -> matchesSearch(d.getFileName(), search))
                .filter(d -> tagSet.isEmpty() || hasAnyTag(d.getTags(), tagSet))
                .sorted(Comparator.comparing(DocumentDO::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        List<FrontendFileNodeVO> items = new ArrayList<>();
        for (FolderDO f : folders) {
            items.add(toFolderVO(f, kbId));
        }
        for (DocumentDO d : documents) {
            items.add(toDocumentVO(d, kbId));
        }

        int total = items.size();
        int from = (pageNo - 1) * size;
        int to = Math.min(from + size, total);
        List<FrontendFileNodeVO> slice = from >= total ? Collections.emptyList() : items.subList(from, to);

        FrontendFileNodePageVO vo = new FrontendFileNodePageVO();
        vo.setItems(slice);
        vo.setPagination(new FrontendFileNodePageVO.Pagination((long) total, pageNo, size));
        return FrontendResult.ok(vo);
    }

    /**
     * 文件夹树（对应 /file-nodes/folder-tree/）
     */
    @GetMapping({"/folder-tree", "/folder-tree/"})
    @Operation(summary = "前端文件夹树")
    public FrontendResult<List<FrontendFolderTreeNodeVO>> folderTree(@PathVariable("kbId") Long kbId) {
        List<FolderDO> tree = folderService.getFolderTree(kbId);
        return FrontendResult.ok(buildFolderTreeNodes(tree));
    }

    // ---------- 变更 ----------

    /**
     * 新建文件夹（对应 /file-nodes/folders/）
     */
    @PostMapping({"/folders", "/folders/"})
    @Operation(summary = "前端新建文件夹")
    public FrontendResult<Long> createFolder(@PathVariable("kbId") Long kbId,
                                             @RequestBody FrontendFileNodeFolderCreateReqVO req) {
        FrontendResult<Long> denied = requireCanManage(kbId);
        if (denied != null) {
            return denied;
        }
        FolderSaveReqVO vo = new FolderSaveReqVO();
        vo.setKbId(kbId);
        vo.setName(req.getName());
        vo.setParentId(req.getParentId());
        return FrontendResult.ok(folderService.createFolder(vo));
    }

    /**
     * 重命名节点（对应 /file-nodes/{id}/rename/，文件夹与文档通用）
     */
    @PatchMapping({"/{id}/rename", "/{id}/rename/"})
    @Operation(summary = "前端重命名文件节点")
    public FrontendResult<Boolean> rename(@PathVariable("kbId") Long kbId,
                                          @PathVariable("id") Long id,
                                          @RequestBody FrontendFileNodeRenameReqVO req) {
        FrontendResult<Boolean> denied = requireCanManage(kbId);
        if (denied != null) {
            return denied;
        }
        FolderDO folder = folderMapper.selectById(id);
        if (folder != null && Objects.equals(folder.getKbId(), kbId)) {
            FolderSaveReqVO vo = new FolderSaveReqVO();
            vo.setId(id);
            vo.setKbId(folder.getKbId());
            vo.setName(req.getName());
            folderService.updateFolder(vo);
            return FrontendResult.ok(true);
        }
        DocumentDO doc = documentMapper.selectById(id);
        if (doc != null && Objects.equals(doc.getKbId(), kbId)) {
            DocumentSaveReqVO vo = new DocumentSaveReqVO();
            vo.setId(id);
            vo.setKbId(doc.getKbId());
            vo.setFileName(req.getName());
            vo.setFileUrl(doc.getFileUrl());
            documentService.updateDocument(vo);
            return FrontendResult.ok(true);
        }
        return FrontendResult.error("节点不存在");
    }

    /**
     * 强制删除节点（对应 /file-nodes/{id}/force-delete/，文件夹级联删除）
     */
    @PostMapping({"/{id}/force-delete", "/{id}/force-delete/"})
    @Operation(summary = "前端强制删除文件节点")
    public FrontendResult<Boolean> forceDelete(@PathVariable("kbId") Long kbId,
                                               @PathVariable("id") Long id) {
        FrontendResult<Boolean> denied = requireCanManage(kbId);
        if (denied != null) {
            return denied;
        }
        int removedDocs = deleteNode(kbId, id);
        if (removedDocs < 0) {
            return FrontendResult.error("节点不存在");
        }
        if (removedDocs > 0) {
            libraryMapper.updateDocCount(kbId, -removedDocs);
        }
        return FrontendResult.ok(true);
    }

    /**
     * 批量强制删除（对应 /file-nodes/batch-force-delete/）
     */
    @PostMapping({"/batch-force-delete", "/batch-force-delete/"})
    @Operation(summary = "前端批量删除文件节点")
    public FrontendResult<Boolean> batchForceDelete(@PathVariable("kbId") Long kbId,
                                                    @RequestBody FrontendFileNodeBatchDeleteReqVO req) {
        FrontendResult<Boolean> denied = requireCanManage(kbId);
        if (denied != null) {
            return denied;
        }
        if (req.getNodeIds() == null || req.getNodeIds().isEmpty()) {
            return FrontendResult.ok(true);
        }
        int removedDocs = 0;
        for (Long id : req.getNodeIds()) {
            int n = deleteNode(kbId, id);
            if (n > 0) {
                removedDocs += n;
            }
        }
        if (removedDocs > 0) {
            libraryMapper.updateDocCount(kbId, -removedDocs);
        }
        return FrontendResult.ok(true);
    }

    /**
     * 批量移动（对应 /file-nodes/batch-move/，支持同库与跨库）
     */
    @PostMapping({"/batch-move", "/batch-move/"})
    @Operation(summary = "前端批量移动文件节点")
    public FrontendResult<Boolean> batchMove(@PathVariable("kbId") Long kbId,
                                             @RequestBody FrontendFileNodeBatchMoveReqVO req) {
        FrontendResult<Boolean> denied = requireCanManage(kbId);
        if (denied != null) {
            return denied;
        }
        if (req.getNodeIds() == null || req.getNodeIds().isEmpty()) {
            return FrontendResult.ok(true);
        }
        Long targetKbId = req.getTargetKnowledgeBaseId() != null ? req.getTargetKnowledgeBaseId() : kbId;
        if (!Objects.equals(targetKbId, kbId)) {
            FrontendResult<Boolean> targetDenied = requireCanManage(targetKbId);
            if (targetDenied != null) {
                return targetDenied;
            }
        }
        Long targetParentId = req.getTargetParentId() != null ? req.getTargetParentId() : FolderDO.PARENT_ID_ROOT;

        int movedDocs = 0;
        for (Long id : req.getNodeIds()) {
            movedDocs += moveNode(kbId, id, targetKbId, targetParentId);
        }
        if (!Objects.equals(targetKbId, kbId) && movedDocs > 0) {
            libraryMapper.updateDocCount(kbId, -movedDocs);
            libraryMapper.updateDocCount(targetKbId, movedDocs);
        }
        return FrontendResult.ok(true);
    }

    /**
     * 重建向量（对应 /file-nodes/{id}/force-retry/）
     */
    @PostMapping({"/{id}/force-retry", "/{id}/force-retry/"})
    @Operation(summary = "前端重建文档向量")
    public FrontendResult<Boolean> forceRetry(@PathVariable("kbId") Long kbId,
                                              @PathVariable("id") Long id) {
        FrontendResult<Boolean> denied = requireCanManage(kbId);
        if (denied != null) {
            return denied;
        }
        try {
            vectorTaskService.retryTask(id);
            return FrontendResult.ok(true);
        } catch (Exception e) {
            return FrontendResult.error(e.getMessage());
        }
    }

    /**
     * 上传文件（对应 /file-nodes/upload/）
     */
    @PostMapping({"/upload", "/upload/"})
    @Operation(summary = "前端上传文件到知识库")
    public FrontendResult<Long> upload(@PathVariable("kbId") Long kbId,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "parent_id", required = false) Long parentId,
                                       @RequestParam(value = "tags", required = false) String tags,
                                       @RequestParam(value = "description", required = false) String description) {
        FrontendResult<Long> denied = requireCanManage(kbId);
        if (denied != null) {
            return denied;
        }
        return FrontendResult.ok(documentService.uploadAndCreate(file, kbId, parentId, description, tags));
    }

    /** 与后台知识库大屏一致：仅 canManage 可改文件；无权限返回错误结果，不抛给 yudao 通用包装。 */
    private <T> FrontendResult<T> requireCanManage(Long kbId) {
        if (!libraryService.canManage(kbId)) {
            return FrontendResult.error("无权限操作该知识库");
        }
        return null;
    }

    // ---------- 内部逻辑 ----------

    /**
     * 删除单个节点，返回删除的文档数量（用于 docCount 扣减），-1 表示节点不存在。
     * 文件夹级联删除其子树与其中的所有文档。
     */
    private int deleteNode(Long kbId, Long id) {
        FolderDO folder = folderMapper.selectById(id);
        if (folder != null && Objects.equals(folder.getKbId(), kbId)) {
            return deleteFolderCascade(kbId, id);
        }
        DocumentDO doc = documentMapper.selectById(id);
        if (doc != null && Objects.equals(doc.getKbId(), kbId)) {
            documentMapper.deleteById(id);
            return 1;
        }
        return -1;
    }

    /** 级联删除文件夹（含子树与其中的文档），返回删除的文档数量 */
    private int deleteFolderCascade(Long kbId, Long folderId) {
        List<FolderDO> all = folderMapper.selectByKbId(kbId);
        Map<Long, List<Long>> childrenByParent = new HashMap<>();
        for (FolderDO f : all) {
            childrenByParent.computeIfAbsent(f.getParentId(), k -> new ArrayList<>()).add(f.getId());
        }
        // 后序遍历（子级先删），避免残留子文件夹
        List<Long> ordered = new ArrayList<>();
        collectPostOrder(folderId, childrenByParent, ordered);

        int removedDocs = 0;
        for (Long fid : ordered) {
            List<DocumentDO> docs = documentMapper.selectList(DocumentDO::getFolderId, fid);
            if (!docs.isEmpty()) {
                documentMapper.deleteByIds(docs.stream().map(DocumentDO::getId).collect(Collectors.toList()));
                removedDocs += docs.size();
            }
            folderMapper.deleteById(fid);
        }
        return removedDocs;
    }

    private void collectPostOrder(Long folderId, Map<Long, List<Long>> childrenByParent, List<Long> out) {
        for (Long child : childrenByParent.getOrDefault(folderId, Collections.emptyList())) {
            collectPostOrder(child, childrenByParent, out);
        }
        out.add(folderId);
    }

    /** 移动单个节点（文件夹整棵子树 / 单个文档），返回移动的文档数量（用于 docCount 调整） */
    private int moveNode(Long kbId, Long id, Long targetKbId, Long targetParentId) {
        FolderDO folder = folderMapper.selectById(id);
        if (folder != null && Objects.equals(folder.getKbId(), kbId)) {
            List<FolderDO> all = folderMapper.selectByKbId(kbId);
            Map<Long, List<Long>> childrenByParent = new HashMap<>();
            for (FolderDO f : all) {
                childrenByParent.computeIfAbsent(f.getParentId(), k -> new ArrayList<>()).add(f.getId());
            }
            List<Long> subtree = new ArrayList<>();
            collectPostOrder(id, childrenByParent, subtree);

            int movedDocs = 0;
            // 子树内文档：仅改 kbId（folderId 不变，文件夹结构保持）
            for (Long fid : subtree) {
                List<DocumentDO> docs = documentMapper.selectList(DocumentDO::getFolderId, fid);
                for (DocumentDO d : docs) {
                    d.setKbId(targetKbId);
                    documentMapper.updateById(d);
                }
                movedDocs += docs.size();
            }
            // 子树内文件夹：仅改 kbId（parentId 不变）
            for (Long fid : subtree) {
                if (Objects.equals(fid, id)) {
                    continue;
                }
                FolderDO f = folderMapper.selectById(fid);
                f.setKbId(targetKbId);
                folderMapper.updateById(f);
            }
            // 顶层文件夹：改 kbId + 挂到目标父级
            folder.setKbId(targetKbId);
            folder.setParentId(targetParentId);
            folderMapper.updateById(folder);
            return movedDocs;
        }
        DocumentDO doc = documentMapper.selectById(id);
        if (doc != null && Objects.equals(doc.getKbId(), kbId)) {
            doc.setKbId(targetKbId);
            doc.setFolderId(targetParentId);
            documentMapper.updateById(doc);
            return 1;
        }
        return 0;
    }

    // ---------- 映射 ----------

    private List<FrontendFolderTreeNodeVO> buildFolderTreeNodes(List<FolderDO> folders) {
        return folders.stream().map(f -> {
            FrontendFolderTreeNodeVO n = new FrontendFolderTreeNodeVO();
            n.setId(f.getId());
            n.setName(f.getName());
            n.setChildren(f.getChildren() == null || f.getChildren().isEmpty()
                    ? Collections.emptyList() : buildFolderTreeNodes(f.getChildren()));
            return n;
        }).collect(Collectors.toList());
    }

    private FrontendFileNodeVO toFolderVO(FolderDO f, Long kbId) {
        FrontendFileNodeVO vo = new FrontendFileNodeVO();
        vo.setId(f.getId());
        vo.setName(f.getName());
        vo.setItemType("folder");
        vo.setTags(Collections.emptyList());
        vo.setCreatedAt(format(f.getCreateTime()));
        vo.setKnowledgeBaseId(kbId);
        FrontendFileNodeVO.Thumbnail t = new FrontendFileNodeVO.Thumbnail();
        t.setType("folder");
        vo.setThumbnail(t);
        return vo;
    }

    private FrontendFileNodeVO toDocumentVO(DocumentDO d, Long kbId) {
        FrontendFileNodeVO vo = new FrontendFileNodeVO();
        vo.setId(d.getId());
        vo.setName(d.getFileName());
        vo.setItemType("document");
        vo.setDocId(d.getId());
        vo.setExtension(d.getFileType());
        vo.setUrl(d.getFileUrl());
        vo.setFilePath(d.getFilePath());
        if (d.getFileSize() != null) {
            vo.setFileSizeMb(String.format("%.2f", d.getFileSize() / 1024.0 / 1024.0));
        }
        vo.setTags(splitTags(d.getTags()));
        vo.setStatus(toStatus(d.getVectorStatus()));
        vo.setStatusDisplay(toStatusDisplay(d.getVectorStatus()));
        vo.setCreatedAt(format(d.getCreateTime()));
        vo.setKnowledgeBaseId(kbId);
        FrontendFileNodeVO.Thumbnail t = new FrontendFileNodeVO.Thumbnail();
        t.setType(d.getFileType());
        vo.setThumbnail(t);
        return vo;
    }

    private String toStatus(Integer vectorStatus) {
        if (vectorStatus == null || vectorStatus == 0) {
            return "pending";
        }
        switch (vectorStatus) {
            case 1:
                return "processing";
            case 2:
                return "completed";
            case 3:
            case 4:
            case 5:
                return "failed";
            default:
                return "pending";
        }
    }

    private String toStatusDisplay(Integer vectorStatus) {
        switch (toStatus(vectorStatus)) {
            case "processing":
                return "正在处理";
            case "completed":
                return "成功";
            case "failed":
                return "失败";
            default:
                return "待处理";
        }
    }

    private boolean matchesSearch(String value, String search) {
        return search == null || search.isEmpty() || (value != null && value.contains(search));
    }

    private Set<String> parseTags(String tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private boolean hasAnyTag(String docTags, Set<String> tagSet) {
        if (docTags == null || docTags.isEmpty()) {
            return false;
        }
        return Arrays.stream(docTags.split(",")).map(String::trim).anyMatch(tagSet::contains);
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    private String format(LocalDateTime time) {
        return time == null ? null : time.format(DATETIME);
    }
}
