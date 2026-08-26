package cn.iocoder.yudao.module.agent.controller.admin.agent;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.agent.controller.admin.agent.vo.AgentPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.agent.vo.AgentRespVO;
import cn.iocoder.yudao.module.agent.controller.admin.agent.vo.AgentSaveReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.agent.AiAgentDO;
import cn.iocoder.yudao.module.agent.service.agent.AiAgentService;
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
 * 管理后台 - 智能体
 *
 * @author 吴皓
 */
@Tag(name = "管理后台 - 智能体")
@RestController
@RequestMapping("/ai-agent/agent")
@Validated
public class AgentController {

    @Resource
    private AiAgentService agentService;

    @PostMapping("/create")
    @Operation(summary = "创建智能体")
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:create')")
    public CommonResult<Long> createAgent(@Valid @RequestBody AgentSaveReqVO createReqVO) {
        // 未指定用户时默认归属当前登录用户
        if (createReqVO.getUserId() == null) {
            createReqVO.setUserId(SecurityFrameworkUtils.getLoginUserId());
        }
        return success(agentService.createAgent(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新智能体")
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:update')")
    public CommonResult<Boolean> updateAgent(@Valid @RequestBody AgentSaveReqVO updateReqVO) {
        agentService.updateAgent(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除智能体")
    @Parameter(name = "id", description = "智能体ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:delete')")
    public CommonResult<Boolean> deleteAgent(@RequestParam("id") Long id) {
        agentService.deleteAgent(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得智能体")
    @Parameter(name = "id", description = "智能体ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:query')")
    public CommonResult<AgentRespVO> getAgent(@RequestParam("id") Long id) {
        AiAgentDO agent = agentService.getAgent(id);
        return success(BeanUtils.toBean(agent, AgentRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得智能体分页")
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:query')")
    public CommonResult<PageResult<AgentRespVO>> getAgentPage(@Valid AgentPageReqVO pageReqVO) {
        PageResult<AiAgentDO> pageResult = agentService.getAgentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AgentRespVO.class));
    }

    @PutMapping("/toggle")
    @Operation(summary = "启停智能体")
    @Parameter(name = "id", description = "智能体ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:update')")
    public CommonResult<Boolean> toggleAgent(@RequestParam("id") Long id) {
        agentService.toggleAgent(id);
        return success(true);
    }

    @GetMapping("/my")
    @Operation(summary = "获得当前用户的智能体列表")
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:query')")
    public CommonResult<List<AgentRespVO>> getMyAgents() {
        List<AiAgentDO> list = agentService.getMyAgents();
        return success(BeanUtils.toBean(list, AgentRespVO.class));
    }

    @GetMapping("/my-default")
    @Operation(summary = "获得当前用户的默认智能体（不存在则自动创建）")
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:query')")
    public CommonResult<AgentRespVO> getMyDefaultAgent() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return success(BeanUtils.toBean(agentService.getOrCreateDefaultAgent(userId,
                TenantContextHolder.getTenantId()),
                AgentRespVO.class));
    }

    @PutMapping("/set-default")
    @Operation(summary = "设为默认智能体")
    @Parameter(name = "id", description = "智能体ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:update')")
    public CommonResult<Boolean> setDefaultAgent(@RequestParam("id") Long id) {
        agentService.setDefaultAgent(id);
        return success(true);
    }

    @PostMapping("/bootstrap-defaults")
    @Operation(summary = "【按钮触发】为当前租户无智能体的用户批量创建默认智能体，供超管/租户管理员一键兜底")
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:create')")
    public CommonResult<Map<String, Object>> bootstrapDefaultAgents() {
        return success(agentService.bootstrapUserDefaultAgents());
    }

}
