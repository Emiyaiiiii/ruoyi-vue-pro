package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
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

    /** 文档/文件数（卡片“X项”展示，取自 kb_library.doc_count） */
    @JsonProperty("document_count")
    private Integer documentCount;

    /** 文件夹数（KB 下 folder 数量，取自 kb_document_folder 聚合，与 document_count 不同指标） */
    @JsonProperty("folder_count")
    private Long folderCount;

    @JsonProperty("organization_name")
    private String organizationName;

    @JsonProperty("created_at")
    private String createdAt;

    /** 更新时间（卡片顶部展示） */
    @JsonProperty("updated_at")
    private String updatedAt;

    /** 关注数（kb_follow 按知识库计数） */
    @JsonProperty("followers_count")
    private Long followersCount;

    /** 存储用量（MB，kb_document.file_size 字节数 ÷ 1024²） */
    @JsonProperty("used_storage_mb")
    private Double usedStorageMb;

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

    /** 标签列表（来自 ext_values 保留 key，前端列表/详情展示） */
    private List<String> tags;

    /** 分类类型（cascader 第一级：分类主键） */
    @JsonProperty("category_type")
    private Long categoryType;

    /** 所属部门 ID */
    private Long organization;

    /** 当前用户是否已关注 */
    @JsonProperty("is_follow")
    private Boolean isFollow;

    /** 流程配置（仅详情接口输出，前端读 process_config.chunking 回显切片配置） */
    @JsonProperty("process_config")
    private Map<String, Object> processConfig;

    /** 切片方法 ID（由 process_config.chunking.strategy 反查，仅详情接口输出） */
    @JsonProperty("chunking_method_id")
    private Long chunkingMethodId;

    /** 切片方法参数模板（JSON 已解析为对象，仅详情接口输出） */
    @JsonProperty("parameters_template")
    private Map<String, Object> parametersTemplate;

    /** 切片方法默认参数（JSON 已解析为对象，仅详情接口输出） */
    @JsonProperty("default_parameters")
    private Map<String, Object> defaultParameters;

}
