package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 前端 C 端 - 新建文件夹请求
 *
 * @author 吴皓
 */
@Data
public class FrontendFileNodeFolderCreateReqVO {

    /** 父文件夹 ID，null=根目录 */
    @JsonProperty("parent_id")
    private Long parentId;

    private String name;
}
