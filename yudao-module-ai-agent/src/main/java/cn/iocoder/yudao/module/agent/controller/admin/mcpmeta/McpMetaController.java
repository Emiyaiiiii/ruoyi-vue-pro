package cn.iocoder.yudao.module.agent.controller.admin.mcpmeta;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.agent.controller.admin.mcpmeta.vo.McpMetaPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.mcpmeta.vo.McpMetaRespVO;
import cn.iocoder.yudao.module.agent.controller.admin.mcpmeta.vo.McpMetaSaveReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.mcpmeta.AiMcpMetaDO;
import cn.iocoder.yudao.module.agent.service.mcpmeta.AiMcpMetaService;
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
        return success(BeanUtils.toBean(pageResult, McpMetaRespVO.class));
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
        return success(BeanUtils.toBean(list, McpMetaRespVO.class));
    }

}
