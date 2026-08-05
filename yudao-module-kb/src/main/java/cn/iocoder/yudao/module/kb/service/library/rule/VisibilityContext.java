package cn.iocoder.yudao.module.kb.service.library.rule;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * 可见性判断的共享上下文
 * <p>
 * 封装用户身份信息、部门范围等，一次构建、多处复用，
 * 避免在 filterVisible / selectPageWithVisibility / validateManagementPermission 中重复计算。
 *
 * @author 吴皓
 */
@Data
@Builder
public class VisibilityContext {

    /** 当前用户ID */
    private Long userId;

    /** 是否为超级管理员/租户管理员（超管跳过所有可见性过滤） */
    private boolean superAdmin;

    /** 用户所属的所有部门ID（系统部门 + kb_user_dept 扩展部门） */
    private Set<Long> userDeptIds;

    /** 用户可见的部门范围 = userDeptIds + 所有祖先部门 */
    private Set<Long> visibleDeptIds;
}
