package cn.iocoder.yudao.module.kb.dal.dataobject.category;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 知识库分类 DO
 *
 * @author 吴皓
 */
@TableName("kb_category")
@KeySequence("kb_category_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDO extends BaseDO {

    public static final Long PARENT_ID_ROOT = 0L;

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 分类名称
     */
    private String name;
    /**
     * 关联层级配置ID
     */
    private Long kbLevelId;
    /**
     * 父分类ID: 0=顶级分类
     */
    private Long parentId;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 状态: 0=启用, 1=禁用
     */
    private Integer status;
    /**
     * 表头配置(JSON): 该分类下文件列表的动态表头，如 [{"key":"name","label":"名称","visible":true},...]
     */
    private String columnConfig;
    /**
     * 是否项目成果库分类: 0=否, 1=是。
     * 该分类及其子分类下创建的知识库会自动标记为项目库，纳入项目成员管理。
     */
    private Integer isProject;

}
