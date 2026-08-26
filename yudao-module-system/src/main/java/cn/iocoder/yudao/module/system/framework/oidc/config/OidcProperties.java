package cn.iocoder.yudao.module.system.framework.oidc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotEmpty;

/**
 * OIDC Provider 配置项
 *
 * 用于使芋道作为 OIDC 身份提供商（IdP），供外部系统（如 dify-sso、dify）通过单点登录识别芋道用户。
 *
 * 注意：仅影响"外部请求带 openid scope 的客户端签发 id_token"这一新增能力，
 * 不影响芋道自身 OAuth2 登录与鉴权链路。
 *
 * @author 芋道源码
 */
@Component
@ConfigurationProperties(prefix = "yudao.oidc")
@Data
public class OidcProperties {

    /**
     * 是否启用 OIDC Provider 能力
     *
     * 默认关闭，开启后：
     * - 对外提供 /.well-known/openid-configuration（discovery）、/.well-known/jwks.json（公钥）
     * - 对带 scope=openid 的 OAuth2 客户端，在 token 响应中附带 id_token
     */
    private Boolean enabled = false;

    /**
     * 行为的身份提供方标识（issuer），也是 id_token 的 iss 声明
     *
     * 需与 discovery（/.well-known/openid-configuration）所在的根路径一致。
     * 例如：http://127.0.0.1:48080
     */
    @NotEmpty(message = "OIDC issuer 不能为空")
    private String issuer = "http://127.0.0.1:48080";

    /**
     * 前端 SSO 授权入口地址（根路径，不含 /sso）
     *
     * OIDC 的 authorization_endpoint 会指向该地址 + "/sso"（浏览器跳转到前端登录/授权页）。
     * 留空时回退到 discovery 动态推导的对外地址（指向后端 authorize），适用于无独立前端页的场景。
     * 例如：http://localhost:5999
     */
    private String frontendUrl = "";

    /**
     * id_token 的有效期（秒），默认 2 小时
     */
    private Long idTokenTtl = 7200L;

    /**
     * JWKS（RSA 签名密钥）配置项
     */
    private Jwks jwks = new Jwks();

    @Data
    public static class Jwks {

        /**
         * 密钥的 key id（kid），用于 JWKS 中标识公钥 / id_token 头中引用
         */
        private String keyId = "yudao-oidc-rsa-1";

        /**
         * RSA 私钥（Base64，PKCS#8 编码）
         *
         * 生产环境建议使用环境变量注入（如 ${OIDC_PRIVATE_KEY}），避免明文写在配置文件中。
         * 未配置时，进程启动时自动生成一个 RSA 2048 私钥（仅当前进程生命周期有效，重启后公钥变化）。
         */
        private String privateKey;
    }

}