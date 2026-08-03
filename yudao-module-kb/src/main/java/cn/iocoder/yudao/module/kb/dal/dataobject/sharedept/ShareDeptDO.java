package cn.iocoder.yudao.module.kb.dal.dataobject.sharedept;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 知识库共享部门关联 DO
 *
 * @author 吴皓
 */
@TableName("kb_share_dept")
@KeySequence("kb_share_dept_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareDeptDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 知识库ID
     */
    private Long kbId;
    /**
     * 共享目标部门ID
     */
    private Long deptId;


}