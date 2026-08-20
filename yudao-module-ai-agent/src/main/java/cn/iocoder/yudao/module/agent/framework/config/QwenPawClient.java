package cn.iocoder.yudao.module.agent.framework.config;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.agent.enums.ErrorCodeConstants.QWENPAW_AGENT_NOT_FOUND;
import static cn.iocoder.yudao.module.agent.enums.ErrorCodeConstants.QWENPAW_CONNECT_FAILED;
import static cn.iocoder.yudao.module.agent.enums.ErrorCodeConstants.QWENPAW_MCP_CONNECTING;

/**
 * QwenPaw HTTP 客户端
 *
 * <p>对 QwenPaw 的 agent 级 REST API 的轻量封装：
 * /api/agents、/api/agents/{id}/toggle、/api/agents/{id}/mcp、/api/agents/{id}/skills、/api/agents/{id}/console/chat
 *
 * <p>注意：当前为同步 REST 桩。SSE 流式对话（console/chat）后续接入时，
 * 建议改用 WebClient / Spring MVC async 逐块转发，本类保留同步版便于骨架联调。
 *
 * @author 吴皓
 */
@Component
@Slf4j
public class QwenPawClient {

    private static final String API_AGENTS = "/api/agents";
    private static final String API_MODELS = "/api/models";
    private static final String API_CHATS = "/chats";
    private static final String API_TOOLS = "/tools";
    private static final String API_MCP = "/mcp";
    private static final String API_SKILLS = "/skills";
    private static final String API_SKILLS_POOL = "/api/skills/pool";
    private static final String API_CONSOLE_CHAT_STOP = "/console/chat/stop";
    private static final String API_CONSOLE_UPLOAD = "/api/console/upload";
    private static final String API_FILES_PREVIEW = "/api/files/preview";

