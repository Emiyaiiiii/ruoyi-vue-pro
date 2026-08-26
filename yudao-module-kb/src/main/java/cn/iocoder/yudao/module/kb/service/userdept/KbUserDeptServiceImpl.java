package cn.iocoder.yudao.module.kb.service.userdept;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.kb.controller.admin.userdept.vo.DeptMemberPageReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.userdept.vo.DeptMemberRespVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.userdept.KbUserDeptDO;
import cn.iocoder.yudao.module.kb.dal.mysql.userdept.KbUserDeptMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.userdept.SystemUserSimpleVO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    public Long getSecondLevelDeptId(Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null || user.getDeptId() == null || user.getDeptId() == 0L) {
            return null;
        }
        List<Long> chain = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        Long currentId = user.getDeptId();
        int guard = 20;
        while (currentId != null && currentId != 0L && guard-- > 0 && seen.add(currentId)) {
            chain.add(currentId);
            DeptRespDTO dept = deptApi.getDept(currentId);
            if (dept == null) {
                break;
            }
            Long parentId = dept.getParentId();
            if (parentId == null || parentId == 0L) {
                break;
            }
            currentId = parentId;
        }
        if (chain.size() < 2) {
            return null;
        }
        return chain.get(chain.size() - 2);
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

    @Override
    public PageResult<DeptMemberRespVO> getDeptMemberPage(DeptMemberPageReqVO reqVO) {
        // 1. 计算目标部门ID集合（是否包含子部门）
        Set<Long> targetDeptIds = new HashSet<>();
        targetDeptIds.add(reqVO.getDeptId());
        Map<Long, String> deptNameMap = new HashMap<>();
        // 获取当前部门名称
        DeptRespDTO currentDept = deptApi.getDept(reqVO.getDeptId());
        if (currentDept != null) {
            deptNameMap.put(currentDept.getId(), currentDept.getName());
        }
        if (Boolean.TRUE.equals(reqVO.getIncludeChildren())) {
            List<DeptRespDTO> childDepts = deptApi.getChildDeptList(reqVO.getDeptId());
            if (childDepts != null) {
                childDepts.forEach(d -> {
                    targetDeptIds.add(d.getId());
                    deptNameMap.put(d.getId(), d.getName());
                });
            }
        }

        // 2. 数据库级分页查询 system_user 表中这些部门下的用户
        Page<SystemUserSimpleVO> page = new Page<>(reqVO.getPageNo(), reqVO.getPageSize());
        IPage<SystemUserSimpleVO> userPage = kbUserDeptMapper.selectSystemUserPageByDeptIds(page, targetDeptIds);
        
        if (userPage.getRecords() == null || userPage.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        // 3. 查询当前页用户对应的 kb_user_dept 角色记录
        Set<Long> userIds = userPage.getRecords().stream()
                .map(SystemUserSimpleVO::getId)
                .collect(Collectors.toSet());
        List<KbUserDeptDO> userDeptList = kbUserDeptMapper.selectList(
                new LambdaQueryWrapper<KbUserDeptDO>()
                        .in(KbUserDeptDO::getUserId, userIds)
                        .in(KbUserDeptDO::getDeptId, targetDeptIds));
        Map<Long, KbUserDeptDO> userDeptMap = userDeptList.stream()
                .collect(Collectors.toMap(KbUserDeptDO::getUserId, item -> item, (a, b) -> a));

        // 4. 合并：系统用户为基础，叠加 kb_user_dept 的角色信息
        List<DeptMemberRespVO> pageList = userPage.getRecords().stream().map(sysUser -> {
            DeptMemberRespVO vo = new DeptMemberRespVO();
            vo.setUserId(sysUser.getId());
            vo.setDeptId(sysUser.getDeptId());
            vo.setDeptName(deptNameMap.getOrDefault(sysUser.getDeptId(), ""));
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

        return new PageResult<>(pageList, userPage.getTotal());
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
