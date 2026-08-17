package cn.iocoder.yudao.module.kb.service.library.rule;

import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.mysql.levelconfig.LevelConfigMapper;
import cn.iocoder.yudao.module.kb.service.userdept.KbUserDeptService;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 可见性规则引擎（统一入口）
 * <p>
 * 整合所有 VisibilityRuleHandler，提供统一的：
 * - 上下文构建（buildContext）
 * - 可见性判断（canSee / filterVisible）
 * - 管理权限判断（canManage）
 * - SQL 条件生成（buildSqlConditions）
 * <p>
 * 超管/租户管理员的短路判断在此统一处理，各 Handler 无需重复判断。
 *
 * @author 吴皓
 */
@Component
@Slf4j
public class VisibilityRuleEngine {

    @Resource
    private List<VisibilityRuleHandler> handlers;

    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Resource
    private KbUserDeptService kbUserDeptService;

    @Resource
    private LevelConfigMapper levelConfigMapper;

    /** rule 值 → Handler 映射 */
    private Map<Integer, VisibilityRuleHandler> handlerMap;

    @PostConstruct
    void init() {
        handlerMap = handlers.stream()
                .collect(Collectors.toMap(VisibilityRuleHandler::rule, Function.identity()));
        log.info("[可见性引擎] 已注册 {} 个规则处理器: {}", handlerMap.size(), handlerMap.keySet());
    }

    /**
     * 构建可见性上下文（一次计算，多处复用）
     *
     * @param userId 当前用户ID，null 则返回超管上下文
     */
    public VisibilityContext buildContext(Long userId) {
        if (userId == null) {
            return VisibilityContext.builder().superAdmin(true).build();
        }
        boolean isSuper = securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode());

        Set<Long> userDeptIds = Collections.emptySet();
        Set<Long> visibleDeptIds = Collections.emptySet();
        if (!isSuper) {
            userDeptIds = kbUserDeptService.getDeptIdsByUserId(userId);
            visibleDeptIds = new HashSet<>(userDeptIds);
            for (Long deptId : userDeptIds) {
                visibleDeptIds.addAll(kbUserDeptService.getDeptAncestorIds(deptId));
            }
        }

        return VisibilityContext.builder()
                .userId(userId)
                .superAdmin(isSuper)
                .userDeptIds(userDeptIds)
                .visibleDeptIds(visibleDeptIds)
                .build();
    }

    /**
     * 判断知识库是否可见（单条判断，替代 filterVisible 中的 switch-case）
     */
    public boolean canSee(VisibilityContext ctx, LibraryDO lib) {
        if (ctx.isSuperAdmin()) {
            return true;
        }
        LevelConfigDO cfg = levelConfigMapper.selectById(lib.getKbLevelId());
        if (cfg == null || cfg.getVisibilityRule() == null) {
            log.debug("[可见性] libId={} kbLevelId={} 配置不存在 → 不可见", lib.getId(), lib.getKbLevelId());
            return false;
        }
        VisibilityRuleHandler handler = handlerMap.get(cfg.getVisibilityRule());
        if (handler == null) {
            log.debug("[可见性] libId={} rule={} 无对应处理器 → 不可见", lib.getId(), cfg.getVisibilityRule());
            return false;
        }
        return handler.canSee(ctx, lib, cfg);
    }

    /**
     * 批量过滤可见的知识库（替代 filterVisible 方法）
     */
    public List<LibraryDO> filterVisible(VisibilityContext ctx, List<LibraryDO> allLibs) {
        if (allLibs == null || allLibs.isEmpty()) {
            return allLibs;
        }
        if (ctx.isSuperAdmin()) {
            return allLibs;
        }
        // 预加载所有层级配置
        Map<Long, LevelConfigDO> configMap = levelConfigMapper.selectList()
                .stream()
                .collect(Collectors.toMap(LevelConfigDO::getId, Function.identity()));

        return allLibs.stream()
                .filter(lib -> {
                    LevelConfigDO cfg = configMap.get(lib.getKbLevelId());
                    if (cfg == null || cfg.getVisibilityRule() == null) {
                        return false;
                    }
                    VisibilityRuleHandler handler = handlerMap.get(cfg.getVisibilityRule());
                    return handler != null && handler.canSee(ctx, lib, cfg);
                })
                .collect(Collectors.toList());
    }

    /**
     * 判断是否可管理（替代 validateManagementPermission）
     *
     * @return true=可管理
     */
    public boolean canManage(VisibilityContext ctx, LibraryDO lib) {
        if (ctx.isSuperAdmin()) {
            return true;
        }
        LevelConfigDO cfg = levelConfigMapper.selectById(lib.getKbLevelId());
        if (cfg == null || cfg.getVisibilityRule() == null) {
            // 配置不存在则放行，由 API 层权限控制兜底
            log.debug("[管理权限] libId={} 配置为空 → 放行（API层兜底）", lib.getId());
            return true;
        }
        VisibilityRuleHandler handler = handlerMap.get(cfg.getVisibilityRule());
        if (handler == null) {
            log.debug("[管理权限] libId={} rule={} 无对应处理器 → 放行", lib.getId(), cfg.getVisibilityRule());
            return true;
        }
        return handler.canManage(ctx, lib, cfg);
    }

    /**
     * 生成所有规则的 SQL 可见性条件（OR 组合）
     * <p>
     * 用于分页查询下推到 SQL 层，替代 LibraryMapper.selectPageWithVisibility 中的硬编码逻辑。
     *
     * @param ctx 可见性上下文
     * @return SQL 条件列表，调用方用 OR 拼接后添加到 wrapper.apply()
     */
    public List<String> buildSqlConditions(VisibilityContext ctx) {
        List<LevelConfigDO> allConfigs = levelConfigMapper.selectList();
        List<String> conditions = new ArrayList<>();

        for (LevelConfigDO cfg : allConfigs) {
            if (cfg.getVisibilityRule() == null) {
                continue;
            }
            VisibilityRuleHandler handler = handlerMap.get(cfg.getVisibilityRule());
            if (handler == null) {
                continue;
            }
            String condition = handler.toSqlCondition(ctx, cfg);
            if (condition != null && !condition.isEmpty()) {
                conditions.add(condition);
            }
        }

        return conditions;
    }

    /**
     * 获取指定 rule 的 Handler（供外部扩展使用）
     */
    public VisibilityRuleHandler getHandler(int rule) {
        return handlerMap.get(rule);
    }
}
