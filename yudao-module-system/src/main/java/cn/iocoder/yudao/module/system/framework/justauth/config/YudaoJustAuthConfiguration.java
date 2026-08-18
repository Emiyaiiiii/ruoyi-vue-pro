package cn.iocoder.yudao.module.system.framework.justauth.config;

import cn.iocoder.yudao.module.system.framework.justauth.core.AuthRequestFactory;
import com.xkcoding.justauth.autoconfigure.JustAuthProperties;
import com.xkcoding.justauth.support.cache.RedisStateCache;
import me.zhyd.oauth.cache.AuthStateCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * JustAuth 配置类 TODO 芋艿：等 justauth 1.4.1 版本发布！！！
 *
 * <p>
 * 说明：justauth-spring-boot-starter 1.4.0 仅兼容 Spring Boot 2.X。Spring Boot 3.X 下，
 * 其自动配置（基于 spring.factories 机制）不会生效，所以这里手动注册 JustAuthProperties
 * 与 justAuthRedisCacheTemplate 两个 Bean，替代失效的自动配置。
 *
 * @author 芋道源码
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JustAuthProperties.class)
public class YudaoJustAuthConfiguration {

    @Bean(name = "authRequestFactory2") // TODO @芋艿：等 justauth1.4.1 发布，可以去掉
    @ConditionalOnProperty(
            prefix = "justauth",
            value = {"enabled"},
            havingValue = "true",
            matchIfMissing = true
    )
    public AuthRequestFactory authRequestFactory(JustAuthProperties properties, AuthStateCache authStateCache) {
        return new AuthRequestFactory(properties, authStateCache);
    }

    @Bean
    public AuthStateCache authStateCache(@Qualifier("justAuthRedisCacheTemplate") RedisTemplate<String, String> justAuthRedisCacheTemplate,
                                         JustAuthProperties justAuthProperties) {
        return new RedisStateCache(justAuthRedisCacheTemplate, justAuthProperties.getCache());
    }

    /**
     * 创建 justAuthRedisCacheTemplate Bean，对应 justauth-spring-boot-starter 自动配置中的同名 Bean。
     * 仅当不存在该 Bean 时创建（@ConditionalOnMissingBean），避免与其它配置冲突
     */
    @Bean(name = "justAuthRedisCacheTemplate")
    @ConditionalOnMissingBean(name = "justAuthRedisCacheTemplate")
    public RedisTemplate<String, String> justAuthRedisCacheTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(stringRedisSerializer);
        template.setHashValueSerializer(stringRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }

}
