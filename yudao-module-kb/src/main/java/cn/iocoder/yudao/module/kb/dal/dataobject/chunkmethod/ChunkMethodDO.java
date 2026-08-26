package cn.iocoder.yudao.module.kb.dal.dataobject.chunkmethod;

import lombok.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 切片方法 DO
 *
 * @author 吴皓
 */
@TableName("kb_chunk_method")
@KeySequence("kb_chunk_method_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkMethodDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 方法名称
     */
    private String name;

    /**
     * 方法类型: fixed_size=固定大小, semantic=语义分段, hierarchical=层次分段,
     * recursive=递归分割, sentence=按句子, paragraph=按段落, section=按章节, custom=自定义
     */
    private String methodType;

    /**
     * 方法描述
     */
    private String description;

    /**
     * 方法代码(如 fixed_size)
     */
    private String code;

    /**
     * 参数模板(JSON Schema格式)
     */
    private String parametersTemplate;

    /**
     * 默认参数(JSON格式)
     */
    private String defaultParameters;

    /**
     * 处理器类全路径
     */
    private String handlerClass;

    /**
     * 是否启用: 0=停用, 1=启用
     */
    private Integer isActive;

    /**
     * 是否默认切片方法: 0=否, 1=是
     */
    private Integer isDefaultMethod;

}
