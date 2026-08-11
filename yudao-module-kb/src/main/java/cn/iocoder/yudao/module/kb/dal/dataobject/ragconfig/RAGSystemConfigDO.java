package cn.iocoder.yudao.module.kb.dal.dataobject.ragconfig;

import lombok.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RAG系统配置 DO
 *
 * @author 吴皓
 */
@TableName("kb_rag_config")
@KeySequence("kb_rag_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGSystemConfigDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 所属模块: retrieval=检索, rerank=重排序, chunking=切片,
     * llm=大模型, cache=缓存, batch=批量处理, conversation=对话
     */
    private String module;

    /**
     * 配置键名
     */
    @TableField("`key`")
    private String key;

    /**
     * 配置值(字符串存储，根据value_type解析)
     */
    private String value;

    /**
     * 值类型: int=整数, float=浮点数, bool=布尔值, str=字符串, json=JSON对象
     */
    private String valueType;

    /**
     * 配置说明
     */
    private String description;

    /**
     * 是否启用: 0=停用, 1=启用
     */
    private Integer isActive;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 获取类型转换后的值（不持久化，仅用于序列化输出）
     */
    @TableField(exist = false)
    private Object typedValue;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 计算类型转换后的值
     */
    public void computeTypedValue() {
        if (this.value == null) {
            this.typedValue = null;
            return;
        }
        if (this.valueType == null) {
            this.typedValue = this.value;
            return;
        }
        try {
            switch (this.valueType) {
                case "int":
                    this.typedValue = Integer.parseInt(this.value);
                    break;
                case "float":
                    this.typedValue = Double.parseDouble(this.value);
                    break;
                case "bool":
                    this.typedValue = "true".equalsIgnoreCase(this.value) || "1".equals(this.value);
                    break;
                case "json":
                    this.typedValue = OBJECT_MAPPER.readValue(this.value, Object.class);
                    break;
                default:
                    this.typedValue = this.value;
            }
        } catch (Exception e) {
            this.typedValue = this.value;
        }
    }

    // ==================== 模块显示映射 ====================

    public static final java.util.Map<String, String> MODULE_DISPLAY_MAP = new java.util.LinkedHashMap<>();
    static {
        MODULE_DISPLAY_MAP.put("retrieval", "检索模块");
        MODULE_DISPLAY_MAP.put("rerank", "重排序模块");
        MODULE_DISPLAY_MAP.put("chunking", "切片模块");
        MODULE_DISPLAY_MAP.put("llm", "大模型模块");
        MODULE_DISPLAY_MAP.put("cache", "缓存模块");
        MODULE_DISPLAY_MAP.put("batch", "批量处理模块");
        MODULE_DISPLAY_MAP.put("conversation", "对话模块");
    }

    // ==================== 值类型显示映射 ====================

    public static final java.util.Map<String, String> VALUE_TYPE_DISPLAY_MAP = new java.util.LinkedHashMap<>();
    static {
        VALUE_TYPE_DISPLAY_MAP.put("int", "整数");
        VALUE_TYPE_DISPLAY_MAP.put("float", "浮点数");
        VALUE_TYPE_DISPLAY_MAP.put("bool", "布尔值");
        VALUE_TYPE_DISPLAY_MAP.put("str", "字符串");
        VALUE_TYPE_DISPLAY_MAP.put("json", "JSON对象");
    }
}
