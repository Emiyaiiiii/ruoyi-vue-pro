package cn.iocoder.yudao.module.agent.service.chatsession;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatMessagePageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatSendReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatSendRespVO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatSessionPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatSessionRespVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.agent.AiAgentDO;
import cn.iocoder.yudao.module.agent.dal.dataobject.chatmessage.AiChatMessageDO;
import cn.iocoder.yudao.module.agent.dal.dataobject.chatsession.AiChatSessionDO;
import cn.iocoder.yudao.module.agent.dal.mysql.agent.AiAgentMapper;
import cn.iocoder.yudao.module.agent.dal.mysql.chatsession.AiChatSessionMapper;
import cn.iocoder.yudao.module.agent.framework.config.QwenPawClient;
import cn.iocoder.yudao.module.agent.service.chatmessage.AiChatMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.agent.enums.ErrorCodeConstants.*;

/**
 * 问答会话 Service 实现类
 *
 * <p>当前为同步对话骨架：调用 QwenPaw 的 /console/chat 后整体返回。
 * 生产建议改为 SSE 流式转发（见 QwenPawClient 注释），并在响应中回填 session_id。
 *
 * @author 吴皓
 */
@Service
@Validated
@Slf4j
public class AiChatSessionServiceImpl implements AiChatSessionService {

