package cn.iocoder.yudao.module.kb.service.library.rule;

import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.service.userdept.KbUserDeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.stream.Collectors;

/**
 * rule=2 院级/部门级知识库规则处理器
 * <p>
 * 可见性：用户所属部门+祖先部门范围内可见
 * 管理权限：ownerId 或其祖先部门的管理员可管理
 *
 * @author 吴皓
 */
@Component
@Slf4j
public class DeptRuleHandler implements VisibilityRuleHandler {

    @Resource
    private KbUserDeptService kbUserDeptService;

    @Override
    public int rule() {
        return 2;
    }

    @Override
    public boolean canSee(VisibilityContext ctx, LibraryDO lib, LevelConfigDO cfg) {
        boolean visible = lib.getOwnerId() != null && ctx.getVisibleDeptIds().contains(lib.getOwnerId());
        log.debug("[rule=2] libId={} ownerId={} visibleDeptIds={} → {}", lib.getId(), lib.getOwnerId(), ctx.getVisibleDeptIds(), visible ? "可见" : "不可见");
        return visible;
    }

    @Override
    public boolean canManage(VisibilityContext ctx, LibraryDO lib, LevelConfigDO cfg) {
        if (lib.getOwnerId() == null) {
            log.debug("[rule=2管理] libId={} ownerId为空 → 拒绝", lib.getId());
            return false;
        }
        // 精确匹配：用户是该部门管理员
        if (kbUserDeptService.isAdmin(ctx.getUserId(), lib.getOwnerId())) {
            log.debug("[rule=2管理] libId={} ownerId={} 精确匹配管理员 → 放行", lib.getId(), lib.getOwnerId());
            return true;
        }
        // 向上查找：管理员在父部门可管理子部门库
        for (Long ancestorId : kbUserDeptService.getDeptAncestorIds(lib.getOwnerId())) {
            if (kbUserDeptService.isAdmin(ctx.getUserId(), ancestorId)) {
                log.debug("[rule=2管理] libId={} ownerId={} 祖先部门{}是管理员 → 放行", lib.getId(), lib.getOwnerId(), ancestorId);
                return true;
            }
        }
        log.debug("[rule=2管理] libId={} ownerId={} 非管理员 → 拒绝", lib.getId(), lib.getOwnerId());
        return false;
    }

    @Override
    public String toSqlCondition(VisibilityContext ctx, LevelConfigDO cfg) {
        if (ctx.getVisibleDeptIds().isEmpty()) {
            return null;
        }
        String deptIds = ctx.getVisibleDeptIds().stream().map(String::valueOf).collect(Collectors.joining(","));
        return "(kb_level_id = " + cfg.getId() + " AND owner_id IN (" + deptIds + "))";
    }
}
