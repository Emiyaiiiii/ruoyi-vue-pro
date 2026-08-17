package cn.iocoder.yudao.module.agent.controller.admin.agent;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.agent.service.agent.AiAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 智能体内置工具
 *
 * <p>透传 QwenPaw 的 per-agent 内置工具开关与配置能力。
 *
 * @author 吴皓
 */
@Tag(name = "管理后台 - 智能体内置工具")
@RestController
@RequestMapping("/ai-agent/agent-tool")
@Validated
public class AgentToolController {

    @Resource
    private AiAgentService agentService;

    @GetMapping("/list")
    @Operation(summary = "获得智能体内置工具列表")
    @Parameter(name = "agentId", description = "智能体ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:query')")
    public CommonResult<List<Map<String, Object>>> listTools(@RequestParam("agentId") Long agentId) {
        return success(agentService.listAgentTools(agentId));
    }

    @PutMapping("/toggle")
    @Operation(summary = "切换内置工具启用状态")
    @Parameter(name = "agentId", description = "智能体ID", required = true)
    @Parameter(name = "toolName", description = "工具名称", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:update')")
    public CommonResult<Map<String, Object>> toggleTool(@RequestParam("agentId") Long agentId,
                                                        @RequestParam("toolName") String toolName) {
        return success(agentService.toggleAgentTool(agentId, toolName));
    }

    @GetMapping("/config")
    @Operation(summary = "获得内置工具配置")
    @Parameter(name = "agentId", description = "智能体ID", required = true)
    @Parameter(name = "toolName", description = "工具名称", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:query')")
    public CommonResult<Map<String, Object>> getConfig(@RequestParam("agentId") Long agentId,
                                                       @RequestParam("toolName") String toolName) {
        return success(agentService.getAgentToolConfig(agentId, toolName));
    }

    @PostMapping("/config")
    @Operation(summary = "更新内置工具配置")
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:update')")
    public CommonResult<Map<String, Object>> updateConfig(@RequestBody AgentToolConfigReqVO reqVO) {
        return success(agentService.updateAgentToolConfig(reqVO.getAgentId(), reqVO.getToolName(), reqVO.getConfig()));
    }

    /**
     * 工具配置更新 Req VO（内联定义，字段少）
     */
    public static class AgentToolConfigReqVO {

        @NotNull(message = "智能体ID不能为空")
        private Long agentId;

        @NotEmpty(message = "工具名称不能为空")
        private String toolName;

        private Map<String, Object> config;

        public Long getAgentId() {
            return agentId;
        }

        public void setAgentId(Long agentId) {
            this.agentId = agentId;
        }

        public String getToolName() {
            return toolName;
        }

        public void setToolName(String toolName) {
            this.toolName = toolName;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }
    }

}
