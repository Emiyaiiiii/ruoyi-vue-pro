package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 前端 C 端 - 标签 VO
 *
 * <p>对齐 Python 端 {@code apis/knowledge/tags} 返回字段，C 端上传下拉读取 {@code name}，
 * 标签管理页读取 {@code name/color/created_at}。
 *
 * @author 吴皓
 */
@Data
public class FrontendTagVO {

    private Long id;

    private String name;

    private String color;

    private String type;

    /** 归属用户 ID，null=全局标签 */
    private Long owner;

    @JsonProperty("is_global")
    private Boolean isGlobal;

    @JsonProperty("created_at")
    private String createdAt;

}