package cn.iocoder.yudao.module.agent.service.agent;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.agent.controller.admin.agent.vo.AgentPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.agent.vo.AgentSaveReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.agent.AiAgentDO;

import java.util.List;
import java.util.Map;

/**
 * 智能体实例 Service 接口
 *
 * @author 吴皓
 */
public interface AiAgentService {

    /**
     * 创建智能体（同步创建 QwenPaw agent）
     */
    Long createAgent(AgentSaveReqVO createReqVO);

    /**
     * 更新智能体
     */
    void updateAgent(AgentSaveReqVO updateReqVO);

    /**
     * 删除智能体（同步删除 QwenPaw agent 及其绑定）
     */
    void deleteAgent(Long id);

    /**
     * 启停智能体（同步 QwenPaw）
     */
    void toggleAgent(Long id);

    /**
     * 获取智能体详情
     */
    AiAgentDO getAgent(Long id);

    /**
     * 校验并返回智能体（只读场景）：存在 + 已注册 QwenPaw，且当前用户「本人 / 超管 / 公开(visibility=1)」可见。
     *
     * <p>用于他人公开智能体的查看/预览历史等只读操作。
     */
    AiAgentDO getAgentForRead(Long id);

    /**
     * 校验并返回智能体（写操作场景）：存在 + 已注册 QwenPaw，且当前用户「本人 / 超管」。
     *
     * <p>用于新建会话、发送对话、改配置等写操作；他人公开智能体只读，不可写。
     */
    AiAgentDO getAgentForWrite(Long id);

    /**
     * 分页查询智能体
     */
    PageResult<AiAgentDO> getAgentPage(AgentPageReqVO pageReqVO);

    /**
     * 查询当前登录用户的智能体列表
     */
    List<AiAgentDO> getMyAgents();

    /**
     * 查询「当前用户可见」的启用智能体列表（水利智囊 - 智能体页签）：
     * 公开的（visibility=1，同租户所有用户）+ 我自己创建的。
     */
    List<AiAgentDO> getVisibleAgentList(Long userId);

    /**
     * 校验智能体是否「可被当前用户关注」：公开（visibility=1）、或当前用户自己创建的、或超管。
     *
     * @return 智能体 DO
     */
    AiAgentDO validateFollowable(Long id);

    /**
     * 获取当前登录用户的默认智能体；不存在则按默认模板自动创建一个（懒加载）。
     *
     * <p>核心保证「每人至少有一个默认智能体」，前端列表页因此永远非空。
     *
     * @return 用户默认智能体；QwenPaw 不可用且配置了降级时，返回 status=0 的待初始化智能体
     */
    AiAgentDO getOrCreateDefaultAgent(Long userId, Long tenantId);

    /**
     * 将指定智能体设为当前用户的默认智能体（先清旧后设新，唯一约束保证一人一个默认）。
     */
    void setDefaultAgent(Long agentId);

    /**
     * 【超管/租户管理员】批量兜底：为「本地库中尚无任何智能体」的用户各创建一个默认智能体。
     *
     * @return {用户名 -> 结果}，结果取值 success / skipped / failed
     */
    Map<String, Object> bootstrapUserDefaultAgents();

    /**
     * 按 QwenPaw agent id 查询
     */
    AiAgentDO getAgentByQwenpawAgentId(String qwenpawAgentId);

    /**
     * 列出智能体内置工具（含启用状态）
     */
    List<Map<String, Object>> listAgentTools(Long agentId);

    /**
     * 切换智能体内置工具启用状态
     */
    Map<String, Object> toggleAgentTool(Long agentId, String toolName);

    /**
     * 获取智能体内置工具配置
     */
    Map<String, Object> getAgentToolConfig(Long agentId, String toolName);

    /**
     * 更新智能体内置工具配置
     */
    Map<String, Object> updateAgentToolConfig(Long agentId, String toolName, Map<String, Object> config);

    /**
     * 获得智能体运行状态（idle / running / disabled）
     */
    Map<String, Object> getAgentStatus(Long agentId);

    /**
     * 列出智能体在 QwenPaw 侧实际注册的 MCP clients
     */
    List<Map<String, Object>> listAgentQwenpawMcps(Long agentId);

