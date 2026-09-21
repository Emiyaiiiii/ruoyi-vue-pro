package cn.iocoder.yudao.module.kb.dal.dataobject.chunk;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 切片修订历史 DO（#7 chunk 编辑 + 修订历史）
 *
 * <p>每次变更（编辑/回滚）写入一个「变更后」内容快照，revision 从 1 递增，
 * 当前版本 = MAX(revision)，无记录表示从未编辑（版本号 0）。
 * 回滚 = 取目标版本内容应用为当前，并新增一条 revert 快照。</p>
 */
@TableName("kb_chunk_revision")
@KeySequence("kb_chunk_revision_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class KbChunkRevisionDO extends BaseDO {

    @TableId
    private Long id;

    /** 切片标识（复合串：{document_id}_{chunk_index}，与 ES/Milvus doc id 一致） */
    private String chunkId;

    /** 所属文档ID */
    private Long documentId;

    /** 所属知识库ID */
    private Long kbId;

    /** 版本号（从1递增；0=从未编辑） */
    private Integer revision;

    /** 该版本内容快照（变更后） */
    private String content;

    /** 内容 sha256 */
    private String contentHash;

    /** 编辑者用户ID */
    private Long editorId;

    /** 编辑者昵称 */
    private String editorName;

    /** 变更类型: edit=编辑, revert=回滚 */
    private String changeType;
}
