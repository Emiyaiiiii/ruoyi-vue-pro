package cn.iocoder.yudao.module.kb.service.news;

import java.util.Map;

/**
 * 新闻同步核心服务 — 外部数据库连接 + 数据同步
 *
 * @author 吴皓
 */
public interface NewsSyncService {

    /**
     * 同步指定数据源
     *
     * @param sourceId 数据源ID
     * @param syncType 同步类型: full=全量, incremental=增量
     * @return 同步结果统计
     */
    Map<String, Object> syncSource(Long sourceId, String syncType);

    /**
     * 同步所有启用同步的数据源
     *
     * @param syncType 同步类型
     * @return 所有数据源的同步结果
     */
    Map<String, Object> syncAllEnabledSources(String syncType);
}
