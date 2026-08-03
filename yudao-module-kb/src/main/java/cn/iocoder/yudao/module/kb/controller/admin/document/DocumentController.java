package cn.iocoder.yudao.module.kb.controller.admin.document;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import javax.validation.constraints.*;
import javax.validation.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.IOException;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.iocoder.yudao.module.kb.controller.admin.document.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.service.document.DocumentService;
import cn.iocoder.yudao.module.kb.service.library.LibraryService;
import cn.iocoder.yudao.module.kb.service.projectmember.ProjectMemberService;

@Tag(name = "管理后台 - 知识库文件")
@RestController
@RequestMapping("/kb/document")
@Validated
public class DocumentController {

    @Resource
    private DocumentService documentService;

    @Resource
    private LibraryService libraryService;

    @Resource
    private ProjectMemberService projectMemberService;

    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @PostMapping("/create")
    @Operation(summary = "创建知识库文件")
    @PreAuthorize("@ss.hasPermission('kb:document:create')")
    public CommonResult<Long> createDocument(@Valid @RequestBody DocumentSaveReqVO createReqVO) {
        return success(documentService.createDocument(createReqVO));
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件到知识库", description = "上传文件到芋道文件存储，并自动创建知识库文档记录")
    @PreAuthorize("@ss.hasPermission('kb:document:create')")
    public CommonResult<Long> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("kbId") Long kbId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags) {
        return success(documentService.uploadAndCreate(file, kbId, description, tags));
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库文件")
    @PreAuthorize("@ss.hasPermission('kb:document:update')")
    public CommonResult<Boolean> updateDocument(@Valid @RequestBody DocumentSaveReqVO updateReqVO) {
        documentService.updateDocument(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库文件")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:delete')")
    public CommonResult<Boolean> deleteDocument(@RequestParam("id") Long id) {
        documentService.deleteDocument(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除知识库文件")
                @PreAuthorize("@ss.hasPermission('kb:document:delete')")
    public CommonResult<Boolean> deleteDocumentList(@RequestParam("ids") List<Long> ids) {
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

        // 超级管理员/租户管理员 → 可访问所有项目成果库内容
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId != null && securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode())) {
            return;
        }

        // 项目成果库 → 校验当前用户是否为项目成员
        if (userId == null || !projectMemberService.isMember(kbId, userId)) {
            throw exception(DOCUMENT_PERMISSION_DENIED);
        }
    }

}
