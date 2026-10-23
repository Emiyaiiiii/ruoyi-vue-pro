package cn.iocoder.yudao.module.kb.dal.mysql.vectortask;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo.VectorTaskPageReqVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.vectortask.VectorTaskDO;
import cn.iocoder.yudao.module.kb.enums.VectorTaskStatusEnum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface VectorTaskMapper extends BaseMapperX<VectorTaskDO> {

    default PageResult<VectorTaskDO> selectPage(VectorTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<VectorTaskDO>()
                .likeIfPresent(VectorTaskDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(VectorTaskDO::getDocId, reqVO.getDocId())
                .eqIfPresent(VectorTaskDO::getKbId, reqVO.getKbId())
                .eqIfPresent(VectorTaskDO::getStatus, reqVO.getStatus())
                .orderByDesc(VectorTaskDO::getId));
    }

    default VectorTaskDO selectByTaskId(String taskId) {
        return selectOne(VectorTaskDO::getTaskId, taskId);
    }

    /**
     * 查询某文档最新的在途（PENDING/PROCESSING）任务，用于删除文档时取消其进行中的向量任务
     */
    default VectorTaskDO selectActiveByDocId(Long docId) {
        return selectOne(new LambdaQueryWrapperX<VectorTaskDO>()
                .eq(VectorTaskDO::getDocId, docId)
                .in(VectorTaskDO::getStatus,
                        VectorTaskStatusEnum.PENDING.getStatus(),
                        VectorTaskStatusEnum.PROCESSING.getStatus())
                .orderByDesc(VectorTaskDO::getId)
                .last("LIMIT 1"));
    }

    /**
     * 执行超时：仅 PROCESSING，且心跳（heartbeat_at）早于阈值。
     * 兼容历史数据：heartbeat_at 为空时回退用 update_time。
     */
    default List<VectorTaskDO> selectProcessingTimeoutTasks(LocalDateTime threshold) {
        return selectList(new LambdaQueryWrapperX<VectorTaskDO>()
                .eq(VectorTaskDO::getStatus, VectorTaskStatusEnum.PROCESSING.getStatus())
                .and(w -> w.lt(VectorTaskDO::getHeartbeatAt, threshold)
                        .or()
                        .isNull(VectorTaskDO::getHeartbeatAt)
                        .lt(VectorTaskDO::getUpdateTime, threshold)));
    }

    /**
     * 排队超时：仅 PENDING，按 queued_at 判断（仅用于告警，不判超时）。
     */
    default List<VectorTaskDO> selectQueuedTimeoutTasks(LocalDateTime threshold) {
        return selectList(new LambdaQueryWrapperX<VectorTaskDO>()
                .eq(VectorTaskDO::getStatus, VectorTaskStatusEnum.PENDING.getStatus())
                .lt(VectorTaskDO::getQueuedAt, threshold));
    }

    /**
     * 删除某文档的全部向量任务记录（文档删除时联动清理）
     */
    default void deleteByDocId(Long docId) {
        delete(new LambdaQueryWrapperX<VectorTaskDO>()
                .eq(VectorTaskDO::getDocId, docId));
    }

    /**
     * 删除某知识库的全部向量任务记录（知识库删除时联动清理）
     */
    default void deleteByKbId(Long kbId) {
        delete(new LambdaQueryWrapperX<VectorTaskDO>()
                .eq(VectorTaskDO::getKbId, kbId));
    }

    // ========== 监控统计 ==========

    /**
     * 各状态任务数：status -> count
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM kb_vector_task WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> selectStatusCounts();

    /**
     * 近段时间已完成任务的平均耗时（秒）
     */
    @Select("SELECT AVG(TIMESTAMPDIFF(SECOND, create_time, update_time)) FROM kb_vector_task " +
            "WHERE deleted = 0 AND status = 2 AND update_time >= #{since}")
    Double selectAvgCompletedDurationSeconds(@Param("since") LocalDateTime since);

    /**
     * 近段时间超时任务数
     */
    @Select("SELECT COUNT(*) FROM kb_vector_task WHERE deleted = 0 AND status = 5 AND update_time >= #{since}")
    Long selectTimeoutCountSince(@Param("since") LocalDateTime since);

    /**
     * 近段时间已结束任务数（完成/失败/超时）
     */
    @Select("SELECT COUNT(*) FROM kb_vector_task WHERE deleted = 0 AND status IN (2,3,5) AND update_time >= #{since}")
    Long selectFinishedCountSince(@Param("since") LocalDateTime since);

    /**
     * 最早排队任务的入队时间
     */
    @Select("SELECT MIN(queued_at) FROM kb_vector_task WHERE deleted = 0 AND status = 0")
    LocalDateTime selectOldestPendingQueuedAt();
}
