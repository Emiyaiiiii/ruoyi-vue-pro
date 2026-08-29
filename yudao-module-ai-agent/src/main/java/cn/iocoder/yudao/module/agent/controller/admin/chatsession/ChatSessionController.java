package cn.iocoder.yudao.module.agent.controller.admin.chatsession;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.agent.service.chatsession.AiChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 问答对话（透传模式）
 *
 * <p>QwenPaw 是会话/消息的 source of truth。Java 端不做任何本地持久化，
 * 所有读写操作都通过 {@link AiChatSessionService} 转发到 QwenPaw。
 *
 * <p>前端调用规范：
 * <ol>
 *   <li>先选智能体（agentId），调用 {@link #listChats(Long)} 拉会话列表</li>
 *   <li>点"新建对话"调用 {@link #createChat} 拿到 chatId</li>
 *   <li>打开历史会话调用 {@link #getChat(Long, String)} 拉 messages</li>
 *   <li>问答调用 {@link #sendMessageStream(Long, String, String)} 流式</li>
 *   <li>切回页面时调用 {@link #reconnectStream(Long, String)} 续推</li>
 * </ol>
 *
 * @author 吴皓
 */
@Tag(name = "管理后台 - 问答对话（透传 QwenPaw）")
@RestController
@RequestMapping("/ai-agent/chat")
@Validated
public class ChatSessionController {

    @Resource
    private AiChatSessionService chatSessionService;

    // ==================== 会话 ====================

    @GetMapping("/list-chats")
    @Operation(summary = "列出指定智能体下的所有 QwenPaw 会话")
    @Parameter(name = "agentId", description = "智能体 ID（芋道 Java 主键）", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public CommonResult<List<Map<String, Object>>> listChats(@RequestParam("agentId") Long agentId) {
        return success(chatSessionService.listChats(agentId));
    }

    @PostMapping("/create-chat")
    @Operation(summary = "在 QwenPaw 预创建一个空会话",
            description = "用户点新建对话时调用，返回 chatId（UUID）作为后续问答的会话标识。")
    @Parameter(name = "agentId", description = "智能体 ID（芋道 Java 主键）", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:create')")
    public CommonResult<Map<String, Object>> createChat(@RequestParam("agentId") Long agentId,
                                                        @RequestParam(value = "name", required = false) String name) {
        return success(chatSessionService.createChat(agentId, name));
    }

    @GetMapping("/get-chat")
    @Operation(summary = "获取 QwenPaw 会话详情（含完整 messages 数组）",
            description = "打开历史会话时调用，data 字段直接是 QwenPaw ChatSpec。")
    @Parameter(name = "agentId", description = "智能体 ID", required = true)
    @Parameter(name = "chatId", description = "QwenPaw 会话 ID（UUID）", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public CommonResult<Map<String, Object>> getChat(@RequestParam("agentId") Long agentId,
                                                     @RequestParam("chatId") String chatId) {
        return success(chatSessionService.getChat(agentId, chatId));
    }

    @DeleteMapping("/delete-chat")
    @Operation(summary = "删除 QwenPaw 会话")
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:delete')")
    public CommonResult<Boolean> deleteChat(@RequestParam("agentId") Long agentId,
                                            @RequestParam("chatId") String chatId) {
        chatSessionService.deleteChat(agentId, chatId);
        return success(true);
    }

    @PutMapping("/rename-chat")
    @Operation(summary = "重命名 QwenPaw 会话")
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:update')")
    public CommonResult<Map<String, Object>> renameChat(@RequestParam("agentId") Long agentId,
                                                        @RequestParam("chatId") String chatId,
                                                        @RequestParam("name") String name) {
        return success(chatSessionService.renameChat(agentId, chatId, name));
    }

    // ==================== 对话 ====================

    @PostMapping(value = "/send-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送消息（SSE 流式对话，透传 QwenPaw）",
            description = """
                    <p>事件协议（Content-Type: text/event-stream，data 字段约定）：
                    <ul>
                      <li><b>message</b>：data 为 QwenPaw 透传的原始事件（object=content/message/reasoning/tool_call 等）</li>
                      <li><b>done</b>：连接正常关闭（前端根据 onclose 事件判断完成）</li>
                      <li><b>error</b>：data 为 JSON {"message":"错误信息"}，表示流异常中断</li>
                    </ul>
                    <p>请求体为 JSON：{"message":"文本", "content":[{"type":"text","text":"..."},{"type":"image","image_url":"..."}]}，
                    content 为可选附件 content 数组（上传文件对话时使用），为空则仅发纯文本消息。
                    <p>本接口不做任何本地持久化、不做流锁；多标签重复请求 QwenPaw 会按 chat_id 自然去重。""")
    @Parameter(name = "agentId", description = "智能体 ID", required = true)
    @Parameter(name = "chatId", description = "QwenPaw 会话 ID（首次可为空，让 QwenPaw 自动开新会话）", required = false)
    @Parameter(name = "sessionId", description = "QwenPaw 会话唯一标识（UUID，新建时生成并缓存，续接时必传）", required = false)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:update')")
    public SseEmitter sendMessageStream(@RequestParam("agentId") Long agentId,
                                        @RequestParam(value = "chatId", required = false) String chatId,
                                        @RequestParam(value = "sessionId", required = false) String sessionId,
                                        @RequestBody(required = false) Map<String, Object> body) {
        String message = body != null && body.get("message") != null ? String.valueOf(body.get("message")) : "";
        List<Map<String, Object>> contentItems = null;
        if (body != null && body.get("content") instanceof List) {
            List<?> content = (List<?>) body.get("content");
            contentItems = new ArrayList<>();
            for (Object item : content) {
                if (item instanceof Map) {
                    contentItems.add((Map<String, Object>) item);
                }
            }
        }
        // 用户本轮勾选的知识库（用于检索范围提示词注入），可为空
        List<Long> kbIds = null;
        if (body != null && body.get("kbIds") instanceof List) {
            List<?> kbIdList = (List<?>) body.get("kbIds");
            kbIds = new ArrayList<>();
            for (Object id : kbIdList) {
                try {
                    kbIds.add(Long.valueOf(String.valueOf(id)));
                } catch (NumberFormatException ignored) {
                    // 非法 id 跳过
                }
            }
        }
        // 会话级免审批：用户在审批框选"本次会话不再审批"后置 true，透传 approval_level=off
        boolean approvalOff = body != null && Boolean.TRUE.equals(body.get("approvalOff"));
        return chatSessionService.sendMessageStream(agentId, chatId, sessionId, message, contentItems, kbIds, approvalOff);
    }

    @GetMapping(value = "/reconnect-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "重新挂载到 QwenPaw 正在运行的流",
            description = """
                    <p>用于「流进行中切走页面，再切回」或「刷新页面」场景。
                    <p>原理：向 QwenPaw 发 {reconnect: true, chat_id: ...} 请求，
                    QwenPaw 内部会先回放历史 buffer 事件（带 type=replay_end 标记），
                    再放后续 live 增量。前端 replay 段应直接跳过，避免与 /get-chat 返回的 history 重复。
                    <p>流已结束则返回空 SSE 然后关闭连接，前端 fallback 到 /get-chat 拉历史。""")
    @Parameter(name = "agentId", description = "智能体 ID", required = true)
    @Parameter(name = "chatId", description = "QwenPaw 会话 ID", required = true)
    @Parameter(name = "sessionId", description = "QwenPaw 会话唯一标识（UUID，新建时生成并缓存，续接时必传）", required = false)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public SseEmitter reconnectStream(@RequestParam("agentId") Long agentId,
                                      @RequestParam("chatId") String chatId,
                                      @RequestParam(value = "sessionId", required = false) String sessionId) {
        return chatSessionService.reconnectStream(agentId, chatId, sessionId);
    }

    @PostMapping("/stop-stream")
    @Operation(summary = "停止当前正在进行的对话（转发 QwenPaw chat/stop）",
            description = "返回 {\"stopped\": true/false}")
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:update')")
    public CommonResult<Map<String, Object>> stopStream(@RequestParam("agentId") Long agentId,
                                                        @RequestParam("chatId") String chatId) {
        return success(chatSessionService.stopStream(agentId, chatId));
    }

    // ==================== 模型切换 ====================

    @GetMapping("/models")
    @Operation(summary = "列出所有 Provider（含其下模型列表），供模型选择器使用")
    @Parameter(name = "agentId", description = "智能体 ID（芋道 Java 主键）", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public CommonResult<List<Map<String, Object>>> listModels(@RequestParam("agentId") Long agentId) {
        return success(chatSessionService.listProviders(agentId));
    }

    @GetMapping("/active-model")
    @Operation(summary = "获取智能体当前激活模型（scope=agent）")
    @Parameter(name = "agentId", description = "智能体 ID（芋道 Java 主键）", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public CommonResult<Map<String, Object>> getActiveModel(@RequestParam("agentId") Long agentId) {
        return success(chatSessionService.getActiveModel(agentId));
    }

    @PutMapping("/active-model")
    @Operation(summary = "切换智能体激活模型（scope=agent，持久化到 QwenPaw）")
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:update')")
    public CommonResult<Map<String, Object>> setActiveModel(@RequestParam("agentId") Long agentId,
                                                            @RequestParam("providerId") String providerId,
                                                            @RequestParam("model") String model) {
        return success(chatSessionService.setActiveModel(agentId, providerId, model));
    }

    // ==================== 文件上传 / 预览 ====================

    @PostMapping("/upload")
    @Operation(summary = "上传文件到智能体（对话附件，QwenPaw 保存到 console media_dir）",
            description = "multipart 字段 file；返回 {\"url\": 绝对路径, \"file_name\": ..., \"size\": ...}")
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:update')")
    public CommonResult<Map<String, Object>> uploadFile(@RequestParam("agentId") Long agentId,
                                                        @RequestParam("file") MultipartFile file) throws IOException {
        return success(chatSessionService.uploadFile(agentId, file.getBytes(), file.getOriginalFilename()));
    }

    @GetMapping("/file-preview")
    @Operation(summary = "获取聊天文件预览（转发 QwenPaw /files/preview 文件字节流）")
    @Parameter(name = "agentId", description = "智能体 ID（芋道 Java 主键）", required = true)
    @Parameter(name = "path", description = "文件绝对路径（上传接口返回的 url）", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public ResponseEntity<byte[]> filePreview(@RequestParam("agentId") Long agentId,
                                              @RequestParam("path") String path) {
        byte[] data = chatSessionService.previewFile(agentId, path);
        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType = MediaTypeFactory.getMediaType(path).orElse(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentType(mediaType);
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

}
