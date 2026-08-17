package cn.iocoder.yudao.module.agent.controller.admin.model;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.agent.controller.admin.model.vo.*;
import cn.iocoder.yudao.module.agent.service.model.AiModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 模型管理
 *
 * @author 吴皓
 */
@Tag(name = "管理后台 - 模型管理")
@RestController
@RequestMapping("/ai-agent/model")
@Validated
public class ModelController {

    @Resource
    private AiModelService modelService;

    @GetMapping("/provider/list")
    @Operation(summary = "列出所有 Provider（含模型列表）")
    @PreAuthorize("@ss.hasPermission('ai-agent:model:query')")
    public CommonResult<List<ProviderRespVO>> listProviders() {
        return success(modelService.listProviders());
    }

    @PutMapping("/provider/configure")
    @Operation(summary = "配置 Provider（API key、base_url 等）")
    @PreAuthorize("@ss.hasPermission('ai-agent:model:update')")
    public CommonResult<ProviderRespVO> configureProvider(@Valid @RequestBody ProviderConfigReqVO reqVO) {
        return success(modelService.configureProvider(reqVO));
    }

    @PostMapping("/provider/test")
    @Operation(summary = "测试 Provider 连接")
    @PreAuthorize("@ss.hasPermission('ai-agent:model:query')")
    @Parameter(name = "providerId", description = "Provider ID", required = true)
    public CommonResult<TestConnectionRespVO> testProvider(@RequestParam("providerId") String providerId) {
        return success(modelService.testProvider(providerId));
    }

    @PostMapping("/model/test")
    @Operation(summary = "测试特定模型连接")
    @PreAuthorize("@ss.hasPermission('ai-agent:model:query')")
    public CommonResult<TestConnectionRespVO> testModel(@RequestParam("providerId") String providerId,
                                                       @RequestParam("modelId") String modelId) {
        return success(modelService.testModel(providerId, modelId));
    }

    @PostMapping("/provider/discover")
    @Operation(summary = "从 Provider 发现可用模型")
    @PreAuthorize("@ss.hasPermission('ai-agent:model:query')")
    @Parameter(name = "providerId", description = "Provider ID", required = true)
    public CommonResult<List<ModelInfoVO>> discoverModels(@RequestParam("providerId") String providerId) {
        return success(modelService.discoverModels(providerId));
    }

    @PostMapping("/add")
    @Operation(summary = "向 Provider 添加模型")
    @PreAuthorize("@ss.hasPermission('ai-agent:model:create')")
    public CommonResult<ProviderRespVO> addModel(@Valid @RequestBody AddModelReqVO reqVO) {
        return success(modelService.addModel(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "从 Provider 删除模型")
    @PreAuthorize("@ss.hasPermission('ai-agent:model:delete')")
    public CommonResult<Boolean> deleteModel(@RequestParam("providerId") String providerId,
                                             @RequestParam("modelId") String modelId) {
        modelService.deleteModel(providerId, modelId);
        return success(true);
    }

    @PutMapping("/configure")
    @Operation(summary = "配置模型参数")
    @PreAuthorize("@ss.hasPermission('ai-agent:model:update')")
    public CommonResult<ProviderRespVO> configureModel(@Valid @RequestBody ModelConfigReqVO reqVO) {
        return success(modelService.configureModel(reqVO));
    }

    @GetMapping("/active")
    @Operation(summary = "获取当前激活模型")
    @PreAuthorize("@ss.hasPermission('ai-agent:model:query')")
    public CommonResult<ActiveModelRespVO> getActiveModel(
            @RequestParam(value = "scope", defaultValue = "effective") String scope,
            @RequestParam(value = "agentId", required = false) String agentId) {
        return success(modelService.getActiveModel(scope, agentId));
    }

    @PutMapping("/active")
    @Operation(summary = "设置激活模型")
    @PreAuthorize("@ss.hasPermission('ai-agent:model:update')")
    public CommonResult<ActiveModelRespVO> setActiveModel(@Valid @RequestBody SetActiveModelReqVO reqVO) {
        return success(modelService.setActiveModel(reqVO));
    }

    @PostMapping("/custom-provider")
    @Operation(summary = "创建自定义 Provider")
    @PreAuthorize("@ss.hasPermission('ai-agent:model:create')")
    public CommonResult<ProviderRespVO> createCustomProvider(@RequestBody Map<String, Object> providerInfo) {
        return success(modelService.createCustomProvider(providerInfo));
    }

    @DeleteMapping("/custom-provider")
    @Operation(summary = "删除自定义 Provider")
    @PreAuthorize("@ss.hasPermission('ai-agent:model:delete')")
    @Parameter(name = "providerId", description = "Provider ID", required = true)
    public CommonResult<Boolean> deleteCustomProvider(@RequestParam("providerId") String providerId) {
        modelService.deleteCustomProvider(providerId);
        return success(true);
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有可用模型（扁平列表，供下拉选择）")
    @PreAuthorize("@ss.hasPermission('ai-agent:model:query')")
    public CommonResult<List<Map<String, Object>>> listAllModels() {
        return success(modelService.listAllModels());
    }
}
