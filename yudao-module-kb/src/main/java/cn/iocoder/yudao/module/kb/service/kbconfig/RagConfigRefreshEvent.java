package cn.iocoder.yudao.module.kb.service.kbconfig;

import lombok.Getter;

/**
 * RAG 配置 / 模型配置变更事件
 *
 * <p>租户级 RAG 配置（kb_rag_config）或模型配置（kb_model_config）变更后由对应
 * Service 发布；KbProcessConfigResolver 监听后全量重推 KB 合并配置到 python-vector，
 * 保证「KB 显式 > 租户默认」两级合并结果始终与 Redis 缓存一致。</p>
 */
@Getter
public class RagConfigRefreshEvent {

    /** 触发来源：rag（租户默认变更） / model（模型配置变更） */
    private final String source;

    public RagConfigRefreshEvent(String source) {
        this.source = source;
    }
}
