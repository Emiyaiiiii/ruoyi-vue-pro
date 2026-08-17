package cn.iocoder.yudao.module.kb.controller.admin.levelconfig;

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

import cn.iocoder.yudao.module.kb.controller.admin.levelconfig.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.service.levelconfig.LevelConfigService;

@Tag(name = "管理后台 - 知识库层级配置")
@RestController
@RequestMapping("/kb/level-config")
@Validated
public class LevelConfigController {

    @Resource
    private LevelConfigService levelConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建知识库层级配置")
    @PreAuthorize("@ss.hasPermission('kb:level-config:create')")
    public CommonResult<Long> createLevelConfig(@Valid @RequestBody LevelConfigSaveReqVO createReqVO) {
        return success(levelConfigService.createLevelConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库层级配置")
    @PreAuthorize("@ss.hasPermission('kb:level-config:update')")
    public CommonResult<Boolean> updateLevelConfig(@Valid @RequestBody LevelConfigSaveReqVO updateReqVO) {
        levelConfigService.updateLevelConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库层级配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('kb:level-config:delete')")
    public CommonResult<Boolean> deleteLevelConfig(@RequestParam("id") Long id) {
        levelConfigService.deleteLevelConfig(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除知识库层级配置")
                @PreAuthorize("@ss.hasPermission('kb:level-config:delete')")
    public CommonResult<Boolean> deleteLevelConfigList(@RequestParam("ids") List<Long> ids) {
        levelConfigService.deleteLevelConfigListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得知识库层级配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('kb:level-config:query')")
    public CommonResult<LevelConfigRespVO> getLevelConfig(@RequestParam("id") Long id) {
        LevelConfigDO levelConfig = levelConfigService.getLevelConfig(id);
        return success(BeanUtils.toBean(levelConfig, LevelConfigRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得知识库层级配置分页")
    @PreAuthorize("@ss.hasPermission('kb:level-config:query')")
    public CommonResult<PageResult<LevelConfigRespVO>> getLevelConfigPage(@Valid LevelConfigPageReqVO pageReqVO) {
        PageResult<LevelConfigDO> pageResult = levelConfigService.getLevelConfigPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, LevelConfigRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得知识库层级配置精简列表（用于下拉选择）")
    public CommonResult<List<LevelConfigSimpleVO>> getSimpleLevelConfigList() {
        List<LevelConfigDO> list = levelConfigService.getSimpleLevelConfigList();
        return success(BeanUtils.toBean(list, LevelConfigSimpleVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出知识库层级配置 Excel")
    @PreAuthorize("@ss.hasPermission('kb:level-config:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportLevelConfigExcel(@Valid LevelConfigPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<LevelConfigDO> list = levelConfigService.getLevelConfigPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "知识库层级配置.xls", "数据", LevelConfigRespVO.class,
                        BeanUtils.toBean(list, LevelConfigRespVO.class));
    }

}