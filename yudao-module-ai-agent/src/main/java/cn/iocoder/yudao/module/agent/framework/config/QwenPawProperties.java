package cn.iocoder.yudao.module.agent.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

/**
 * QwenPaw 连接配置
 *
 * @author 吴皓
 */
@ConfigurationProperties(prefix = "yudao.ai.qwenpaw")
@Validated
@Data
public class QwenPawProperties {

    /**
     * QwenPaw 服务地址，例如 http://127.0.0.1:8088
     */
    @NotEmpty(message = "QwenPaw 服务地址不能为空")
    private String baseUrl;

    /**
     * 是否启用 QwenPaw 鉴权（QWENPAW_AUTH_ENABLED）
     */
    private Boolean authEnabled = false;

    /**
     * 服务账号 token（authEnabled=true 时必填）
     */
    private String authToken;

    /**
     * 默认模型
     */
    private String defaultModel = "qwen3-coder-flash";

    /**
     * HTTP 连接超时（毫秒）
     */
    private Integer connectTimeout = 3000;

    /**
     * HTTP 读取超时（毫秒）
     */
    private Integer readTimeout = 30000;

}
