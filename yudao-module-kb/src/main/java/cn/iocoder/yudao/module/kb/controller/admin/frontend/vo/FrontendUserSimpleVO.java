package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * C 端用户精简信息，供「项目成员」模糊搜索多选。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FrontendUserSimpleVO {

    /** 用户ID，字符串避免前端雪花精度丢失 */
    private String id;

    /** 昵称 */
    private String nickname;

    /** 兼容旧 Python 前端用 name 展示 */
    private String name;

    private String username;

    private Long deptId;
}
