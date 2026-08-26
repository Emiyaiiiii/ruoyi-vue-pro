package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 前端 C 端 - 文件节点分页结果
 *
 * <p>对应 Python 端 items-paginated 的 {@code {items:[...], pagination:{total,page,page_size}}} 结构。
 *
 * @author 吴皓
 */
@Data
public class FrontendFileNodePageVO {

    private List<FrontendFileNodeVO> items;

    private Pagination pagination;

    @Data
    public static class Pagination {

        private Long total;

        private Integer page;

        @JsonProperty("page_size")
        private Integer pageSize;

        public Pagination() {
        }

        public Pagination(Long total, Integer page, Integer pageSize) {
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }
    }
}
