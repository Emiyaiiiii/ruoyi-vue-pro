package cn.iocoder.yudao.module.kb.controller.admin.chunkmethod;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.*;
import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.module.kb.controller.admin.chunkmethod.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.chunkmethod.ChunkMethodDO;
import cn.iocoder.yudao.module.kb.service.chunkmethod.ChunkMethodService;

@Tag(name = "管理后台 - 切片方法")
@RestController
@RequestMapping("/kb/chunk-method")
@Validated
public class ChunkMethodController {

    @Resource
    private ChunkMethodService chunkMethodService;

    @PostMapping("/create")
    @Operation(summary = "创建切片方法")
    @PreAuthorize("@ss.hasPermission('kb:chunk-method:create')")
    public CommonResult<Long> createChunkMethod(@Valid @RequestBody ChunkMethodSaveReqVO createReqVO) {
        return success(chunkMethodService.createChunkMethod(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新切片方法")
    @PreAuthorize("@ss.hasPermission('kb:chunk-method:update')")
    public CommonResult<Boolean> updateChunkMethod(@Valid @RequestBody ChunkMethodSaveReqVO updateReqVO) {
        chunkMethodService.updateChunkMethod(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除切片方法")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('kb:chunk-method:delete')")
    public CommonResult<Boolean> deleteChunkMethod(@RequestParam("id") Long id) {
        chunkMethodService.deleteChunkMethod(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得切片方法")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('kb:chunk-method:query')")
    public CommonResult<ChunkMethodRespVO> getChunkMethod(@RequestParam("id") Long id) {
        ChunkMethodDO method = chunkMethodService.getChunkMethod(id);
        return success(BeanUtils.toBean(method, ChunkMethodRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得切片方法分页")
    @PreAuthorize("@ss.hasPermission('kb:chunk-method:query')")
    public CommonResult<PageResult<ChunkMethodRespVO>> getChunkMethodPage(@Valid ChunkMethodPageReqVO pageReqVO) {
        PageResult<ChunkMethodDO> pageResult = chunkMethodService.getChunkMethodPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ChunkMethodRespVO.class));
    }

    @PostMapping("/test")
    @Operation(summary = "测试切片方法")
    @PreAuthorize("@ss.hasPermission('kb:chunk-method:test')")
    public CommonResult<ChunkMethodTestRespVO> testChunkMethod(@Valid @RequestBody ChunkMethodTestReqVO reqVO) {
        return success(chunkMethodService.testChunkMethod(reqVO));
    }

    @PostMapping("/set-default")
    @Operation(summary = "设置默认切片方法")
    @PreAuthorize("@ss.hasPermission('kb:chunk-method:update')")
    public CommonResult<Boolean> setDefaultChunkMethod(@RequestParam("id") Long id) {
        chunkMethodService.setDefaultChunkMethod(id);
        return success(true);
    }

    @PostMapping("/batch-activate")
    @Operation(summary = "批量激活/停用切片方法")
    @PreAuthorize("@ss.hasPermission('kb:chunk-method:update')")
    public CommonResult<Integer> batchActivate(@RequestParam("isActive") Boolean isActive,
                                                @RequestBody List<Long> ids) {
        return success(chunkMethodService.batchActivate(ids, isActive));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得激活的切片方法精简列表（用于下拉选择）")
    @PreAuthorize("@ss.hasPermission('kb:chunk-method:query')")
    public CommonResult<List<ChunkMethodSimpleVO>> getSimpleList() {
        return success(chunkMethodService.getSimpleChunkMethodList());
    }

}
