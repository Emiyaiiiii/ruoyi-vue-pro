package cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 知识库层级配置 DO
 *
 * @author 吴皓
 */
@TableName("kb_level_config")
@KeySequence("kb_level_config_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelConfigDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 层级编码
     */
    private String levelCode;
    /**
     * 层级名称
     */
    private String levelName;
    /**
     * 可见规则: 1=按所有者, 2=按归属部门, 3=全员, 5=指定部门列表
     */
    private Integer visibilityRule;
    /**
     * 归属维度: 0=无, 1=用户, 2=部门
     */
    private Integer ownerDim;
    /**
     * 分类可见部门范围: NULL=全员可见, JSON数组[101,102]=仅指定部门
     */
    private String deptScope;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 状态: 0=启用, 1=禁用
     */
    private Integer status;


}