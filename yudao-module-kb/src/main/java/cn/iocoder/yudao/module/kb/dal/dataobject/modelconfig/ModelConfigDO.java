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
     * 部署类型:
     * doubao=豆包, bailian=百炼, lite=LiteLLM, openai=OpenAI,
     * api=通用API, xinf=Xinference, vllm=VLLM, zhipu=智谱AI, other=其他
     */
    private String deploy;

    /**
     * 是否启用思考能力: 0=否, 1=是
     */
    private Integer thinkingEnabled;

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
     * 元数据(JSON格式)
     */
    private String metadata;

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
     * 支持平台: web=Web端, app=App端, both=两者都支持
     */
    private String platform;

    /**
     * 激活时间
     */
    private LocalDateTime activatedAt;

}
