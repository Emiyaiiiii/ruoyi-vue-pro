package cn.iocoder.yudao.module.agent.controller.admin.chatsession;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatMessagePageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatSendReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatSendRespVO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatSessionPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatSessionRespVO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatSessionSaveReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatSessionUpdateTitleReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.chatmessage.AiChatMessageDO;
import cn.iocoder.yudao.module.agent.dal.dataobject.chatsession.AiChatSessionDO;
import cn.iocoder.yudao.module.agent.service.chatsession.AiChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 问答会话
 *
 * @author 吴皓
 */
@Tag(name = "管理后台 - 问答会话")
@RestController
@RequestMapping("/ai-agent/chat-session")
@Validated
public class ChatSessionController {

    @Resource
    private AiChatSessionService chatSessionService;

    @PostMapping("/create")
    @Operation(summary = "创建会话")
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:create')")
    public CommonResult<Long> createSession(@Valid @RequestBody ChatSessionSaveReqVO createReqVO) {
        // 未指定用户时默认归属当前登录用户
        if (createReqVO.getUserId() == null) {
            createReqVO.setUserId(SecurityFrameworkUtils.getLoginUserId());
        }
        return success(chatSessionService.createSession(
                createReqVO.getAgentId(), createReqVO.getUserId(), createReqVO.getTitle()));
    }

    @GetMapping("/get")
    @Operation(summary = "获得会话")
    @Parameter(name = "id", description = "会话ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public CommonResult<ChatSessionRespVO> getSession(@RequestParam("id") Long id) {
        AiChatSessionDO session = chatSessionService.getSession(id);
        return success(BeanUtils.toBean(session, ChatSessionRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得会话分页")
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public CommonResult<PageResult<ChatSessionRespVO>> getSessionPage(@Valid ChatSessionPageReqVO pageReqVO) {
        return success(chatSessionService.getSessionPage(pageReqVO));
    }

    @PutMapping("/close")
    @Operation(summary = "关闭会话")
    @Parameter(name = "id", description = "会话ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:update')")
    public CommonResult<Boolean> closeSession(@RequestParam("id") Long id) {
        chatSessionService.closeSession(id);
        return success(true);
    }

    @PutMapping("/rename")
    @Operation(summary = "重命名会话")
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:update')")
    public CommonResult<Boolean> renameSession(@Valid @RequestBody ChatSessionUpdateTitleReqVO reqVO) {
        chatSessionService.updateTitle(reqVO.getId(), reqVO.getTitle());
        return success(true);
    }

    @DeleteMapping("/clear-messages")
    @Operation(summary = "清空会话消息（同时重置 QwenPaw 侧会话上下文）")
    @Parameter(name = "id", description = "会话ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:update')")
    public CommonResult<Boolean> clearMessages(@RequestParam("id") Long id) {
        chatSessionService.clearMessages(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会话")
    @Parameter(name = "id", description = "会话ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:delete')")
    public CommonResult<Boolean> deleteSession(@RequestParam("id") Long id) {
        chatSessionService.deleteSession(id);
        return success(true);
    }

    @PostMapping("/send")
    @Operation(summary = "发送消息（同步对话）")
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:update')")
    public CommonResult<ChatSendRespVO> sendMessage(@RequestParam("sessionId") Long sessionId,
                                                    @Valid @RequestBody ChatSendReqVO reqVO) {
        return success(chatSessionService.sendMessage(sessionId, reqVO));
    }

    @PostMapping(value = "/send-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送消息（SSE 流式对话）",
            description = "事件协议：message（data 为增量文本块）→ done（data 为 JSON {\"content\":\"完整回答\",\"sessionId\":\"...\"}）→ error（data 为 JSON {\"message\":\"错误信息\"}）")
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:update')")
    public SseEmitter sendMessageStream(@RequestParam("sessionId") Long sessionId,
                                        @Valid @RequestBody ChatSendReqVO reqVO) {
        return chatSessionService.sendMessageStream(sessionId, reqVO);
    }

    @PostMapping("/stop-stream")
    @Operation(summary = "停止当前会话正在进行的对话",
            description = "转发 QwenPaw chat/stop，返回 {\"stopped\": bool}")
    @Parameter(name = "sessionId", description = "会话ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:update')")
    public CommonResult<Map<String, Object>> stopStream(@RequestParam("sessionId") Long sessionId) {
        return success(chatSessionService.stopStream(sessionId));
    }

    @GetMapping("/message-list")
    @Operation(summary = "获得会话历史消息")
    @Parameter(name = "sessionId", description = "会话ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public CommonResult<List<AiChatMessageDO>> getMessageList(@RequestParam("sessionId") Long sessionId) {
        return success(chatSessionService.getMessageList(sessionId));
    }

    @GetMapping("/message-page")
    @Operation(summary = "获得消息分页")
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public CommonResult<PageResult<AiChatMessageDO>> getMessagePage(@Valid ChatMessagePageReqVO pageReqVO) {
        return success(chatSessionService.getMessagePage(pageReqVO));
    }

}
