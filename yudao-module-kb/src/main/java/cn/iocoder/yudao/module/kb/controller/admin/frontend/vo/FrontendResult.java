package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 前端 C 端兼容层统一返回结构
 *
 * <p>前端 xiaoyu-ai-front 的 axios 拦截器读取 {@code code/msg/data}；
 * 列表页额外读取顶层 {@code total}（data 直接是数组，而非 yudao 的 {@code {list,total}}）。
 *
 * @author 吴皓
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FrontendResult<T> {

    private Integer code = 0;

    private String msg = "";

    private T data;

    /** 列表总条数（列表接口才有，菜单接口为 null 自动省略） */
    private Long total;

    private Integer page;

    @JsonProperty("page_size")
    private Integer pageSize;

    public static <T> FrontendResult<T> ok(T data) {
        FrontendResult<T> result = new FrontendResult<>();
        result.setData(data);
        return result;
    }

    public static <T> FrontendResult<List<T>> okList(List<T> data, Long total) {
        FrontendResult<List<T>> result = new FrontendResult<>();
        result.setData(data);
        result.setTotal(total);
        return result;
    }

    public static <T> FrontendResult<T> error(String msg) {
        FrontendResult<T> result = new FrontendResult<>();
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }
}