    /**
     * 切换智能体在 QwenPaw 侧某个 MCP client 的启用状态
     */
    Map<String, Object> toggleAgentQwenpawMcp(Long agentId, String clientKey);

    /**
     * 列出智能体某个 MCP client 的可用工具
     */
    List<Map<String, Object>> listAgentMcpTools(Long agentId, String clientKey);

    /**
     * 更新智能体某个 MCP client 的工具白名单（QwenPaw 侧；对任意已注册 MCP 生效）
     *
     * @param toolsJson 工具白名单 JSON 数组字符串；为 null 表示移除白名单启用全部
     */
    List<Map<String, Object>> updateAgentMcpTools(Long agentId, String clientKey, String toolsJson);

    /**
     * 更新智能体某个 MCP client 的完整配置（QwenPaw 侧；含 transport/url/headers/command/args/env/cwd/tools）
     */
    Map<String, Object> updateAgentMcpConfig(Long agentId, String clientKey, Map<String, Object> config);

    /**
     * 列出智能体工作区的 Skills（QwenPaw 侧实际安装）
     */
    List<Map<String, Object>> listAgentQwenpawSkills(Long agentId);

    /**
     * 获得智能体可用的斜杠操作指令（/clean、/new 等，QwenPaw commands/available 透传，区别于 skill 指令）
     */
    List<Map<String, Object>> listAgentQwenpawCommands(Long agentId);

    /**
     * 列出 QwenPaw 全局技能池（内置 + 自定义技能）
     *
     * <p>按技能商场（skill-meta）可见性过滤：仅返回公开 + 当前用户个人的技能，
     * 保证「个人/公共」区分——他人个人的技能模板不出现在安装列表中
     */
    List<Map<String, Object>> listQwenpawSkillPool(Long userId);

    /**
     * 获取 QwenPaw 技能池中某个技能的详情（含 YAML 定义）
     */
    Map<String, Object> getQwenpawSkillPoolDetail(String skillName);

    /**
     * 从技能池安装技能到智能体
     */
    void installQwenpawSkill(Long agentId, String skillName);

    /**
     * 卸载智能体上安装的技能
     */
    void uninstallQwenpawSkill(Long agentId, String skillName);

    /**
     * 读取智能体已安装技能的详情（含 SKILL.md 内容；QwenPaw 工作区实例，非技能池模板）
     */
    Map<String, Object> getAgentSkillDetail(Long agentId, String skillName);

    /**
     * 保存智能体已安装技能的 SKILL.md 内容（仅作用于该智能体，不回写技能池/商场模板）
     */
    Map<String, Object> saveAgentSkillContent(Long agentId, String skillName, String content);

    /**
     * 注册 MCP 到智能体（QwenPaw 侧）
     *
     * @param clientKey  客户端标识
     * @param transport  传输方式：streamable-http / stdio
     * @param url        HTTP 传输的地址（streamable-http）
     * @param command    stdio 传输的命令（如 npx、python）
     * @param commandArgs stdio 传输的参数（JSON 数组字符串）
     * @param headersJson 请求头（JSON 对象字符串）
     * @param toolsJson  工具白名单（JSON 数组字符串，空为全部）
     */
    Map<String, Object> registerQwenpawMcp(Long agentId, String clientKey, String transport, String url,
                                           String command, String commandArgs, String headersJson, String toolsJson);

    /**
     * 删除智能体上的 MCP 注册（QwenPaw 侧）
     */
    void deleteQwenpawMcp(Long agentId, String clientKey);

    /**
     * 按 QwenPaw 智能体 ID 注入/摘除 Wiki 研究员指南提示词（WIKI_GUIDE.md）。
     *
     * <p>由问答会话在「用户选中的 KB 中任一开启 wiki」时调用（对齐 WeKnora
     * builtin-wiki-researcher）：开启则把 WeKnora wiki_researcher 系统提示词写入
     * QwenPaw 工作区并加入 system_prompt_files；关闭则摘除。幂等。
     *
     * @param qwenpawAgentId QwenPaw 智能体 ID
     * @param enabled        是否注入
     */
    void applyWikiGuidePrompt(String qwenpawAgentId, boolean enabled);

}
