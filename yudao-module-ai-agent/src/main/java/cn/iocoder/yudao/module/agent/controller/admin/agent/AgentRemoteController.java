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
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 智能体远程能力（透传 QwenPaw）
 *
 * <p>覆盖 agent 运行状态、QwenPaw 侧实际注册的 MCP / Skills 等只读或启停能力。
 *
 * @author 吴皓
 */
@Tag(name = "管理后台 - 智能体远程能力")
@RestController
@RequestMapping("/ai-agent/agent-remote")
@Validated
public class AgentRemoteController {

    @Resource
    private AiAgentService agentService;

    @GetMapping("/status")
    @Operation(summary = "获得智能体运行状态")
    @Parameter(name = "agentId", description = "智能体ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:query')")
    public CommonResult<Map<String, Object>> getStatus(@RequestParam("agentId") Long agentId) {
        return success(agentService.getAgentStatus(agentId));
    }

    @GetMapping("/mcp/list")
    @Operation(summary = "获得智能体在 QwenPaw 侧注册的 MCP 列表")
    @Parameter(name = "agentId", description = "智能体ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:query')")
    public CommonResult<List<Map<String, Object>>> listMcps(@RequestParam("agentId") Long agentId) {
        return success(agentService.listAgentQwenpawMcps(agentId));
    }

    @PutMapping("/mcp/toggle")
    @Operation(summary = "切换智能体在 QwenPaw 侧 MCP 启用状态")
    @Parameter(name = "agentId", description = "智能体ID", required = true)
    @Parameter(name = "clientKey", description = "MCP client key", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:update')")
    public CommonResult<Map<String, Object>> toggleMcp(@RequestParam("agentId") Long agentId,
                                                       @RequestParam("clientKey") String clientKey) {
        return success(agentService.toggleAgentQwenpawMcp(agentId, clientKey));
    }

    @GetMapping("/mcp/tools")
    @Operation(summary = "获得智能体某个 MCP 的工具列表")
    @Parameter(name = "agentId", description = "智能体ID", required = true)
    @Parameter(name = "clientKey", description = "MCP client key", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:query')")
    public CommonResult<List<Map<String, Object>>> listMcpTools(@RequestParam("agentId") Long agentId,
                                                                @RequestParam("clientKey") String clientKey) {
        return success(agentService.listAgentMcpTools(agentId, clientKey));
    }

    @GetMapping("/skills")
    @Operation(summary = "获得智能体在 QwenPaw 侧安装的 Skills 列表")
    @Parameter(name = "agentId", description = "智能体ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:query')")
    public CommonResult<List<Map<String, Object>>> listSkills(@RequestParam("agentId") Long agentId) {
        return success(agentService.listAgentQwenpawSkills(agentId));
    }

    @GetMapping("/skill-pool")
    @Operation(summary = "列出 QwenPaw 全局技能池（内置 + 自定义技能）")
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:query')")
    public CommonResult<List<Map<String, Object>>> listSkillPool() {
        return success(agentService.listQwenpawSkillPool());
    }

    @GetMapping("/skill-pool/{skillName}")
    @Operation(summary = "获得技能池中某个技能的详情（含 YAML 定义）")
    @Parameter(name = "skillName", description = "技能名称", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:query')")
    public CommonResult<Map<String, Object>> getSkillPoolDetail(@PathVariable("skillName") String skillName) {
        return success(agentService.getQwenpawSkillPoolDetail(skillName));
    }

    @PostMapping("/skill/install")
    @Operation(summary = "从技能池安装技能到智能体")
    @Parameter(name = "agentId", description = "智能体ID", required = true)
    @Parameter(name = "skillName", description = "技能名称", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:update')")
    public CommonResult<Boolean> installSkill(@RequestParam("agentId") Long agentId,
                                              @RequestParam("skillName") String skillName) {
        agentService.installQwenpawSkill(agentId, skillName);
        return success(true);
    }

    @DeleteMapping("/skill")
    @Operation(summary = "卸载智能体上安装的技能")
    @Parameter(name = "agentId", description = "智能体ID", required = true)
    @Parameter(name = "skillName", description = "技能名称", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:update')")
    public CommonResult<Boolean> uninstallSkill(@RequestParam("agentId") Long agentId,
                                                @RequestParam("skillName") String skillName) {
        agentService.uninstallQwenpawSkill(agentId, skillName);
        return success(true);
    }

    @PostMapping("/mcp/register")
    @Operation(summary = "注册 MCP 到智能体（QwenPaw 侧）")
    @Parameter(name = "agentId", description = "智能体ID", required = true)
    @Parameter(name = "clientKey", description = "MCP client key", required = true)
    @Parameter(name = "transport", description = "传输方式：streamable-http / stdio")
    @Parameter(name = "url", description = "远程地址（streamable-http 必填）")
    @Parameter(name = "command", description = "stdio 启动命令")
    @Parameter(name = "commandArgs", description = "stdio 启动参数（JSON 数组字符串）")
    @Parameter(name = "headersJson", description = "请求头（JSON 对象字符串）")
    @Parameter(name = "toolsJson", description = "工具白名单（JSON 数组字符串，空为全部）")
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:update')")
    public CommonResult<Map<String, Object>> registerMcp(@RequestParam("agentId") Long agentId,
                                                         @RequestParam("clientKey") String clientKey,
                                                         @RequestParam(value = "transport", required = false) String transport,
                                                         @RequestParam(value = "url", required = false) String url,
                                                         @RequestParam(value = "command", required = false) String command,
                                                         @RequestParam(value = "commandArgs", required = false) String commandArgs,
                                                         @RequestParam(value = "headersJson", required = false) String headersJson,
                                                         @RequestParam(value = "toolsJson", required = false) String toolsJson) {
        return success(agentService.registerQwenpawMcp(agentId, clientKey, transport, url,
                command, commandArgs, headersJson, toolsJson));
    }

    @DeleteMapping("/mcp")
    @Operation(summary = "删除智能体上的 MCP 注册（QwenPaw 侧）")
    @Parameter(name = "agentId", description = "智能体ID", required = true)
    @Parameter(name = "clientKey", description = "MCP client key", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:update')")
    public CommonResult<Boolean> deleteMcp(@RequestParam("agentId") Long agentId,
                                           @RequestParam("clientKey") String clientKey) {
        agentService.deleteQwenpawMcp(agentId, clientKey);
        return success(true);
    }

}
