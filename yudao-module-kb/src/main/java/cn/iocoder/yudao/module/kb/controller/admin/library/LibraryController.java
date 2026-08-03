package cn.iocoder.yudao.module.kb.controller.admin.library;

import org.springframework.web.bind.annotation.*;
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
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.iocoder.yudao.module.kb.controller.admin.library.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.service.library.LibraryService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;

@Tag(name = "管理后台 - 知识库")
@RestController
@RequestMapping("/kb/library")
@Validated
public class LibraryController {

    @Resource
    private LibraryService libraryService;

    @PostMapping("/create")
    @Operation(summary = "创建知识库")
    @PreAuthorize("@ss.hasPermission('kb:library:create')")
    public CommonResult<Long> createLibrary(@Valid @RequestBody LibrarySaveReqVO createReqVO) {
        return success(libraryService.createLibrary(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库")
    @PreAuthorize("@ss.hasPermission('kb:library:update')")
    public CommonResult<Boolean> updateLibrary(@Valid @RequestBody LibrarySaveReqVO updateReqVO) {
        libraryService.updateLibrary(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('kb:library:delete')")
    public CommonResult<Boolean> deleteLibrary(@RequestParam("id") Long id) {
        libraryService.deleteLibrary(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除知识库")
                @PreAuthorize("@ss.hasPermission('kb:library:delete')")
    public CommonResult<Boolean> deleteLibraryList(@RequestParam("ids") List<Long> ids) {
        libraryService.deleteLibraryListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得知识库")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('kb:library:query')")
    public CommonResult<LibraryRespVO> getLibrary(@RequestParam("id") Long id) {
        LibraryDO library = libraryService.getLibrary(id);
        return success(BeanUtils.toBean(library, LibraryRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得知识库分页（管理后台：返回全部，由权限串控制）")
    @PreAuthorize("@ss.hasPermission('kb:library:query')")
    public CommonResult<PageResult<LibraryRespVO>> getLibraryPage(@Valid LibraryPageReqVO pageReqVO) {
        // 管理后台分页查询，不做可见性过滤（管理员通过权限串控制访问范围）
        PageResult<LibraryDO> pageResult = libraryService.getLibraryPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, LibraryRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出知识库 Excel")
    @PreAuthorize("@ss.hasPermission('kb:library:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportLibraryExcel(@Valid LibraryPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<LibraryDO> list = libraryService.getLibraryPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "知识库.xls", "数据", LibraryRespVO.class,
                        BeanUtils.toBean(list, LibraryRespVO.class));
    }

    @PutMapping("/toggle-public")
    @Operation(summary = "切换知识库公开状态")
    @PreAuthorize("@ss.hasPermission('kb:library:update')")
    public CommonResult<Boolean> togglePublic(@RequestParam("id") Long id) {
        libraryService.togglePublic(id);
        return success(true);
    }

    @GetMapping("/public-page")
    @Operation(summary = "获得广场公开知识库分页（全部公开）")
    @PreAuthorize("@ss.hasPermission('kb:library:query')")
    public CommonResult<PageResult<LibraryRespVO>> getPublicPage(@Valid PageParam pageParam) {
        PageResult<LibraryDO> pageResult = libraryService.getPublicPage(pageParam);
        return success(BeanUtils.toBean(pageResult, LibraryRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得知识库精简列表（用于下拉选择）")
    @Parameter(name = "isProject", description = "是否项目成果库（可选，不传=全部）")
    @PreAuthorize("@ss.hasPermission('kb:library:query')")
    public CommonResult<List<LibrarySimpleVO>> getSimpleLibraryList(
            @RequestParam(value = "isProject", required = false) Integer isProject) {
        List<LibraryDO> list = libraryService.getSimpleLibraryList(isProject);
        return success(BeanUtils.toBean(list, LibrarySimpleVO.class));
    }

    @GetMapping("/my-public-page")
    @Operation(summary = "获得我公开的知识库分页")
    @PreAuthorize("@ss.hasPermission('kb:library:query')")
    public CommonResult<PageResult<LibraryRespVO>> getMyPublicPage(@Valid PageParam pageParam) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        PageResult<LibraryDO> pageResult = libraryService.getMyPublicPage(pageParam, userId);
        return success(BeanUtils.toBean(pageResult, LibraryRespVO.class));
    }

}