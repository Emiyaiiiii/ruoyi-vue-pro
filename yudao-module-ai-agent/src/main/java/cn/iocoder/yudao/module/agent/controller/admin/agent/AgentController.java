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
import cn.iocoder.yudao.module.agent.enums.AiMetaTypeEnum;
import cn.iocoder.yudao.module.agent.service.agent.AiAgentService;
import cn.iocoder.yudao.module.agent.service.follow.AiMetaFollowService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Resource
    private AiMetaFollowService metaFollowService;

    @Resource
    private AdminUserApi adminUserApi;

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
        return success(toRespList(list));
    }

    @PostMapping("/{id}/follow")
    @Operation(summary = "关注智能体（返回最新关注数，便于前端实时刷新）")
    @Parameter(name = "id", description = "智能体ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:create')")
    public CommonResult<Long> followAgent(@PathVariable("id") Long id) {
        // 公开智能体允许被其他用户关注；校验存在性 + 公开/本人/超管
        agentService.validateFollowable(id);
        metaFollowService.follow(AiMetaTypeEnum.AGENT.getType(), id, SecurityFrameworkUtils.getLoginUserId());
        return success(getFollowCount(id));
    }

    @GetMapping("/visible-list")
    @Operation(summary = "获得当前用户可见的智能体列表（水利智囊用：公开 + 自己的）")
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:query')")
    public CommonResult<List<AgentRespVO>> getVisibleAgentList() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        List<AiAgentDO> list = agentService.getVisibleAgentList(userId);
        return success(toRespList(list));
    }

    @DeleteMapping("/{id}/follow")
    @Operation(summary = "取消关注智能体（返回最新关注数）")
    @Parameter(name = "id", description = "智能体ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:delete')")
    public CommonResult<Long> unfollowAgent(@PathVariable("id") Long id) {
        metaFollowService.unfollow(AiMetaTypeEnum.AGENT.getType(), id, SecurityFrameworkUtils.getLoginUserId());
        return success(getFollowCount(id));
    }

    /** 查询某智能体最新关注数 */
    private Long getFollowCount(Long id) {
        Map<Long, Long> countMap = metaFollowService.getFollowCountMap(
                AiMetaTypeEnum.AGENT.getType(), Collections.singleton(id));
        return countMap.getOrDefault(id, 0L);
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

    /**
     * DO → VO，并批量补充创建者昵称（ownerName）、是否本人（isOwner）、
     * 关注数（followersCount）与当前用户是否已关注（isFollowed）
     */
    private List<AgentRespVO> toRespList(List<AiAgentDO> list) {
        List<AgentRespVO> vos = BeanUtils.toBean(list, AgentRespVO.class);
        if (vos.isEmpty()) {
            return vos;
        }
        List<Long> ids = list.stream().map(AiAgentDO::getId).collect(Collectors.toList());
        Map<Long, Long> countMap = metaFollowService.getFollowCountMap(AiMetaTypeEnum.AGENT.getType(), ids);
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        Set<Long> followedIds = userId == null ? Collections.emptySet()
                : new HashSet<>(metaFollowService.getFollowedIds(AiMetaTypeEnum.AGENT.getType(), userId));
        // 批量解析创建者昵称/用户名，避免 N+1
        Set<Long> ownerIds = list.stream().map(AiAgentDO::getUserId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> ownerNameMap = new HashMap<>();
        for (Long ownerId : ownerIds) {
            try {
                AdminUserRespDTO user = adminUserApi.getUser(ownerId);
                if (user != null) {
                    ownerNameMap.put(ownerId, user.getNickname());
                }
            } catch (Exception ignore) {
                // 单个用户查询失败不影响整体列表
            }
        }
        for (int i = 0; i < list.size(); i++) {
            AiAgentDO agent = list.get(i);
            Long id = agent.getId();
            vos.get(i).setFollowersCount(countMap.getOrDefault(id, 0L));
            vos.get(i).setIsFollowed(followedIds.contains(id));
            vos.get(i).setOwnerName(ownerNameMap.get(agent.getUserId()));
            vos.get(i).setIsOwner(userId != null && userId.equals(agent.getUserId()));
        }
        return vos;
    }

}
