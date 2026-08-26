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
 * 向量解析任务发布器
 *
 * <p>将解析任务以**原始 JSON** 发布到 RabbitMQ 交换机 {@code kb.ingest} →
 * 路由键/队列 {@code kb.ingest.tasks}，由 python-vector 的原生 pika Worker
 * （core/worker.py）直接消费。</p>
 */
@Component
@Slf4j
public class VectorTaskPublisher {

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private VectorTaskConfig vectorTaskConfig;

    /**
     * 发布解析任务消息
     *
     * @param payload 消息体（含 task_id / doc_id / kb_id / file_url / file_type /
     *                tenant_id / user_id / document_title / chunking_config）
     * @param taskId  业务任务ID，作为消息 correlationId，便于对齐
     */
    public void publish(Map<String, Object> payload, String taskId) {
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setContentEncoding("UTF-8");
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        props.setCorrelationId(taskId);

        Message message = new Message(JsonUtils.toJsonByte(payload), props);
        rabbitTemplate.convertAndSend(vectorTaskConfig.getIngestExchange(),
                vectorTaskConfig.getIngestRoutingKey(), message);
        log.info("[publish] 已发布解析任务: taskId={}, exchange={}, routingKey={}",
                taskId, vectorTaskConfig.getIngestExchange(), vectorTaskConfig.getIngestRoutingKey());
    }
}