package cn.iocoder.yudao.module.kb.service.visibility;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.sharedept.ShareDeptDO;
import cn.iocoder.yudao.module.kb.dal.mysql.levelconfig.LevelConfigMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.sharedept.ShareDeptMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.userdept.KbUserDeptMapper;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KbVisibilityServiceImpl implements KbVisibilityService {

    private final LevelConfigMapper levelConfigMapper;
    private final ShareDeptMapper  shareDeptMapper;
    private final KbUserDeptMapper kbUserDeptMapper;
    private final SecurityFrameworkService securityFrameworkService;

    @Override
    public List<LibraryDO> filterVisible(List<LibraryDO> allLibs,
                                           Long userId,
                                           Long userDeptId) {

        if (CollUtil.isEmpty(allLibs)) return allLibs;

        // 超级管理员/租户管理员 → 可见所有知识库
        if (userId != null && securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode())) {
            return allLibs;
        }

        // 加载所有层级配置到 Map
        Map<Long, LevelConfigDO> configMap = levelConfigMapper.selectList()
                .stream()
                .collect(Collectors.toMap(LevelConfigDO::getId, Function.identity()));

        // 预加载用户关联的所有部门ID（支持一人多院）
        Set<Long> userDeptIds = kbUserDeptMapper.selectDeptIdsByUserId(userId);

        return allLibs.stream()
                .filter(lib -> {
                    LevelConfigDO cfg = configMap.get(lib.getKbLevelId());
                    if (cfg == null) return false;

                    switch (cfg.getVisibilityRule()) {
                        case 1:   // 个人知识库 → 只有所有者可见
                            return lib.getOwnerId() != null && lib.getOwnerId().equals(userId);

                        case 2:   // 院级 / 咨询评估 → 用户属于该部门（通过 kb_user_dept 关联）
                            return lib.getOwnerId() != null && userDeptIds.contains(lib.getOwnerId());

                        case 3:   // 公司知识库 → 全员可见
                            return true;

                        case 5:   // 指定部门列表 → 用户部门在共享列表中
                            return shareDeptMapper.selectCount(
                                    new LambdaQueryWrapper<ShareDeptDO>()
                                            .eq(ShareDeptDO::getKbId, lib.getId())
                                            .eq(ShareDeptDO::getDeptId, userDeptId)
                            ) > 0;

                        case 6:   // 知识库广场 → 公开的知识库全员可见
                            return true;

                        default:
                            return false;
                    }
                })
                .collect(Collectors.toList());
    }
}