    @Resource
    private QwenPawProperties properties;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * 用于发送 PATCH 请求的 RestTemplate。
     *
     * <p>Spring Boot 3.x 下使用 {@code JdkClientHttpRequestFactory}（基于 JDK 11+
     * 内置的 {@code java.net.http.HttpClient}），原生支持 PATCH 方法，无需额外依赖。
     */
    /**
     * PATCH 专用 RestTemplate，使用 HttpClient5（基于 HttpComponentsClientHttpRequestFactory），
     * 避免 JdkClientHttpRequestFactory 在 PATCH 时被 uvicorn 返回 400 Invalid HTTP request received。
     */
    private static final RestTemplate patchRestTemplate;
    static {
        // 连接超时 10 秒，读取超时 30 秒
        org.apache.hc.client5.http.config.RequestConfig requestConfig = org.apache.hc.client5.http.config.RequestConfig.custom()
                .setConnectionRequestTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(10))
                .setResponseTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(30))
                .build();
        org.apache.hc.client5.http.impl.classic.CloseableHttpClient httpClient = org.apache.hc.client5.http.impl.classic.HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
        patchRestTemplate = new RestTemplate(new org.springframework.http.client.HttpComponentsClientHttpRequestFactory(httpClient));
    }

    // ==================== 智能体生命周期 ====================

    /**
     * 创建智能体，返回 qwenpaw_agent_id（通常为入参 id，未传则使用服务端生成值）
     */
    public String createAgent(String agentId, String name, String description,
                              String workspaceDir, String providerId, String model) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", agentId);
        body.put("name", name);
        body.put("description", description == null ? "" : description);
        body.put("workspace_dir", workspaceDir == null ? "" : workspaceDir);
        body.put("language", "zh-CN");
        // active_model 需要是对象格式：{provider_id, model}
        Map<String, Object> activeModel = new LinkedHashMap<>();
        activeModel.put("provider_id", providerId == null ? "" : providerId);
        activeModel.put("model", model == null ? properties.getDefaultModel() : model);
        body.put("active_model", activeModel);
        String resp = postJson(API_AGENTS, toJson(body));
        return parseAgentId(resp, agentId);
    }

    /**
     * 查询智能体列表
     */
    public List<Map<String, Object>> listAgents() {
        ResponseEntity<String> resp = get(API_AGENTS);
        // 返回 [{id, name, ...}] 或 {agents: [...]}
        try {
            Object value = objectMapper.readValue(resp.getBody(), Object.class);
            if (value instanceof List) {
                return toMapList(value);
            }
            if (value instanceof Map) {
                Object agents = ((Map<?, ?>) value).get("agents");
                if (agents instanceof List) {
                    return toMapList(agents);
                }
            }
        } catch (Exception e) {
            log.warn("[listAgents] 响应解析失败，响应={}", resp.getBody());
        }
        return new ArrayList<>();
    }

    /**
     * 启停智能体
     */
    public void toggleAgent(String agentId, boolean enabled) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", enabled);
        patchJson(API_AGENTS + "/" + encode(agentId) + "/toggle", toJson(body));
    }

    /**
     * 更新智能体配置（部分更新：仅更新传入的字段）
     *
     * <p>对应 QwenPaw {@code PUT /api/agents/{agentId}}。QwenPaw 使用
     * {@code exclude_unset=True} 做部分更新，只有显式传入的字段才会覆盖。
     *
     * @param agentId QwenPaw 智能体 ID
     * @param fields  要更新的字段（name、description、active_model 等）
     */
    public Map<String, Object> updateAgent(String agentId, Map<String, Object> fields) {
        String resp = putJson(API_AGENTS + "/" + encode(agentId), toJson(fields));
        return parseJsonObject(resp);
    }

    /**
     * 删除智能体
     */
    public void deleteAgent(String agentId) {
        delete(API_AGENTS + "/" + encode(agentId));
    }

    // ==================== 内置工具管理（per-agent 作用域） ====================

    /**
     * 列出智能体的内置工具及启用状态
     *
     * <p>对应 QwenPaw {@code GET /api/agents/{agentId}/tools}，返回 ToolInfo 数组：
     * name / enabled / description / async_execution / icon / requires_config / config_fields / config_values
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listTools(String agentId) {
        String path = API_AGENTS + "/" + encode(agentId) + API_TOOLS;
        ResponseEntity<String> resp = get(path);
        try {
            Object value = objectMapper.readValue(resp.getBody(), Object.class);
            if (value instanceof List) {
                return toMapList(value);
            }
        } catch (Exception e) {
            log.warn("[listTools] 响应解析失败，agentId={}, 响应={}", agentId, resp.getBody());
        }
        return new ArrayList<>();
    }

    /**
     * 切换内置工具启用状态（QwenPaw 端自动翻转 enabled）
     *
     * <p>对应 QwenPaw {@code PATCH /api/agents/{agentId}/tools/{toolName}/toggle}
     */
    public Map<String, Object> toggleTool(String agentId, String toolName) {
        String path = API_AGENTS + "/" + encode(agentId) + API_TOOLS + "/"
                + encode(toolName) + "/toggle";
        String resp = patchJson(path, "{}");
        return parseJsonObject(resp);
    }

    /**
     * 获取内置工具配置（敏感字段已掩码）
     *
     * <p>对应 QwenPaw {@code GET /api/agents/{agentId}/tools/{toolName}/config}
     */
    public Map<String, Object> getToolConfig(String agentId, String toolName) {
        String path = API_AGENTS + "/" + encode(agentId) + API_TOOLS + "/"
                + encode(toolName) + "/config";
        ResponseEntity<String> resp = get(path);
        return parseJsonObject(resp.getBody());
    }

    /**
     * 更新内置工具配置
     *
     * <p>对应 QwenPaw {@code POST /api/agents/{agentId}/tools/{toolName}/config}
     */
    public Map<String, Object> updateToolConfig(String agentId, String toolName, Map<String, Object> config) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("config", config);
        String path = API_AGENTS + "/" + encode(agentId) + API_TOOLS + "/"
                + encode(toolName) + "/config";
        String resp = postJson(path, toJson(body));
        return parseJsonObject(resp);
    }

    // ==================== 用量 / 状态 / 主动消息（业务补充） ====================

    /**
     * 查询 Token 用量汇总（全局维度，按日期/模型/provider 聚合）
     *
     * <p>对应 QwenPaw {@code GET /api/token-usage}。
     *
     * @param startDate 起始日期 YYYY-MM-DD，可为 null
     * @param endDate   结束日期 YYYY-MM-DD，可为 null
     * @param model     按模型名过滤，可为 null
     * @param provider  按 provider 过滤，可为 null
     */
    public Map<String, Object> getTokenUsage(String startDate, String endDate, String model, String provider) {
        String path = "/api/token-usage" + buildQuery(startDate, endDate, model, provider);
        return parseJsonObject(get(path).getBody());
    }

    /**
     * 查询 Token 用量明细（全局维度，按日期×provider×model 一行）
     *
     * <p>对应 QwenPaw {@code GET /api/token-usage/details}，返回数组。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTokenUsageDetails(String startDate, String endDate, String model, String provider) {
        String path = "/api/token-usage/details" + buildQuery(startDate, endDate, model, provider);
        ResponseEntity<String> resp = get(path);
        try {
            Object value = objectMapper.readValue(resp.getBody(), Object.class);
            if (value instanceof List) {
                return toMapList(value);
            }
        } catch (Exception e) {
            log.warn("[getTokenUsageDetails] 响应解析失败，响应={}", resp.getBody());
        }
        return new ArrayList<>();
    }

    /**
     * 查询智能体运行状态
     *
     * <p>对应 QwenPaw {@code GET /api/agents/{agentId}/agent-status}，
     * 返回 status(idle/running/disabled) / running_task_count / last_run_at / last_finish_at。
     */
    public Map<String, Object> getAgentStatus(String agentId) {
        String path = API_AGENTS + "/" + encode(agentId) + "/agent-status";
        return parseJsonObject(get(path).getBody());
    }

    /**
     * 列出智能体在 QwenPaw 侧实际注册的 MCP clients
     *
     * <p>对应 QwenPaw {@code GET /api/agents/{agentId}/mcp}，返回 MCPClientInfo 数组。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listAgentMcps(String agentId) {
        String path = API_AGENTS + "/" + encode(agentId) + API_MCP;
        ResponseEntity<String> resp = get(path);
        try {
            Object value = objectMapper.readValue(resp.getBody(), Object.class);
            if (value instanceof List) {
                return toMapList(value);
            }
        } catch (Exception e) {
            log.warn("[listAgentMcps] 响应解析失败，agentId={}, 响应={}", agentId, resp.getBody());
        }
        return new ArrayList<>();
    }

    /**
     * 切换智能体在 QwenPaw 侧某个 MCP client 的启用状态
     *
     * <p>对应 QwenPaw {@code PATCH /api/agents/{agentId}/mcp/toggle/{clientKey}}，
     * 返回更新后的 MCPClientInfo。
     */
    public Map<String, Object> toggleAgentMcp(String agentId, String clientKey) {
        String path = API_AGENTS + "/" + encode(agentId) + API_MCP + "/toggle/" + encode(clientKey);
        String resp = patchJson(path, "{}");
        return parseJsonObject(resp);
    }

    /**
     * 判断智能体在 QwenPaw 侧是否已注册指定 client_key 的 MCP
     *
     * <p>对应 QwenPaw {@code GET /api/agents/{agentId}/mcp} 列表查询，匹配 client_key。
     * 仅用于"启停 MCP 启用"等场景的"client 是否存在"兜底校验，失败时返回 false 让上层走重建路径。
     */
    public boolean existsAgentMcp(String agentId, String clientKey) {
        if (agentId == null || agentId.isEmpty() || clientKey == null || clientKey.isEmpty()) {
            return false;
        }
        try {
            List<Map<String, Object>> mcps = listAgentMcps(agentId);
            for (Map<String, Object> mcp : mcps) {
                Object key = mcp.get("client_key");
                if (key == null) {
                    key = mcp.get("clientKey");
                }
                if (key != null && clientKey.equals(String.valueOf(key))) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("[existsAgentMcp] 查询失败，agentId={}, clientKey={}", agentId, clientKey, e);
        }
        return false;
    }

    /**
     * 列出智能体某个 MCP client 的可用工具（含白名单启用状态）
     *
     * <p>对应 QwenPaw {@code GET /api/agents/{agentId}/mcp/tools/{clientKey}}，
     * 返回 MCPToolInfo 数组。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listMcpTools(String agentId, String clientKey) {
        String path = API_AGENTS + "/" + encode(agentId) + API_MCP + "/tools/" + encode(clientKey);
        // MCP 为懒连接，首个请求往往处于 connecting 态，短暂重试几次以等待连接完成
        int maxAttempts = 5;
        for (int attempt = 1; ; attempt++) {
            try {
                ResponseEntity<String> resp = get(path);
                try {
                    Object value = objectMapper.readValue(resp.getBody(), Object.class);
                    if (value instanceof List) {
                        return toMapList(value);
                    }
                } catch (Exception e) {
                    log.warn("[listMcpTools] 响应解析失败，agentId={}, clientKey={}, 响应={}",
                            agentId, clientKey, resp.getBody());
                }
                return new ArrayList<>();
            } catch (HttpStatusCodeException e) {
                // QwenPaw 侧 MCP 仍在连接中 -> 短暂等待后续试；非 5xx 保留下游异常原义
                if (!e.getStatusCode().is5xxServerError()) {
                    throw e;
                }
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ServiceException(QWENPAW_MCP_CONNECTING);
                    }
                    continue;
                }
                throw new ServiceException(QWENPAW_MCP_CONNECTING);
            }
        }
    }

    /**
     * 列出智能体工作区的 Skills（QwenPaw 侧实际安装）
     *
     * <p>对应 QwenPaw {@code GET /api/agents/{agentId}/skills}，返回 SkillInfo 数组。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listAgentSkills(String agentId) {
        String path = API_AGENTS + "/" + encode(agentId) + API_SKILLS;
        ResponseEntity<String> resp = get(path);
        try {
            Object value = objectMapper.readValue(resp.getBody(), Object.class);
            if (value instanceof List) {
                return toMapList(value);
            }
        } catch (Exception e) {
            log.warn("[listAgentSkills] 响应解析失败，agentId={}, 响应={}", agentId, resp.getBody());
        }
        return new ArrayList<>();
    }

    /**
     * 主动向渠道发送消息
     *
     * <p>对应 QwenPaw {@code POST /api/messages/send}，通过 X-Agent-Id 请求头指定 agent。
     */
    public Map<String, Object> sendMessage(String agentId, String channel, String targetUser,
                                           String targetSession, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("channel", channel);
        body.put("target_user", targetUser);
        body.put("target_session", targetSession);
        body.put("text", text);

        HttpHeaders headers = buildHeaders();
        if (agentId != null && !agentId.isEmpty()) {
            headers.set("X-Agent-Id", agentId);
        }
        HttpEntity<String> entity = new HttpEntity<>(toJson(body), headers);
        String resp = exchange("/api/messages/send", HttpMethod.POST, entity);
        return parseJsonObject(resp);
    }

    /**
     * 停止智能体正在运行的对话
     *
     * <p>对应 QwenPaw {@code POST /api/agents/{agentId}/console/chat/stop?chat_id=xxx}，
     * 返回 {"stopped": bool}。
     */
    public Map<String, Object> stopChat(String agentId, String chatId) {
        String path = API_AGENTS + "/" + encode(agentId) + API_CONSOLE_CHAT_STOP
                + "?chat_id=" + encode(chatId);
        String resp = postJson(path, "{}");
        return parseJsonObject(resp);
    }

    /**
     * 拼接 token-usage 的查询参数
     */
    private String buildQuery(String startDate, String endDate, String model, String provider) {
        StringBuilder sb = new StringBuilder();
        if (startDate != null && !startDate.isEmpty()) {
            sb.append("&start_date=").append(encode(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            sb.append("&end_date=").append(encode(endDate));
        }
        if (model != null && !model.isEmpty()) {
            sb.append("&model=").append(encode(model));
        }
        if (provider != null && !provider.isEmpty()) {
            sb.append("&provider=").append(encode(provider));
        }
        return sb.length() == 0 ? "" : sb.toString().replaceFirst("&", "?");
    }

    // ==================== MCP 管理 ====================

    /**
     * 注册 MCP client 到指定智能体
     *
     * <p>QwenPaw 期望的请求体结构：{@code {client_key, client: {name, transport, url, command, ...}}}
     *
     * @param name           MCP 客户端显示名称（必填）
     * @param transport      stdio / streamable_http / sse
     * @param url            远程地址（streamable_http/sse 必填）
     * @param command        stdio 启动命令
     * @param commandArgs    stdio 启动参数（JSON 数组字符串，可为 null）
     * @param headersJson    JSON 对象，远程鉴权头
     * @param toolsJson      JSON 数组，工具白名单（null 表示全部）
     */
    public void registerMcp(String agentId, String clientKey, String name, String transport, String url,
                            String command, String commandArgs, String headersJson, String toolsJson) {
        Map<String, Object> client = new LinkedHashMap<>();
        client.put("name", name != null ? name : clientKey);
        client.put("transport", transport != null ? transport : "stdio");
        if (url != null && !url.isEmpty()) {
            client.put("url", url);
        }
        if (command != null && !command.isEmpty()) {
            client.put("command", command);
        }
        if (commandArgs != null && !commandArgs.isEmpty()) {
            client.put("args", parseJsonArray(commandArgs));
        }
        if (headersJson != null && !headersJson.isEmpty()) {
            client.put("headers", parseJsonObject(headersJson));
        }
        if (toolsJson != null && !toolsJson.isEmpty()) {
            client.put("tools", parseJsonArray(toolsJson));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("client_key", clientKey);
        body.put("client", client);
        postJson(API_AGENTS + "/" + encode(agentId) + "/mcp", toJson(body));
    }

    /**
     * 删除 MCP client
     */
    public void deleteMcp(String agentId, String clientKey) {
        delete(API_AGENTS + "/" + encode(agentId) + "/mcp/" + encode(clientKey));
    }

    // ==================== Skills 管理 ====================

    /**
     * 安装技能（创建/绑定到 agent，POST /api/agents/{agentId}/skills）
     *
     * <p>QwenPaw 要求 body 必须包含 {@code content}（技能 Markdown 内容），
     * 启用字段为 {@code enable}。此方法优先从技能池详情获取 content，
     * 若获取失败则跳过（content 缺失时 QwenPaw 会返回 422）。
     */
    public void installSkill(String agentId, String skillName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", skillName);
        // 从技能池详情获取 content（接口必填字段）
        try {
            Map<String, Object> detail = getSkillPoolDetail(skillName);
            Object content = detail.get("content");
            if (content != null && !content.toString().isEmpty()) {
                body.put("content", content.toString());
            } else {
                log.warn("[installSkill] 技能池详情缺少 content，跳过安装，skillName={}", skillName);
                return;
            }
        } catch (Exception e) {
            log.warn("[installSkill] 获取技能池详情失败，跳过安装，skillName={}", skillName, e);
            return;
        }
        body.put("enable", true);
        postJson(API_AGENTS + "/" + encode(agentId) + "/skills", toJson(body));
    }

    /**
     * 启停技能
     */
    public void toggleSkill(String agentId, String skillName, boolean enabled) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", skillName);
        body.put("enabled", enabled);
        patchJson(API_AGENTS + "/" + encode(agentId) + "/skills/" + encode(skillName), toJson(body));
    }

    /**
     * 列出 QwenPaw 全局技能池（内置 + 自定义技能）
     *
     * <p>对应 QwenPaw {@code GET /api/skills/pool}，返回 PoolSkillSpec 数组。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listSkillPool() {
        ResponseEntity<String> resp = get(API_SKILLS_POOL);
        try {
            Object value = objectMapper.readValue(resp.getBody(), Object.class);
            if (value instanceof List) {
                return toMapList(value);
            }
            if (value instanceof Map) {
                Object skills = ((Map<?, ?>) value).get("skills");
                if (skills instanceof List) {
                    return toMapList(skills);
                }
            }
        } catch (Exception e) {
            log.warn("[listSkillPool] 响应解析失败，响应={}", resp.getBody());
        }
        return new ArrayList<>();
    }

    /**
     * 获取技能池中某个技能的详情（含 YAML 定义）
     *
     * <p>对应 QwenPaw {@code GET /api/skills/pool/{skillName}}
     */
    public Map<String, Object> getSkillPoolDetail(String skillName) {
        String path = API_SKILLS_POOL + "/" + encode(skillName);
        return parseJsonObject(get(path).getBody());
    }

    /**
     * 上传 zip 到技能池（直接上传到全局池）
     *
     * <p>对应 QwenPaw {@code POST /api/skills/pool/upload-zip}，multipart 上传。
     *
     * @param data       zip 文件字节
     * @param fileName   文件名
     * @param targetName 目标技能名（可为 null，由 QwenPaw 自动推断）
     * @return 上传结果（imported/count/conflicts）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadToPoolZip(byte[] data, String fileName, String targetName) {
        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // 构建 multipart body
        MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
        ByteArrayResource resource = new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return fileName != null ? fileName : "skill.zip";
            }
        };
        multipartBody.add("file", resource);

        String path = API_SKILLS_POOL + "/upload-zip";
        if (targetName != null && !targetName.isEmpty()) {
            path += "?target_name=" + encode(targetName);
        }

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(multipartBody, headers);
        String resp = exchange(path, HttpMethod.POST, entity);
        return parseJsonObject(resp);
    }

    /**
     * 删除技能池中的技能
     *
     * <p>对应 QwenPaw {@code DELETE /api/skills/pool/{skillName}}
     */
    public void deletePoolSkill(String skillName) {
        delete(API_SKILLS_POOL + "/" + encode(skillName));
    }

    /**
     * 从技能池下载技能到指定智能体工作区
     *
     * <p>对应 QwenPaw {@code POST /api/skills/pool/download}。
     *
     * @param skillName     技能池中的技能名
     * @param workspaceId   目标工作区 ID（即 QwenPaw agent_id）
     * @param overwrite     是否覆盖已有技能
     * @return 下载结果
     */
    public Map<String, Object> downloadPoolSkillToWorkspace(String skillName, String workspaceId, boolean overwrite) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("workspace_id", workspaceId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("skill_name", skillName);
        body.put("targets", java.util.Collections.singletonList(target));
        body.put("overwrite", overwrite);

        String resp = postJson(API_SKILLS_POOL + "/download", toJson(body));
        return parseJsonObject(resp);
    }

    /**
     * 卸载智能体上安装的技能
     *
     * <p>对应 QwenPaw {@code DELETE /api/agents/{agentId}/skills/{skillName}}
     */
    public void uninstallSkill(String agentId, String skillName) {
        delete(API_AGENTS + "/" + encode(agentId) + API_SKILLS + "/" + encode(skillName));
    }

    // ==================== 模型/Provider 管理 ====================

    /**
     * 列出所有 Provider（含其下模型列表）
     *
     * @return 原始 JSON 解析后的 List，每个元素为一个 Provider 信息
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listProviders() {
        ResponseEntity<String> resp = get(API_MODELS);
        try {
            Object value = objectMapper.readValue(resp.getBody(), Object.class);
            if (value instanceof List) {
                return toMapList(value);
            }
            if (value instanceof Map) {
                // 兼容 {providers: [...]} 包装
                Object providers = ((Map<?, ?>) value).get("providers");
                if (providers instanceof List) {
                    return toMapList(providers);
                }
            }
        } catch (Exception e) {
            log.warn("[listProviders] 响应解析失败，响应={}", resp.getBody());
        }
        return new ArrayList<>();
    }

    /**
     * 配置 Provider（API key、base_url 等）
     */
    public Map<String, Object> configureProvider(String providerId, Map<String, Object> config) {
        String resp = putJson(API_MODELS + "/" + encode(providerId) + "/config", toJson(config));
        return parseJsonObject(resp);
    }

    /**
     * 测试 Provider 连接
     */
    public Map<String, Object> testProvider(String providerId) {
        String resp = postJson(API_MODELS + "/" + encode(providerId) + "/test", "{}");
        return parseJsonObject(resp);
    }

    /**
     * 测试特定模型连接
     */
    public Map<String, Object> testModel(String providerId, String modelId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model_id", modelId);
        String resp = postJson(API_MODELS + "/" + encode(providerId) + "/models/test", toJson(body));
        return parseJsonObject(resp);
    }

    /**
     * 从 Provider API 发现可用模型
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> discoverModels(String providerId) {
        String resp = postJson(API_MODELS + "/" + encode(providerId) + "/discover", "{}");
        try {
            Object value = objectMapper.readValue(resp, Object.class);
            if (value instanceof List) {
                return toMapList(value);
            }
            if (value instanceof Map) {
                Object models = ((Map<?, ?>) value).get("models");
                if (models instanceof List) {
                    return toMapList(models);
                }
            }
        } catch (Exception e) {
            log.warn("[discoverModels] 响应解析失败，响应={}", resp);
        }
        return new ArrayList<>();
    }

    /**
     * 向 Provider 添加模型
     */
    public Map<String, Object> addModel(String providerId, Map<String, Object> modelInfo) {
        String resp = postJson(API_MODELS + "/" + encode(providerId) + "/models", toJson(modelInfo));
        return parseJsonObject(resp);
    }

    /**
     * 从 Provider 删除模型
     */
    public void deleteModel(String providerId, String modelId) {
        delete(API_MODELS + "/" + encode(providerId) + "/models/" + encode(modelId));
    }

    /**
     * 配置模型参数（max_tokens、temperature 等）
     */
    public Map<String, Object> configureModel(String providerId, String modelId, Map<String, Object> config) {
        String resp = putJson(API_MODELS + "/" + encode(providerId) + "/models/" + encode(modelId) + "/config", toJson(config));
        return parseJsonObject(resp);
    }

    /**
     * 获取当前激活的模型
     *
     * @param scope   effective / global / agent
     * @param agentId 当 scope=agent 时必填
     */
    public Map<String, Object> getActiveModel(String scope, String agentId) {
        String path = API_MODELS + "/active?scope=" + encode(scope != null ? scope : "effective");
        if (agentId != null && !agentId.isEmpty()) {
            path += "&agent_id=" + encode(agentId);
        }
        ResponseEntity<String> resp = get(path);
        return parseJsonObject(resp.getBody());
    }

    /**
     * 设置激活模型
     *
     * @param scope     global / agent
     * @param providerId provider id
     * @param model     模型 id
     * @param agentId   当 scope=agent 时必填
     */
    public Map<String, Object> setActiveModel(String scope, String providerId, String model, String agentId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("scope", scope);
        body.put("provider_id", providerId);
        body.put("model", model);
        if (agentId != null && !agentId.isEmpty()) {
            body.put("agent_id", agentId);
        }
        String resp = putJson(API_MODELS + "/active", toJson(body));
        return parseJsonObject(resp);
    }

    /**
     * 创建自定义 Provider
     */
    public Map<String, Object> createCustomProvider(Map<String, Object> providerInfo) {
        String resp = postJson(API_MODELS + "/custom-providers", toJson(providerInfo));
        return parseJsonObject(resp);
    }

    /**
     * 删除自定义 Provider
     */
    public void deleteCustomProvider(String providerId) {
        delete(API_MODELS + "/custom-providers/" + encode(providerId));
    }

    // ==================== 会话管理 ====================
    // 注：QwenPaw 是会话/消息的 source of truth。Java 端只做按智能体的代理透传，
    //     不再维护本地 session/message 表，也不再做 user_id 过滤。
    //     前端 URL 用 agentId + chatId 唯一定位一个会话。

    /**
     * 列出指定智能体的所有会话
     *
     * @param agentId QwenPaw 智能体 ID
     * @return 会话列表，每个元素包含 id、name、meta 等字段
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listChatsForAgent(String agentId) {
        String path = API_AGENTS + "/" + encode(agentId) + API_CHATS;
        ResponseEntity<String> resp = get(path);
        try {
            Object value = objectMapper.readValue(resp.getBody(), Object.class);
            if (value instanceof List) {
                return toMapList(value);
            }
            if (value instanceof Map) {
                Object chats = ((Map<?, ?>) value).get("chats");
                if (chats instanceof List) {
                    return toMapList(chats);
                }
            }
        } catch (Exception e) {
            log.warn("[listChatsForAgent] 响应解析失败，agentId={}, 响应={}", agentId, resp.getBody());
        }
        return new ArrayList<>();
    }

    /**
     * 在 QwenPaw 预创建会话，返回 chatId（UUID）
     *
     * <p>Java 端不再持久化任何会话字段；前端拿到 chatId 后直接作为 URL 参数。
     *
     * <p>三元组 <b>(session_id, user_id, channel)</b> 会写入 ChatSpec 并被 QwenPaw 用于
     * <code>get_or_create_chat</code> 查重。预创建和后续发问必须传入完全相同的三元组，
     * QwenPaw 才能命中预创建的 chat、复用 chat.id 作为 <code>task_tracker.run_key</code> 续接。
     * session_id 由 Java 端自动生成 UUID，user_id 取芋道用户名，channel 固定为 "console"。
     *
     * @param agentId  QwenPaw 智能体 ID
     * @param userId   芋道用户名（字符串），用于三元组，<b>不参与权限过滤</b>
     * @param name     会话名（首次可传 "新对话"）
     * @return QwenPaw ChatSpec 字典（含 id/name/session_id/user_id/channel/...）
     */
    public Map<String, Object> createChat(String agentId, String userId, String name) {
        String path = API_AGENTS + "/" + encode(agentId) + API_CHATS;
        String sessionId = UUID.randomUUID().toString();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("session_id", sessionId);
        body.put("user_id", userId);
        body.put("name", name == null || name.isEmpty() ? "新对话" : name);
        body.put("channel", "console");
        try {
            String json = objectMapper.writeValueAsString(body);
            String respBody = postJson(path, json);
            Object value = objectMapper.readValue(respBody, Object.class);
            if (value instanceof Map) {
                return (Map<String, Object>) value;
            }
        } catch (Exception e) {
            log.warn("[createChat] 创建 QwenPaw 会话失败，agentId={}, userId={}", agentId, userId, e);
        }
        return new LinkedHashMap<>();
    }

    /**
     * 删除 QwenPaw 会话
     *
     * @param agentId QwenPaw 智能体 ID（用于路由到正确的 agent 作用域）
     * @param chatId  QwenPaw 会话 ID
     */
    public void deleteChat(String agentId, String chatId) {
        delete(API_AGENTS + "/" + encode(agentId) + API_CHATS + "/" + encode(chatId));
    }

    /**
     * 获取 QwenPaw 会话详情（包含完整消息历史）
     *
     * @param agentId QwenPaw 智能体 ID
     * @param chatId  QwenPaw 会话 ID
     * @return 会话详情，包含 messages 数组
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getChat(String agentId, String chatId) {
        String path = API_AGENTS + "/" + encode(agentId) + API_CHATS + "/" + encode(chatId);
        ResponseEntity<String> resp = get(path);
        try {
            Object value = objectMapper.readValue(resp.getBody(), Object.class);
            if (value instanceof Map) {
                return (Map<String, Object>) value;
            }
        } catch (Exception e) {
            log.warn("[getChat] 响应解析失败，agentId={}, chatId={}, 响应={}", agentId, chatId, resp.getBody());
        }
        return new LinkedHashMap<>();
    }

    /**
     * 重命名 QwenPaw 会话
     *
     * <p>对应 QwenPaw {@code PUT /api/agents/{agentId}/chats/{chatId}}，
     * payload 携带 {@code name}（ChatUpdate 的可变字段）。
     *
     * @param agentId QwenPaw 智能体 ID
     * @param chatId  QwenPaw 会话 ID
     * @param name    新的会话名称
     * @return 更新后的会话 spec
     */
    public Map<String, Object> updateChatName(String agentId, String chatId, String name) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        String path = API_AGENTS + "/" + encode(agentId) + API_CHATS + "/" + encode(chatId);
        String resp = putJson(path, toJson(body));
        return parseJsonObject(resp);
    }

    // ==================== 对话 ====================
    // 对话请求必须携带三元组 (session_id, user_id, channel)，与 createChat 时一致，
    // QwenPaw 才能通过 get_or_create_chat 命中已有会话，避免重复创建。

    /**
     * 同步对话（仅供调试，SSE 流式版本为主）
     *
     * @param chatId    QwenPaw chat id（UUID），首次可传 null/空 让 QwenPaw 自动开新会话
     * @param userId    芋道用户 ID（字符串），必须与预创建时一致
     * @param sessionId 会话唯一标识（UUID），必须与 {@link #createChat} 时一致
     */
    public String chat(String agentId, String chatId, String userId, String sessionId, String message) {
        Map<String, Object> textContent = new LinkedHashMap<>();
        textContent.put("type", "text");
        textContent.put("text", message);

        Map<String, Object> inputMessage = new LinkedHashMap<>();
        inputMessage.put("role", "user");
        inputMessage.put("content", java.util.Collections.singletonList(textContent));
        inputMessage.put("type", "message");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("input", java.util.Collections.singletonList(inputMessage));
        body.put("session_id", sessionId);
        body.put("user_id", userId);
        body.put("channel", "console");
        if (chatId != null && !chatId.isEmpty()) {
            body.put("chat_id", chatId);
        }
        body.put("stream", false);
        String resp = postJson(API_AGENTS + "/" + encode(agentId) + "/console/chat", toJson(body));
        return extractAnswer(resp);
    }

    /**
     * SSE 流式对话
     *
     * <p>向 /console/chat 发起流式请求，逐行读取 SSE 的 data 块，通过 {@code onChunk} 回调转发。
     * 每收到一个 data 行回调一次，收到 {@code [DONE]} 结束。
     *
     * <p>三元组 <b>(session_id, user_id, channel)</b> 必须与 {@link #createChat} 时一致，
     * QwenPaw 才能通过 <code>get_or_create_chat</code> 命中预创建的 chat，
     * 再用 <code>chat.id</code> 作为 <code>task_tracker.run_key</code> 续接正在运行的流。
     *
     * @param chatId  QwenPaw chat id（UUID），首次可传 null/空 让 QwenPaw 自动开新会话
     * @param userId  芋道用户 ID（字符串），必须与预创建时一致
     * @param sessionId 会话唯一标识（UUID），必须与 {@link #createChat} 时一致
     * @param onChunk data 增量回调（原始 data 行内容，未做 JSON 解析）
     */
    public void chatStream(String agentId, String chatId, String userId, String sessionId, String message, Consumer<String> onChunk) {
        List<Map<String, Object>> contentItems = new ArrayList<>();
        Map<String, Object> textContent = new LinkedHashMap<>();
        textContent.put("type", "text");
        textContent.put("text", message);
        contentItems.add(textContent);
        chatStream(agentId, chatId, userId, sessionId, contentItems, onChunk);
    }

    /**
     * SSE 流式对话（支持附件 content 数组）
     *
     * <p>与 {@link #chatStream(String, String, String, String, String, Consumer)} 等价，
     * 但允许自定义 content 项数组（如 {@code [{type:"text",text:"..."}, {type:"image",image_url:"..."}]}），
     * 对应 QwenPaw 官网上传文件后发送的 content 结构，用于"上传文件对话"。
     *
     * @param contentItems 用户消息的 content 项数组（text/image/file/video/audio 等）
     */
    public void chatStream(String agentId, String chatId, String userId, String sessionId,
                           List<Map<String, Object>> contentItems, Consumer<String> onChunk) {
        Map<String, Object> inputMessage = new LinkedHashMap<>();
        inputMessage.put("role", "user");
        inputMessage.put("content", contentItems == null || contentItems.isEmpty()
                ? java.util.Collections.singletonList(new LinkedHashMap<String, Object>() {{
                    put("type", "text");
                    put("text", "");
                }})
                : contentItems);
        inputMessage.put("type", "message");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("input", java.util.Collections.singletonList(inputMessage));
        body.put("session_id", sessionId);
        body.put("user_id", userId);
        body.put("channel", "console");
        if (chatId != null && !chatId.isEmpty()) {
            body.put("chat_id", chatId);
        }
        body.put("stream", true);
        executeSse(agentId, body, onChunk, "chatStream");
    }

    /**
     * 以 reconnect 方式重新挂载到正在运行的 QwenPaw 流
     *
     * <p>对应 QwenPaw {@code POST /api/agents/{agentId}/console/chat} 接收 {@code reconnect: true} 请求，
     * 服务端会用 {@code (session_id, user_id, channel)} 找到预创建的 chat，再 attach 到 running 流。
     * 流结束后会返回空 SSE，前端需要 fallback 到 {@code getChat(agentId, chatId)} 拉历史 messages。
     *
     * <p>三元组 <b>(session_id, user_id, channel)</b> 必须与首次发问时一致。
     *
     * @param agentId QwenPaw 智能体 ID
     * @param chatId  QwenPaw 会话 ID（UUID）
     * @param userId  芋道用户 ID（字符串）
     * @param sessionId 会话唯一标识（UUID），必须与 {@link #createChat} 时一致
     * @param onChunk data 增量回调（与 chatStream 同样的协议）
     */
    public void reconnectStream(String agentId, String chatId, String userId, String sessionId, Consumer<String> onChunk) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reconnect", true);
        body.put("chat_id", chatId);
        body.put("session_id", sessionId);
        body.put("user_id", userId);
        body.put("input", java.util.Collections.emptyList());
        body.put("channel", "console");
        body.put("stream", true);
        executeSse(agentId, body, onChunk, "reconnectStream");
    }

    /**
     * 共享的 SSE 请求/读取逻辑（chatStream 与 reconnectStream 复用）
     */
    private void executeSse(String agentId, Map<String, Object> body, Consumer<String> onChunk, String op) {
        HttpHeaders headers = buildHeaders();
        headers.setAccept(java.util.Collections.singletonList(MediaType.TEXT_EVENT_STREAM));
        String path = API_AGENTS + "/" + encode(agentId) + "/console/chat";
        try {
            restTemplate.execute(url(path), HttpMethod.POST,
                    request -> {
                        request.getHeaders().putAll(headers);
                        try {
                            request.getBody().write(toJson(body).getBytes(StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            throw new RuntimeException("QwenPaw SSE 请求体写入失败", e);
                        }
                    },
                    response -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data:")) {
                                    continue;
                                }
                                String data = line.substring(5).trim();
                                if (data.isEmpty() || "[DONE]".equals(data)) {
                                    continue;
                                }
                                onChunk.accept(data);
                            }
                        }
                        return null;
                    });
        } catch (HttpStatusCodeException e) {
            log.warn("[{}] QwenPaw 返回非 2xx，path={}, code={}, body={}",
                    op, path, e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new ServiceException(QWENPAW_CONNECT_FAILED);
        } catch (ResourceAccessException e) {
            log.warn("[{}] QwenPaw 连接失败，path={}", op, path, e);
            throw new ServiceException(QWENPAW_CONNECT_FAILED);
        }
    }

    // ==================== 聊天文件上传 / 预览 ====================

    /**
     * 上传文件到智能体（用于"上传文件对话"）
     *
     * <p>对应 QwenPaw {@code POST /api/console/upload}，multipart 字段 {@code file}。
     * QwenPaw 保存到 console channel 的 media_dir，返回 {@code {url: 绝对路径, file_name, size}}。
     * 需携带 X-Agent-Id 请求头，QwenPaw {@code get_agent_for_request} 据此解析智能体工作区。
     *
     * @param agentId  QwenPaw 智能体 ID
     * @param data     文件字节
     * @param fileName 文件名
     * @return 上传结果（url/file_name/size），url 为文件在服务端的绝对路径
     */
    public Map<String, Object> uploadChatFile(String agentId, byte[] data, String fileName) {
        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (agentId != null && !agentId.isEmpty()) {
            headers.set("X-Agent-Id", agentId);
        }

        MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
        ByteArrayResource resource = new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return fileName != null && !fileName.isEmpty() ? fileName : "file";
            }
        };
        multipartBody.add("file", resource);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(multipartBody, headers);
        String resp = exchange(API_CONSOLE_UPLOAD, HttpMethod.POST, entity);
        return parseJsonObject(resp);
    }

    /**
     * 获取聊天文件预览（转发文件字节流）
     *
     * <p>对应 QwenPaw {@code GET /api/files/preview/{filepath}}，filepath 为服务端绝对路径
     * （上传接口返回的 url），按路径分段 URL 编码（保留 '/'）。QwenPaw 返回 FileResponse 文件字节流。
     * 需携带 X-Agent-Id 请求头。
     *
     * @param agentId QwenPaw 智能体 ID
     * @param path    服务端文件绝对路径（上传接口返回的 url）
     * @return 文件字节
     */
    public byte[] previewFile(String agentId, String path) {
        String encoded = encodePath(path);
        HttpHeaders headers = buildHeaders();
        if (agentId != null && !agentId.isEmpty()) {
            headers.set("X-Agent-Id", agentId);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<byte[]> resp = restTemplate.exchange(
                    url(API_FILES_PREVIEW + "/" + encoded), HttpMethod.GET, entity, byte[].class);
            return resp.getBody() == null ? new byte[0] : resp.getBody();
        } catch (HttpStatusCodeException e) {
            log.warn("[previewFile] QwenPaw 返回非 2xx，path={}, code={}", path, e.getStatusCode().value());
            throw new ServiceException(QWENPAW_CONNECT_FAILED);
        } catch (ResourceAccessException e) {
            log.warn("[previewFile] QwenPaw 连接失败，path={}", path, e);
            throw new ServiceException(QWENPAW_CONNECT_FAILED);
        }
    }

    // ==================== 私有方法 ====================

    private String postJson(String path, String json) {
        HttpHeaders headers = buildHeaders();
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        return exchange(path, HttpMethod.POST, entity);
    }

    private String patchJson(String path, String json) {
        HttpHeaders headers = buildHeaders();
        // QwenPaw 后端基于 uvicorn，Spring 的 JdkClientHttpRequestFactory 会被 uvicorn 拒绝（400 Invalid HTTP request received），
        // 而 patchRestTemplate 已配置为 HttpComponentsClientHttpRequestFactory（基于 HttpClient5），uvicorn 兼容。
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (json != null && !json.isEmpty()) {
            byte[] bodyBytes = json.getBytes(StandardCharsets.UTF_8);
            headers.setContentLength(bodyBytes.length);
        }
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        try {
            ResponseEntity<String> response = patchRestTemplate.exchange(
                    url(path), HttpMethod.PATCH, entity, String.class);
            return response.getBody();
        } catch (ResourceAccessException | HttpStatusCodeException e) {
            log.warn("[patchJson] QwenPaw 连接失败，path={}, body={}", path, json, e);
            throw new ServiceException(QWENPAW_CONNECT_FAILED);
        }
    }

    private String putJson(String path, String json) {
        HttpHeaders headers = buildHeaders();
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        return exchange(path, HttpMethod.PUT, entity);
    }

    private ResponseEntity<String> get(String path) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        return restTemplate.exchange(url(path), HttpMethod.GET, entity, String.class);
    }

    private void delete(String path) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        restTemplate.exchange(url(path), HttpMethod.DELETE, entity, String.class);
    }

    private String exchange(String path, HttpMethod method, HttpEntity<?> entity) {
        try {
            ResponseEntity<String> resp = restTemplate.exchange(url(path), method, entity, String.class);
            return resp.getBody();
        } catch (HttpStatusCodeException e) {
            log.warn("[exchange] QwenPaw 返回非 2xx，path={}, code={}, body={}",
                    path, e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new ServiceException(QWENPAW_CONNECT_FAILED);
        } catch (ResourceAccessException e) {
            log.warn("[exchange] QwenPaw 连接失败，path={}", path, e);
            throw new ServiceException(QWENPAW_CONNECT_FAILED);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (Boolean.TRUE.equals(properties.getAuthEnabled())
                && properties.getAuthToken() != null && !properties.getAuthToken().isEmpty()) {
            headers.setBearerAuth(properties.getAuthToken());
        }
        return headers;
    }

    private String url(String path) {
        String base = properties.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    private String toJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }

    private String parseAgentId(String resp, String fallback) {
        if (resp == null || resp.isEmpty()) {
            return fallback;
        }
        try {
            Object value = objectMapper.readValue(resp, Object.class);
            if (value instanceof Map) {
                Object id = ((Map<?, ?>) value).get("id");
                if (id != null) {
                    return String.valueOf(id);
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private String extractAnswer(String resp) {
        if (resp == null || resp.isEmpty()) {
            return "";
        }
        try {
            Object value = objectMapper.readValue(resp, Object.class);
            if (value instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) value;
                Object answer = map.get("answer");
                if (answer == null) {
                    answer = map.get("response");
                }
                if (answer == null) {
                    answer = map.get("message");
                }
                if (answer instanceof Map) {
                    Object content = ((Map<?, ?>) answer).get("content");
                    if (content != null) {
                        return String.valueOf(content);
                    }
                }
                if (answer != null) {
                    return String.valueOf(answer);
                }
            }
        } catch (Exception ignored) {
        }
        return resp;
    }

    /**
     * 从 SSE 增量块中提取可展示文本
     *
     * <p>QwenPaw SSE 事件格式（object 字段区分类型）：
     * <ul>
     *     <li>内容增量块：{@code {"object":"content","type":"text","text":"实际文本","delta":true,...}}</li>
     *     <li>响应信封：{@code {"object":"response","status":"created|in_progress|completed",...}}</li>
     *     <li>消息信封：{@code {"object":"message","type":"message","content":[...],...}}</li>
     * </ul>
     *
     * @return 提取到的文本；非文本事件返回空串（跳过该块）
     */
    public String extractChunkText(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return "";
        }
        try {
            Object value = objectMapper.readValue(chunk, Object.class);
            if (value instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) value;
                String object = String.valueOf(map.get("object"));

                // 1. 内容增量块：object=content, type=text, text=实际文本
                if ("content".equals(object)) {
                    String type = String.valueOf(map.get("type"));
                    if ("text".equals(type)) {
                        Object text = map.get("text");
                        return text != null ? String.valueOf(text) : "";
                    }
                    return ""; // 非文本内容（image/audio/data 等）跳过
                }

                // 2. 消息信封：提取 content 数组中的文本
                if ("message".equals(object)) {
                    Object content = map.get("content");
                    if (content instanceof List) {
                        StringBuilder sb = new StringBuilder();
                        for (Object item : (List<?>) content) {
                            if (item instanceof Map) {
                                Object text = ((Map<?, ?>) item).get("text");
                                if (text != null) {
                                    sb.append(text);
                                }
                            }
                        }
                        return sb.toString();
                    }
                    return "";
                }

                // 3. 响应信封 / turn_usage 等非文本事件 → 跳过
                return "";
            }
            return "";
        } catch (Exception e) {
            // 非 JSON（如纯文本增量），原样返回
            return chunk;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toMapList(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map) {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
    }

    private Map<String, Object> parseJsonObject(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 解析失败: " + json, e);
        }
    }

    private List<String> parseJsonArray(String json) {
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 解析失败: " + json, e);
        }
    }

    private String encode(String value) {
        // 骨架用简单 URL 编码；复杂值建议使用 UriUtils.encodePathSegment
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * 按路径分段 URL 编码（保留 '/' 分隔符），用于 /files/preview/{filepath:path} 路径参数。
     *
     * <p>QwenPaw 服务端对 filepath 做 {@code unquote} 解码，因此每个分段需单独编码
     * （空格编码为 %20 而非 +），分隔符 '/' 原样保留。
     */
    private String encodePath(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        // Windows 绝对路径 C:\... 统一转成 C:/... 便于按段编码
        String v = value.replace("\\", "/");
        String[] segments = v.split("/", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                sb.append('/');
            }
            if (!segments[i].isEmpty()) {
                sb.append(encodePathSegment(segments[i]));
            }
        }
        return sb.toString();
    }

    private String encodePathSegment(String segment) {
        try {
            return java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return segment;
        }
    }
}
