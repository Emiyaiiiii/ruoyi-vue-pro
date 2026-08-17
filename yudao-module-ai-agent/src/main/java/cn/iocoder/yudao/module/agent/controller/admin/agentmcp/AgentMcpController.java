package cn.iocoder.yudao.module.agent.controller.admin.agentmcp;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.agent.controller.admin.agentmcp.vo.AgentMcpPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.agentmcp.vo.AgentMcpRespVO;
import cn.iocoder.yudao.module.agent.controller.admin.agentmcp.vo.AgentMcpSaveReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.agentmcp.AiAgentMcpDO;
import cn.iocoder.yudao.module.agent.service.agentmcp.AiAgentMcpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 智能体-MCP 绑定
 *
 * @author 吴皓
 */
@Tag(name = "管理后台 - 智能体-MCP 绑定")
@RestController
@RequestMapping("/ai-agent/agent-mcp")
@Validated
public class AgentMcpController {

    @Resource
    private AiAgentMcpService agentMcpService;

    @PostMapping("/create")
    @Operation(summary = "创建绑定")
    @PreAuthorize("@ss.hasPermission('ai-agent:agent-mcp:create')")
    public CommonResult<Long> createAgentMcp(@Valid @RequestBody AgentMcpSaveReqVO createReqVO) {
        return success(agentMcpService.createAgentMcp(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新绑定")
    @PreAuthorize("@ss.hasPermission('ai-agent:agent-mcp:update')")
    public CommonResult<Boolean> updateAgentMcp(@Valid @RequestBody AgentMcpSaveReqVO updateReqVO) {
        agentMcpService.updateAgentMcp(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除绑定")
    @Parameter(name = "id", description = "绑定ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent-mcp:delete')")
    public CommonResult<Boolean> deleteAgentMcp(@RequestParam("id") Long id) {
        agentMcpService.deleteAgentMcp(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得绑定")
    @Parameter(name = "id", description = "绑定ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent-mcp:query')")
    public CommonResult<AgentMcpRespVO> getAgentMcp(@RequestParam("id") Long id) {
        AiAgentMcpDO bind = agentMcpService.getAgentMcp(id);
        return success(BeanUtils.toBean(bind, AgentMcpRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得绑定分页")
    @PreAuthorize("@ss.hasPermission('ai-agent:agent-mcp:query')")
    public CommonResult<PageResult<AgentMcpRespVO>> getAgentMcpPage(@Valid AgentMcpPageReqVO pageReqVO) {
        return success(agentMcpService.getAgentMcpPage(pageReqVO));
    }

    @GetMapping("/list")
    @Operation(summary = "获得某智能体的绑定列表")
    @Parameter(name = "agentId", description = "智能体ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent-mcp:query')")
    public CommonResult<List<AgentMcpRespVO>> getAgentMcpList(@RequestParam("agentId") Long agentId) {
        return success(agentMcpService.getAgentMcpListByAgentId(agentId));
    }

    @PutMapping("/toggle")
    @Operation(summary = "启停绑定")
    @Parameter(name = "id", description = "绑定ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent-mcp:update')")
    public CommonResult<Boolean> toggleAgentMcp(@RequestParam("id") Long id) {
        agentMcpService.toggleAgentMcp(id);
        return success(true);
    }

}
