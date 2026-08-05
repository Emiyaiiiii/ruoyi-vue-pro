package cn.iocoder.yudao.module.kb.dal.mysql.userdept;

import lombok.Data;

/**
 * system_user 表简单查询结果 VO（仅用于部门成员分页查询）
 *
 * @author 吴皓
 */
@Data
public class SystemUserSimpleVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 部门ID
     */
    private Long deptId;

}
