package cn.iocoder.yudao.module.kb.dal.dataobject.userdept;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 知识库用户部门关联 DO
 * 统一管理用户与部门（院/公司/咨询评估中心）的多对多关系
 * role=0 为成员（列表可见），role=1 为管理员（列表可见+管理权限）
 *
 * @author 吴皓
 */
@TableName("kb_user_dept")
@KeySequence("kb_user_dept_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbUserDeptDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 部门ID（院/公司/咨询评估中心）
     */
    private Long deptId;
    /**
     * 角色: 0=成员, 1=管理员
     */
    private Integer role;

}
