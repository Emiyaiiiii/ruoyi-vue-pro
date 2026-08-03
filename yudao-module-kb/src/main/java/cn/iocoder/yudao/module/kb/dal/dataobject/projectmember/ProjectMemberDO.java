package cn.iocoder.yudao.module.kb.dal.dataobject.projectmember;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 知识库项目成员 DO
 * 项目成果库的文档内容访问控制，仅项目成员可看文档内容
 *
 * @author 吴皓
 */
@TableName("kb_project_member")
@KeySequence("kb_project_member_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 知识库ID（项目）
     */
    private Long kbId;
    /**
     * 项目成员用户ID
     */
    private Long userId;

}
