package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 前端导航菜单节点
 *
 * <p>对应 Python 端 SystemMenu 的 {@code label/value/icon/sort/status/visible/children} 结构，
 * 前端 Layout.vue 的 handleData 会在此基础上补齐 path 与 level。
 *
 * @author 吴皓
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FrontendMenuNodeVO {

    /** 节点 id：菜单节点为数字主键，分类节点为 value（slug）字符串，前端只当字符串/唯一键使用 */
    private Object id;

    private String label;

    private String value;

    private String icon;

    private Integer sort;

    /** 菜单类型：1=目录，2=菜单 */
    private Integer type;

    private String path;

    /** 是否启用（true=启用），前端按布尔值理解 */
    private Boolean status;

    /** 是否可见（false 的节点会被前端过滤，如「新增智能体」） */
    private Boolean visible;

    /**
     * 分类的表头配置（JSON 字符串），用于知识库列表动态表头 / 新建表单；菜单节点为 null。
     * 输出 snake_case，兼容 C 端读取 columnConfig / column_config。
     */
    @JsonProperty("column_config")
    @JsonAlias({"columnConfig", "column_config"})
    private String columnConfig;

    /** 层级可见规则：1=个人（可公开到广场），2=院级，3=公司。菜单节点为 null */
    @JsonProperty("visibility_rule")
    @JsonAlias({"visibilityRule", "visibility_rule"})
    private Integer visibilityRule;

    /** 是否项目成果库分类：1=是。C 端用来决定新建时是否展示「项目成员」多选 */
    @JsonProperty("is_project")
    @JsonAlias({"isProject", "is_project"})
    private Integer isProject;

    private List<FrontendMenuNodeVO> children;
}
