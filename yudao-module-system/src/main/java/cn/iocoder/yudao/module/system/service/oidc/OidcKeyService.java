package cn.iocoder.yudao.module.system.service.oidc;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.system.framework.oidc.config.OidcProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * OIDC 签名密钥服务
 *
 * 负责加载/生成 RSA 签名密钥对，提供：
 * 1. 暴露给第三方的 JWKS（公钥集合），用于校验 id_token 签名
 * 2. 对 id_token 进行 RS256 签名
 *
 * 密钥来源：优先读取 {@link OidcProperties#getJwks()#privateKey} 中配置的私钥（生产建议环境变量注入），
 * 未配置时启动时自动生成（进程内有效，重启后公钥会变化，适合开发阶段）。
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class OidcKeyService {

    /**
     * RSA 私钥（用于签名 id_token）
     */
    private final PrivateKey privateKey;

    /**
     * 公钥 JWK
     */
    private final RSAKey publicJwk;

    private final String keyId;

    public OidcKeyService(OidcProperties properties) throws Exception {
        this.keyId = properties.getJwks().getKeyId();
        RSAKey rsaKey = loadOrGenerateRsaKey(properties);
        this.privateKey = rsaKey.toPrivateKey();
        this.publicJwk = rsaKey.toPublicJWK();
        log.info("[OidcKeyService][初始化完成，kid={}]", keyId);
    }

    private RSAKey loadOrGenerateRsaKey(OidcProperties properties) {
        String privateKeyStr = properties.getJwks().getPrivateKey();
        if (StrUtil.isNotBlank(privateKeyStr)) {
            try {
                return buildRsaKeyFromBase64(privateKeyStr, properties);
            } catch (Exception ex) {
                log.warn("[OidcKeyService][私钥解析失败，转为自动生成。原因：{}]", ex.getMessage());
            }
        }
        return generateKeyPair(properties);
    }

    /**
     * 通过 Base64（PKCS#8）私钥，构造带 kid 的 RSAKey
     */
    private RSAKey buildRsaKeyFromBase64(String privateKeyStr, OidcProperties properties) throws Exception {
        byte[] keyBytes = Base64.getMimeDecoder().decode(privateKeyStr.trim());
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        // 从 CRT 私钥规格推导出 modulus 与 publicExponent，生成公钥对象后构造带 kid 的 RSAKey
        RSAPrivateCrtKeySpec crtKeySpec = keyFactory.getKeySpec(privateKey, RSAPrivateCrtKeySpec.class);
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(crtKeySpec.getModulus(), crtKeySpec.getPublicExponent());
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
        return new RSAKey.Builder((RSAPublicKey) publicKey)
                .privateKey(privateKey)
                .keyID(properties.getJwks().getKeyId())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build();
    }

    /**
     * 自动生成 RSA 密钥对
     */
    private RSAKey generateKeyPair(OidcProperties properties) {
        try {
            log.warn("[OidcKeyService][未配置私钥，自动生成 RSA 密钥对。生产环境请配置 yudao.oidc.jwks.private-key，" +
                    "以保证重启后公钥不变，避免已集成的客户端验签失败]");
            return new RSAKeyGenerator(2048)
                    .keyID(properties.getJwks().getKeyId())
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .generate();
        } catch (JOSEException ex) {
            throw new IllegalStateException("自动生成 RSA 密钥失败", ex);
        }
    }

    /**
     * 获得公钥 JWKS（JSON 字符串），供 /.well-known/jwks.json 使用
     *
     * @return JWKS JSON
     */
    public String getPublicJwkSetJson() {
        return new JWKSet(publicJwk).toString();
    }

    /**
     * 当前使用的 key id（kid）
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * 对 id_token 的 JSON payload 进行 RS256 签名，返回最终 JWT 字符串
     *
     * @param payload id_token 的 JSON payload
     * @return 签名后的 JWT
     */
    public String sign(String payload) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(keyId)
                    .build();
            JWSObject jwsObject = new JWSObject(header, new Payload(payload));
            jwsObject.sign(new RSASSASigner(privateKey));
            return jwsObject.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("id_token 签名失败", ex);
        }
    }

}