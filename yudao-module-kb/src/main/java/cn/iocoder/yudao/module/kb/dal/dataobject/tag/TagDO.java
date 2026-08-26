package cn.iocoder.yudao.module.kb.dal.dataobject.tag;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 标签 DO
 *
 * <p>对应 Python 端 {@code tags} 表。归属（ownerId）为空表示「全局标签」，对所有用户可见；
 * 非空表示「个人标签」，仅归属用户本人可见。可见性规则对齐 Python 端
 * {@code apis/knowledge/tags/views.py}：普通用户可见「本人 + 全局」，管理员可见全部。
 *
 * @author 吴皓
 */
@TableName("kb_tag")
@KeySequence("kb_tag_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 标签名称
     */
    private String name;
    /**
     * 标签颜色
     */
    private String color;
    /**
     * 标签类型: knowledge_base=知识库 / document=文档 / chunk=文档切片 / other=其他
     */
    private String type;
    /**
     * 归属用户ID，null=全局标签。
     * 更新时必须能写成 null（个人 → 全局），因此不能走 MyBatis-Plus 默认的 NOT_NULL 策略。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long ownerId;

}