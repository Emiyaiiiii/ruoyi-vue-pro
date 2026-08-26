package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 前端 C 端 - 文件节点（文件夹/文档）列表项
 *
 * <p>对应 Python 端 file-nodes 接口返回的节点结构，前端
 * knowledge-folder-detail 组件读取 item_type / status / thumbnail 等字段。
 *
 * @author 吴皓
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FrontendFileNodeVO {

    /** 节点 ID：文件夹=文件夹主键，文档=文档主键 */
    private Long id;

    /** 节点名称：文件夹名 / 文件名 */
    private String name;

    /** 节点类型：folder=文件夹，document=文档 */
    @JsonProperty("item_type")
    private String itemType;

    /** 缩略图/类型图标（placeholder 为前端静态图标，暂不返回，由 P4 补齐） */
    private Thumbnail thumbnail;

    /** 标签列表 */
    private List<String> tags;

    /** 创建时间 */
    @JsonProperty("created_at")
    private String createdAt;

    /** 文档处理状态：pending/processing/completed/failed（文件夹为 null） */
    private String status;

    /** 状态中文显示：待处理/正在处理/成功/失败 */
    @JsonProperty("status_display")
    private String statusDisplay;

    /** 底层文档 ID（仅文档节点），用于重建向量 */
    @JsonProperty("doc_id")
    private Long docId;

    /** 文件扩展名（仅文档节点） */
    private String extension;

    /** 文件访问 URL（仅文档节点） */
    private String url;

    /** 文件存储路径（仅文档节点） */
    @JsonProperty("file_path")
    private String filePath;

    /** 文件大小（MB，保留两位小数） */
    @JsonProperty("file_size_mb")
    private String fileSizeMb;

    @JsonProperty("knowledge_base_id")
    private Long knowledgeBaseId;

    @JsonProperty("knowledge_base_name")
    private String knowledgeBaseName;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Thumbnail {
        private String placeholder;
        private String type;
    }
}
