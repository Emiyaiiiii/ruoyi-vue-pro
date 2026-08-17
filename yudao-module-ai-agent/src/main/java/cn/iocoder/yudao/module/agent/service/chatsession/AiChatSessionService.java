package cn.iocoder.yudao.module.agent.service.chatsession;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatMessagePageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatSendReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatSendRespVO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatSessionPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.chatsession.vo.ChatSessionRespVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.chatmessage.AiChatMessageDO;
import cn.iocoder.yudao.module.agent.dal.dataobject.chatsession.AiChatSessionDO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 问答会话 Service 接口
 *
 * @author 吴皓
 */
public interface AiChatSessionService {

    /**
     * 创建会话
     *
     * @param agentId 智能体ID
     * @param userId  用户ID
     * @param title   标题
     */
    Long createSession(Long agentId, Long userId, String title);

    /**
     * 获取会话详情
     */
    AiChatSessionDO getSession(Long id);

    /**
     * 分页查询会话
     */
    PageResult<ChatSessionRespVO> getSessionPage(ChatSessionPageReqVO pageReqVO);

    /**
     * 关闭会话
     */
    void closeSession(Long id);

    /**
     * 重命名会话：同步重命名 QwenPaw 侧会话（PUT /chats/{id} 更新 name），本地标题作为展示镜像同步
     */
    void updateTitle(Long id, String title);

    /**
     * 清空会话消息：删除 QwenPaw 侧会话（deleteChat，以 QwenPaw 为唯一事实来源），
     * 同步清空本地消息并更换 sessionKey，保证后续对话不再携带历史上下文
     */
    void clearMessages(Long id);

    /**
     * 删除会话（级联删除消息）
     */
    void deleteSession(Long id);

    /**
     * 发送消息（同步对话，SSE 流式后续补齐）
     */
    ChatSendRespVO sendMessage(Long sessionId, ChatSendReqVO reqVO);

    /**
     * SSE 流式发送消息
     *
     * <p>事件协议（前后端约定）：
     * <ul>
     *     <li>message：data 为增量文本块</li>
     *     <li>done：data 为 JSON {@code {"content":"完整回答","sessionId":"..."}}</li>
     *     <li>error：data 为 JSON {@code {"message":"错误信息"}}</li>
     * </ul>
     */
    SseEmitter sendMessageStream(Long sessionId, ChatSendReqVO reqVO);

    /**
     * 停止当前会话正在进行的对话（转发 QwenPaw chat/stop）
     *
     * @return QwenPaw 返回的 {@code {"stopped": bool}}
     */
    Map<String, Object> stopStream(Long sessionId);

    /**
     * 查询会话历史消息
     */
    List<AiChatMessageDO> getMessageList(Long sessionId);

    /**
     * 分页查询消息
     */
    PageResult<AiChatMessageDO> getMessagePage(ChatMessagePageReqVO pageReqVO);

}
