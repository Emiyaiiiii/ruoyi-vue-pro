package cn.iocoder.yudao.module.kb.dal.mysql.userdept;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.userdept.KbUserDeptDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识库用户部门关联 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface KbUserDeptMapper extends BaseMapperX<KbUserDeptDO> {

    /**
     * 获取用户关联的所有部门ID（含成员和管理员角色）
     */
    default Set<Long> selectDeptIdsByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<KbUserDeptDO>()
                .eq(KbUserDeptDO::getUserId, userId))
                .stream()
                .map(KbUserDeptDO::getDeptId)
                .collect(Collectors.toSet());
    }

    /**
     * 检查用户是否为指定部门的管理员
     */
    default boolean isAdmin(Long userId, Long deptId) {
        return selectCount(new LambdaQueryWrapperX<KbUserDeptDO>()
                .eq(KbUserDeptDO::getUserId, userId)
                .eq(KbUserDeptDO::getDeptId, deptId)
                .eq(KbUserDeptDO::getRole, 1)) > 0;
    }

    /**
     * 检查用户是否属于指定部门（成员或管理员）
     */
    default boolean isMember(Long userId, Long deptId) {
        return selectCount(new LambdaQueryWrapperX<KbUserDeptDO>()
                .eq(KbUserDeptDO::getUserId, userId)
                .eq(KbUserDeptDO::getDeptId, deptId)) > 0;
    }

    /**
     * 获取部门下所有关联用户
     */
    default List<KbUserDeptDO> selectByDeptId(Long deptId) {
        return selectList(new LambdaQueryWrapperX<KbUserDeptDO>()
                .eq(KbUserDeptDO::getDeptId, deptId));
    }

    /**
     * 获取用户作为管理员的所有部门ID
     */
    default Set<Long> selectAdminDeptIdsByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<KbUserDeptDO>()
                .eq(KbUserDeptDO::getUserId, userId)
                .eq(KbUserDeptDO::getRole, 1))
                .stream()
                .map(KbUserDeptDO::getDeptId)
                .collect(Collectors.toSet());
    }
}
