package cn.iocoder.yudao.module.kb.controller.admin.sharedept;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.constraints.*;
import jakarta.validation.*;
import jakarta.servlet.http.*;
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

import cn.iocoder.yudao.module.kb.controller.admin.sharedept.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.sharedept.ShareDeptDO;
import cn.iocoder.yudao.module.kb.service.sharedept.ShareDeptService;

@Tag(name = "管理后台 - 知识库共享部门关联")
@RestController
@RequestMapping("/kb/share-dept")
@Validated
public class ShareDeptController {

    @Resource
    private ShareDeptService shareDeptService;

    @PostMapping("/create")
    @Operation(summary = "创建知识库共享部门关联")
    @PreAuthorize("@ss.hasPermission('kb:share-dept:create')")
    public CommonResult<Long> createShareDept(@Valid @RequestBody ShareDeptSaveReqVO createReqVO) {
        return success(shareDeptService.createShareDept(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库共享部门关联")
    @PreAuthorize("@ss.hasPermission('kb:share-dept:update')")
    public CommonResult<Boolean> updateShareDept(@Valid @RequestBody ShareDeptSaveReqVO updateReqVO) {
        shareDeptService.updateShareDept(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库共享部门关联")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('kb:share-dept:delete')")
    public CommonResult<Boolean> deleteShareDept(@RequestParam("id") Long id) {
        shareDeptService.deleteShareDept(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除知识库共享部门关联")
                @PreAuthorize("@ss.hasPermission('kb:share-dept:delete')")
    public CommonResult<Boolean> deleteShareDeptList(@RequestParam("ids") List<Long> ids) {
        shareDeptService.deleteShareDeptListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得知识库共享部门关联")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('kb:share-dept:query')")
    public CommonResult<ShareDeptRespVO> getShareDept(@RequestParam("id") Long id) {
        ShareDeptDO shareDept = shareDeptService.getShareDept(id);
        return success(BeanUtils.toBean(shareDept, ShareDeptRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得知识库共享部门关联分页")
    @PreAuthorize("@ss.hasPermission('kb:share-dept:query')")
    public CommonResult<PageResult<ShareDeptRespVO>> getShareDeptPage(@Valid ShareDeptPageReqVO pageReqVO) {
        PageResult<ShareDeptDO> pageResult = shareDeptService.getShareDeptPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ShareDeptRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出知识库共享部门关联 Excel")
    @PreAuthorize("@ss.hasPermission('kb:share-dept:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportShareDeptExcel(@Valid ShareDeptPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ShareDeptDO> list = shareDeptService.getShareDeptPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "知识库共享部门关联.xls", "数据", ShareDeptRespVO.class,
                        BeanUtils.toBean(list, ShareDeptRespVO.class));
    }

}