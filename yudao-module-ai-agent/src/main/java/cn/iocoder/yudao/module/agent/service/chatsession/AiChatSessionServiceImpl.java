package cn.iocoder.yudao.module.agent.service.chatsession;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.agent.dal.dataobject.agent.AiAgentDO;
import cn.iocoder.yudao.module.agent.dal.mysql.agent.AiAgentMapper;
import cn.iocoder.yudao.module.agent.framework.config.QwenPawClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static cn.iocoder.yudao.module.agent.enums.ErrorCodeConstants.*;

/**
 * 问答会话 Service 实现（透传模式）
 *
 * <p>本实现不维护任何本地会话/消息表：所有数据走 QwenPaw，Java 端只做
 * （1）智能体存在性校验；（2）线程级 SSE 转发；（3）租户上下文透传。
 *
 * @author 吴皓
 */
@Service
@Validated
@Slf4j
public class AiChatSessionServiceImpl implements AiChatSessionService {

    @Resource
    private QwenPawClient qwenPawClient;
    @Resource
    private AiAgentMapper agentMapper;
    @Resource(name = "qwenpawChatTaskExecutor")
    private ThreadPoolTaskExecutor chatTaskExecutor;

    // ==================== 会话 ====================

    @Override
    public List<Map<String, Object>> listChats(Long agentId) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.listChatsForAgent(agent.getQwenpawAgentId());
    }

    @Override
    public Map<String, Object> createChat(Long agentId, String name) {
        AiAgentDO agent = validateAgent(agentId);
        String userId = currentUserId();
        Map<String, Object> chat = qwenPawClient.createChat(agent.getQwenpawAgentId(), userId, name);
        if (chat == null || chat.isEmpty() || chat.get("id") == null) {
            throw ServiceExceptionUtil.exception(QWENPAW_CHAT_CREATE_FAILED);
        }
        return chat;
    }

    @Override
    public Map<String, Object> getChat(Long agentId, String chatId) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.getChat(agent.getQwenpawAgentId(), chatId);
    }

    @Override
    public void deleteChat(Long agentId, String chatId) {
        AiAgentDO agent = validateAgent(agentId);
        qwenPawClient.deleteChat(agent.getQwenpawAgentId(), chatId);
    }

    @Override
    public Map<String, Object> renameChat(Long agentId, String chatId, String name) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.updateChatName(agent.getQwenpawAgentId(), chatId, name);
    }

    // ==================== 对话 ====================

    @Override
    public SseEmitter sendMessageStream(Long agentId, String chatId, String sessionId, String message) {
        return sendMessageStream(agentId, chatId, sessionId, message, null);
    }

    @Override
    public SseEmitter sendMessageStream(Long agentId, String chatId, String sessionId, String message,
                                        List<Map<String, Object>> contentItems) {
        AiAgentDO agent = validateAgent(agentId);
        String qwenpawAgentId = agent.getQwenpawAgentId();
        String userId = currentUserId();
        // sessionId 为空时兜底生成（首次发问无预创建 chat 的场景）
        String effectiveSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId : UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);
        Long tenantId = TenantContextHolder.getTenantId();

        CompletableFuture.runAsync(() -> {
            TenantContextHolder.setTenantId(tenantId);
            try {
                if (contentItems != null && !contentItems.isEmpty()) {
                    qwenPawClient.chatStream(qwenpawAgentId, chatId, userId, effectiveSessionId, contentItems,
                            chunk -> forwardChunk(emitter, chunk, agentId, chatId, "sendMessageStream"));
                } else {
                    qwenPawClient.chatStream(qwenpawAgentId, chatId, userId, effectiveSessionId, message,
                            chunk -> forwardChunk(emitter, chunk, agentId, chatId, "sendMessageStream"));
                }
                // 上游 QwenPaw 流已结束，必须 complete 关闭 SseEmitter。
                // 若不调用，连接（及经 nginx 中转时末尾缓冲）永不关闭，
                // 前端读不到 {object:response,status:completed}，onDone 不触发、列表不刷新。
                emitter.complete();
            } catch (Exception e) {
                log.error("[sendMessageStream] QwenPaw 流式对话失败，agentId={}, chatId={}", agentId, chatId, e);
                sendErrorAndComplete(emitter, e);
            } finally {
                TenantContextHolder.clear();
            }
        }, chatTaskExecutor);

        return emitter;
    }

    @Override
    public List<Map<String, Object>> listProviders(Long agentId) {
        validateAgent(agentId);
        return qwenPawClient.listProviders();
    }

    @Override
    public Map<String, Object> getActiveModel(Long agentId) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.getActiveModel("agent", agent.getQwenpawAgentId());
    }

    @Override
    public Map<String, Object> setActiveModel(Long agentId, String providerId, String model) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.setActiveModel("agent", providerId, model, agent.getQwenpawAgentId());
    }

    @Override
    public Map<String, Object> uploadFile(Long agentId, byte[] data, String fileName) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.uploadChatFile(agent.getQwenpawAgentId(), data, fileName);
    }

    @Override
    public byte[] previewFile(Long agentId, String path) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.previewFile(agent.getQwenpawAgentId(), path);
    }

    @Override
    public SseEmitter reconnectStream(Long agentId, String chatId, String sessionId) {
        AiAgentDO agent = validateAgent(agentId);
        String qwenpawAgentId = agent.getQwenpawAgentId();
        String userId = currentUserId();
        String effectiveSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId : UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);
        Long tenantId = TenantContextHolder.getTenantId();

        CompletableFuture.runAsync(() -> {
            TenantContextHolder.setTenantId(tenantId);
            try {
                qwenPawClient.reconnectStream(qwenpawAgentId, chatId, userId, effectiveSessionId,
                        chunk -> forwardChunk(emitter, chunk, agentId, chatId, "reconnectStream"));
                // 流已结束或未运行：通知前端
                try {
                    emitter.send(SseEmitter.event().name("done").data("{\"status\":\"reconnect_completed\"}"));
                } catch (IOException ignored) {
                }
                emitter.complete();
            } catch (Exception e) {
                log.error("[reconnectStream] 重连失败，agentId={}, chatId={}", agentId, chatId, e);
                sendErrorAndComplete(emitter, e);
            } finally {
                TenantContextHolder.clear();
            }
        }, chatTaskExecutor);

        return emitter;
    }

    @Override
    public Map<String, Object> stopStream(Long agentId, String chatId) {
        AiAgentDO agent = validateAgent(agentId);
        try {
            return qwenPawClient.stopChat(agent.getQwenpawAgentId(), chatId);
        } catch (Exception e) {
            log.warn("[stopStream] QwenPaw 停止失败，agentId={}, chatId={}", agentId, chatId, e);
            Map<String, Object> empty = new HashMap<>();
            empty.put("stopped", false);
            return empty;
        }
    }

    // ==================== 私有辅助 ====================

    /**
     * 校验智能体存在并返回 DO（命中芋道租户隔离）
     */
    private AiAgentDO validateAgent(Long agentId) {
        AiAgentDO agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw ServiceExceptionUtil.exception(AGENT_NOT_EXISTS);
        }
        if (agent.getQwenpawAgentId() == null || agent.getQwenpawAgentId().isEmpty()) {
            throw ServiceExceptionUtil.exception(QWENPAW_AGENT_NOT_FOUND);
        }
        return agent;
    }

    /**
     * 获取当前登录芋道用户的用户名，用于 QwenPaw 三元组
     *
     * <p>仅作为 QwenPaw 侧 {@code (session_id, user_id, channel)} 查重的 key 之一，
     * 避免不同芋道用户的会话串台；不参与任何权限过滤。
     *
     * @return 用户名；未登录时 fallback 为 "anonymous"
     */
    private String currentUserId() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return "anonymous";
        }
        // 获取用户名，优先从info中获取username，否则使用nickname，最后回退到用户ID
        String username = MapUtil.getStr(loginUser.getInfo(), "username");
        if (StrUtil.isEmpty(username)) {
            username = MapUtil.getStr(loginUser.getInfo(), LoginUser.INFO_KEY_NICKNAME);
        }
        if (StrUtil.isEmpty(username)) {
            username = String.valueOf(loginUser.getId());
        }
        return username;
    }

    /**
     * 透传单个 SSE chunk 到前端（仅做错误 swallow，避免关闭连接导致后续 chunk 全部失败）
     */
    private void forwardChunk(SseEmitter emitter, String chunk, Long agentId, String chatId, String op) {
        try {
            emitter.send(SseEmitter.event().data(chunk));
        } catch (Exception e) {
            log.debug("[{}] 转发 SSE 事件失败，agentId={}, chatId={}", op, agentId, chatId, e);
        }
    }

    /**
     * 发送 error 事件并关闭连接
     */
    private void sendErrorAndComplete(SseEmitter emitter, Exception e) {
        try {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("message", e.getMessage() == null ? "未知错误" : e.getMessage());
            emitter.send(SseEmitter.event().name("error").data(
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(errorData)));
        } catch (Exception ignored) {
        }
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }

}
