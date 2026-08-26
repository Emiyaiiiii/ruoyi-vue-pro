package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 前端推荐知识库单项
 *
 * <p>字段采用 snake_case，与 Python 端 {@code RecommendationService.recommend_for_user}
 * 返回的每一项保持一致；前端 XyAgent/index.vue 读取 {@code kb_id/name/documents_count/...}，
 * 并用 {@code kb_id} 作为列表项的 {@code id}。
 *
 * @author 吴皓
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FrontendRecommendationVO {

    @JsonProperty("kb_id")
    private Long kbId;

    private String name;

    private String description;

    @JsonProperty("documents_count")
    private Integer documentsCount;

    @JsonProperty("followers_count")
    private Integer followersCount;

    private List<String> tags;

    private String category;

    @JsonProperty("is_public")
    private Boolean isPublic;

    private Double score;

    private String reason;

    @JsonProperty("reason_detail")
    private List<String> reasonDetail;
}
