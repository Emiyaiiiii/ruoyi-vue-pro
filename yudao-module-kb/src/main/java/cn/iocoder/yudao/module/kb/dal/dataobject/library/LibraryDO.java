package cn.iocoder.yudao.module.kb.dal.dataobject.library;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 知识库 DO
 *
 * @author 吴皓
 */
@TableName("kb_library")
@KeySequence("kb_library_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 知识库名称
     */
    private String name;
    /**
     * 分类ID
     */
    private Long categoryId;
    /**
     * 关联层级配置ID
     */
    private Long kbLevelId;
    /**
     * 所有者ID: 用户或部门, 取决于层级配置的owner_dim
     */
    private Long ownerId;
    /**
     * 描述
     */
    private String description;
    /**
     * 封面图片URL
     */
    private String coverUrl;
    /**
     * 文档数量
     */
    private Integer docCount;
    /**
     * 状态: 0=启用, 1=禁用
     */
    private Integer status;
    /**
     * 是否公开到广场: 0=否, 1=是
     */
    private Integer isPublic;
    /**
     * 是否项目成果库: 0=否, 1=是
     */
    private Integer isProject;

}
