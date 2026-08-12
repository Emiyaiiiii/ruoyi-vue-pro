package cn.iocoder.yudao.module.kb.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.kb.service.news.NewsSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.Map;

/**
 * 新闻数据同步定时任务
 *
 * 每小时自动执行一次增量同步（由 infra_job 表中的 Cron 表达式控制）。
 * 可在管理后台「基础设施 → 定时任务」中手动触发、暂停或修改执行频率。
 *
 * @author 吴皓
 */
@Component("newsSyncJob")
@Slf4j
public class NewsSyncJob implements JobHandler {

    @Resource
    private NewsSyncService newsSyncService;

    @Override
    @TenantIgnore
    public String execute(String param) throws Exception {
        log.info("========== 新闻数据同步定时任务开始执行 ==========");

        // param 可用于控制同步行为（扩展预留）：
        //   null / "" → 增量同步全部启用的数据源
        //   "full"    → 全量同步全部启用的数据源
        //   "{sourceId}" → 仅同步指定数据源（增量）
        String syncType = "incremental";
        Map<String, Object> result;

        if (param != null && "full".equalsIgnoreCase(param.trim())) {
            syncType = "full";
            result = newsSyncService.syncAllEnabledSources(syncType);
        } else if (param != null && param.trim().matches("\\d+")) {
            // 纯数字 → 指定数据源ID
            Long sourceId = Long.valueOf(param.trim());
            result = newsSyncService.syncSource(sourceId, syncType);
        } else {
            // 默认：增量同步全部数据源
            result = newsSyncService.syncAllEnabledSources(syncType);
        }

        log.info("新闻数据同步定时任务执行完成: syncType={}, totalSources={}, successCount={}, failCount={}",
                syncType,
                result.getOrDefault("totalSources", 0),
                result.getOrDefault("successCount", 0),
                result.getOrDefault("failCount", 0));

        return String.format("新闻数据同步完成: 类型=%s, 数据源=%s, 成功=%s, 失败=%s",
                syncType,
                result.get("totalSources"),
                result.get("successCount"),
                result.get("failCount"));
    }
}
