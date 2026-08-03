package cn.iocoder.yudao.module.kb.dal.dataobject.document;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 知识库文件 DO
 *
 * @author 芋道源码
 */
@TableName("kb_document")
@KeySequence("kb_document_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDO extends BaseDO {

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
     * 文件名称
     */
    private String fileName;
    /**
     * 文件访问URL (芋道文件管理返回)
     */
    private String fileUrl;
    /**
     * 文件类型: pdf/docx/xlsx/pptx/jpg/png等
     */
    private String fileType;
    /**
     * 文件大小(字节)
     */
    private Long fileSize;
    /**
     * 芋道文件配置ID (infra_file_config.id)
     */
    private Long fileConfigId;
    /**
     * 文件存储路径 (芋道文件管理返回)
     */
    private String filePath;
    /**
     * 文件描述
     */
    private String description;
    /**
     * 标签 (逗号分隔)
     */
    private String tags;
    /**
     * 下载次数
     */
    private Integer downloadCount;
    /**
     * 查看次数
     */
    private Integer viewCount;
    /**
     * 状态: 0=正常, 1=禁用
     */
    private Integer status;


}