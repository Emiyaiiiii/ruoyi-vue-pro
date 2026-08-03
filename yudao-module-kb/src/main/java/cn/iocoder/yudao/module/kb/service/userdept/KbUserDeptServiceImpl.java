package cn.iocoder.yudao.module.kb.service.userdept;

import cn.iocoder.yudao.module.kb.controller.admin.userdept.vo.DeptMemberRespVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.userdept.KbUserDeptDO;
import cn.iocoder.yudao.module.kb.dal.mysql.userdept.KbUserDeptMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库用户部门关联 Service 实现类
 * 合并芋道系统用户（system_user.dept_id）与 kb_user_dept 角色信息
 *
 * @author 吴皓
 */
@Service
@Validated
@RequiredArgsConstructor
public class KbUserDeptServiceImpl implements KbUserDeptService {

    private final KbUserDeptMapper kbUserDeptMapper;
    private final AdminUserApi adminUserApi;
    private final DeptApi deptApi;

    @Override
    public Long addMember(Long userId, Long deptId) {
        return upsert(userId, deptId, 0);
    }

    @Override
    public Long addAdmin(Long userId, Long deptId) {
        return upsert(userId, deptId, 1);
    }

    @Override
    public void remove(Long userId, Long deptId) {
        kbUserDeptMapper.delete(new LambdaQueryWrapper<KbUserDeptDO>()
                .eq(KbUserDeptDO::getUserId, userId)
                .eq(KbUserDeptDO::getDeptId, deptId));
    }

    @Override
    public void setRole(Long userId, Long deptId, Integer role) {
        KbUserDeptDO existing = kbUserDeptMapper.selectOne(new LambdaQueryWrapper<KbUserDeptDO>()
                .eq(KbUserDeptDO::getUserId, userId)
                .eq(KbUserDeptDO::getDeptId, deptId));
        if (existing != null) {
            existing.setRole(role);
            kbUserDeptMapper.updateById(existing);
        } else {
            // 系统部门成员无 kb_user_dept 记录时，直接创建（如设为管理员）
            KbUserDeptDO record = KbUserDeptDO.builder()
                    .userId(userId)
                    .deptId(deptId)
                    .role(role)
                    .build();
            kbUserDeptMapper.insert(record);
        }
    }

    @Override
    public Set<Long> getDeptIdsByUserId(Long userId) {
        Set<Long> deptIds = new HashSet<>();
        // 1. 系统部门（system_user.dept_id）
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user != null && user.getDeptId() != null) {
            deptIds.add(user.getDeptId());
        }
        // 2. 扩展部门（kb_user_dept 补充配置）
        deptIds.addAll(kbUserDeptMapper.selectDeptIdsByUserId(userId));
        return deptIds;
    }

    @Override
    public boolean isAdmin(Long userId, Long deptId) {
        return kbUserDeptMapper.isAdmin(userId, deptId);
    }

    @Override
    public boolean isMember(Long userId, Long deptId) {
        return kbUserDeptMapper.isMember(userId, deptId);
    }

    @Override
    public Set<Long> getDeptAncestorIds(Long deptId) {
        Set<Long> ancestors = new LinkedHashSet<>();
        Long currentId = deptId;
        int maxDepth = 20; // 防止部门循环引用导致死循环
        while (currentId != null && currentId != 0 && maxDepth-- > 0) {
            DeptRespDTO dept = deptApi.getDept(currentId);
            if (dept == null) break; // 部门不存在于系统中则停止，不加入结果集
            ancestors.add(currentId);
            currentId = dept.getParentId();
        }
        return ancestors;
    }

    @Override
    public Set<Long> getAdminDeptIds(Long userId) {
        return kbUserDeptMapper.selectAdminDeptIdsByUserId(userId);
    }

    @Override
    public List<DeptMemberRespVO> getByDeptId(Long deptId) {
        // 1. 查询系统用户表中该部门下的所有用户
        List<AdminUserRespDTO> systemUsers = adminUserApi.getUserListByDeptIds(
                Collections.singleton(deptId));
        if (systemUsers == null || systemUsers.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查询 kb_user_dept 表中该部门的角色记录
        List<KbUserDeptDO> userDeptList = kbUserDeptMapper.selectByDeptId(deptId);
        Map<Long, KbUserDeptDO> userDeptMap = userDeptList.stream()
                .collect(Collectors.toMap(KbUserDeptDO::getUserId, item -> item, (a, b) -> a));

        // 3. 合并：系统用户为基础，叠加 kb_user_dept 的角色信息
        return systemUsers.stream().map(sysUser -> {
            DeptMemberRespVO vo = new DeptMemberRespVO();
            vo.setUserId(sysUser.getId());
            vo.setDeptId(deptId);
            vo.setNickname(sysUser.getNickname());
            vo.setRole(0); // 默认为成员

            KbUserDeptDO userDept = userDeptMap.get(sysUser.getId());
            if (userDept != null) {
                vo.setId(userDept.getId());
                vo.setRole(userDept.getRole());
                vo.setCreateTime(userDept.getCreateTime());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 新增或更新用户部门关联（幂等操作）
     */
    private Long upsert(Long userId, Long deptId, Integer role) {
        KbUserDeptDO existing = kbUserDeptMapper.selectOne(new LambdaQueryWrapper<KbUserDeptDO>()
                .eq(KbUserDeptDO::getUserId, userId)
                .eq(KbUserDeptDO::getDeptId, deptId));
        if (existing != null) {
            existing.setRole(role);
            kbUserDeptMapper.updateById(existing);
            return existing.getId();
        }
        KbUserDeptDO record = KbUserDeptDO.builder()
                .userId(userId)
                .deptId(deptId)
                .role(role)
                .build();
        kbUserDeptMapper.insert(record);
        return record.getId();
    }

}
