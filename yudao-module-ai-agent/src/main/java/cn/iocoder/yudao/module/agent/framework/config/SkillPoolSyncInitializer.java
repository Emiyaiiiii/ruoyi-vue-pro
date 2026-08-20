package cn.iocoder.yudao.module.agent.framework.config;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.agent.dal.mysql.skillmeta.AiSkillMetaMapper;
import cn.iocoder.yudao.module.agent.dal.dataobject.skillmeta.AiSkillMetaDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 应用启动时自动从 QwenPaw 技能池同步一次到 Java 侧元数据表（为每个租户执行）。
 *
 * <p>实现要点（针对 Spring Boot 3.x 启动时序问题）：
 * <ul>
 *   <li>ContextRefreshedEvent 阶段首次 HTTP 请求未到达，{@code defaultValidator} Bean
 *       尚未创建。若此阶段触发 {@code @Validated} 方法，会去懒加载
 *       {@code defaultValidator}；而启动 Runner 紧跟其后是销毁窗口，
 *       极易撞上 {@code BeanCreationNotAllowedException}。</li>
 *   <li>因此本类不再依赖 {@code @Validated} 的 Service，而是直接用 {@code JdbcTemplate}
 *       写库（与 MyBatis 走同一连接池，不引入额外 Bean），并延迟 5 秒后异步执行，
 *       确保启动阶段已稳定、HTTP 上下文已就绪。</li>
 *   <li>QwenPaw 不可用时仅打印警告，不影响启动。</li>
 * </ul>
 *
 * @author 吴皓
 */
@Component
@Slf4j
@Order(100)
public class SkillPoolSyncInitializer implements ApplicationListener<ContextRefreshedEvent> {

    @Resource
    private AiSkillMetaMapper skillMetaMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private QwenPawClient qwenPawClient;

    private final ScheduledExecutorService delayExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "skill-pool-sync-delay");
                t.setDaemon(true);
                return t;
            });

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 监听 ContextRefreshedEvent，但延迟 5 秒再执行：
        // 1) 避开启动销毁窗口
        // 2) 等到 defaultValidator / HTTP 上下文就绪
        delayExecutor.schedule(this::doSync, 5, TimeUnit.SECONDS);
    }

    void doSync() {
        List<Long> tenantIds;
        try {
            // 直接查租户表，绕过 @Validated 路径
            tenantIds = jdbcTemplate.queryForList(
                    "SELECT id FROM system_tenant WHERE status = 0", Long.class);
        } catch (Exception e) {
            log.warn("[SkillPoolSync] 获取租户列表失败，跳过同步: {}", e.getMessage());
            return;
        }

        log.info("[SkillPoolSync] 开始从 QwenPaw 技能池同步元数据，共 {} 个租户...", tenantIds.size());
        int totalSynced = 0;
        for (Long tenantId : tenantIds) {
            try {
                int synced = syncForTenant(tenantId);
                totalSynced += synced;
            } catch (Exception e) {
                log.warn("[SkillPoolSync] 租户 {} 同步失败: {}", tenantId, e.getMessage());
            }
        }
        log.info("[SkillPoolSync] 同步完成，共新增 {} 条记录", totalSynced);
    }

    private int syncForTenant(Long tenantId) {
        Long oldTenantId = TenantContextHolder.getTenantId();
        Boolean oldIgnore = TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setTenantId(tenantId);
            TenantContextHolder.setIgnore(false);

            List<Map<String, Object>> poolSkills;
            try {
                poolSkills = qwenPawClient.listSkillPool();
            } catch (Exception e) {
                log.warn("[SkillPoolSync] QwenPaw 技能池拉取失败，tenantId={}: {}",
                        tenantId, e.getMessage());
                return 0;
            }

            int synced = 0;
            for (Map<String, Object> poolSkill : poolSkills) {
                String skillName = String.valueOf(poolSkill.getOrDefault("name", ""));
                if (skillName.isEmpty()) {
                    continue;
                }

                AiSkillMetaDO existing = skillMetaMapper.selectBySkillName(skillName);
                if (existing != null) {
                    // 直接走 BaseMapperX.updateById，不触发 @Validated AOP
                    AiSkillMetaDO updateObj = new AiSkillMetaDO();
                    updateObj.setId(existing.getId());
                    updateObj.setSource(String.valueOf(
                            poolSkill.getOrDefault("source", "customized")));
                    Object versionText = poolSkill.get("version_text");
                    if (versionText != null) {
                        updateObj.setVersion(String.valueOf(versionText));
                    }
                    skillMetaMapper.updateById(updateObj);
                    continue;
                }

                AiSkillMetaDO meta = AiSkillMetaDO.builder()
                        .tenantId(tenantId)
                        .skillName(skillName)
                        .displayName(String.valueOf(poolSkill.getOrDefault("name", skillName)))
                        .description(String.valueOf(poolSkill.getOrDefault("description", "")))
                        .icon(String.valueOf(poolSkill.getOrDefault("emoji", "")))
                        .source(String.valueOf(poolSkill.getOrDefault("source", "customized")))
                        .visibility(1)
                        .status(1)
                        .build();
                Object versionText = poolSkill.get("version_text");
                if (versionText != null) {
                    meta.setVersion(String.valueOf(versionText));
                }
                skillMetaMapper.insert(meta);
                synced++;
            }
            return synced;
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }
}
