package cn.iocoder.yudao.module.agent.framework.config;

import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.agent.service.skillmeta.AiSkillMetaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 应用启动时自动从 QwenPaw 技能池同步一次到 Java 侧元数据表（为每个租户执行）。
 *
 * <p>使用 ApplicationRunner 在所有 Bean 初始化完成后执行；
 * QwenPaw 不可用时仅打印警告，不影响启动。
 *
 * @author 吴皓
 */
@Component
@Slf4j
@Order(100)
public class SkillPoolSyncInitializer implements ApplicationRunner {

    @Resource
    private AiSkillMetaService skillMetaService;
    @Resource
    private TenantFrameworkService tenantFrameworkService;

    @Override
    public void run(ApplicationArguments args) {
        List<Long> tenantIds;
        try {
            tenantIds = tenantFrameworkService.getTenantIds();
        } catch (Exception e) {
            log.warn("[SkillPoolSync] 获取租户列表失败，跳过同步: {}", e.getMessage());
            return;
        }

        log.info("[SkillPoolSync] 开始从 QwenPaw 技能池同步元数据，共 {} 个租户...", tenantIds.size());
        int totalSynced = 0;
        for (Long tenantId : tenantIds) {
            try {
                int synced = TenantUtils.execute(tenantId, () -> skillMetaService.syncFromQwenPaw());
                totalSynced += synced;
            } catch (Exception e) {
                log.warn("[SkillPoolSync] 租户 {} 同步失败: {}", tenantId, e.getMessage());
            }
        }
        log.info("[SkillPoolSync] 同步完成，共新增 {} 条记录", totalSynced);
    }

}
