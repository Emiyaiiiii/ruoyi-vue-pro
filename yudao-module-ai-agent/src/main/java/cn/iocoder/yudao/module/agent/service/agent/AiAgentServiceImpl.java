package cn.iocoder.yudao.module.agent.service.agent;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.agent.controller.admin.agent.vo.AgentPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.agent.vo.AgentSaveReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.agent.AiAgentDO;
import cn.iocoder.yudao.module.agent.dal.mysql.agent.AiAgentMapper;
import cn.iocoder.yudao.module.agent.framework.config.AiAgentDefaultProperties;
import cn.iocoder.yudao.module.agent.framework.config.QwenPawClient;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.agent.enums.ErrorCodeConstants.*;

/**
 * 智能体实例 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
@Slf4j
public class AiAgentServiceImpl implements AiAgentService {

    @Resource
    private AiAgentMapper agentMapper;
    @Resource
    private QwenPawClient qwenPawClient;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private AiAgentDefaultProperties defaultProperties;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private AdminUserApi adminUserApi;

    /** 知识库检索 MCP 服务地址（python-vector，Docker 内部服务名） */
    @Value("${yudao.ai.kb-retrieval.base-url:http://python-vector:8100}")
    private String kbRetrievalBaseUrl;
    /** 注册到 QwenPaw 的知识库检索 MCP client_key */
    @Value("${yudao.ai.kb-retrieval.client-key:knowledge-base-retrieval}")
    private String kbRetrievalClientKey;

    /** 知识库检索结果呈现指南提示词文件名（写入 QwenPaw 智能体工作区根目录） */
    private static final String KB_GUIDE_FILE = "KB_GUIDE.md";
    /** QwenPaw 默认系统提示词文件列表（KB_GUIDE 追加在其后） */
    private static final List<String> DEFAULT_SYSTEM_PROMPT_FILES =
            Arrays.asList("AGENTS.md", "SOUL.md", "PROFILE.md");
    /** 指导模型在引用检索结果时原样保留 Markdown 图片/链接，避免图片连接不显示 */
    private static final String KB_GUIDE_CONTENT = """
            # 知识库检索结果呈现规则

            当你通过 knowledge-base-retrieval 工具检索知识库时，工具返回的 content 可能携带 Markdown 格式的图片或链接，例如：

            - 图片：`![图片](http://...)` 或 `![](http://...)`
            - 链接：`[标题](http://...)`

            请严格遵守以下规则：
            1. 在回答中引用检索到的文字内容时，必须【原样保留】其中的 Markdown 图片与链接语法，不要删除、不要改写成普通纯文本、不要去掉感叹号。
            2. 图片对用户很重要。检索内容里的每张图片都应在你的回答中保留在对应位置，必要时可补充简短的图片说明，但 Markdown 语法本身不得改动。
            3. 如果检索结果里没有图片或链接，正常回答即可，无需额外说明。
            """;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAgent(AgentSaveReqVO createReqVO) {
        // 重名校验（同用户下唯一）
        validateNameUnique(createReqVO.getUserId(), createReqVO.getName(), null);

        // 生成 QwenPaw agent id：u{userId}_{随机串}
        String qwenpawAgentId = buildQwenpawAgentId(createReqVO.getUserId());

        // 先落库（失败回滚，避免孤儿记录）
        AiAgentDO agent = BeanUtils.toBean(createReqVO, AiAgentDO.class);
        if (agent.getStatus() == null) {
            agent.setStatus(1);
        }
        if (agent.getEnableKbTool() == null) {
            agent.setEnableKbTool(Boolean.TRUE);
        }
        if (agent.getSortOrder() == null) {
            agent.setSortOrder(0);
        }
        if (agent.getApprovalLevel() == null) {
            agent.setApprovalLevel("auto");
        }
        agent.setQwenpawAgentId(qwenpawAgentId);
        agentMapper.insert(agent);

        // 同步创建 QwenPaw agent
        try {
            String createdId = qwenPawClient.createAgent(qwenpawAgentId, agent.getName(),
                    agent.getDescription(), agent.getWorkspaceDir(),
                    agent.getModelProvider(), agent.getModelName());
            if (createdId == null || createdId.isEmpty()) {
                throw new IllegalStateException("QwenPaw 未返回 agent id");
            }
            agent.setQwenpawAgentId(createdId);
            agentMapper.updateById(agent);
        } catch (Exception e) {
            log.error("[createAgent] QwenPaw 智能体创建失败，agentId={}", agent.getId(), e);
            throw exception(AGENT_QWENPAW_CREATE_FAILED);
        }

        // 启用知识库工具时，自动注册检索 MCP（streamable_http + X-Tenant-Id 连接头）
        if (Boolean.TRUE.equals(agent.getEnableKbTool())) {
            try {
                registerKbRetrievalMcp(agent);
            } catch (Exception e) {
                log.warn("[createAgent] 注册知识库检索 MCP 失败，agentId={}", agent.getId(), e);
            }
        }

        // 同步工具审批级别到 QwenPaw（决定后续是否弹工具审批框）
        applyAgentApprovalLevel(agent);

        // 创建后安装初始技能（从 QwenPaw 技能池按 name 安装）
        List<String> initialSkills = createReqVO.getInitialSkills();
        if (initialSkills != null && !initialSkills.isEmpty()) {
            for (String skillName : initialSkills) {
                if (skillName != null && !skillName.trim().isEmpty()) {
                    try {
                        installQwenpawSkill(agent.getId(), skillName.trim());
                    } catch (Exception e) {
                        log.warn("[createAgent] 初始技能安装失败，skill={}", skillName, e);
                    }
                }
            }
        }
        return agent.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAgent(AgentSaveReqVO updateReqVO) {
        AiAgentDO agent = validateExists(updateReqVO.getId());
        // 重名校验（排除自身）
        validateNameUnique(updateReqVO.getUserId() != null ? updateReqVO.getUserId() : agent.getUserId(),
                updateReqVO.getName(), updateReqVO.getId());

        AiAgentDO updateObj = BeanUtils.toBean(updateReqVO, AiAgentDO.class);
        agentMapper.updateById(updateObj);

        // 同步更新 QwenPaw（名称/描述/模型变更；QwenPaw 为部分更新，仅传变更字段）
        try {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("name", updateReqVO.getName() != null ? updateReqVO.getName() : agent.getName());
            if (updateReqVO.getDescription() != null) {
                fields.put("description", updateReqVO.getDescription());
            }
            // active_model：仅当模型供应商或模型名有变更时下发，避免空对象覆盖
            String provider = updateReqVO.getModelProvider() != null
                    ? updateReqVO.getModelProvider() : agent.getModelProvider();
            String model = updateReqVO.getModelName() != null
                    ? updateReqVO.getModelName() : agent.getModelName();
            if (updateReqVO.getModelProvider() != null || updateReqVO.getModelName() != null) {
                Map<String, Object> activeModel = new LinkedHashMap<>();
                activeModel.put("provider_id", provider == null ? "" : provider);
                activeModel.put("model", model == null ? "" : model);
                fields.put("active_model", activeModel);
            }
            qwenPawClient.updateAgent(agent.getQwenpawAgentId(), fields);
        } catch (Exception e) {
            log.warn("[updateAgent] QwenPaw 同步失败，agentId={}", agent.getQwenpawAgentId(), e);
        }

        // 审批级别有变更时同步到 QwenPaw running-config
        if (updateReqVO.getApprovalLevel() != null) {
            try {
                qwenPawClient.setAgentApprovalLevel(agent.getQwenpawAgentId(), updateReqVO.getApprovalLevel());
            } catch (Exception e) {
                log.warn("[updateAgent] 同步审批级别失败，agentId={}", agent.getId(), e);
            }
        }

        // 知识库工具开关与 QwenPaw 的检索 MCP 保持一致：
        // 启用时确保已注册（幂等），关闭时注销，避免取消"知识库问答"后 MCP 工具仍挂载在 QwenPaw 上
        boolean enableKb = Boolean.TRUE.equals(
                updateReqVO.getEnableKbTool() != null ? updateReqVO.getEnableKbTool() : agent.getEnableKbTool());
        if (enableKb) {
            try {
                registerKbRetrievalMcp(agent);
            } catch (Exception e) {
                log.warn("[updateAgent] 注册知识库检索 MCP 失败，agentId={}", agent.getId(), e);
            }
        } else {
            try {
                qwenPawClient.deleteMcp(agent.getQwenpawAgentId(), kbRetrievalClientKey);
            } catch (Exception e) {
                log.warn("[updateAgent] 注销知识库检索 MCP 失败，agentId={}", agent.getId(), e);
            }
            // 同步摘除检索结果呈现指南，避免污染未启用知识库问答的智能体
            try {
                applyKbGuidePrompt(agent.getQwenpawAgentId(), false);
            } catch (Exception e) {
                log.warn("[updateAgent] 摘除知识库检索指南失败，agentId={}", agent.getId(), e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgent(Long id) {
        AiAgentDO agent = validateExists(id);
        // 本地 MCP 绑定已废弃（对齐 skills，QwenPaw 为权威源），删除 agent 时由 QwenPaw 一并清空其 MCP/Skills/Chat
        agentMapper.deleteById(id);

        // 同步删除 QwenPaw agent（其下的所有 chat 会被 QwenPaw 一并清空）
        try {
            qwenPawClient.deleteAgent(agent.getQwenpawAgentId());
        } catch (Exception e) {
            log.warn("[deleteAgent] QwenPaw 删除失败，agentId={}", agent.getQwenpawAgentId(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleAgent(Long id) {
        AiAgentDO agent = validateExists(id);
        boolean enabled = !Integer.valueOf(1).equals(agent.getStatus());
        // 同步 QwenPaw 后再更新本地
        qwenPawClient.toggleAgent(agent.getQwenpawAgentId(), enabled);
        AiAgentDO updateObj = new AiAgentDO();
        updateObj.setId(id);
        updateObj.setStatus(enabled ? 1 : 0);
        agentMapper.updateById(updateObj);
    }

    @Override
    public AiAgentDO getAgent(Long id) {
        return validateExists(id);
    }

    @Override
    public PageResult<AiAgentDO> getAgentPage(AgentPageReqVO pageReqVO) {
        // 非超管/租户管理员：强制只看当前用户的智能体，实现用户级隔离
        if (!isSuperAdmin()) {
            pageReqVO.setUserId(SecurityFrameworkUtils.getLoginUserId());
        }
        return agentMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AiAgentDO> getMyAgents() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return agentMapper.selectEnabledByUserId(0L);
        }
        // 懒加载：保证每个用户至少有一个默认智能体，列表页因此永远非空
        getOrCreateDefaultAgent(userId, TenantContextHolder.getTenantId());
        return agentMapper.selectEnabledByUserId(userId);
    }

    @Override
    public AiAgentDO getOrCreateDefaultAgent(Long userId, Long tenantId) {
        // 1. 已有默认智能体则直接返回
        AiAgentDO defaultAgent = agentMapper.selectDefaultByUserId(userId);
        if (defaultAgent != null) {
            return defaultAgent;
        }
        // 2. 无默认智能体，但有普通智能体：把最早创建的提升为默认（对准 V3 迁移的存量兜底语义）
        List<AiAgentDO> myAgents = agentMapper.selectEnabledByUserId(userId);
        if (!myAgents.isEmpty()) {
            AiAgentDO first = myAgents.stream()
                    .min(Comparator.comparing(AiAgentDO::getId))
                    .orElse(myAgents.get(0));
            setDefaultFlag(first.getId(), userId);
            return first;
        }
        // 3. 完全没有智能体：按默认模板新建一个【默认】智能体
        return createDefaultAgent(userId, tenantId);
    }

    @Override
    public void setDefaultAgent(Long agentId) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        validateExists(agentId); // 顺带做用户级权限校验
        agentMapper.clearDefaultByUserId(userId);
        setDefaultFlag(agentId, userId);
    }

    @Override
    public Map<String, Object> bootstrapUserDefaultAgents() {
        Map<String, Object> result = new LinkedHashMap<>();
        Long currentTenant = TenantContextHolder.getTenantId();
        // 只处理【启用】用户（本模块 status=0 为启用，CommonStatusEnum.ENABLE）；
        // deleted 过滤器由 BaseMapperX 自动注入，天然排除已删除用户；
        // 且 AdminUserDO 为租户级数据，TenantDatabaseInterceptor 会自动限定到当前租户，故仅回填当前租户
        List<AdminUserDO> users = adminUserMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
        for (AdminUserDO user : users) {
            Long userId = user.getId();
            String label = user.getUsername() + "(" + user.getNickname() + ")";
            AiAgentDO existing = agentMapper.selectDefaultByUserId(userId);
            if (existing != null) {
                result.put(label, "skipped");
                continue;
            }
            boolean hasAny = agentMapper.selectCountByUserId(userId) > 0;
            if (!hasAny) {
                try {
                    createDefaultAgent(userId, currentTenant);
                    result.put(label, "success");
                } catch (Exception e) {
                    log.error("[bootstrapUserDefaultAgents] 为用户创建默认智能体失败，userId={}", userId, e);
                    result.put(label, "failed");
                }
            } else {
                // 用户有普通智能体但缺默认，沿用「最早提升为默认」
                try {
                    AiAgentDO any = agentMapper.selectEnabledByUserId(userId).stream()
                            .min(Comparator.comparing(AiAgentDO::getId)).orElse(null);
                    if (any != null) {
                        setDefaultFlag(any.getId(), userId);
                        result.put(label, "promoted");
                    }
                } catch (Exception e) {
                    log.error("[bootstrapUserDefaultAgents] 提升默认智能体失败，userId={}", userId, e);
                    result.put(label, "failed");
                }
            }
        }
        return result;
    }

    @Override
    public AiAgentDO getAgentByQwenpawAgentId(String qwenpawAgentId) {
        return agentMapper.selectByQwenpawAgentId(qwenpawAgentId);
    }

    @Override
    public List<Map<String, Object>> listAgentTools(Long agentId) {
        AiAgentDO agent = validateExists(agentId);
        List<Map<String, Object>> tools = qwenPawClient.listTools(agent.getQwenpawAgentId());
        // QwenPaw 返回 snake_case，转换为前端 camelCase
        for (Map<String, Object> tool : tools) {
            normalizeToolMap(tool);
        }
        return tools;
    }

    @Override
    public Map<String, Object> toggleAgentTool(Long agentId, String toolName) {
        AiAgentDO agent = validateExists(agentId);
        Map<String, Object> result = qwenPawClient.toggleTool(agent.getQwenpawAgentId(), toolName);
        normalizeToolMap(result);
        return result;
    }

    @Override
    public Map<String, Object> getAgentToolConfig(Long agentId, String toolName) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.getToolConfig(agent.getQwenpawAgentId(), toolName);
    }

    @Override
    public Map<String, Object> updateAgentToolConfig(Long agentId, String toolName, Map<String, Object> config) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.updateToolConfig(agent.getQwenpawAgentId(), toolName, config);
    }

    @Override
    public Map<String, Object> getAgentStatus(Long agentId) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.getAgentStatus(agent.getQwenpawAgentId());
    }

    @Override
    public List<Map<String, Object>> listAgentQwenpawMcps(Long agentId) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.listAgentMcps(agent.getQwenpawAgentId());
    }

    @Override
    public Map<String, Object> toggleAgentQwenpawMcp(Long agentId, String clientKey) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.toggleAgentMcp(agent.getQwenpawAgentId(), clientKey);
    }

    @Override
    public List<Map<String, Object>> listAgentMcpTools(Long agentId, String clientKey) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.listMcpTools(agent.getQwenpawAgentId(), clientKey);
    }

    @Override
    public List<Map<String, Object>> updateAgentMcpTools(Long agentId, String clientKey, String toolsJson) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.updateMcpTools(agent.getQwenpawAgentId(), clientKey, toolsJson);
    }

    @Override
    public Map<String, Object> updateAgentMcpConfig(Long agentId, String clientKey, Map<String, Object> config) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.updateMcpConfig(agent.getQwenpawAgentId(), clientKey, config);
    }

    @Override
    public List<Map<String, Object>> listAgentQwenpawSkills(Long agentId) {
        AiAgentDO agent = validateExists(agentId);
        return qwenPawClient.listAgentSkills(agent.getQwenpawAgentId());
    }

    @Override
    public List<Map<String, Object>> listQwenpawSkillPool() {
        return qwenPawClient.listSkillPool();
    }

    @Override
    public Map<String, Object> getQwenpawSkillPoolDetail(String skillName) {
        return qwenPawClient.getSkillPoolDetail(skillName);
    }

    @Override
    public void installQwenpawSkill(Long agentId, String skillName) {
        AiAgentDO agent = validateExists(agentId);
        // 优先使用池下载安装，失败则降级为直接安装
        try {
            qwenPawClient.downloadPoolSkillToWorkspace(skillName, agent.getQwenpawAgentId(), false);
        } catch (Exception e) {
            log.warn("[installQwenpawSkill] 池下载安装失败，降级为直接安装，skillName={}, agentId={}",
                    skillName, agentId, e);
            qwenPawClient.installSkill(agent.getQwenpawAgentId(), skillName);
        }
    }

    @Override
    public void uninstallQwenpawSkill(Long agentId, String skillName) {
        AiAgentDO agent = validateExists(agentId);
        qwenPawClient.uninstallSkill(agent.getQwenpawAgentId(), skillName);
    }

    @Override
    public Map<String, Object> registerQwenpawMcp(Long agentId, String clientKey, String transport, String url,
                                                  String command, String commandArgs, String headersJson, String toolsJson) {
        AiAgentDO agent = validateExists(agentId);
        qwenPawClient.registerMcp(agent.getQwenpawAgentId(), clientKey, clientKey, transport, url,
                command, commandArgs, headersJson, toolsJson);
        // 返回注册后的最新 MCP 列表，便于前端刷新
        return qwenPawClient.listAgentMcps(agent.getQwenpawAgentId()).stream()
                .filter(m -> clientKey.equals(m.get("client_key")))
                .findFirst().orElse(null);
    }

    @Override
    public void deleteQwenpawMcp(Long agentId, String clientKey) {
        AiAgentDO agent = validateExists(agentId);
        qwenPawClient.deleteMcp(agent.getQwenpawAgentId(), clientKey);
    }

    /**
     * 注册知识库检索 MCP 到 QwenPaw 智能体。
     *
     * <p>传输使用 {@code streamable_http}，URL 指向 python-vector 的 /mcp/retrieval；
     * 租户 ID 通过连接级请求头 {@code X-Tenant-Id} 固定传入，供检索工具内 CurrentHeaders 兜底读取。
     *
     * <p>幂等：QwenPaw 以 client_key 维度 upsert，重复调用安全。
     */
    private void registerKbRetrievalMcp(AiAgentDO agent) {
        // 尾斜杠必须保留：FastMCP streamable-http 端点挂载后要求带斜杠访问，
        // 否则 QwenPaw MCP 客户端 POST 到无斜杠 URL 会收到 307 重定向，握手失败导致驱动无法激活
        String url = kbRetrievalBaseUrl.replaceAll("/+$", "") + "/mcp/retrieval/";
        String headersJson = "{\"X-Tenant-Id\":\"" + String.valueOf(agent.getTenantId()) + "\"}";
        qwenPawClient.registerMcp(agent.getQwenpawAgentId(), kbRetrievalClientKey, kbRetrievalClientKey,
                "streamable_http", url, null, null, headersJson, null);

        // 写入检索结果呈现指南并加入系统提示词，指导模型保留检索内容里的 Markdown 图片/链接
        applyKbGuidePrompt(agent.getQwenpawAgentId(), true);
    }

    /**
     * 将智能体配置的工具审批级别同步到 QwenPaw running-config
     *（strict=所有工具需审批 / auto=智能 / off=免审批）
     */
    private void applyAgentApprovalLevel(AiAgentDO agent) {
        if (agent.getQwenpawAgentId() == null || agent.getApprovalLevel() == null) {
            return;
        }
        try {
            qwenPawClient.setAgentApprovalLevel(agent.getQwenpawAgentId(), agent.getApprovalLevel());
        } catch (Exception e) {
            log.warn("[applyAgentApprovalLevel] 同步审批级别失败，agentId={}", agent.getId(), e);
        }
    }

    /**
     * 按知识库工具开关，向 QwenPaw 系统提示词注入/摘除检索结果呈现指南（KB_GUIDE.md）。
     *
     * <p>开启：向智能体工作区写入 KB_GUIDE.md 并追加到 system_prompt_files，QwenPaw 会将其
     * 一并拼进系统提示词，指导模型在引用检索结果时原样保留 Markdown 图片/链接（否则模型易将其
     * 转成纯文本或剔除，导致前端图片/链接不显示）。关闭：从 system_prompt_files 中摘除。
     *
     * <p>幂等：文件重复写、列表重复添加均安全。
     */
    private void applyKbGuidePrompt(String qwenpawAgentId, boolean enabled) {
        List<String> files = qwenPawClient.getSystemPromptFiles(qwenpawAgentId);
        if (files == null || files.isEmpty()) {
            // 空列表兜底到 QwenPaw 默认文件，避免仅剩 KB_GUIDE 而丢弃标准提示词文件
            files = new ArrayList<>(DEFAULT_SYSTEM_PROMPT_FILES);
        }
        boolean contains = files.contains(KB_GUIDE_FILE);
        if (enabled) {
            qwenPawClient.writeWorkspaceMd(qwenpawAgentId, KB_GUIDE_FILE, KB_GUIDE_CONTENT);
            if (!contains) {
                files.add(KB_GUIDE_FILE);
                qwenPawClient.setSystemPromptFiles(qwenpawAgentId, files);
            }
        } else {
            if (contains) {
                files.remove(KB_GUIDE_FILE);
                qwenPawClient.setSystemPromptFiles(qwenpawAgentId, files);
            }
        }
    }

    // ==================== 默认智能体私有实现 ====================

    /**
     * 将 {@code is_default} 标记为 1（直接落库，不做权限二次校验）。
     *
     * <p>调用方需保证：同一用户旧的默认已清零、且 {userId, isDefault} 不重复，
     * 否则会触发 UNIQUE(user_id, is_default) 冲突。
     */
    private void setDefaultFlag(Long agentId, Long userId) {
        AiAgentDO update = new AiAgentDO();
        update.setId(agentId);
        update.setIsDefault(1);
        update.setUserId(userId); // 仅用于兜底校验，不参与更新
        agentMapper.updateById(update);
    }

    /**
     * 按 {@link AiAgentDefaultProperties} 模板为用户创建一个【默认】智能体。
     *
     * <p>QwenPaw 注册失败时按配置降级：{@code degradeOnQwenpawFailure=true} 则仅落库 status=0
     * （下次进入页面再重试），避免阻塞"用户能否进入聊天页"；否则抛出 {@link #AGENT_QWENPAW_CREATE_FAILED}。
     *
     * <p>并发安全：复用 UNIQUE(user_id, is_default)，若两并发线程同建默认，后到者抛
     * {@link DuplicateKeyException}，捕获后回查已建的默认返回，不重复创建。
     */
    private AiAgentDO createDefaultAgent(Long userId, Long tenantId) {
        AiAgentDO agent = new AiAgentDO();
        agent.setUserId(userId);
        agent.setTenantId(tenantId);
        agent.setName(buildDefaultName(userId));
        agent.setDescription(defaultProperties.getDescription());
        agent.setSystemPrompt(defaultProperties.getSystemPrompt());
        agent.setEnableKbTool(defaultProperties.getEnableKbTool());
        agent.setStatus(1);
        agent.setSortOrder(0);
        agent.setIsDefault(1);
        // 模型留空 => 走 QwenPaw 全局激活模型
        agent.setModelProvider(defaultProperties.getModelProvider());
        agent.setModelName(defaultProperties.getModelName());
        agent.setQwenpawAgentId(buildQwenpawAgentId(userId));
        try {
            agentMapper.insert(agent);
        } catch (DuplicateKeyException e) {
            // 并发下被别人先建了默认，回查返回即可
            log.warn("[createDefaultAgent] 默认智能体已存在（并发），userId={}", userId);
            return agentMapper.selectDefaultByUserId(userId);
        }
        // 同步创建 QwenPaw agent
        try {
            String createdId = qwenPawClient.createAgent(agent.getQwenpawAgentId(), agent.getName(),
                    agent.getDescription(), agent.getWorkspaceDir(),
                    agent.getModelProvider(), agent.getModelName());
            if (createdId != null && !createdId.isEmpty()) {
                agent.setQwenpawAgentId(createdId);
                agentMapper.updateById(agent);
            }
        } catch (Exception e) {
            if (!Boolean.TRUE.equals(defaultProperties.getDegradeOnQwenpawFailure())) {
                log.error("[createDefaultAgent] QwenPaw 创建失败且不允许降级，userId={}", userId, e);
                throw exception(AGENT_QWENPAW_CREATE_FAILED);
            }
            log.warn("[createDefaultAgent] QwenPaw 不可用，降级为仅落库 status=0，userId={}", userId, e);
            AiAgentDO degrade = new AiAgentDO();
            degrade.setId(agent.getId());
            degrade.setStatus(0);
            agentMapper.updateById(degrade);
        }
        installInitialSkillsIfConfigured(agent.getId());
        return agent;
    }

    /**
     * 渲染默认智能体名称，替换 {@code ${nickname}} 占位为用户昵称；查不到昵称则保留模板原样。
     */
    private String buildDefaultName(Long userId) {
        String template = defaultProperties.getName();
        if (template == null || !template.contains("${nickname}")) {
            return template;
        }
        try {
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            if (user != null && user.getNickname() != null && !user.getNickname().isEmpty()) {
                return template.replace("${nickname}", user.getNickname());
            }
        } catch (Exception e) {
            log.debug("[buildDefaultName] 获取用户昵称失败，userId={}", userId, e);
        }
        return template.replace("${nickname}", "助手");
    }

    private void installInitialSkillsIfConfigured(Long agentId) {
        List<String> skills = defaultProperties.getInitialSkills();
        if (skills == null || skills.isEmpty()) {
            return;
        }
        for (String skillName : skills) {
            if (skillName == null || skillName.trim().isEmpty()) {
                continue;
            }
            try {
                installQwenpawSkill(agentId, skillName.trim());
            } catch (Exception e) {
                log.warn("[installInitialSkillsIfConfigured] 默认技能安装失败，skill={}", skillName, e);
            }
        }
    }

    // ==================== 私有辅助方法 ====================

    private AiAgentDO validateExists(Long id) {
        AiAgentDO agent = agentMapper.selectById(id);
        if (agent == null) {
            throw exception(AGENT_NOT_EXISTS);
        }
        // 用户级隔离：非超管只能访问属于自己的智能体
        validateAgentAccess(agent);
        return agent;
    }

    /**
     * 校验当前用户是否有权访问该智能体
     * <p>超管/租户管理员可访问本租户全部智能体；普通用户仅能访问属于自己的。</p>
     */
    private void validateAgentAccess(AiAgentDO agent) {
        if (isSuperAdmin()) {
            return;
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null || !userId.equals(agent.getUserId())) {
            throw exception(AGENT_PERMISSION_DENIED);
        }
    }

    /**
     * 是否超管/租户管理员（可见全部数据）
     */
    private boolean isSuperAdmin() {
        return securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode());
    }

    private void validateNameUnique(Long userId, String name, Long excludeId) {
        AiAgentDO existing = agentMapper.selectByUserIdAndName(userId, name);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw exception(AGENT_NAME_EXISTS);
        }
    }

    private String buildQwenpawAgentId(Long userId) {
        return "u" + userId + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 将 QwenPaw 内置工具返回的 snake_case 字段归一化为 camelCase
     */
    private void normalizeToolMap(Map<String, Object> tool) {
        if (tool == null) {
            return;
        }
        Object asyncExecution = tool.remove("async_execution");
        if (asyncExecution != null) {
            tool.put("asyncExecution", asyncExecution);
        }
        Object requiresConfig = tool.remove("requires_config");
        if (requiresConfig != null) {
            tool.put("requiresConfig", requiresConfig);
        }
        Object configFields = tool.remove("config_fields");
        if (configFields != null) {
            tool.put("configFields", configFields);
        }
        Object configValues = tool.remove("config_values");
        if (configValues != null) {
            tool.put("configValues", configValues);
        }
    }

}
