package cn.iocoder.yudao.module.kb.dal.mysql.userdept;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.userdept.KbUserDeptDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 分页查询指定部门的用户（数据库级分页）
     * 数据来源合并两个部分：
     * 1. system_user.dept_id 属于目标部门（系统主部门）
     * 2. kb_user_dept.dept_id 属于目标部门（KB 补充分配）
     *
     * @param page    分页参数
     * @param deptIds 部门ID集合
     * @return 分页结果
     */
    @Select({
            "<script>",
            "SELECT DISTINCT u.id, u.nickname, u.dept_id",
            "FROM system_users u",
            "LEFT JOIN kb_user_dept k ON u.id = k.user_id AND k.dept_id IN",
            "<foreach collection='deptIds' item='deptId' open='(' separator=',' close=')'>",
            "#{deptId}",
            "</foreach>",
            "WHERE u.deleted = 0",
            "AND (u.dept_id IN",
            "<foreach collection='deptIds' item='deptId' open='(' separator=',' close=')'>",
            "#{deptId}",
            "</foreach>",
            "OR k.user_id IS NOT NULL)",
            "ORDER BY u.id DESC",
            "</script>"
    })
    IPage<SystemUserSimpleVO> selectSystemUserPageByDeptIds(
            Page<SystemUserSimpleVO> page,
            @Param("deptIds") Set<Long> deptIds
    );

}
