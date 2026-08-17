package cn.iocoder.yudao.module.agent.controller.admin.skillmeta.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 技能商店分页 Request VO
 *
 * @author 吴皓
 */
@Schema(description = "管理后台 - 技能商店分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SkillMetaPageReqVO extends PageParam {

    @Schema(description = "来源: builtin/customized", example = "customized")
    private String source;

    @Schema(description = "可见性: 0=个人, 1=公开", example = "1")
    private Integer visibility;

    @Schema(description = "状态: 0=停用, 1=启用", example = "1")
    private Integer status;

    @Schema(description = "关键字：名称/描述", example = "PDF")
    private String search;

}
