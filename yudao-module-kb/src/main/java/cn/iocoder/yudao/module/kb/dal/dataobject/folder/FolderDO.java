package cn.iocoder.yudao.module.kb.dal.dataobject.folder;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 文档文件夹 DO
 *
 * @author 吴皓
 */
@TableName("kb_document_folder")
@KeySequence("kb_document_folder_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderDO extends BaseDO {

    public static final Long PARENT_ID_ROOT = 0L;

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 所属知识库ID
     */
    private Long kbId;
    /**
     * 文件夹名称
     */
    private String name;
    /**
     * 父文件夹ID: 0=根目录
     */
    private Long parentId;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 子文件夹列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<FolderDO> children;

}