package cn.iocoder.yudao.module.agent.controller.admin.tokenusage;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.agent.service.tokenusage.AiTokenUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - Token 用量统计
 *
 * <p>透传 QwenPaw 的全局 token-usage 统计接口（汇总 + 明细）。
 *
 * @author 吴皓
 */
@Tag(name = "管理后台 - Token 用量统计")
@RestController
@RequestMapping("/ai-agent/token-usage")
@Validated
public class TokenUsageController {

    @Resource
    private AiTokenUsageService tokenUsageService;

    @GetMapping("/summary")
    @Operation(summary = "获得 Token 用量汇总")
    @Parameter(name = "startDate", description = "起始日期 YYYY-MM-DD")
    @Parameter(name = "endDate", description = "结束日期 YYYY-MM-DD")
    @Parameter(name = "model", description = "按模型名过滤")
    @Parameter(name = "provider", description = "按 provider 过滤")
    @PreAuthorize("@ss.hasPermission('ai-agent:token-usage:query')")
    public CommonResult<Map<String, Object>> getTokenUsage(@RequestParam(value = "startDate", required = false) String startDate,
                                                           @RequestParam(value = "endDate", required = false) String endDate,
                                                           @RequestParam(value = "model", required = false) String model,
                                                           @RequestParam(value = "provider", required = false) String provider) {
        return success(tokenUsageService.getTokenUsage(startDate, endDate, model, provider));
    }

    @GetMapping("/details")
    @Operation(summary = "获得 Token 用量明细")
    @Parameter(name = "startDate", description = "起始日期 YYYY-MM-DD")
    @Parameter(name = "endDate", description = "结束日期 YYYY-MM-DD")
    @Parameter(name = "model", description = "按模型名过滤")
    @Parameter(name = "provider", description = "按 provider 过滤")
    @PreAuthorize("@ss.hasPermission('ai-agent:token-usage:query')")
    public CommonResult<List<Map<String, Object>>> getTokenUsageDetails(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "provider", required = false) String provider) {
        return success(tokenUsageService.getTokenUsageDetails(startDate, endDate, model, provider));
    }

}
