package cn.iocoder.yudao.module.kb.service.userdept;

import cn.iocoder.yudao.module.kb.controller.admin.userdept.vo.DeptMemberRespVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.userdept.KbUserDeptDO;

import java.util.List;
import java.util.Set;

/**
 * 知识库用户部门关联 Service 接口
 *
 * @author 吴皓
 */
public interface KbUserDeptService {

    /**
     * 添加用户到部门（成员）
     */
    Long addMember(Long userId, Long deptId);

    /**
     * 添加用户为部门管理员
     */
    Long addAdmin(Long userId, Long deptId);

    /**
     * 移除用户与部门的关联（删除 kb_user_dept 记录，用户仍保留在系统部门中）
     */
    void remove(Long userId, Long deptId);

    /**
     * 设置用户在部门中的角色（成员/管理员切换）
     */
    void setRole(Long userId, Long deptId, Integer role);

    /**
     * 获取用户关联的所有部门ID
     */
    Set<Long> getDeptIdsByUserId(Long userId);

    /**
     * 检查用户是否为指定部门的管理员
     */
    boolean isAdmin(Long userId, Long deptId);

    /**
     * 检查用户是否属于指定部门（成员或管理员）
     */
    boolean isMember(Long userId, Long deptId);

    /**
     * 获取部门下所有关联用户（合并系统用户 + kb_user_dept 角色）
     */
    List<DeptMemberRespVO> getByDeptId(Long deptId);

}
