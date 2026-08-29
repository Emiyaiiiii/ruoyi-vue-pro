package cn.iocoder.yudao.module.agent.service.chatsession;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 问答会话 Service 接口（透传模式）
 *
 * <p>QwenPaw 是会话/消息的 source of truth。Java 端只做按智能体的代理透传，
 * 不再维护 ai_chat_session / ai_chat_message 表。调用方必须先选智能体，
 * 然后通过 {@code agentId + chatId} 唯一定位一个会话。
 *
 * @author 吴皓
 */
public interface AiChatSessionService {

    // ==================== 会话 ====================

    /**
     * 列出指定智能体下的所有 QwenPaw 会话
     *
     * @param agentId 芋道智能体主键 ID
     * @return QwenPaw 会话列表（原始结构，由前端直接消费）
     */
    List<Map<String, Object>> listChats(Long agentId);

    /**
     * 在 QwenPaw 中预创建一个空会话（用户点"新建对话"时调用）
     *
     * @param agentId 芋道智能体主键 ID
     * @param name    会话名（首次可传 "新对话"）
     * @return QwenPaw ChatSpec 字典（含 id/name/...）
     */
    Map<String, Object> createChat(Long agentId, String name);

    /**
     * 获取 QwenPaw 会话详情（含完整 messages 数组）
     */
    Map<String, Object> getChat(Long agentId, String chatId);

    /**
     * 删除 QwenPaw 会话
     */
    void deleteChat(Long agentId, String chatId);

    /**
     * 重命名 QwenPaw 会话
     */
    Map<String, Object> renameChat(Long agentId, String chatId, String name);

    // ==================== 对话 ====================

    /**
     * SSE 流式发送消息
     *
     * <p>事件协议：原样转发 QwenPaw 的 SSE data 行；流结束/异常由前端根据连接关闭事件判断。
     *
     * @param agentId   芋道智能体主键 ID
     * @param chatId    QwenPaw 会话 ID（首次可为空，让 QwenPaw 自动开新会话）
     * @param sessionId QwenPaw 会话唯一标识（UUID，创建会话时生成，必须与创建时一致）
     * @param message   用户消息内容
     */
    SseEmitter sendMessageStream(Long agentId, String chatId, String sessionId, String message);

    /**
     * SSE 流式发送消息（支持附件 content 数组）
     *
     * <p>content 项格式与 QwenPaw 官网一致：
     * {@code [{type:"text",text:"..."}, {type:"image",image_url:"..."}, {type:"file",file_url:"..."}]}，
     * 用于"上传文件对话"。contentItems 为空时回退为纯文本消息。
     *
     * @param agentId     芋道智能体主键 ID
     * @param chatId      QwenPaw 会话 ID（首次可为空，让 QwenPaw 自动开新会话）
     * @param sessionId   QwenPaw 会话唯一标识（UUID，创建会话时生成，必须与创建时一致）
     * @param message     用户消息文本（contentItems 非空时作为 text 项一并发送）
     * @param contentItems 用户消息的 content 项数组（text/image/file/video/audio），可为 null
     */
    SseEmitter sendMessageStream(Long agentId, String chatId, String sessionId, String message,
                                 List<Map<String, Object>> contentItems);

    /**
     * SSE 流式发送消息（支持附件 content 数组 + 知识库检索范围）
     *
     * <p>在 {@link #sendMessageStream(Long, String, String, String, List)} 基础上，
     * 支持传入用户本轮勾选的知识库 kbIds。有值时，Java 把「库名(id)」拼进当轮消息前缀，
     * 供 LLM 填 search_knowledge_base 的 knowledge_base_ids 参数；为空时不注入，走全库检索语义。
     *
     * @param kbIds 用户所选知识库 ID 列表（可为 null/空）
     */
    SseEmitter sendMessageStream(Long agentId, String chatId, String sessionId, String message,
                                 List<Map<String, Object>> contentItems, List<Long> kbIds);

    /**
     * SSE 流式发送消息（支持附件 content + 知识库检索范围 + 会话级免审批）
     *
     * <p>在 {@link #sendMessageStream(Long, String, String, String, List, List)} 基础上，
     * approvalOff=true 时向 QwenPaw 传 {@code request_context:{"approval_level":"off"}}，
     * 代表用户在该会话内「不再弹工具审批框」。
     *
     * @param approvalOff 会话级免审批开关（true 表示本次会话后续工具调用不再审批）
     */
    SseEmitter sendMessageStream(Long agentId, String chatId, String sessionId, String message,
                                 List<Map<String, Object>> contentItems, List<Long> kbIds, boolean approvalOff);

    /**
     * 重新挂载到 QwenPaw 正在运行的流（页面切回/标签切换时使用）
     *
     * <p>通过 {@code reconnect: true} 发到 QwenPaw，attach 到同一 chat 正在运行的流上继续推 chunk。
     * 不重新生成；流已结束则返回空 SSE，前端 fallback 到历史。
     *
     * @param agentId   芋道智能体主键 ID
     * @param chatId    QwenPaw 会话 ID
     * @param sessionId QwenPaw 会话唯一标识（UUID，必须与创建时一致）
     */
    SseEmitter reconnectStream(Long agentId, String chatId, String sessionId);

    /**
     * 停止当前正在进行的对话（转发 QwenPaw chat/stop）
     */
    Map<String, Object> stopStream(Long agentId, String chatId);

    // ==================== 模型切换 ====================

    /**
     * 列出所有 Provider（含其下模型列表），供模型选择器使用
     *
     * @param agentId 芋道智能体主键 ID（仅做存在性校验，数据为 QwenPaw 全局模型池）
     */
    List<Map<String, Object>> listProviders(Long agentId);

    /**
     * 获取智能体当前激活模型（scope=agent）
     *
     * @param agentId 芋道智能体主键 ID
     * @return {scope, provider_id, model, ...}
     */
    Map<String, Object> getActiveModel(Long agentId);

    /**
     * 切换智能体激活模型（scope=agent，持久化到 QwenPaw）
     *
     * @param agentId    芋道智能体主键 ID
     * @param providerId Provider ID
     * @param model      模型 ID
     */
    Map<String, Object> setActiveModel(Long agentId, String providerId, String model);

    // ==================== 文件上传 / 预览 ====================

    /**
     * 上传文件到智能体（对话附件，QwenPaw 保存到 console media_dir）
     *
     * @param agentId  芋道智能体主键 ID
     * @param data     文件字节
     * @param fileName 文件名
     * @return {url, file_name, size}，url 为文件在 QwenPaw 服务端的绝对路径
     */
    Map<String, Object> uploadFile(Long agentId, byte[] data, String fileName);

    /**
     * 获取聊天文件预览字节流（转发 QwenPaw /files/preview）
     *
     * @param agentId 芋道智能体主键 ID
     * @param path    文件绝对路径（上传接口返回的 url）
     */
    byte[] previewFile(Long agentId, String path);

}
