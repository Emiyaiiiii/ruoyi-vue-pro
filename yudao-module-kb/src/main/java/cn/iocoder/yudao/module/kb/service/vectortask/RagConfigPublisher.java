package cn.iocoder.yudao.module.kb.service.vectortask;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.kb.framework.config.VectorTaskConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RAG 配置变更发布器
 *
 * <p>复用解析任务交换机 {@code kb.ingest}（direct，durable），以**独立路由键/队列**
 * {@code kb.ingest.rag.config} 将 RAG 配置变更推送给 python-vector 的原生 pika 消费者。
 * 消息体为原始 JSON：{tenant_id, event, module, modules, config}。</p>
 */
@Component
@Slf4j
public class RagConfigPublisher {

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private VectorTaskConfig vectorTaskConfig;

    /**
     * 发布 RAG 配置同步消息
     *
     * @param payload 消息体（含 tenant_id / event / module / config 等）
     */
    public void publish(Map<String, Object> payload) {
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setContentEncoding("UTF-8");
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        Message message = new Message(JsonUtils.toJsonByte(payload), props);
        rabbitTemplate.convertAndSend(vectorTaskConfig.getIngestExchange(),
                vectorTaskConfig.getRagRoutingKey(), message);
        log.info("[publish] 已发布 RAG 配置变更: exchange={}, routingKey={}, module={}",
                vectorTaskConfig.getIngestExchange(), vectorTaskConfig.getRagRoutingKey(),
                payload.get("module"));
    }
}