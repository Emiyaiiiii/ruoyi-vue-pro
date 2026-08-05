package cn.iocoder.yudao.module.kb.service.library.rule;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.sharedept.ShareDeptDO;
import cn.iocoder.yudao.module.kb.dal.mysql.sharedept.ShareDeptMapper;
import cn.iocoder.yudao.module.kb.service.userdept.KbUserDeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.stream.Collectors;

/**
 * rule=5 指定部门列表知识库规则处理器
 * <p>
 * 可见性：用户任一部门在 kb_share_dept 共享列表中可见
 * 管理权限：根据 ownerDim 决定
 *   - ownerDim=1（用户归属）→ 仅所有者本人
 *   - ownerDim=2（部门归属）→ 该部门或其祖先部门的管理员
 *
 * @author 吴皓
 */
@Component
@Slf4j
public class SharedDeptRuleHandler implements VisibilityRuleHandler {

    @Resource
    private ShareDeptMapper shareDeptMapper;

    @Resource
    private KbUserDeptService kbUserDeptService;

    @Override
    public int rule() {
        return 5;
    }

    @Override
    public boolean canSee(VisibilityContext ctx, LibraryDO lib, LevelConfigDO cfg) {
        if (CollUtil.isEmpty(ctx.getUserDeptIds())) {
            log.debug("[rule=5] libId={} userDeptIds为空 → 不可见", lib.getId());
            return false;
        }
        long shareCount = shareDeptMapper.selectCount(
                new LambdaQueryWrapperX<ShareDeptDO>()
                        .eq(ShareDeptDO::getKbId, lib.getId())
                        .in(ShareDeptDO::getDeptId, ctx.getUserDeptIds())
        );
        boolean visible = shareCount > 0;
        log.debug("[rule=5] libId={} 共享匹配数={} → {}", lib.getId(), shareCount, visible ? "可见" : "不可见");
        return visible;
    }

    @Override
    public boolean canManage(VisibilityContext ctx, LibraryDO lib, LevelConfigDO cfg) {
        // 根据 ownerDim 决定管理权限
        if (cfg.getOwnerDim() != null && cfg.getOwnerDim() == 1) {
            // 按用户归属 → 仅所有者
            boolean manageable = lib.getOwnerId() != null && lib.getOwnerId().equals(ctx.getUserId());
            log.debug("[rule=5管理] ownerDim=1 libId={} ownerId={} userId={} → {}", lib.getId(), lib.getOwnerId(), ctx.getUserId(), manageable ? "放行" : "拒绝");
            return manageable;
        }

        // 按部门归属 → 该部门或其祖先部门的管理员
        if (lib.getOwnerId() == null) {
            log.debug("[rule=5管理] ownerDim=2 libId={} ownerId为空 → 拒绝", lib.getId());
            return false;
        }
        if (kbUserDeptService.isAdmin(ctx.getUserId(), lib.getOwnerId())) {
            log.debug("[rule=5管理] ownerDim=2 libId={} ownerId={} 精确匹配管理员 → 放行", lib.getId(), lib.getOwnerId());
            return true;
        }
        for (Long ancestorId : kbUserDeptService.getDeptAncestorIds(lib.getOwnerId())) {
            if (kbUserDeptService.isAdmin(ctx.getUserId(), ancestorId)) {
                log.debug("[rule=5管理] ownerDim=2 libId={} ownerId={} 祖先部门{}是管理员 → 放行", lib.getId(), lib.getOwnerId(), ancestorId);
                return true;
            }
        }
        log.debug("[rule=5管理] ownerDim=2 libId={} ownerId={} 非管理员 → 拒绝", lib.getId(), lib.getOwnerId());
        return false;
    }

    @Override
    public String toSqlCondition(VisibilityContext ctx, LevelConfigDO cfg) {
        if (CollUtil.isEmpty(ctx.getUserDeptIds())) {
            return null;
        }
        String deptIds = ctx.getUserDeptIds().stream().map(String::valueOf).collect(Collectors.joining(","));
        return "id IN (SELECT kb_id FROM kb_share_dept WHERE dept_id IN (" + deptIds + "))";
    }
}
