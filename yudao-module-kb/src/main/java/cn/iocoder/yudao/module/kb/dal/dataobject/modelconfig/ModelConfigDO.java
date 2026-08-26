package cn.iocoder.yudao.module.kb.dal.dataobject.modelconfig;

import lombok.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 大模型配置 DO
 *
 * @author 吴皓
 */
@TableName("kb_model_config")
@KeySequence("kb_model_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 模型唯一标识
     */
    private String uid;

    /**
     * 具体模型名（如 text-embedding-v4 / deepseek-chat / DeepSeek-OCR-2）
     */
    private String model;

    /**
     * 模型名称
     */
    private String name;

    /**
     * API地址
     */
    private String url;

    /**
     * API密钥
     */
    private String appkey;

    /**
     * 用途分类: embedding=嵌入/向量模型, llm=大模型, ocr=OCR/多模态模型
     */
    private String modelType;

    /**
     * 是否启用思考能力: 0=否, 1=是
     */
    private Integer thinkingEnabled;

    /**
     * 是否支持多模态(VL): 0=否, 1=是
     */
    private Boolean vlSupported;

    /**
     * 是否激活: 0=停用, 1=激活
     */
    private Integer isActive;

    /**
     * 模型描述
     */
    private String description;

    /**
     * 最大Token数
     */
    private Integer maxTokens;

    /**
     * 上下文长度
     */
    private Integer contextLength;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * Top-P参数
     */
    private Double topP;

    /**
     * 配置参数(JSON格式)
     */
    private String config;

    /**
     * 排序顺序(升序)
     */
    private Integer sortOrder;

    /**
     * 是否置顶: 0=否, 1=是
     */
    private Integer isPinned;

    /**
     * 激活时间
     */
    private LocalDateTime activatedAt;

}
