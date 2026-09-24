package cn.iocoder.yudao.module.kb.dal.dataobject.wiki;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * Wiki 页面 DO（对标 WeKnora wiki_pages）
 *
 * <p>知识库文档经 LLM 蒸馏成的互联 markdown 页面：每个页面有唯一 slug（KB 内）、
 * 独立修订历史（kb_wiki_revision 只存旧版快照）、in/out_links 双向链接、version 乐观锁。
 * 页面删除走【物理删除】（Slug 可重建同名），故不再依赖 BaseDO 逻辑删除兜底唯一键。</p>
 */
@TableName("kb_wiki_page")
@KeySequence("kb_wiki_page_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class WikiPageDO extends BaseDO {

    @TableId
    private Long id;

    /** 所属知识库ID */
    private Long kbId;

    /** 页面唯一标识(URL友好，KB 内唯一，如 sales-funnel) */
    private String slug;

    /** 页面标题 */
    private String title;

    /** markdown 正文(内嵌 [[slug|显示名]] 双向链接) */
    private String content;

    /** 摘要 */
    private String summary;

    /** 别名列表(JSON: 消歧 + WikiSearch ILIKE) */
    private String aliases;

    /** 页面类型: summary/entity/concept/manual/root */
    private String pageType;

    /** 所属目录ID(0=根) */
    private Long folderId;

    /** 目录层级路径(JSON) */
    private String categoryPath;

    /** 目录深度 */
    private Integer depth;

    /** 来源引用(JSON 文档粒度: "kbId|docTitle") */
    private String sourceRefs;

    /** 关联 chunk id 集合(JSON chunk 粒度溯源) */
    private String chunkRefs;

    /** 反向链接(JSON slug 列表: 谁引用了本页) */
    private String inLinks;

    /** 外链(JSON slug 列表: 本页引用了谁) */
    private String outLinks;

    /** 状态: draft/published/archived */
    private String status;

    /** 当前版本号(仅真实内容变更时+1) */
    private Integer revision;

    /** 乐观锁版本号(WHERE version=? 防并发覆盖) */
    private Integer version;

    /** 最近编辑来源: pipeline/user/revert */
    private String lastEditSource;

    /** 最近编辑者ID */
    private Long lastEditorId;

    /** 最近编辑者昵称 */
    private String lastEditorName;

    /** 兄弟排序 */
    private Integer sortOrder;
}