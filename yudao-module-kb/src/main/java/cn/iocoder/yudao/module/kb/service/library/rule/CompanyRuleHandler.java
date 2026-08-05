package cn.iocoder.yudao.module.kb.service.library.rule;

import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.service.userdept.KbUserDeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * rule=3 公司级知识库规则处理器
 * <p>
 * 可见性：全员可见
 * 管理权限：公司部门管理员（ownerId 或其祖先部门的管理员）可管理
 *
 * @author 吴皓
 */
@Component
@Slf4j
public class CompanyRuleHandler implements VisibilityRuleHandler {

    @Resource
    private KbUserDeptService kbUserDeptService;

    @Override
    public int rule() {
        return 3;
    }

    @Override
    public boolean canSee(VisibilityContext ctx, LibraryDO lib, LevelConfigDO cfg) {
        log.debug("[rule=3] libId={} → 全员可见", lib.getId());
        return true;
    }

    @Override
    public boolean canManage(VisibilityContext ctx, LibraryDO lib, LevelConfigDO cfg) {
        if (lib.getOwnerId() == null) {
            log.debug("[rule=3管理] libId={} ownerId为空 → 拒绝", lib.getId());
            return false;
        }
        // 精确匹配：用户是该部门管理员
        if (kbUserDeptService.isAdmin(ctx.getUserId(), lib.getOwnerId())) {
            log.debug("[rule=3管理] libId={} ownerId={} 精确匹配管理员 → 放行", lib.getId(), lib.getOwnerId());
            return true;
        }
        // 向上查找：管理员在父部门可管理子部门库
        for (Long ancestorId : kbUserDeptService.getDeptAncestorIds(lib.getOwnerId())) {
            if (kbUserDeptService.isAdmin(ctx.getUserId(), ancestorId)) {
                log.debug("[rule=3管理] libId={} ownerId={} 祖先部门{}是管理员 → 放行", lib.getId(), lib.getOwnerId(), ancestorId);
                return true;
            }
        }
        log.debug("[rule=3管理] libId={} ownerId={} 非管理员 → 拒绝", lib.getId(), lib.getOwnerId());
        return false;
    }

    @Override
    public String toSqlCondition(VisibilityContext ctx, LevelConfigDO cfg) {
        // 公司级：全员可见，只需匹配 kb_level_id
        return "kb_level_id = " + cfg.getId();
    }
}
