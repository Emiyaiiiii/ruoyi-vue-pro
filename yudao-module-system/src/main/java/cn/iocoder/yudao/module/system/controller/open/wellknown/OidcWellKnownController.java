package cn.iocoder.yudao.module.system.controller.open.wellknown;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.system.framework.oidc.config.OidcProperties;
import cn.iocoder.yudao.module.system.service.oidc.OidcKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * OIDC 的 Well-Known 端点
 *
 * 放置在该包（非 controller.admin），使其不挂 /admin-api 前缀，符合 OIDC 标准：
 * - discovery：{issuer}/.well-known/openid-configuration
 * - 公钥：{issuer}/.well-known/jwks.json
 *
 * 生产环境 Nginx 需将 /.well-known/* 转发到芋道后端。
 *
 * @author 芋道源码
 */
@Tag(name = "OIDC - Well-Known")
@RestController
@RequestMapping("/.well-known")
@PermitAll
public class OidcWellKnownController {

    private final OidcProperties oidcProperties;
    private final OidcKeyService oidcKeyService;

    public OidcWellKnownController(OidcProperties oidcProperties, OidcKeyService oidcKeyService) {
        this.oidcProperties = oidcProperties;
        this.oidcKeyService = oidcKeyService;
    }

    /**
     * OIDC 提供方的发现配置。第三方（如 dify-sso）通过它获知授权、令牌、公钥等端点
     */
    @GetMapping("/openid-configuration")
    @Operation(summary = "OIDC 发现配置")
    public OpenIdConfiguration openIdConfiguration(HttpServletRequest request) {
        // 端点地址基于本次请求实际使用的协议与 Host 动态生成：
        // - 容器内（dify-sso 通过 host.docker.internal）请求 → 返回 host.docker.internal 端点，容器可访问宿主机芋道
        // - 本地浏览器直接访问 → 返回 127.0.0.1/localhost 端点，同样可用
        // 避免 discovery 写死 127.0.0.1 导致容器内访问自身而无法到达宿主机芋道。
        String configuredIssuer = oidcProperties.getIssuer();
        // id_token 的 iss 保持与配置一致（供需要签名校验的严格客户端），
        // endpoints 使用按请求推导的对外地址 base
        String base = resolveExternalBase(request);
        // 授权端点指向前端 SSO 授权页（浏览器需跳转到登录页完成登录+授权）。
        // 配置了 frontendUrl 时用 frontendUrl + /sso；否则回退到后端 authorize。
        String authBase = StrUtil.isNotBlank(oidcProperties.getFrontendUrl())
                ? oidcProperties.getFrontendUrl() : base;
        OpenIdConfiguration config = new OpenIdConfiguration();
        config.setIssuer(configuredIssuer);
        config.setAuthorizationEndpoint(authBase + "/sso");
        config.setTokenEndpoint(base + "/admin-api/system/oauth2/token");
        config.setUserinfoEndpoint(base + "/admin-api/system/oauth2/oidc/userinfo");
        config.setJwksUri(base + "/.well-known/jwks.json");
        config.setResponseTypesSupported(List.of("code"));
        config.setGrantTypesSupported(List.of("authorization_code", "password", "refresh_token", "client_credentials"));
        config.setSubjectTypesSupported(List.of("public"));
        config.setIdTokenSigningAlgValuesSupported(List.of("RS256"));
        config.setScopesSupported(List.of("openid", "profile", "email"));
        config.setClaimsSupported(List.of("iss", "sub", "aud", "exp", "iat", "preferred_username", "email", "name"));
        config.setCodeChallengeMethodsSupported(Arrays.asList("plain", "S256"));
        return config;
    }

    /**
     * 推导对客户端（含容器内 dify-sso）可访问的端点根地址
     *
     * 优先使用反向代理（Nginx）传递的 X-Forwarded-*，其次是请求自身的协议/主机，
     * 以保证容器内通过 host.docker.internal 访问时也能得到可访问的地址。
     */
    private String resolveExternalBase(HttpServletRequest request) {
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String host = forwardedHost != null && !forwardedHost.isBlank()
                ? forwardedHost
                : request.getHeader("Host");
        if (host == null || host.isBlank()) {
            host = oidcProperties.getIssuer()
                    .replaceFirst("^[a-zA-Z][a-zA-Z0-9+.-]*://", ""); // 回退：去掉配置 issuer 的 scheme
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String scheme = (forwardedProto != null && !forwardedProto.isBlank())
                ? forwardedProto
                : request.getScheme();
        return scheme + "://" + host;
    }

    /**
     * JWKS 公钥集合，第三方用它校验 id_token 的 RS256 签名
     */
    @GetMapping("/jwks.json")
    @Operation(summary = "OIDC 公钥集合")
    public String jwks() {
        return oidcKeyService.getPublicJwkSetJson();
    }

    static class OpenIdConfiguration {
        private String issuer;
        private String authorizationEndpoint;
        private String tokenEndpoint;
        private String userinfoEndpoint;
        private String jwksUri;
        private List<String> responseTypesSupported;
        private List<String> grantTypesSupported;
        private List<String> subjectTypesSupported;
        private List<String> idTokenSigningAlgValuesSupported;
        private List<String> scopesSupported;
        private List<String> claimsSupported;
        private List<String> codeChallengeMethodsSupported;

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public String getAuthorizationEndpoint() { return authorizationEndpoint; }
        public void setAuthorizationEndpoint(String authorizationEndpoint) { this.authorizationEndpoint = authorizationEndpoint; }
        public String getTokenEndpoint() { return tokenEndpoint; }
        public void setTokenEndpoint(String tokenEndpoint) { this.tokenEndpoint = tokenEndpoint; }
        public String getUserinfoEndpoint() { return userinfoEndpoint; }
        public void setUserinfoEndpoint(String userinfoEndpoint) { this.userinfoEndpoint = userinfoEndpoint; }
        public String getJwksUri() { return jwksUri; }
        public void setJwksUri(String jwksUri) { this.jwksUri = jwksUri; }
        public List<String> getResponseTypesSupported() { return responseTypesSupported; }
        public void setResponseTypesSupported(List<String> responseTypesSupported) { this.responseTypesSupported = responseTypesSupported; }
        public List<String> getGrantTypesSupported() { return grantTypesSupported; }
        public void setGrantTypesSupported(List<String> grantTypesSupported) { this.grantTypesSupported = grantTypesSupported; }
        public List<String> getSubjectTypesSupported() { return subjectTypesSupported; }
        public void setSubjectTypesSupported(List<String> subjectTypesSupported) { this.subjectTypesSupported = subjectTypesSupported; }
        public List<String> getIdTokenSigningAlgValuesSupported() { return idTokenSigningAlgValuesSupported; }
        public void setIdTokenSigningAlgValuesSupported(List<String> idTokenSigningAlgValuesSupported) { this.idTokenSigningAlgValuesSupported = idTokenSigningAlgValuesSupported; }
        public List<String> getScopesSupported() { return scopesSupported; }
        public void setScopesSupported(List<String> scopesSupported) { this.scopesSupported = scopesSupported; }
        public List<String> getClaimsSupported() { return claimsSupported; }
        public void setClaimsSupported(List<String> claimsSupported) { this.claimsSupported = claimsSupported; }
        public List<String> getCodeChallengeMethodsSupported() { return codeChallengeMethodsSupported; }
        public void setCodeChallengeMethodsSupported(List<String> codeChallengeMethodsSupported) { this.codeChallengeMethodsSupported = codeChallengeMethodsSupported; }
    }

}