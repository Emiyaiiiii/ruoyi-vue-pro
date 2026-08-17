package cn.iocoder.yudao.module.kb.controller.admin.folder;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.*;
import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.module.kb.controller.admin.folder.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.folder.FolderDO;
import cn.iocoder.yudao.module.kb.service.folder.FolderService;
import cn.iocoder.yudao.module.kb.service.library.LibraryService;

@Tag(name = "管理后台 - 文档文件夹")
@RestController
@RequestMapping("/kb/document-folder")
@Validated
public class FolderController {

    @Resource
    private FolderService folderService;

    @Resource
    private LibraryService libraryService;

    @PostMapping("/create")
    @Operation(summary = "创建文档文件夹")
    @PreAuthorize("@ss.hasPermission('kb:document:create')")
    public CommonResult<Long> createFolder(@Valid @RequestBody FolderSaveReqVO createReqVO) {
        validateFolderManagementAccess(createReqVO.getKbId());
        return success(folderService.createFolder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新文档文件夹（重命名）")
    @PreAuthorize("@ss.hasPermission('kb:document:update')")
    public CommonResult<Boolean> updateFolder(@Valid @RequestBody FolderSaveReqVO updateReqVO) {
        validateFolderManagementAccess(updateReqVO.getKbId());
        folderService.updateFolder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文档文件夹")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:delete')")
    public CommonResult<Boolean> deleteFolder(@RequestParam("id") Long id) {
        FolderDO folder = folderService.getFolder(id);
        if (folder != null) {
            validateFolderManagementAccess(folder.getKbId());
        }
        folderService.deleteFolder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得文档文件夹")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<FolderRespVO> getFolder(@RequestParam("id") Long id) {
        FolderDO folder = folderService.getFolder(id);
        return success(BeanUtils.toBean(folder, FolderRespVO.class));
    }

    @GetMapping("/list-tree")
    @Operation(summary = "获得指定知识库的文件夹树")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<List<FolderRespVO>> getFolderTree(@RequestParam("kbId") Long kbId) {
        List<FolderDO> tree = folderService.getFolderTree(kbId);
        return success(BeanUtils.toBean(tree, FolderRespVO.class));
    }

    /**
     * 文件夹管理权限校验
     * 只有知识库管理员可操作文件夹
     */
    private void validateFolderManagementAccess(Long kbId) {
        if (kbId == null) return;
        if (!libraryService.canManage(kbId)) {
            throw exception(LIBRARY_PERMISSION_DENIED);
        }
    }
}