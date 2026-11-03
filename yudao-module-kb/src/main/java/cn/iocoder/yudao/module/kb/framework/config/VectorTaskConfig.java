package cn.iocoder.yudao.module.kb.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
     * 任务超时时间（分钟），超过该时间未完成则标记为超时（历史配置，保留兼容）
     */
    private Integer taskTimeoutMinutes = 30;

    /**
     * 执行超时（分钟）：PROCESSING 任务心跳中断超过该时间才判超时
     */
    private Integer execTimeoutMinutes = 15;

    /**
     * 排队超时（分钟）：PENDING 任务排队超过该时间仅告警，不判超时
     */
    private Integer queueTimeoutMinutes = 1440;

    /**
     * 心跳间隔（秒）：Python 长静默阶段也会按该间隔刷新 Redis updated_at
     */
    private Integer heartbeatIntervalSeconds = 15;

    /**
     * 任务最大尝试次数（为自动重试预留）
     */
    private Integer maxAttempts = 3;

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

    // ========== chunk 编辑同步调用（重嵌入较慢，放宽超时） ==========

    /**
     * chunk 编辑调 python 的读取超时时间（毫秒）
     */
    private Integer chunkReadTimeoutMs = 60000;

    /**
     * chunk 编辑专用的 RestTemplate（重嵌入可能远超默认 10s，避免超时误报失败）
     */
    @Bean(name = "chunkRestTemplate")
    public RestTemplate chunkRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(chunkReadTimeoutMs);
        return new RestTemplate(factory);
    }

    // ========== 知识库完整导出（流式下载，读超时默认不限） ==========

    /**
     * 导出时调用 python 流式接口的读取超时（毫秒）；0 = 不限（大库导出可能持续数分钟）
     */
    private Integer exportReadTimeoutMs = 0;

    /**
     * 知识库导出专用 RestTemplate：读取 NDJSON 流时不能按默认 10s 超时打断。
     */
    @Bean(name = "kbExportRestTemplate")
    public RestTemplate kbExportRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(exportReadTimeoutMs == null ? 0 : exportReadTimeoutMs);
        // 流式读取：不缓冲请求体，边收边写 ZIP
        factory.setBufferRequestBody(false);
        return new RestTemplate(factory);
    }

    // ========== 知识库导入（分片上传 / 重跑向量） ==========

    /**
     * 导入包分片大小（MB）：前端按此切片上传，规避 multipart 单文件/网关体积限制
     */
    private Integer importChunkSizeMb = 64;

    /**
     * 分片上传会话过期时间（小时）：超时目录由定时任务清理
     */
    private Integer importUploadTtlHours = 24;

    /**
     * 重跑向量等待超时（毫秒）：超时后返回 taskId，后台任务在 python 侧继续执行
     */
    private Integer importReembedTimeoutMs = 1800000;

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
    @Primary
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }
}
