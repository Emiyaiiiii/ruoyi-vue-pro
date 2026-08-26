package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * 前端知识库列表项
 *
 * <p>字段采用 snake_case（与 Python 端返回一致），前端 SquareKnowledge / KnowledgeList 组件读取。
 *
 * @author 吴皓
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FrontendKnowledgeBaseVO {

    private Long id;

    private String name;

    private String description;

    /** 分类值（Python 端为字符串 slug，此处暂不映射，可为 null） */
    private String category;

    /** 所有者 ID，前端用 item.owner 判断是否为本人 */
    private Long owner;

    /** 所有者显示名（用户昵称/部门名，未解析时为 null，前端回退显示 owner id） */
    @JsonProperty("owner_name")
    private String ownerName;

    /** 状态: 0=启用, 1=禁用 */
    private Integer status;

    /** 创建人（昵称） */
    private String creator;

    @JsonProperty("is_public")
    private Boolean isPublic;

    @JsonProperty("document_count")
    private Integer documentCount;

    @JsonProperty("organization_name")
    private String organizationName;

    @JsonProperty("created_at")
    private String createdAt;

    /** 前端据此判断是否可点击进入（false/缺失会被渲染为 disabled） */
    @JsonProperty("can_view")
    private Boolean canView;

    /** 前端据此判断列表编辑/删除是否可点，对应 {@code LibraryService.canManage} */
    @JsonProperty("can_manage")
    private Boolean canManage;

    @JsonProperty("cover_url")
    private String coverUrl;

    /** 自定义字段值（key=字段 key，value=字段值字符串；成员多选为 JSON 数组字符串） */
    @JsonProperty("ext_values")
    private Map<String, String> extValues;
}