    @Resource
    private AiChatSessionMapper chatSessionMapper;
    @Resource
    private AiAgentMapper agentMapper;
    @Resource
    private AiChatMessageService chatMessageService;
    @Resource
    private QwenPawClient qwenPawClient;
    @Resource(name = "qwenpawChatTaskExecutor")
    private ThreadPoolTaskExecutor chatTaskExecutor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSession(Long agentId, Long userId, String title) {
        AiAgentDO agent = validateAgent(agentId);
        AiChatSessionDO session = new AiChatSessionDO();
        session.setTenantId(agent.getTenantId());
        session.setAgentId(agentId);
        session.setUserId(userId);
        session.setTitle(title == null || title.isEmpty() ? "新对话" : title);
        session.setStatus(1);
        // 预生成唯一 session_key，首次对话时作为 QwenPaw 的 session_id 发送
        session.setSessionKey("yudao-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        chatSessionMapper.insert(session);
        return session.getId();
    }

    @Override
    public AiChatSessionDO getSession(Long id) {
        return validateExists(id);
    }

    @Override
    public PageResult<ChatSessionRespVO> getSessionPage(ChatSessionPageReqVO pageReqVO) {
        PageResult<AiChatSessionDO> pageResult = chatSessionMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, ChatSessionRespVO.class, this::fillAgentName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeSession(Long id) {
        AiChatSessionDO session = validateExists(id);
        AiChatSessionDO updateObj = new AiChatSessionDO();
        updateObj.setId(id);
        updateObj.setStatus(0);
        chatSessionMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTitle(Long id, String title) {
        AiChatSessionDO session = validateExists(id);
        if (title == null || title.trim().isEmpty()) {
            throw exception(CHAT_SESSION_TITLE_EMPTY);
        }
        String newTitle = title.trim();

        // 1. 同步到 QwenPaw 引擎：重命名对应会话（以 QwenPaw 为唯一事实来源）
        AiAgentDO agent = agentMapper.selectById(session.getAgentId());
        if (agent != null && session.getSessionKey() != null && !session.getSessionKey().isEmpty()) {
            String chatId = findChatIdBySessionKey(agent.getQwenpawAgentId(), session.getSessionKey());
            if (chatId != null) {
                qwenPawClient.updateChatName(agent.getQwenpawAgentId(), chatId, newTitle);
                log.info("[updateTitle] QwenPaw 会话已重命名，agentId={}, chatId={}",
                        agent.getQwenpawAgentId(), chatId);
            }
            // 会话尚未真正对话时 QwenPaw 侧无 chat 实体，跳过引擎更新，仅更新本地镜像
        }

        // 2. 同步本地标题（本地仅作会话列表展示的镜像）
        AiChatSessionDO updateObj = new AiChatSessionDO();
        updateObj.setId(id);
        updateObj.setTitle(newTitle);
        chatSessionMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearMessages(Long id) {
        AiChatSessionDO session = validateExists(id);
        AiAgentDO agent = agentMapper.selectById(session.getAgentId());

        // 1. 重置 QwenPaw 侧会话上下文：删除旧 chat，避免清空后仍携带历史上下文
        if (agent != null && session.getSessionKey() != null && !session.getSessionKey().isEmpty()) {
            try {
                String chatId = findChatIdBySessionKey(agent.getQwenpawAgentId(), session.getSessionKey());
                if (chatId != null) {
                    qwenPawClient.deleteChat(agent.getQwenpawAgentId(), chatId);
                    log.info("[clearMessages] QwenPaw 会话已重置，agentId={}, chatId={}",
                            agent.getQwenpawAgentId(), chatId);
                }
            } catch (Exception e) {
                log.warn("[clearMessages] QwenPaw 会话重置失败（继续清空本地数据），sessionKey={}",
                        session.getSessionKey(), e);
            }
        }

        // 2. 删除本地消息
        chatMessageService.deleteBySessionId(id);

        // 3. 更换 sessionKey，保证后续对话从新会话开始
        AiChatSessionDO updateObj = new AiChatSessionDO();
        updateObj.setId(id);
        updateObj.setSessionKey("yudao-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        chatSessionMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long id) {
        AiChatSessionDO session = validateExists(id);
        AiAgentDO agent = agentMapper.selectById(session.getAgentId());

        // 1. 同步删除 QwenPaw 中的会话
        if (agent != null && session.getSessionKey() != null && !session.getSessionKey().isEmpty()) {
            try {
                String chatId = findChatIdBySessionKey(agent.getQwenpawAgentId(), session.getSessionKey());
                if (chatId != null) {
                    qwenPawClient.deleteChat(agent.getQwenpawAgentId(), chatId);
                    log.info("[deleteSession] QwenPaw 会话已删除，agentId={}, chatId={}",
                            agent.getQwenpawAgentId(), chatId);
                } else {
                    log.warn("[deleteSession] QwenPaw 中未找到对应会话，sessionKey={}", session.getSessionKey());
                }
            } catch (Exception e) {
                log.warn("[deleteSession] QwenPaw 会话删除失败（继续删除本地数据），sessionKey={}",
                        session.getSessionKey(), e);
            }
        }

        // 2. 删除本地数据
        chatSessionMapper.deleteById(id);
        chatMessageService.deleteBySessionId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSendRespVO sendMessage(Long sessionId, ChatSendReqVO reqVO) {
        AiChatSessionDO session = validateExists(sessionId);
        if (!Integer.valueOf(1).equals(session.getStatus())) {
            throw exception(CHAT_SESSION_CLOSED);
        }
        AiAgentDO agent = validateAgent(session.getAgentId());
        if (!Integer.valueOf(1).equals(agent.getStatus())) {
            throw exception(AGENT_ALREADY_DISABLED);
        }

        // 1. 保存用户消息
        chatMessageService.createMessage(buildMessage(session, "user", reqVO.getMessage(), null, null, 0));

        // 2. 调用 QwenPaw（同步骨架）
        //    TODO: 流式 SSE 时改为逐块转发，并回填 QwenPaw 返回的 session_id
        String answer;
        try {
            answer = qwenPawClient.chat(agent.getQwenpawAgentId(), session.getSessionKey(), reqVO.getMessage());
        } catch (Exception e) {
            log.error("[sendMessage] QwenPaw 对话失败，sessionId={}", sessionId, e);
            throw exception(QWENPAW_CHAT_FAILED);
        }

        // 3. 保存助手消息
        chatMessageService.createMessage(buildMessage(session, "assistant", answer, null, null, 0));

        // 4. 更新会话标题（首轮用问题截断作为标题）
        if ("新对话".equals(session.getTitle())) {
            AiChatSessionDO updateObj = new AiChatSessionDO();
            updateObj.setId(sessionId);
            updateObj.setTitle(buildTitle(reqVO.getMessage()));
            chatSessionMapper.updateById(updateObj);
        }

        ChatSendRespVO respVO = new ChatSendRespVO();
        respVO.setSessionId(sessionId);
        respVO.setContent(answer);
        return respVO;
    }

    @Override
    public SseEmitter sendMessageStream(Long sessionId, ChatSendReqVO reqVO) {
        AiChatSessionDO session = validateExists(sessionId);
        if (!Integer.valueOf(1).equals(session.getStatus())) {
            throw exception(CHAT_SESSION_CLOSED);
        }
        AiAgentDO agent = validateAgent(session.getAgentId());
        if (!Integer.valueOf(1).equals(agent.getStatus())) {
            throw exception(AGENT_ALREADY_DISABLED);
        }

        // 1. 保存用户消息
        chatMessageService.createMessage(buildMessage(session, "user", reqVO.getMessage(), null, null, 0));

        // 2. 创建 SSE 连接（0L = 不超时；生产建议按需设置超时）
        SseEmitter emitter = new SseEmitter(0L);
        ObjectMapper objectMapper = new ObjectMapper();

        // 3. 捕获当前线程的租户上下文（异步线程无法继承 ThreadLocal）
        Long tenantId = TenantContextHolder.getTenantId();

        // 4. 异步流式转发 QwenPaw 的 SSE 数据（使用独立线程池，避免占用 ForkJoinPool.commonPool）
        CompletableFuture.runAsync(() -> {
            // 恢复租户上下文
            TenantContextHolder.setTenantId(tenantId);
            String chatId = null;
            
            try {
                // 1. 直接转发 QwenPaw 的原始 SSE 事件（不做任何解析）
                qwenPawClient.chatStream(agent.getQwenpawAgentId(), session.getSessionKey(),
                        reqVO.getMessage(), chunk -> {
                            try {
                                // 原样转发
                                emitter.send(SseEmitter.event().data(chunk));
                            } catch (Exception e) {
                                log.warn("[sendMessageStream] 转发 SSE 事件失败，sessionId={}", sessionId, e);
                            }
                        });

                // 2. 流结束后，调用 QwenPaw API 获取完整消息
                //    通过 sessionKey 查找 chatId
                chatId = findChatIdBySessionKey(agent.getQwenpawAgentId(), session.getSessionKey());
                if (chatId != null) {
                    Map<String, Object> chat = qwenPawClient.getChat(agent.getQwenpawAgentId(), chatId);
                    
                    // 3. 解析完整消息，保存到数据库
                    saveMessagesFromChat(session, chat, objectMapper);
                } else {
                    log.warn("[sendMessageStream] 未找到 chatId，无法保存消息，sessionKey={}", session.getSessionKey());
                }

                // 4. 更新会话标题（首轮用问题截断作为标题）
                if ("新对话".equals(session.getTitle())) {
                    AiChatSessionDO updateObj = new AiChatSessionDO();
                    updateObj.setId(sessionId);
                    updateObj.setTitle(buildTitle(reqVO.getMessage()));
                    chatSessionMapper.updateById(updateObj);
                }

                // 5. 发送完成事件
                emitter.send(SseEmitter.event().name("done").data("{\"status\":\"completed\"}"));
                emitter.complete();
            } catch (Exception e) {
                log.error("[sendMessageStream] QwenPaw 流式对话失败，sessionId={}", sessionId, e);
                try {
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("message", e.getMessage() == null ? "未知错误" : e.getMessage());
                    emitter.send(SseEmitter.event().name("error").data(objectMapper.writeValueAsString(errorData)));
                } catch (Exception ignored) {
                }
                emitter.complete();
            } finally {
                TenantContextHolder.clear();
            }
        }, chatTaskExecutor);
        return emitter;
    }

    @Override
    public List<AiChatMessageDO> getMessageList(Long sessionId) {
        validateExists(sessionId);
        return chatMessageService.getMessageListBySessionId(sessionId);
    }

    @Override
    public Map<String, Object> stopStream(Long sessionId) {
        AiChatSessionDO session = validateExists(sessionId);
        AiAgentDO agent = validateAgent(session.getAgentId());
        // 通过 sessionKey 解析 QwenPaw chat_id 再停止
        String chatId = findChatIdBySessionKey(agent.getQwenpawAgentId(), session.getSessionKey());
        if (chatId == null) {
            log.warn("[stopStream] 未找到 chatId，无法停止，sessionId={}, sessionKey={}",
                    sessionId, session.getSessionKey());
            Map<String, Object> empty = new HashMap<>();
            empty.put("stopped", false);
            return empty;
        }
        return qwenPawClient.stopChat(agent.getQwenpawAgentId(), chatId);
    }

    @Override
    public PageResult<AiChatMessageDO> getMessagePage(ChatMessagePageReqVO pageReqVO) {
        return chatMessageService.getMessagePage(pageReqVO);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 填充智能体名称冗余展示字段
     */
    private void fillAgentName(ChatSessionRespVO respVO) {
        if (respVO == null || respVO.getAgentId() == null) {
            return;
        }
        AiAgentDO agent = agentMapper.selectById(respVO.getAgentId());
        if (agent != null) {
            respVO.setAgentName(agent.getName());
        }
    }

    private AiChatSessionDO validateExists(Long id) {
        AiChatSessionDO session = chatSessionMapper.selectById(id);
        if (session == null) {
            throw exception(CHAT_SESSION_NOT_EXISTS);
        }
        return session;
    }

    private AiAgentDO validateAgent(Long agentId) {
        AiAgentDO agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw exception(AGENT_NOT_EXISTS);
        }
        return agent;
    }

    private AiChatMessageDO buildMessage(AiChatSessionDO session, String role, String content,
                                         String reasoningContent, String toolCalls, Integer tokens) {
        AiChatMessageDO message = new AiChatMessageDO();
        message.setTenantId(session.getTenantId());
        message.setSessionId(session.getId());
        message.setAgentId(session.getAgentId());
        message.setUserId(session.getUserId());
        message.setRole(role);
        message.setContent(content);
        message.setReasoningContent(reasoningContent == null ? "" : reasoningContent);
        message.setToolCalls(toolCalls == null ? "" : toolCalls);
        message.setTokens(tokens == null ? 0 : tokens);
        return message;
    }

    private String buildTitle(String message) {
        String title = message == null ? "" : message.trim().replaceAll("\\s+", " ");
        if (title.length() > 30) {
            title = title.substring(0, 30) + "...";
        }
        return title.isEmpty() ? "新对话" : title;
    }

    /**
     * 根据 session_key 查找 QwenPaw 中的 chat_id
     *
     * <p>QwenPaw 的 chat meta 中包含 session_id 字段，通过遍历智能体的会话列表匹配。
     */
    private String findChatIdBySessionKey(String qwenpawAgentId, String sessionKey) {
        try {
            List<Map<String, Object>> chats = qwenPawClient.listChatsForAgent(qwenpawAgentId);
            for (Map<String, Object> chat : chats) {
                Object meta = chat.get("meta");
                if (meta instanceof Map) {
                    Object sid = ((Map<?, ?>) meta).get("session_id");
                    if (sessionKey.equals(String.valueOf(sid))) {
                        return String.valueOf(chat.get("id"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[findChatIdBySessionKey] 查询 QwenPaw 会话列表失败，agentId={}", qwenpawAgentId, e);
        }
        return null;
    }

    /**
     * 从 QwenPaw 返回的完整 chat 对象中解析并保存消息
     *
     * @param session      当前会话
     * @param chat         QwenPaw 返回的 chat 对象
     * @param objectMapper JSON 解析器
     */
    @SuppressWarnings("unchecked")
    private void saveMessagesFromChat(AiChatSessionDO session, Map<String, Object> chat, ObjectMapper objectMapper) {
        try {
            Object messagesObj = chat.get("messages");
            if (!(messagesObj instanceof List)) {
                log.warn("[saveMessagesFromChat] messages 不是数组，chat={}", chat);
                return;
            }
            
            List<Object> messages = (List<Object>) messagesObj;
            if (messages.isEmpty()) {
                return;
            }

            // 遍历消息，提取助手消息
            for (Object msgObj : messages) {
                if (!(msgObj instanceof Map)) {
                    continue;
                }
                Map<String, Object> msg = (Map<String, Object>) msgObj;
                String role = String.valueOf(msg.get("role"));
                
                // 只保存助手消息（用户消息已在发送时保存）
                if (!"assistant".equals(role)) {
                    continue;
                }

                // 提取内容
                String content = extractTextFromContent(msg.get("content"));
                if (content == null || content.isEmpty()) {
                    continue;
                }

                // 提取思考过程（如果有）
                String reasoning = null;
                Object reasoningObj = msg.get("reasoning");
                if (reasoningObj instanceof Map) {
                    reasoning = extractTextFromContent(((Map<String, Object>) reasoningObj).get("content"));
                }

                // 提取工具调用（如果有）
                String toolCalls = null;
                Object toolCallsObj = msg.get("tool_calls");
                if (toolCallsObj instanceof List && !((List<?>) toolCallsObj).isEmpty()) {
                    toolCalls = objectMapper.writeValueAsString(toolCallsObj);
                }

                // 提取 token 用量（如果有）
                Integer tokens = 0;
                Object usageObj = msg.get("usage");
                if (usageObj instanceof Map) {
                    Object totalTokens = ((Map<String, Object>) usageObj).get("total_tokens");
                    if (totalTokens instanceof Number) {
                        tokens = ((Number) totalTokens).intValue();
                    }
                }

                // 保存消息
                chatMessageService.createMessage(buildMessage(session, "assistant", content, reasoning, toolCalls, tokens));
            }
        } catch (Exception e) {
            log.error("[saveMessagesFromChat] 解析并保存消息失败，sessionId={}", session.getId(), e);
        }
    }

    /**
     * 从 QwenPaw 消息的 content 字段中提取文本
     *
     * <p>QwenPaw 的 content 可能是：
     * - 字符串：直接返回
     * - 数组：遍历提取 type="text" 的文本并拼接
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromContent(Object contentObj) {
        if (contentObj == null) {
            return null;
        }
        
        // 字符串类型
        if (contentObj instanceof String) {
            return (String) contentObj;
        }
        
        // 数组类型
        if (contentObj instanceof List) {
            List<Object> contentList = (List<Object>) contentObj;
            StringBuilder sb = new StringBuilder();
            for (Object item : contentList) {
                if (item instanceof Map) {
                    Map<String, Object> itemMap = (Map<String, Object>) item;
                    String type = String.valueOf(itemMap.get("type"));
                    if ("text".equals(type)) {
                        Object text = itemMap.get("text");
                        if (text != null) {
                            sb.append(text);
                        }
                    }
                }
            }
            return sb.length() > 0 ? sb.toString() : null;
        }
        
        return null;
    }

}
