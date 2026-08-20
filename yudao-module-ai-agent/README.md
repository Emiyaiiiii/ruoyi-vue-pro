# yudao-module-ai-agent

智能体管理模块：把 **QwenPaw** 作为共享的「智能体仓库」，芋道侧负责智能体生命周期、系统级 MCP 商店与 Skills 技能池、以及知识库问答会话。MCP / Skills 的挂载以 QwenPaw 为唯一权威源（对齐 skills 的做法，无本地绑定表）。

## 目录结构

```
src/main/java/cn/iocoder/yudao/module/agent/
├── controller/admin/          # 管理后台接口（agent / mcpmeta / skillmeta / agentskill / chatsession / agent-remote）
├── dal/dataobject/            # DO（ai_agent / ai_mcp_meta / ai_skill_meta / ai_chat_session / ai_chat_message）
├── dal/mysql/                 # Mapper（基于 BaseMapperX + LambdaQueryWrapperX，无需 XML）
├── service/                   # Service 接口与实现（含 QwenPaw 下发桩）
├── enums/ErrorCodeConstants.java
└── framework/config/          # QwenPawProperties + QwenPawClient（HTTP 客户端）
```

## 接入步骤

1. **建表**：已由 Flyway 迁移脚本统一管理
   （`yudao-server/src/main/resources/db/migration/V1__init_ai_and_kb_tables.sql`，
   含 ai/kb 表结构 + 菜单权限 + 字典，启动时自动执行，幂等安全）。
2. **注册模块**：在根 `pom.xml` 的 `<modules>` 中加入
   `<module>yudao-module-ai-agent</module>`（位于 `yudao-module-kb` 之后）。
3. **装配到应用**：在 `yudao-server/pom.xml` 增加依赖
   `cn.iocoder.boot:yudao-module-ai-agent`，并确认 `yudao-server` 的
   `ruoyi-vue-pro-server.yaml` 中 `mybatis-plus.mapper-locations` 能扫描本模块
   （默认扫描 `classpath*:mapper/**/*.xml` 即可，本模块无 XML）。
4. **配置 QwenPaw 连接**：在 `application.yaml` 增加：

```yaml
yudao:
  ai:
    qwenpaw:
      base-url: http://127.0.0.1:8088   # QwenPaw 服务地址
      auth-enabled: false                # 是否启用 QwenPaw 鉴权
      auth-token: ""                     # 服务账号 token（auth-enabled=true 时必填）
      default-model: qwen3-coder-flash    # 默认模型
```

## 说明

- 包名使用 `cn.iocoder.yudao.module.agent`，避免与官方 `yudao-module-ai` 的
  `cn.iocoder.yudao.module.ai` 冲突。
- 错误码使用 `1-020-000-000` 段，与 kb 模块（`1-010` 段）不冲突。
- `QwenPawClient` 当前为同步 HTTP 桩（RestTemplate）；SSE 流式对话与 kb-mcp 桥接
  服务后续按 `框架设计文档` 第 4 章补齐。
