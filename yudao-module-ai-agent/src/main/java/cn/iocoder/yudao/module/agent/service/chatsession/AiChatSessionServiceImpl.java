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
import cn.iocoder.yudao.module.agent.service.agent.AiAgentService;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.service.kbconfig.KbProcessConfigResolver;
import cn.iocoder.yudao.module.kb.service.library.LibraryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    @Resource
    private LibraryService libraryService;
    @Resource
    private AiAgentService aiAgentService;
    @Resource
    private KbProcessConfigResolver kbProcessConfigResolver;

    // ==================== 会话分享（公开访问） ====================
    // 分享 token -> 会话映射（内存态，服务重启后失效；如需持久化可改为写入 QwenPaw 会话 spec 或落库）。
    // token 为不透明随机串，作为"能力令牌"：持有即可匿名读取对应会话，无需登录。
    private final Map<String, ShareEntry> shareTokens = new ConcurrentHashMap<>();
    /** 分享 token 默认有效期：30 天 */
    private static final long SHARE_TOKEN_TTL_MS = 30L * 24 * 60 * 60 * 1000;

    private static final class ShareEntry {
        final String qwenpawAgentId;
        final String chatId;
        final long expireAt;

        ShareEntry(String qwenpawAgentId, String chatId, long expireAt) {
            this.qwenpawAgentId = qwenpawAgentId;
            this.chatId = chatId;
            this.expireAt = expireAt;
        }
    }

    // ==================== 会话 ====================

    @Override
    public List<Map<String, Object>> listChats(Long agentId) {
        AiAgentDO agent = validateAgentReadable(agentId);
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
        AiAgentDO agent = validateAgentReadable(agentId);
        return qwenPawClient.getChat(agent.getQwenpawAgentId(), chatId);
    }

    @Override
    public Map<String, Object> createShare(Long agentId, String chatId) {
        AiAgentDO agent = validateAgent(agentId);
        // 校验会话确实存在，避免为不存在的会话生成分享链接
        qwenPawClient.getChat(agent.getQwenpawAgentId(), chatId);
        String token = UUID.randomUUID().toString().replace("-", "");
        long expireAt = System.currentTimeMillis() + SHARE_TOKEN_TTL_MS;
        shareTokens.put(token, new ShareEntry(agent.getQwenpawAgentId(), chatId, expireAt));
        Map<String, Object> result = new HashMap<>(4);
        result.put("token", token);
        result.put("expireAt", expireAt);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSharedChat(String token) {
        ShareEntry entry = shareTokens.get(token);
        if (entry == null || entry.expireAt < System.currentTimeMillis()) {
            if (entry != null) {
                shareTokens.remove(token);
            }
            throw ServiceExceptionUtil.exception(CHAT_SHARE_INVALID);
        }
        Map<String, Object> chat = qwenPawClient.getChat(entry.qwenpawAgentId, entry.chatId);
        Map<String, Object> result = new HashMap<>(8);
        result.put("title", chat.get("name"));
        Object createdAt = chat.get("created_at");
        if (createdAt == null) {
            createdAt = chat.get("createdAt");
        }
        result.put("createdAt", createdAt);
        // 直接透传 QwenPaw 原始消息数组，前端用与 ChatPanel 完全相同的 normalizeQwenPawMessage 归一化，
        // 从而保证思考(reasoning)/工具调用(tool_calls)/附件/气泡等展示与系统内完全一致。
        Object rawMessages = chat.get("messages");
        result.put("messages", rawMessages instanceof List ? rawMessages : new ArrayList<>());
        return result;
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

    @Override
    public Map<String, Object> setChatPinned(Long agentId, String chatId, boolean pinned) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.updateChatPinned(agent.getQwenpawAgentId(), chatId, pinned);
    }

    // ==================== 会话分组 ====================

    @Override
    public List<Map<String, Object>> listChatGroups(Long agentId) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.listChatGroups(agent.getQwenpawAgentId());
    }

    @Override
    public Map<String, Object> createChatGroup(Long agentId, String name) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.createChatGroup(agent.getQwenpawAgentId(), name);
    }

    @Override
    public Map<String, Object> updateChatGroup(Long agentId, String groupId, String name) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.updateChatGroup(agent.getQwenpawAgentId(), groupId, name);
    }

    @Override
    public void deleteChatGroup(Long agentId, String groupId) {
        AiAgentDO agent = validateAgent(agentId);
        qwenPawClient.deleteChatGroup(agent.getQwenpawAgentId(), groupId);
    }

    @Override
    public void reorderChatGroups(Long agentId, List<String> groupIds) {
        AiAgentDO agent = validateAgent(agentId);
        qwenPawClient.reorderChatGroups(agent.getQwenpawAgentId(), groupIds);
    }

    @Override
    public Map<String, Object> updateChatGroupId(Long agentId, String chatId, String groupId) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.updateChatGroupId(agent.getQwenpawAgentId(), chatId, groupId);
    }

    // ==================== 对话 ====================

    @Override
    public SseEmitter sendMessageStream(Long agentId, String chatId, String sessionId, String message) {
        return sendMessageStream(agentId, chatId, sessionId, message, null, null);
    }

    @Override
    public SseEmitter sendMessageStream(Long agentId, String chatId, String sessionId, String message,
                                        List<Map<String, Object>> contentItems) {
        return sendMessageStream(agentId, chatId, sessionId, message, contentItems, null);
    }

    @Override
    public SseEmitter sendMessageStream(Long agentId, String chatId, String sessionId, String message,
                                        List<Map<String, Object>> contentItems, List<Long> kbIds) {
        return sendMessageStream(agentId, chatId, sessionId, message, contentItems, kbIds, false);
    }

    @Override
    public SseEmitter sendMessageStream(Long agentId, String chatId, String sessionId, String message,
                                        List<Map<String, Object>> contentItems, List<Long> kbIds, boolean approvalOff) {
        return sendMessageStream(agentId, chatId, sessionId, message, contentItems, kbIds, approvalOff, null);
    }

    @Override
    public SseEmitter sendMessageStream(Long agentId, String chatId, String sessionId, String message,
                                        List<Map<String, Object>> contentItems, List<Long> kbIds,
                                        boolean approvalOff, String approvalLevel) {
        AiAgentDO agent = validateAgent(agentId);
        String qwenpawAgentId = agent.getQwenpawAgentId();
        String userId = currentUserId();
        // 审批级别：问答框模式（strict/auto/off）优先；兼容旧 approvalOff=true → off。
        // 仅接受合法值，非法值忽略（回落 QwenPaw 智能体自身配置）。
        String level = null;
        if ("strict".equalsIgnoreCase(approvalLevel) || "auto".equalsIgnoreCase(approvalLevel)
                || "off".equalsIgnoreCase(approvalLevel)) {
            level = approvalLevel.toLowerCase();
        } else if (approvalOff) {
            level = "off";
        }
        Map<String, Object> requestContext = level == null
                ? null : java.util.Collections.singletonMap("approval_level", level);
        // 知识库检索范围注入：用户勾选 kbIds 时，先按第一层可见性过滤（权限校验由 Java 端负责，
        // 剔除当前用户不可见的库，防止越权检索其它租户/不可见库内容），再把「库名(id)」拼进当轮消息前缀，
        // 供 LLM 填 search_knowledge_base 的 knowledge_base_ids 参数（QwenPaw 不改，提示词注入）。
        List<Long> effectiveKbIds = filterVisibleKbIds(kbIds);
        String scopePrefix = buildKbScopePrefix(effectiveKbIds);
        // Wiki 研究员提示词运行时挂接：选中的 KB 中任一开启 wiki → 注入 WIKI_GUIDE，
        // 否则摘除（与知识库问答开关解耦，避免模型误用 wiki 工具）。
        applyWikiGuideForKbs(qwenpawAgentId, effectiveKbIds);
        if (StrUtil.isNotEmpty(scopePrefix) && contentItems != null && !contentItems.isEmpty()) {
            // 若以 content 数组发送，找到首个 text 项，把前缀拼进其 text 开头
            for (Map<String, Object> item : contentItems) {
                if ("text".equals(item.get("type")) && item.get("text") instanceof String) {
                    item.put("text", scopePrefix + item.get("text"));
                    break;
                }
            }
        }
        final String sendText = StrUtil.isNotEmpty(scopePrefix) ? scopePrefix + message : message;
        // sessionId 为空时兜底：
        // - 若已携带 chatId，则复用该会话在 QwenPaw 侧的 session_id，确保三元组 (session_id,user_id,channel) 一致，
        //   命中预创建会话，避免 QwenPaw get_or_create_chat 又新建一个空会话（C 端"新建对话后发送"场景）；
        // - 否则（首次发问无 chatId 的管理端场景）生成新 session_id，由 QwenPaw 自动开新会话。
        String effectiveSessionId;
        if (sessionId != null && !sessionId.isEmpty()) {
            effectiveSessionId = sessionId;
        } else if (chatId != null && !chatId.isEmpty()) {
            Map<String, Object> existingChat = qwenPawClient.getChat(qwenpawAgentId, chatId);
            Object sid = existingChat != null ? existingChat.get("session_id") : null;
            effectiveSessionId = sid != null ? String.valueOf(sid) : UUID.randomUUID().toString();
        } else {
            effectiveSessionId = UUID.randomUUID().toString();
        }
        SseEmitter emitter = new SseEmitter(0L);
        Long tenantId = TenantContextHolder.getTenantId();

        CompletableFuture.runAsync(() -> {
            TenantContextHolder.setTenantId(tenantId);
            try {
                if (contentItems != null && !contentItems.isEmpty()) {
                    qwenPawClient.chatStream(qwenpawAgentId, chatId, userId, effectiveSessionId, contentItems,
                            requestContext,
                            chunk -> forwardChunk(emitter, chunk, agentId, chatId, "sendMessageStream"));
                } else {
                    qwenPawClient.chatStream(qwenpawAgentId, chatId, userId, effectiveSessionId, sendText,
                            requestContext,
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
        AiAgentDO agent = validateAgent(agentId);
        List<Map<String, Object>> providers = qwenPawClient.listProviders(agent.getQwenpawAgentId());
        // 对话页「模型选择」只展示「已配置」的 provider：
        // 与管理端模型管理页（AiModelServiceImpl.convertToProviderVO）的 configured 口径保持一致，
        // 避免把未配置 api_key/base_url、已禁用的 provider 列出。
        List<Map<String, Object>> eligible = new ArrayList<>();
        for (Map<String, Object> provider : providers) {
            if (isEligibleProvider(provider)) {
                eligible.add(provider);
            }
        }
        return eligible;
    }

    /**
     * 判断 provider 是否「已配置」，可作为对话页模型选择的候选。
     *
     * <p>口径与管理端模型管理页 configured 一致：
     * <ol>
     *   <li>显式 disabled 或无任何模型（models + extra_models 均为空）的排除；</li>
     *   <li>内嵌本地 provider（qwenpaw-local / copaw-local）恒为已配置；</li>
     *   <li>自定义 provider 且 base_url 非空即视为已配置；</li>
     *   <li>require_api_key === false（免费/本地服务商）视为已配置；</li>
     *   <li>其余需 api_key 的 provider：需 api_key 非空（QwenPaw 返回为脱敏后的非空串）。</li>
     * </ol>
     */
    private boolean isEligibleProvider(Map<String, Object> provider) {
        if (provider == null) {
            return false;
        }
        if (Boolean.FALSE.equals(provider.get("enabled"))) {
            return false;
        }
        if (sizeOfList(provider.get("models")) + sizeOfList(provider.get("extra_models")) == 0) {
            return false;
        }
        String providerId = StrUtil.toStringOrNull(provider.get("id"));
        if ("qwenpaw-local".equals(providerId) || "copaw-local".equals(providerId)) {
            return true;
        }
        if (Boolean.TRUE.equals(provider.get("is_custom"))
                && StrUtil.isNotBlank(StrUtil.toStringOrNull(provider.get("base_url")))) {
            return true;
        }
        if (Boolean.FALSE.equals(provider.get("require_api_key"))) {
            return true;
        }
        return StrUtil.isNotBlank(StrUtil.toStringOrNull(provider.get("api_key")));
    }

    /** 安全取列表长度（非 List 或 null 返回 0） */
    private int sizeOfList(Object value) {
        return value instanceof List ? ((List<?>) value).size() : 0;
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
     * 校验智能体存在并返回 DO（写操作：仅创建者 / 超管）。
     *
     * <p>他人公开智能体「只读」：任何写操作（新建会话、发送对话、改配置等）一律拒绝，
     * 抛出 {@link ErrorCodeConstants#AGENT_READ_ONLY}。
     */
    private AiAgentDO validateAgent(Long agentId) {
        return aiAgentService.getAgentForWrite(agentId);
    }

    /**
     * 校验智能体存在并返回 DO（只读场景：本人 / 超管 / 公开智能体 可见）。
     *
     * <p>用于查看、预览他人公开智能体的会话历史等只读操作。
     */
    private AiAgentDO validateAgentReadable(Long agentId) {
        return aiAgentService.getAgentForRead(agentId);
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

    /**
     * 按用户选中的 KB 集合判定是否注入 Wiki 研究员提示词（WIKI_GUIDE.md）。
     *
     * <p>任一选中的知识库开启了 wiki 后处理（resolvePostprocessConfig 的 wiki_enabled）即注入，
     * 否则摘除。与「知识库问答」开关解耦：即便开了问答但选中的库都没开 wiki，也不注入，
     * 避免模型在非 wiki 场景误调 wiki_* 工具。调用失败仅告警，不阻断问答主流程。
     */
    private void applyWikiGuideForKbs(String qwenpawAgentId, List<Long> kbIds) {
        boolean wikiEnabled = false;
        if (kbIds != null && !kbIds.isEmpty()) {
            for (Long kbId : kbIds) {
                if (kbId == null) {
                    continue;
                }
                try {
                    Object cfg = kbProcessConfigResolver.resolvePostprocessConfig(kbId).get("wiki_enabled");
                    if (Boolean.TRUE.equals(cfg)) {
                        wikiEnabled = true;
                        break;
                    }
                } catch (Exception e) {
                    log.debug("[applyWikiGuideForKbs] 判定 kb={} wiki 开关失败，忽略", kbId, e);
                }
            }
        }
        try {
            aiAgentService.applyWikiGuidePrompt(qwenpawAgentId, wikiEnabled);
        } catch (Exception e) {
            log.warn("[applyWikiGuideForKbs] 注入/摘除 WIKI_GUIDE 失败，agentId={}", qwenpawAgentId, e);
        }
    }

    /**
     * 过滤当前用户不可见的知识库（第一层可见性，权限校验由 Java 端负责）。
     *
     * <p>kbIds 由前端勾选/请求体传入，可能含用户不可见（其它租户/无权限）的库；检索链路
     * （python MCP）只按传入 kb_id 过滤、不做归属校验，故此处统一剔除不可见项，防止越权检索。
     * 校验失败/库不存在按不可见处理；过滤后为空时保持「未勾选库」语义（不注入范围前缀）。
     */
    private List<Long> filterVisibleKbIds(List<Long> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) {
            return kbIds;
        }
        List<Long> visible = new ArrayList<>();
        for (Long kbId : kbIds) {
            if (kbId == null) {
                continue;
            }
            try {
                if (libraryService.canSeeKb(kbId)) {
                    visible.add(kbId);
                } else {
                    log.warn("[sendMessageStream] 剔除不可见知识库 kbId={}（越权检索防护）", kbId);
                }
            } catch (Exception e) {
                log.debug("[sendMessageStream] 校验知识库可见性失败，按不可见剔除 kbId={}", kbId, e);
            }
        }
        return visible;
    }

    /**
     * 构造知识库检索范围前缀，形如「[本次检索范围：规章库(id=1)、产品手册(id=5)]\n」。
     *
     * <p>kbIds 为空/查询不到库名时返回空串，保持「全库检索」语义。库名取不到则仅保留 id，
     * 不因权限/缺失导致整轮消息失败。
     */
    private String buildKbScopePrefix(List<Long> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (Long kbId : kbIds) {
            if (kbId == null) {
                continue;
            }
            String name = "";
            try {
                LibraryDO library = libraryService.getLibrary(kbId);
                if (library != null) {
                    name = library.getName();
                }
            } catch (Exception e) {
                log.debug("[buildKbScopePrefix] 查询知识库名失败 kbId={}", kbId, e);
            }
            parts.add(name + "(id=" + kbId + ")");
        }
        if (parts.isEmpty()) {
            return "";
        }
        return "[本次检索范围：" + String.join("、", parts) + "]\n";
    }

}
