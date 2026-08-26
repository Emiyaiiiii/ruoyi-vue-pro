package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * 前端 C 端 - 知识库新增/修改 Request VO
 *
 * <p>对齐 Python 端 {@code /knowledge/bases/} 的入参结构（snake_case）。
 * 分类字段 {@code category} 既可能是分类主键字符串（kb_category.id），也可能是兼容 slug。
 * 自定义字段值统一放在 {@code ext_values}（key=字段 key，value=字段值字符串）。
 *
 * @author 吴皓
 */
@Data
public class FrontendKnowledgeBaseSaveReqVO {

    /** 知识库名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 分类值：分类主键字符串或 slug */
    private String category;

    /** 是否公开到广场 */
    @JsonProperty("is_public")
    private Boolean isPublic;

    /** 封面图片 URL */
    @JsonProperty("cover_url")
    private String coverUrl;

    /** 自定义字段值（key=字段 key，value=字段值字符串；成员多选为 JSON 数组字符串） */
    @JsonProperty("ext_values")
    private Map<String, String> extValues;

    /** 项目成员用户ID（写入 kb_project_member，仅项目成果库生效；可用数字或数字字符串，避免前端雪花ID精度丢失） */
    @JsonProperty("member_ids")
    private java.util.List<String> memberIds;

    /** 旧版扩展信息（Python 端 structured 字段，当前已由 columnConfig 自定义字段替代，暂不处理） */
    @JsonProperty("extra_info")
    private Map<String, Object> extraInfo;

}
