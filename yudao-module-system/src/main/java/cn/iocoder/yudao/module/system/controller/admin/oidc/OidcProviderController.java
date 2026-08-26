package cn.iocoder.yudao.module.system.controller.admin.oidc;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser;

/**
 * OIDC Provider 端点（管理后台）
 *
 * 注意：userinfo 端点默认需要认证（携带有效 Bearer token），不属于免登录范围。
 * 通过 {@link cn.iocoder.yudao.framework.security.core.filter.TokenAuthenticationFilter} 校验 access_token 后，
 * 使用当前登录用户信息返回标准 OIDC userinfo 声明。
 *
 * @author 芋道源码
 */
@Tag(name = "OIDC Provider")
@RestController
@RequestMapping("/system/oauth2/oidc")
@Validated
@Slf4j
public class OidcProviderController {

    @Resource
    private AdminUserService userService;

    @GetMapping("/userinfo")
    @Operation(summary = "获取用户信息（OIDC userinfo）")
    public Map<String, Object> userinfo() {
        Long userId = getLoginUserId();
        // 跨租户单点登录：以令牌归属的租户上下文加载用户，避免请求方携带的 tenant-id 与令牌归属租户不一致时取不到用户
        Long tokenTenantId = getLoginUser().getTenantId();
        AdminUserDO user = TenantUtils.execute(tokenTenantId, () -> userService.getUser(userId));
        // 标准 userinfo 声明
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", String.valueOf(user.getId()));
        claims.put("preferred_username", user.getUsername());
        claims.put("name", user.getNickname());
        // email：优先用用户真实邮箱；为空时回退为 {username}@yrec.cn，
        // 保证 OIDC 依赖 email 作为唯一标识的下游（如 dify 建号）始终能拿到 email 声明
        String email = user.getEmail();
        if (StrUtil.isBlank(email)) {
            email = user.getUsername() + "@yrec.cn";
        }
        claims.put("email", email);
        return claims;
    }

}