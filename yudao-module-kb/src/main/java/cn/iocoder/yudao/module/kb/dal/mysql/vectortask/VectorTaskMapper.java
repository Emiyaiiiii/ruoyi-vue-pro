package cn.iocoder.yudao.module.kb.dal.mysql.vectortask;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo.VectorTaskPageReqVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.vectortask.VectorTaskDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface VectorTaskMapper extends BaseMapperX<VectorTaskDO> {

    default PageResult<VectorTaskDO> selectPage(VectorTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<VectorTaskDO>()
                .eqIfPresent(VectorTaskDO::getDocId, reqVO.getDocId())
                .eqIfPresent(VectorTaskDO::getKbId, reqVO.getKbId())
                .eqIfPresent(VectorTaskDO::getStatus, reqVO.getStatus())
                .orderByDesc(VectorTaskDO::getId));
    }

    default VectorTaskDO selectByTaskId(String taskId) {
        return selectOne(VectorTaskDO::getTaskId, taskId);
    }

    /**
     * 查询超时的任务：状态为 PENDING/PROCESSING 且更新时间早于指定时间
     */
    default List<VectorTaskDO> selectTimeoutTasks(List<Integer> statuses, java.time.LocalDateTime threshold) {
        return selectList(new LambdaQueryWrapperX<VectorTaskDO>()
                .in(VectorTaskDO::getStatus, statuses)
                .lt(VectorTaskDO::getUpdateTime, threshold));
    }
}
