package cn.iocoder.yudao.module.kb.controller.admin.document;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.constraints.*;
import jakarta.validation.*;
import jakarta.servlet.http.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.IOException;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.http.HttpStatus;

import cn.iocoder.yudao.module.kb.controller.admin.chunk.vo.ChunkItemVO;
import cn.iocoder.yudao.module.kb.controller.admin.document.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.service.chunk.KbChunkService;
import cn.iocoder.yudao.module.kb.service.document.DocumentService;
import cn.iocoder.yudao.module.kb.service.library.LibraryService;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "管理后台 - 知识库文件")
@RestController
@RequestMapping("/kb/document")
@Validated
@Slf4j
public class DocumentController {

    @Resource
    private DocumentService documentService;

    @Resource
    private LibraryService libraryService;

    @Resource
    private KbChunkService kbChunkService;

    /** 文本预览最大字符数（约 120KB，超出提示下载原文件） */
    private static final int MAX_PREVIEW_TEXT_LENGTH = 120000;

    @PostMapping("/create")
    @Operation(summary = "创建知识库文件")
    @PreAuthorize("@ss.hasPermission('kb:document:create')")
    public CommonResult<Long> createDocument(@Valid @RequestBody DocumentSaveReqVO createReqVO) {
        validateDocumentManagementAccess(createReqVO.getKbId());
        return success(documentService.createDocument(createReqVO));
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件到知识库", description = "上传文件到芋道文件存储，并自动创建知识库文档记录")
    @PreAuthorize("@ss.hasPermission('kb:document:create')")
    public CommonResult<Long> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("kbId") Long kbId,
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestParam(value = "relPath", required = false) String relPath,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags) {
        validateDocumentManagementAccess(kbId);
        return success(documentService.uploadAndCreate(file, kbId, folderId, relPath, description, tags));
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库文件")
    @PreAuthorize("@ss.hasPermission('kb:document:update')")
    public CommonResult<Boolean> updateDocument(@Valid @RequestBody DocumentSaveReqVO updateReqVO) {
        validateDocumentManagementAccess(updateReqVO.getKbId());
        documentService.updateDocument(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库文件")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:delete')")
    public CommonResult<Boolean> deleteDocument(@RequestParam("id") Long id) {
        DocumentDO document = documentService.getDocument(id);
        if (document != null) {
            validateDocumentManagementAccess(document.getKbId());
        }
        documentService.deleteDocument(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除知识库文件")
                @PreAuthorize("@ss.hasPermission('kb:document:delete')")
    public CommonResult<Boolean> deleteDocumentList(@RequestParam("ids") List<Long> ids) {
        for (Long id : ids) {
            DocumentDO document = documentService.getDocument(id);
            if (document != null) {
                validateDocumentManagementAccess(document.getKbId());
            }
        }
        documentService.deleteDocumentListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得知识库文件")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<DocumentRespVO> getDocument(@RequestParam("id") Long id) {
        DocumentDO document = documentService.getDocument(id);
        // 项目成果库内容访问校验：仅项目成员可查看文档详情
        validateProjectContentAccess(document.getKbId());
        return success(BeanUtils.toBean(document, DocumentRespVO.class));
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "获得文档预览信息",
            description = "返回短时效预签名地址；传 chunkIndex 附带切片页码/bbox/正文，传 includeText 附带解析后的全文，"
                    + "供前端跳页定位与 Office 类文档的文本预览")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<DocumentPreviewRespVO> previewDocument(
            @PathVariable("id") Long id,
            @RequestParam(value = "expiresIn", required = false, defaultValue = "1800") Integer expiresIn,
            @RequestParam(value = "chunkIndex", required = false) Integer chunkIndex,
            @RequestParam(value = "includeText", required = false, defaultValue = "false") Boolean includeText) {
        DocumentDO document = documentService.getDocument(id);
        if (document == null) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
        // 项目成果库内容访问校验：仅项目成员可查看文档内容
        // 注意：此处不加 @PreAuthorize —— 问答页为前台普通用户使用，无 kb:document:* 管理权限；
        // 访问边界由「登录态 + 项目成果库成员校验」承担（与 FrontendFileNodeController 一致）
        validateProjectContentAccess(document.getKbId());

        DocumentPreviewRespVO preview = documentService.getDocumentPreview(document, expiresIn);
        boolean needChunks = chunkIndex != null || Boolean.TRUE.equals(includeText);
        if (!needChunks || document.getKbId() == null) {
            return success(preview);
        }
        // 切片只查一次：既用于定位（页码/bbox/正文），也用于文本预览的全文拼装
        List<ChunkItemVO> chunks;
        try {
            chunks = kbChunkService.listChunks(id, document.getKbId());
        } catch (Exception e) {
            log.warn("[previewDocument] 读取切片失败 docId={}: {}", id, e.getMessage());
            return success(preview);
        }
        if (chunkIndex != null) {
            chunks.stream()
                    .filter(chunk -> chunkIndex.equals(chunk.getChunkIndex()))
                    .findFirst()
                    .ifPresent(chunk -> {
                        preview.setPageStart(chunk.getPageStart());
                        preview.setPageEnd(chunk.getPageEnd());
                        preview.setBboxes(chunk.getBboxes());
                        preview.setPageSource(chunk.getPageSource());
                        // 正文供前端在页内做全文定位（bbox 只覆盖对齐到的块，正文更全）
                        String content = chunk.getContent();
                        if (StrUtil.isNotBlank(content)) {
                            preview.setChunkContent(content.length() > 2000
                                    ? content.substring(0, 2000) : content);
                        }
                    });
        }
        if (Boolean.TRUE.equals(includeText)) {
            preview.setTextContent(assembleChunkText(chunks));
        }
        return success(preview);
    }

    /**
     * 把切片按序号拼成纯文本，供无法内嵌渲染的文档（docx/xlsx/pptx 等）做文本预览。
     * 跳过父块（父块是上下文副本，拼进去会重复）；超过上限截断，避免响应体过大。
     */
    private static String assembleChunkText(List<ChunkItemVO> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        chunks.stream()
                .filter(chunk -> !Boolean.TRUE.equals(chunk.getIsParent()))
                .filter(chunk -> !Boolean.TRUE.equals(chunk.getIsImage()))
                .sorted(Comparator.comparing(ChunkItemVO::getChunkIndex,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(chunk -> {
                    if (sb.length() >= MAX_PREVIEW_TEXT_LENGTH) {
                        return;
                    }
                    String content = chunk.getContextHeader() != null && sb.length() == 0
                            ? chunk.getContextHeader() + "\n" + chunk.getContent()
                            : chunk.getContent();
                    if (StrUtil.isBlank(content)) {
                        return;
                    }
                    if (sb.length() > 0) {
                        sb.append("\n\n");
                    }
                    sb.append(content);
                });
        if (sb.length() == 0) {
            return null;
        }
        return sb.length() > MAX_PREVIEW_TEXT_LENGTH
                ? sb.substring(0, MAX_PREVIEW_TEXT_LENGTH) + "\n\n...(文本预览已截断，可下载原文件查看完整内容)"
                : sb.toString();
    }

    @GetMapping("/{id}/content")
    @Operation(summary = "获取文档原始字节",
            description = "同源输出文件字节，供前端 PDF 渲染（规避对象存储跨域限制）")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public void previewContent(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        DocumentDO document = documentService.getDocument(id);
        if (document == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        // 与 preview 一致：登录态 + 项目成果库成员校验（前台用户无 kb:document:* 管理权限）
        validateProjectContentAccess(document.getKbId());
        byte[] content = documentService.getDocumentContent(document);
        if (content == null || content.length == 0) {
            log.warn("[previewContent] 文档内容为空 docId={} fileName={}", id, document.getFileName());
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        String ext = StrUtil.blankToDefault(document.getFileType(),
                FileUtil.extName(document.getFileName()));
        ext = StrUtil.blankToDefault(ext, "").toLowerCase(Locale.ROOT);
        String fileName = StrUtil.blankToDefault(document.getFileName(), "document." + ext);
        response.setContentType(contentTypeOf(ext));
        response.setContentLength(content.length);
        response.setHeader("Content-Disposition",
                "inline; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()) + "\"");
        response.setHeader("Cache-Control", "private, max-age=300");
        response.getOutputStream().write(content);
    }

    /** 按扩展名给出内联预览用的 Content-Type */
    private static String contentTypeOf(String ext) {
        switch (ext) {
            case "pdf":
                return "application/pdf";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "txt":
            case "md":
                return "text/plain;charset=UTF-8";
            default:
                return "application/octet-stream";
        }
    }

    @GetMapping("/page")
    @Operation(summary = "获得知识库文件分页")
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<PageResult<DocumentRespVO>> getDocumentPage(@Valid DocumentPageReqVO pageReqVO) {
        // 项目成果库内容访问校验：仅项目成员可查看文档列表
        if (pageReqVO.getKbId() != null) {
            validateProjectContentAccess(pageReqVO.getKbId());
        }
        PageResult<DocumentDO> pageResult = documentService.getDocumentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, DocumentRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出知识库文件 Excel")
    @PreAuthorize("@ss.hasPermission('kb:document:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDocumentExcel(@Valid DocumentPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<DocumentDO> list = documentService.getDocumentPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "知识库文件.xls", "数据", DocumentRespVO.class,
                        BeanUtils.toBean(list, DocumentRespVO.class));
    }

    /**
     * 项目成果库内容访问校验
     * 如果知识库是项目成果库（is_project=1），则只有项目成员可以查看文档内容
     */
    private void validateProjectContentAccess(Long kbId) {
        if (kbId == null) return;
        LibraryDO library = libraryService.getLibrary(kbId);
        if (library == null) return;

        // 非项目成果库 → 无额外限制
        if (library.getIsProject() == null || library.getIsProject() != 1) return;

        // 项目成果库 → 需为项目成员（或租户管理员/超管短路）；实时读「打开依据成员字段」ext 值判断
        if (!libraryService.canOpenProjectLibrary(kbId)) {
            throw exception(DOCUMENT_PERMISSION_DENIED);
        }
    }

    /**
     * 文档管理权限校验（创建/上传/编辑/删除）
     * 只有知识库管理员可操作文档和文件夹
     */
    private void validateDocumentManagementAccess(Long kbId) {
        if (kbId == null) return;
        if (!libraryService.canManage(kbId)) {
            throw exception(LIBRARY_PERMISSION_DENIED);
        }
    }
}
