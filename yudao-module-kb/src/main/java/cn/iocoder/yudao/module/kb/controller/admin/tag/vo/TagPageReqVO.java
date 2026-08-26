package cn.iocoder.yudao.module.kb.controller.admin.tag.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 标签分页 Request VO")
@Data
public class TagPageReqVO extends PageParam {

    @Schema(description = "标签名称（模糊搜索）", example = "知识库")
    private String name;

    @Schema(description = "标签类型", example = "document")
    private String type;

    @Schema(description = "归属范围: global=全局, personal=个人，为空=默认（个人+全局）", example = "global")
    private String scope;

}