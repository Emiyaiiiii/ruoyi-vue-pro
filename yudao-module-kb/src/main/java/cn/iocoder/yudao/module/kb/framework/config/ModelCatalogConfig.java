package cn.iocoder.yudao.module.kb.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 模型目录（LiteLLM model_prices_and_context_window.json）配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "kb.model-catalog")
public class ModelCatalogConfig {

    /**
     * 是否启用模型目录查表（模型参数探测的第①层）
     */
    private Boolean enabled = true;

    /**
     * 远程模型清单地址（jsDelivr CDN，国内可访问；生产环境建议锁定 tag 而非 main）
     */
    private String remoteUrl = "https://cdn.jsdelivr.net/gh/BerriAI/litellm@main/model_prices_and_context_window.json";

    /**
     * classpath 内置快照路径（放置于 resources 下即可离线使用；不存在时自动跳过）
     */
    private String classpathFile = "model_catalog/model_prices_and_context_window.json";

    /**
     * 远程刷新间隔（小时）
     */
    private Integer refreshHours = 24;

    /**
     * 拉取连接超时（毫秒）
     */
    private Integer connectTimeoutMs = 5000;

    /**
     * 拉取读取超时（毫秒）
     */
    private Integer readTimeoutMs = 20000;

    @Bean(name = "modelCatalogRestTemplate")
    public RestTemplate modelCatalogRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }
}
