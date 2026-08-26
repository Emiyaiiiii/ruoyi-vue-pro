package cn.iocoder.yudao.module.system.service.oidc;

import cn.iocoder.yudao.module.system.framework.oidc.config.OidcProperties;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;

/**
 * OIDC ID Token（身份令牌）服务
 *
 * 负责构建标准 OIDC 的 id_token，并交给 {@link OidcKeyService} 进行 RS256 签名。
 * id_token 只在客户端请求了 openid scope 时签发，与芋道自身的 access_token 并存互斥（各自独立）。
 *
 * @author 芋道源码
 */
@Service
public class OidcIdTokenService {

    @Resource
    private OidcProperties oidcProperties;

    @Resource
    private OidcKeyService oidcKeyService;

    /**
     * 创建并签发 id_token
     *
     * @param userId    芋道用户编号（作为标准 sub 声明）
     * @param username  用户账号
     * @param nickname  用户昵称
     * @param email     用户邮箱（可为 null）
     * @param clientId  请求的 OAuth2 客户端编号（作为 aud 声明）
     * @param nonce     授权请求携带的随机数，用于防重放；为 null 时不写入
     * @return 签名后的 id_token（JWT 字符串）
     */
    public String createIdToken(Long userId, String username, String nickname, String email,
                                String clientId, String nonce) {
        long now = System.currentTimeMillis();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(oidcProperties.getIssuer()) // iss：身份提供方标识
                .subject(String.valueOf(userId))     // sub：用户唯一标识
                .audience(clientId)                  // aud：接收方（客户端）
                .issueTime(new Date(now))            // iat：签发时间
                .expirationTime(new Date(now + oidcProperties.getIdTokenTtl() * 1000L)) // exp：过期时间
                .claim("preferred_username", username)
                .claim("name", nickname);
        if (email != null) {
            builder.claim("email", email);
        }
        if (nonce != null) {
            builder.claim("nonce", nonce);
        }
        return oidcKeyService.sign(builder.build().toString());
    }

}