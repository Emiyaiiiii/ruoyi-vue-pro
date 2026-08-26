package cn.iocoder.yudao.module.agent.framework.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * QwenPaw 配置类
 *
 * @author 吴皓
 */
@Configuration
@EnableConfigurationProperties({QwenPawProperties.class, AiAgentDefaultProperties.class})
public class QwenPawConfiguration {

    /**
     * SSE 流式对话专用线程池
     *
     * <p>避免流式转发占用 {@link java.util.concurrent.ForkJoinPool#commonPool()}。
     * 线程池不参与 Spring 事务管理，租户上下文由调用方手动捕获/恢复。
     */
    @Bean("qwenpawChatTaskExecutor")
    public ThreadPoolTaskExecutor qwenpawChatTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("qwenpaw-chat-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

}
