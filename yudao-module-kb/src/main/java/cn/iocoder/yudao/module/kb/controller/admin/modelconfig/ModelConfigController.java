package cn.iocoder.yudao.module.kb.controller.admin.modelconfig;

import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

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

import cn.iocoder.yudao.module.kb.controller.admin.modelconfig.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.modelconfig.ModelConfigDO;
import cn.iocoder.yudao.module.kb.service.modelconfig.ModelConfigService;

@Tag(name = "管理后台 - 大模型配置")
@RestController
@RequestMapping("/kb/model-config")
@Validated
public class ModelConfigController {

    @Resource
    private ModelConfigService modelConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建大模型配置")
    @PreAuthorize("@ss.hasPermission('kb:model-config:create')")
    public CommonResult<Long> createModelConfig(@Valid @RequestBody ModelConfigSaveReqVO createReqVO) {
        return success(modelConfigService.createModelConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新大模型配置")
    @PreAuthorize("@ss.hasPermission('kb:model-config:update')")
    public CommonResult<Boolean> updateModelConfig(@Valid @RequestBody ModelConfigSaveReqVO updateReqVO) {
        modelConfigService.updateModelConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除大模型配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('kb:model-config:delete')")
    public CommonResult<Boolean> deleteModelConfig(@RequestParam("id") Long id) {
        modelConfigService.deleteModelConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得大模型配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('kb:model-config:query')")
    public CommonResult<ModelConfigRespVO> getModelConfig(@RequestParam("id") Long id) {
        ModelConfigDO config = modelConfigService.getModelConfig(id);
        return success(BeanUtils.toBean(config, ModelConfigRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得大模型配置分页")
    @PreAuthorize("@ss.hasPermission('kb:model-config:query')")
    public CommonResult<PageResult<ModelConfigRespVO>> getModelConfigPage(@Valid ModelConfigPageReqVO pageReqVO) {
        PageResult<ModelConfigDO> pageResult = modelConfigService.getModelConfigPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ModelConfigRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出大模型配置 Excel")
    @PreAuthorize("@ss.hasPermission('kb:model-config:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportModelConfigExcel(@Valid ModelConfigPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ModelConfigDO> list = modelConfigService.getModelConfigPage(pageReqVO).getList();
        ExcelUtils.write(response, "大模型配置.xls", "数据", ModelConfigRespVO.class,
                        BeanUtils.toBean(list, ModelConfigRespVO.class));
    }

    @PostMapping("/activate")
    @Operation(summary = "激活大模型配置")
    @PreAuthorize("@ss.hasPermission('kb:model-config:update')")
    public CommonResult<Boolean> activateModelConfig(@RequestParam("id") Long id) {
        modelConfigService.activateModelConfig(id);
        return success(true);
    }

    @PostMapping("/deactivate")
    @Operation(summary = "停用大模型配置")
    @PreAuthorize("@ss.hasPermission('kb:model-config:update')")
    public CommonResult<Boolean> deactivateModelConfig(@RequestParam("id") Long id) {
        modelConfigService.deactivateModelConfig(id);
        return success(true);
    }

    @PostMapping("/test")
    @Operation(summary = "测试大模型配置连接")
    @PreAuthorize("@ss.hasPermission('kb:model-config:test')")
    public CommonResult<ModelConfigTestRespVO> testModelConfig(@Valid @RequestBody ModelConfigTestReqVO reqVO) {
        return success(modelConfigService.testModelConfig(reqVO));
    }

    @PostMapping("/copy")
    @Operation(summary = "复制大模型配置")
    @PreAuthorize("@ss.hasPermission('kb:model-config:copy')")
    public CommonResult<Long> copyModelConfig(@Valid @RequestBody ModelConfigCopyReqVO reqVO) {
        return success(modelConfigService.copyModelConfig(reqVO));
    }

    @PostMapping("/set-default")
    @Operation(summary = "设置默认大模型配置")
    @PreAuthorize("@ss.hasPermission('kb:model-config:update')")
    public CommonResult<Boolean> setDefaultModelConfig(@RequestParam("id") Long id) {
        modelConfigService.setDefaultModelConfig(id);
        return success(true);
    }

    @PostMapping("/batch")
    @Operation(summary = "批量操作大模型配置（激活/停用/删除）")
    @PreAuthorize("@ss.hasPermission('kb:model-config:batch')")
    public CommonResult<Integer> batchOperation(@Valid @RequestBody ModelConfigBatchReqVO reqVO) {
        return success(modelConfigService.batchOperation(reqVO));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获得大模型配置统计信息")
    @PreAuthorize("@ss.hasPermission('kb:model-config:query')")
    public CommonResult<ModelConfigStatisticsRespVO> getStatistics() {
        return success(modelConfigService.getStatistics());
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得激活的大模型配置精简列表（用于下拉选择）")
    @PreAuthorize("@ss.hasPermission('kb:model-config:query')")
    public CommonResult<List<ModelConfigSimpleVO>> getSimpleList() {
        return success(modelConfigService.getSimpleModelConfigList());
    }

}
