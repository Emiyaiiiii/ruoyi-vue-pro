package cn.iocoder.yudao.module.kb.controller.admin.frontend.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 前端 C 端 - 批量移动节点请求
 *
 * @author 吴皓
 */
@Data
public class FrontendFileNodeBatchMoveReqVO {

    @JsonProperty("node_ids")
    private List<Long> nodeIds;

    /** 目标父文件夹 ID，null=目标知识库根目录 */
    @JsonProperty("target_parent_id")
    private Long targetParentId;

    /** 目标知识库 ID，null=当前知识库 */
    @JsonProperty("target_knowledge_base_id")
    private Long targetKnowledgeBaseId;

    /** 是否覆盖同名 */
    private Boolean overwrite;
}
