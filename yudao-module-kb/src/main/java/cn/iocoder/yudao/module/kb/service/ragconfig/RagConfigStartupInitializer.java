package cn.iocoder.yudao.module.kb.service.ragconfig;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RAG 配置启动预热
 *
 * <p>RAG/rerank 配置采用触发式推送（仅配置变更时 publish）。服务重启后 python-vector
 * 侧的 Redis 缓存为空，会导致检索回退到本地缺省值。本 Bean 在应用启动完成后，把所有
 * 租户的激活 RAG 配置经 RabbitMQ 重推一遍，让 worker 重写 Redis 缓存，免去逐个手工
 * 保存/触发。RabbitMQ 队列为 durable、消息持久化，因此与 python-vector 启动先后无关。</p>
 */
@Component
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class RagConfigStartupInitializer implements ApplicationRunner {

    @Resource
    private RAGSystemConfigService ragSystemConfigService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ragSystemConfigService.publishAllToVector();
        } catch (Exception ex) {
            log.error("[startup] RAG 配置预热失败（不影响服务启动）", ex);
        }
    }
}