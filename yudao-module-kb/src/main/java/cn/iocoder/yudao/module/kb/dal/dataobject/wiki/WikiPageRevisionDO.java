package cn.iocoder.yudao.module.kb.dal.dataobject.wiki;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * Wiki 页面修订历史 DO（对标 WeKnora wiki_page_revisions）
 *
 * <p>只存「被替换的旧版本」快照，(page_id, revision) 唯一幂等（靠捕获 DuplicateKeyException
 * 兜并发重复写）。当前版本只活在 kb_wiki_page 主表。回滚/对比靠它；内容真实变化时写入。</p>
 */
@TableName("kb_wiki_revision")
@KeySequence("kb_wiki_revision_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class WikiPageRevisionDO extends BaseDO {

    @TableId
    private Long id;

    /** 页面ID（页面在 ES 后不再使用，保留兼容） */
    private Long pageId;

    /** 知识库ID（页面在 ES 后作为修订定位键） */
    private Long kbId;

    /** 页面 slug（页面在 ES 后作为修订定位键） */
    private String slug;

    /** 版本号(被替换的旧版本) */
    private Integer revision;

    /** 该版本标题 */
    private String title;

    /** 该版本内容快照 */
    private String content;

    /** 该版本摘要 */
    private String summary;

    /** 该版本别名(JSON) */
    private String aliases;

    /** 页类型 */
    private String pageType;

    /** 状态 */
    private String status;

    /** 编辑来源: pipeline/user/revert */
    private String editSource;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人昵称 */
    private String operatorName;
}