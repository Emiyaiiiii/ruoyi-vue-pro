package cn.iocoder.yudao.module.agent.controller.admin.mcpmeta;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.agent.controller.admin.mcpmeta.vo.McpMetaPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.mcpmeta.vo.McpMetaRespVO;
import cn.iocoder.yudao.module.agent.controller.admin.mcpmeta.vo.McpMetaSaveReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.mcpmeta.AiMcpMetaDO;
import cn.iocoder.yudao.module.agent.enums.AiMetaTypeEnum;
import cn.iocoder.yudao.module.agent.service.follow.AiMetaFollowService;
import cn.iocoder.yudao.module.agent.service.mcpmeta.AiMcpMetaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - MCP 商店
 *
 * @author 吴皓
 */
@Tag(name = "管理后台 - MCP 商店")
@RestController
@RequestMapping("/ai-agent/mcp-meta")
@Validated
public class McpMetaController {

    @Resource
    private AiMcpMetaService mcpMetaService;

    @Resource
    private AiMetaFollowService metaFollowService;

    @PostMapping("/create")
    @Operation(summary = "创建 MCP 商店项")
    @PreAuthorize("@ss.hasPermission('ai-agent:mcp-meta:create')")
    public CommonResult<Long> createMcpMeta(@Valid @RequestBody McpMetaSaveReqVO createReqVO) {
        return success(mcpMetaService.createMcpMeta(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 MCP 商店项")
    @PreAuthorize("@ss.hasPermission('ai-agent:mcp-meta:update')")
    public CommonResult<Boolean> updateMcpMeta(@Valid @RequestBody McpMetaSaveReqVO updateReqVO) {
        mcpMetaService.updateMcpMeta(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 MCP 商店项")
    @Parameter(name = "id", description = "MCP 商店项ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:mcp-meta:delete')")
    public CommonResult<Boolean> deleteMcpMeta(@RequestParam("id") Long id) {
        mcpMetaService.deleteMcpMeta(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 MCP 商店项")
    @Parameter(name = "id", description = "MCP 商店项ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:mcp-meta:query')")
    public CommonResult<McpMetaRespVO> getMcpMeta(@RequestParam("id") Long id) {
        AiMcpMetaDO meta = mcpMetaService.getMcpMeta(id);
        return success(BeanUtils.toBean(meta, McpMetaRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 MCP 商店分页")
    @PreAuthorize("@ss.hasPermission('ai-agent:mcp-meta:query')")
    public CommonResult<PageResult<McpMetaRespVO>> getMcpMetaPage(@Valid McpMetaPageReqVO pageReqVO) {
        PageResult<AiMcpMetaDO> pageResult = mcpMetaService.getMcpMetaPage(pageReqVO);
        return success(new PageResult<>(toRespList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/list-enabled")
    @Operation(summary = "获得启用的 MCP 商店项列表")
    @PreAuthorize("@ss.hasPermission('ai-agent:mcp-meta:query')")
    public CommonResult<List<McpMetaRespVO>> getEnabledMcpMetaList() {
        List<AiMcpMetaDO> list = mcpMetaService.getEnabledMcpMetaList();
        return success(BeanUtils.toBean(list, McpMetaRespVO.class));
    }

    @GetMapping("/visible-list")
    @Operation(summary = "获得当前用户可见的 MCP 商店项列表（公开 + 自己的个人）")
    @PreAuthorize("@ss.hasPermission('ai-agent:mcp-meta:query')")
    public CommonResult<List<McpMetaRespVO>> getVisibleMcpMetaList() {
        List<AiMcpMetaDO> list = mcpMetaService.getVisibleMcpMetaList();
        return success(toRespList(list));
    }

    @GetMapping("/hot")
    @Operation(summary = "获得 MCP 热榜（按关注数倒序取前 N 个）")
    @Parameter(name = "limit", description = "返回条数，默认 10", example = "10")
    @PreAuthorize("@ss.hasPermission('ai-agent:mcp-meta:query')")
    public CommonResult<List<McpMetaRespVO>> getHotMcpMetaList(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        List<McpMetaRespVO> list = toRespList(mcpMetaService.getVisibleMcpMetaList());
        // 关注数倒序，同数按 id 倒序，保证榜单稳定
        list.sort((a, b) -> {
            int byFollow = Long.compare(b.getFollowersCount(), a.getFollowersCount());
            return byFollow != 0 ? byFollow : Long.compare(b.getId(), a.getId());
        });
        int capped = limit == null || limit < 1 ? 10 : Math.min(limit, 50);
        return success(list.size() > capped ? list.subList(0, capped) : list);
    }

    @PostMapping("/{id}/follow")
    @Operation(summary = "关注 MCP")
    @Parameter(name = "id", description = "MCP 商店项ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:mcp-meta:create')")
    public CommonResult<Boolean> followMcpMeta(@PathVariable("id") Long id) {
        mcpMetaService.getMcpMeta(id); // 存在性校验
        metaFollowService.follow(AiMetaTypeEnum.MCP.getType(), id, SecurityFrameworkUtils.getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/{id}/follow")
    @Operation(summary = "取消关注 MCP")
    @Parameter(name = "id", description = "MCP 商店项ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:mcp-meta:delete')")
    public CommonResult<Boolean> unfollowMcpMeta(@PathVariable("id") Long id) {
        metaFollowService.unfollow(AiMetaTypeEnum.MCP.getType(), id, SecurityFrameworkUtils.getLoginUserId());
        return success(true);
    }

    /**
     * DO → VO，并批量补充关注数（followersCount）与当前用户是否已关注（isFollowed）
     */
    private List<McpMetaRespVO> toRespList(List<AiMcpMetaDO> list) {
        List<McpMetaRespVO> vos = BeanUtils.toBean(list, McpMetaRespVO.class);
        if (vos.isEmpty()) {
            return vos;
        }
        List<Long> ids = list.stream().map(AiMcpMetaDO::getId).collect(Collectors.toList());
        Map<Long, Long> countMap = metaFollowService.getFollowCountMap(AiMetaTypeEnum.MCP.getType(), ids);
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        Set<Long> followedIds = userId == null ? Collections.emptySet()
                : new HashSet<>(metaFollowService.getFollowedIds(AiMetaTypeEnum.MCP.getType(), userId));
        for (int i = 0; i < list.size(); i++) {
            Long id = list.get(i).getId();
            vos.get(i).setFollowersCount(countMap.getOrDefault(id, 0L));
            vos.get(i).setIsFollowed(followedIds.contains(id));
        }
        return vos;
    }

}
