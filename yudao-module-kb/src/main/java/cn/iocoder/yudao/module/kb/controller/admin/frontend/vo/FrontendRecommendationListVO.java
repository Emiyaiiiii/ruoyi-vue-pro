package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 前端推荐知识库列表（对应 /recommendations/knowledge-bases/）
 *
 * <p>Python 端返回 {@code data = {recommendations, count, elapsed_ms}}，前端读取
 * {@code res.data.recommendations}。
 *
 * @author 吴皓
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FrontendRecommendationListVO {

    private List<FrontendRecommendationVO> recommendations;

    private Integer count;

    @JsonProperty("elapsed_ms")
    private Integer elapsedMs;
}
