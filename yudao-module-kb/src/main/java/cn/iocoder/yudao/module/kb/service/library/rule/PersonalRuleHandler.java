package cn.iocoder.yudao.module.kb.service.library.rule;

import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * rule=1 个人知识库规则处理器
 * <p>
 * 可见性：仅所有者本人可见
 * 管理权限：仅所有者本人可管理
 *
 * @author 吴皓
 */
@Component
@Slf4j
public class PersonalRuleHandler implements VisibilityRuleHandler {

    @Override
    public int rule() {
        return 1;
    }

    @Override
    public boolean canSee(VisibilityContext ctx, LibraryDO lib, LevelConfigDO cfg) {
        boolean visible = lib.getOwnerId() != null && lib.getOwnerId().equals(ctx.getUserId());
        log.debug("[rule=1] libId={} ownerId={} userId={} → {}", lib.getId(), lib.getOwnerId(), ctx.getUserId(), visible ? "可见" : "不可见");
        return visible;
    }

    @Override
    public boolean canManage(VisibilityContext ctx, LibraryDO lib, LevelConfigDO cfg) {
        boolean manageable = lib.getOwnerId() != null && lib.getOwnerId().equals(ctx.getUserId());
        log.debug("[rule=1管理] libId={} ownerId={} userId={} → {}", lib.getId(), lib.getOwnerId(), ctx.getUserId(), manageable ? "放行" : "拒绝");
        return manageable;
    }

    @Override
    public String toSqlCondition(VisibilityContext ctx, LevelConfigDO cfg) {
        return "(kb_level_id = " + cfg.getId() + " AND owner_id = " + ctx.getUserId() + ")";
    }
}
