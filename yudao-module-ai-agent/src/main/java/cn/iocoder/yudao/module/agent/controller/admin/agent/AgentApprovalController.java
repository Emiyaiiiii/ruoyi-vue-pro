package cn.iocoder.yudao.module.agent.controller.admin.agent;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.agent.controller.admin.agent.vo.ApprovalActionReqVO;
import cn.iocoder.yudao.module.agent.framework.config.QwenPawClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 工具审批代理
 *
 * <p>透传 QwenPaw 的审批接口（list/approve/deny），供前端在对话页弹审批框使用。
 * session_id 与对话时透传给 QwenPaw 的会话ID一致。
 *
 * @author 吴皓
 */
@Tag(name = "管理后台 - 工具审批代理")
@RestController
@RequestMapping("/ai-agent/agent/approval")
@Validated
public class AgentApprovalController {

    @Resource
    private QwenPawClient qwenPawClient;

    @GetMapping("/list")
    @Operation(summary = "查询会话下待审批的工具调用")
    @Parameter(name = "sessionId", description = "会话ID（与对话时一致）", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:query')")
    public CommonResult<List<Map<String, Object>>> list(@RequestParam("sessionId") String sessionId) {
        return success(qwenPawClient.listApprovals(sessionId));
    }

    @PostMapping("/approve")
    @Operation(summary = "允许某个待审批的工具调用")
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:update')")
    public CommonResult<Map<String, Object>> approve(@Valid @RequestBody ApprovalActionReqVO reqVO) {
        return success(qwenPawClient.approveApproval(reqVO.getRequestId(), reqVO.getSessionId(), reqVO.getScope()));
    }

    @PostMapping("/deny")
    @Operation(summary = "拒绝某个待审批的工具调用")
    @PreAuthorize("@ss.hasPermission('ai-agent:agent:update')")
    public CommonResult<Map<String, Object>> deny(@Valid @RequestBody ApprovalActionReqVO reqVO) {
        return success(qwenPawClient.denyApproval(reqVO.getRequestId(), reqVO.getSessionId(), reqVO.getReason()));
    }

}