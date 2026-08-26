package cn.iocoder.yudao.module.kb.service.vectortask;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo.VectorTaskPageReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo.VectorTaskSubmitReqVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.vectortask.VectorTaskDO;

/**
 * 向量处理任务 Service 接口
 */
public interface VectorTaskService {

    /**
     * 提交向量处理任务
     *
     * @return 任务ID（taskId）
     * @throws cn.iocoder.yudao.framework.common.exception.ServiceException 调用 Python 服务失败时抛出
     */
    String submitTask(VectorTaskSubmitReqVO reqVO);

    /**
     * 取消向量处理任务
     *
     * @param taskId 任务ID
     */
    void cancelTask(String taskId);

    /**
     * 重试失败文档的向量处理
     *
     * 根据文档ID重新创建并提交一个新的向量任务。
     * 仅当文档当前向量状态为终态（失败/超时/提交失败等）时才允许重试。
     *
     * @param docId 文档ID
     * @return 新任务ID（taskId）
     */
    String retryTask(Long docId);

    /**
     * 根据 taskId 查询任务
     */
    VectorTaskDO getTaskByTaskId(String taskId);

    /**
     * 分页查询任务
     */
    PageResult<VectorTaskDO> getTaskPage(VectorTaskPageReqVO pageReqVO);

    /**
     * 处理状态回调（内部使用，也可作为 Redis Stream 消费的兜底通道）
     */
    void handleTaskCallback(String taskId, String status, Integer progress,
                            String step, Integer chunkCount, String errorMsg);

    /**
     * 删除文档对应的向量数据（Milvus + ES）
     *
     * 同步调用 Python 向量服务清理指定文档在本知识库的所有向量分片。
     * 用于知识库文档删除时联动清库，避免脏数据残留。
     *
     * @param docId 文档ID
     * @param kbId  知识库ID
     * @return 是否清理成功
     */
    boolean deleteDocumentVectors(Long docId, Long kbId);
}
