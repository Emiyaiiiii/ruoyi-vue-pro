package cn.iocoder.yudao.module.kb.dal.dataobject.wiki;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * Wiki 目录树 DO（对齐 WeKnora wiki_folders）
 *
 * <p>邻接表树（parent_id，0=根）；path 为物化的 "/" 连接名称链（如 AI/RAG），
 * 供廉价展示/排序；页面经 WikiPageDO.folderId 挂到树上。
 * 目录可先于页面存在（先建骨架再归档页面）。</p>
 */
@TableName("kb_wiki_folder")
@KeySequence("kb_wiki_folder_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class WikiFolderDO extends BaseDO {

    @TableId
    private Long id;

    /** 所属知识库ID */
    private Long kbId;

    /** 父目录ID（0=根） */
    private Long parentId;

    /** 目录名称（同级下唯一） */
    private String name;

    /** 物化路径("/" 连接的名称链，如 AI/RAG) */
    private String path;

    /** 目录深度（根目录的直接子为 1） */
    private Integer depth;

    /** 兄弟排序 */
    private Integer sortOrder;
}
