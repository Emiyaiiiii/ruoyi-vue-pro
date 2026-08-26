package cn.iocoder.yudao.module.kb.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * 向量处理任务配置
 */
@Data
@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "kb.vector")
public class VectorTaskConfig {

    /**
     * Python 向量处理服务地址
     */
    private String pythonServiceUrl = "http://localhost:8100";

    /**
     * 任务超时时间（分钟），超过该时间未完成则标记为超时
     */
    private Integer taskTimeoutMinutes = 30;

    /**
     * Redis Stream 事件流 Key
     */
    private String redisStreamKey = "kb:task:events";

    /**
     * Redis 消费者组名称
     */
    private String redisConsumerGroup = "java-consumer-group";

    /**
     * Redis 消费者名称
     */
    private String redisConsumerName = "java-consumer-1";

    /**
     * Redis 中任务状态的 Key 前缀（Python 写入）
     */
    private String redisTaskKeyPrefix = "kb:task:";

    // ========== RabbitMQ 解析任务通道（任务通过原生 JSON 消息进入 Python Worker） ==========

    /**
     * 解析任务交换机（direct，durable）
     */
    private String ingestExchange = "kb.ingest";

    /**
     * 解析任务队列
     */
    private String ingestQueue = "kb.ingest.tasks";

    /**
     * 解析任务路由键
     */
    private String ingestRoutingKey = "kb.ingest.tasks";

    // ========== RAG 配置同步通道（复用 kb.ingest 交换机，独立路由键/队列） ==========

    /**
     * RAG 配置变更路由键
     */
    private String ragRoutingKey = "kb.ingest.rag.config";

    /**
     * RAG 配置变更队列
     */
    private String ragQueue = "kb.ingest.rag.config";

    // ========== 通信超时配置 ==========

    /**
     * 调用 Python 服务的连接超时时间（毫秒）
     */
    private Integer connectTimeoutMs = 5000;

    /**
     * 调用 Python 服务的读取超时时间（毫秒）
     */
    private Integer readTimeoutMs = 10000;

    // ========== 重试配置 ==========

    /**
     * 调用 Python 服务失败时的最大重试次数
     */
    private Integer maxRetries = 3;

    /**
     * 重试间隔（毫秒）
     */
    private Integer retryDelayMs = 1000;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }
}
