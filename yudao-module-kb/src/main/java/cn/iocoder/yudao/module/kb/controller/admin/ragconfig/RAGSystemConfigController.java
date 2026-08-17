package cn.iocoder.yudao.module.kb.controller.admin.ragconfig;

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

import cn.iocoder.yudao.module.kb.controller.admin.ragconfig.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.ragconfig.RAGSystemConfigDO;
import cn.iocoder.yudao.module.kb.service.ragconfig.RAGSystemConfigService;

@Tag(name = "管理后台 - RAG配置")
@RestController
@RequestMapping("/kb/rag-config")
@Validated
public class RAGSystemConfigController {

    @Resource
    private RAGSystemConfigService ragSystemConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建RAG配置")
    @PreAuthorize("@ss.hasPermission('kb:rag-config:create')")
    public CommonResult<Long> createRAGConfig(@Valid @RequestBody RAGConfigSaveReqVO createReqVO) {
        return success(ragSystemConfigService.createRAGConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新RAG配置")
    @PreAuthorize("@ss.hasPermission('kb:rag-config:update')")
    public CommonResult<Boolean> updateRAGConfig(@Valid @RequestBody RAGConfigSaveReqVO updateReqVO) {
        ragSystemConfigService.updateRAGConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除RAG配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('kb:rag-config:delete')")
    public CommonResult<Boolean> deleteRAGConfig(@RequestParam("id") Long id) {
        ragSystemConfigService.deleteRAGConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得RAG配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('kb:rag-config:query')")
    public CommonResult<RAGConfigRespVO> getRAGConfig(@RequestParam("id") Long id) {
        RAGSystemConfigDO config = ragSystemConfigService.getRAGConfig(id);
        return success(convertToRespVO(config));
    }

    @GetMapping("/page")
    @Operation(summary = "获得RAG配置分页")
    @PreAuthorize("@ss.hasPermission('kb:rag-config:query')")
    public CommonResult<PageResult<RAGConfigRespVO>> getRAGConfigPage(@Valid RAGConfigPageReqVO pageReqVO) {
        PageResult<RAGSystemConfigDO> pageResult = ragSystemConfigService.getRAGConfigPage(pageReqVO);
        List<RAGConfigRespVO> voList = new ArrayList<>();
        for (RAGSystemConfigDO config : pageResult.getList()) {
            voList.add(convertToRespVO(config));
        }
        PageResult<RAGConfigRespVO> voResult = new PageResult<>();
        voResult.setList(voList);
        voResult.setTotal(pageResult.getTotal());
        return success(voResult);
    }

    @GetMapping("/by-module")
    @Operation(summary = "按模块获取配置键值对")
    @Parameter(name = "module", description = "模块代码", required = true, example = "retrieval")
    @PreAuthorize("@ss.hasPermission('kb:rag-config:query')")
    public CommonResult<Map<String, Object>> getByModule(@RequestParam("module") String module) {
        Map<String, Object> config = ragSystemConfigService.getConfigByModule(module);
        return success(config);
    }

    @PostMapping("/batch-update")
    @Operation(summary = "批量更新配置")
    @PreAuthorize("@ss.hasPermission('kb:rag-config:update')")
    public CommonResult<Map<String, Object>> batchUpdate(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> configs = (List<Map<String, Object>>) body.get("configs");
        if (configs == null || configs.isEmpty()) {
            return success(Collections.emptyMap());
        }
        Map<String, Object> result = ragSystemConfigService.batchUpdate(configs);
        return success(result);
    }

    @PostMapping("/refresh-cache")
    @Operation(summary = "刷新配置缓存")
    @PreAuthorize("@ss.hasPermission('kb:rag-config:update')")
    public CommonResult<Boolean> refreshCache(@RequestBody(required = false) Map<String, Object> body) {
        String module = body != null ? (String) body.get("module") : null;
        String key = body != null ? (String) body.get("key") : null;
        ragSystemConfigService.refreshCache(module, key);
        return success(true);
    }

    @GetMapping("/modules")
    @Operation(summary = "获取模块列表及计数")
    @PreAuthorize("@ss.hasPermission('kb:rag-config:query')")
    public CommonResult<List<Map<String, Object>>> getModules() {
        return success(ragSystemConfigService.getModules());
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取统计信息")
    @PreAuthorize("@ss.hasPermission('kb:rag-config:query')")
    public CommonResult<Map<String, Object>> getStatistics() {
        return success(ragSystemConfigService.getStatistics());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将 DO 转换为 RespVO，补充显示名和typedValue
     */
    private RAGConfigRespVO convertToRespVO(RAGSystemConfigDO config) {
        RAGConfigRespVO vo = BeanUtils.toBean(config, RAGConfigRespVO.class);
        // 设置显示名
        vo.setModuleDisplay(RAGSystemConfigDO.MODULE_DISPLAY_MAP.getOrDefault(config.getModule(), config.getModule()));
        vo.setValueTypeDisplay(RAGSystemConfigDO.VALUE_TYPE_DISPLAY_MAP.getOrDefault(config.getValueType(), config.getValueType()));
        // 传递已计算的 typedValue
        vo.setTypedValue(config.getTypedValue());
        return vo;
    }
}
