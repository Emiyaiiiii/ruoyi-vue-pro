package cn.iocoder.yudao.module.kb.dal.dataobject.vectortask;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 向量处理任务 DO
 *
 * 用于跟踪文档的异步向量处理流程（OCR → 分块 → Embedding → 向量存储）
 */
@TableName("kb_vector_task")
@KeySequence("kb_vector_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class VectorTaskDO extends BaseDO {

    @TableId
    private Long id;
    /** 任务ID（唯一标识，Java 生成） */
    private String taskId;
    /** 文档ID */
    private Long docId;
    /** 知识库ID */
    private Long kbId;
    /** 文件下载地址（供 Python 拉取） */
    private String fileUrl;
    /** 文件类型 */
    private String fileType;
    /** 状态：0-待提交 1-处理中 2-已完成 3-失败 4-提交失败 5-超时 6-已取消 */
    private Integer status;
    /** 进度（0-100） */
    private Integer progress;
    /** 当前处理步骤 */
    private String currentStep;
    /** 分块数量 */
    private Integer chunkCount;
    /** 错误信息 */
    private String errorMsg;
    /** 处理参数（JSON 格式） */
    private String params;
}
