package cn.iocoder.yudao.module.kb.dal.dataobject.libraryext;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 知识库自定义字段值 DO
 *
 * @author 吴皓
 */
@TableName("kb_library_ext")
@KeySequence("kb_library_ext_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryExtDO extends BaseDO {

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
     * 字段key（来自分类 column_config 的自定义字段定义）
     */
    private String fieldKey;
    /**
     * 字段值（文本；成员多选为 JSON 数组字符串；部门/日期/数字/下拉为字符串）
     */
    private String fieldValue;

}
