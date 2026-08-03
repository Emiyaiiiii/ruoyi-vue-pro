package cn.iocoder.yudao.module.kb.service.userdept;

import cn.iocoder.yudao.module.kb.controller.admin.userdept.vo.DeptMemberRespVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.userdept.KbUserDeptDO;
import cn.iocoder.yudao.module.kb.dal.mysql.userdept.KbUserDeptMapper;
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
        }
    }

    @Override
    public Set<Long> getDeptIdsByUserId(Long userId) {
        return kbUserDeptMapper.selectDeptIdsByUserId(userId);
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
