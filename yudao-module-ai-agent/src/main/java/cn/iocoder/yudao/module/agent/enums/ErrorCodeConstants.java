package cn.iocoder.yudao.module.agent.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * ai-agent 模块错误码枚举类
 *
 * ai-agent 系统，使用 1-020-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 智能体 1-020-001-000 ==========
    ErrorCode AGENT_NOT_EXISTS = new ErrorCode(1_020_001_000, "智能体不存在");
    ErrorCode AGENT_NAME_EXISTS = new ErrorCode(1_020_001_001, "已存在同名智能体");
    ErrorCode AGENT_QWENPAW_CREATE_FAILED = new ErrorCode(1_020_001_002, "QwenPaw 智能体创建失败");
    ErrorCode AGENT_QWENPAW_SYNC_FAILED = new ErrorCode(1_020_001_003, "QwenPaw 智能体同步失败");
    ErrorCode AGENT_ALREADY_ENABLED = new ErrorCode(1_020_001_004, "智能体已启用");
    ErrorCode AGENT_ALREADY_DISABLED = new ErrorCode(1_020_001_005, "智能体已停用");
    ErrorCode AGENT_PERMISSION_DENIED = new ErrorCode(1_020_001_006, "无权访问该智能体");

    // ========== MCP 商店 1-020-002-000 ==========
    ErrorCode MCP_META_NOT_EXISTS = new ErrorCode(1_020_002_000, "MCP 商店项不存在");
    ErrorCode MCP_META_CODE_EXISTS = new ErrorCode(1_020_002_001, "MCP 编码已存在");

    // ========== Skills 商店 1-020-003-000 ==========
    ErrorCode SKILL_META_NOT_EXISTS = new ErrorCode(1_020_003_000, "技能商店项不存在");
    ErrorCode SKILL_META_NAME_EXISTS = new ErrorCode(1_020_003_001, "技能名称已存在");
    ErrorCode SKILL_META_UPLOAD_FAILED = new ErrorCode(1_020_003_002, "技能上传到 QwenPaw 技能池失败");

    // ========== 智能体-MCP 绑定 1-020-004-000 ==========
    ErrorCode AGENT_MCP_NOT_EXISTS = new ErrorCode(1_020_004_000, "智能体 MCP 绑定不存在");
    ErrorCode AGENT_MCP_DUPLICATE = new ErrorCode(1_020_004_001, "该智能体已绑定该 MCP");

    // ========== 问答会话 1-020-006-000 ==========
    ErrorCode CHAT_SESSION_NOT_EXISTS = new ErrorCode(1_020_006_000, "问答会话不存在");
    ErrorCode CHAT_SESSION_CLOSED = new ErrorCode(1_020_006_001, "会话已关闭，无法继续问答");
    ErrorCode CHAT_SESSION_TITLE_EMPTY = new ErrorCode(1_020_006_002, "会话标题不能为空");

    // ========== 问答消息 1-020-007-000 ==========
    ErrorCode CHAT_MESSAGE_NOT_EXISTS = new ErrorCode(1_020_007_000, "问答消息不存在");

    // ========== QwenPaw 对接 1-020-008-000 ==========
    ErrorCode QWENPAW_CONNECT_FAILED = new ErrorCode(1_020_008_000, "QwenPaw 服务连接失败，请检查配置");
    ErrorCode QWENPAW_AGENT_NOT_FOUND = new ErrorCode(1_020_008_001, "QwenPaw 侧智能体不存在");
    ErrorCode QWENPAW_CHAT_FAILED = new ErrorCode(1_020_008_002, "QwenPaw 对话调用失败");
    ErrorCode QWENPAW_CHAT_CREATE_FAILED = new ErrorCode(1_020_008_003, "QwenPaw 会话创建失败，请稍后重试");
    ErrorCode CHAT_SESSION_STREAMING = new ErrorCode(1_020_008_004, "当前会话正在生成中，请勿重复发送");
    ErrorCode QWENPAW_MCP_CONNECTING = new ErrorCode(1_020_008_005, "MCP 服务连接中，请稍后重试");

}
