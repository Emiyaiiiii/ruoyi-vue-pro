package cn.iocoder.yudao.module.agent.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/**
 * 每个用户"默认智能体"的创建模板配置。
 *
 * <p>当用户首次进入智能体/聊天页面、还没有任何 agent 时，
 * {@code AiAgentService#getOrCreateDefaultAgent} 会按此配置自动生成 1 条默认智能体，
 * 保证前端列表页永远非空，用户无需手动"新建智能体"就能开始对话。
 *
 * <p>前缀：{@code yudao.ai.agent.default}。与 qwenpaw 配置同属 {@code yudao.ai.*} 家族。
 *
 * @author 吴皓
 */
@ConfigurationProperties(prefix = "yudao.ai.agent.default")
@Data
public class AiAgentDefaultProperties {

    /**
     * 默认智能体名称模板，支持占位符 {@code ${nickname}} 动态替换为用户昵称。
     * 若用户昵称为空或查不到，则回退为纯文本（不做替换）。
     */
    private String name = "我的智能助手";

    /**
     * 默认智能体描述
     */
    private String description = "企业知识库问答助手：可以向我提问公司制度、产品文档、项目资料等内容。";

    /**
     * 默认系统提示词；为空时不额外写入，走 QwenPaw 激活模型的全局 prompt。
     */
    private String systemPrompt = "";

    /**
     * 是否默认启用知识库工具（既然是知识库平台，默认开启）。
     */
    private Boolean enableKbTool = Boolean.TRUE;

    /**
     * 默认模型供应商；为空时沿用 QwenPaw 侧"全局激活模型"（推荐留空，避免与配置面板不一致）。
     */
    private String modelProvider = "";

    /**
     * 默认模型名；为空时沿用 QwenPaw 全局默认（通常与 {@code QwenPawProperties#defaultModel} 保持一致或留空）。
     */
    private String modelName = "";

    /**
     * 默认初始技能列表（技能池中的技能标识名，按 name 精确匹配）。
     * 为空则不安装任何初始技能，用户后续按需手动安装。
     */
    private List<String> initialSkills = Collections.emptyList();

    /**
     * QwenPaw 注册失败时的降级策略：
     *   true = 仅落库 ai_agent（status=0 停用），下次进入页面再尝试注册 QwenPaw；
     *   false = 直接抛异常（AGENT_QWENPAW_CREATE_FAILED），阻塞前端进入页面。
     * 默认 true，保障"用户总能进聊天页"的体验。
     */
    private Boolean degradeOnQwenpawFailure = Boolean.TRUE;

}
