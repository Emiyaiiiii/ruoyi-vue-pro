package cn.iocoder.yudao.module.kb.dal.mysql.news;

import java.util.*;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSyncLogDO;
import cn.iocoder.yudao.module.kb.controller.admin.news.vo.NewsSyncLogPageReqVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 新闻同步日志 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface NewsSyncLogMapper extends BaseMapperX<NewsSyncLogDO> {

    /**
     * 分页查询（支持数据源/同步类型/状态过滤）
     */
    default PageResult<NewsSyncLogDO> selectPage(NewsSyncLogPageReqVO reqVO) {
        LambdaQueryWrapperX<NewsSyncLogDO> wrapper = new LambdaQueryWrapperX<NewsSyncLogDO>()
                .eqIfPresent(NewsSyncLogDO::getSourceId, reqVO.getSourceId())
                .eqIfPresent(NewsSyncLogDO::getSyncType, reqVO.getSyncType())
                .eqIfPresent(NewsSyncLogDO::getStatus, reqVO.getStatus());

        wrapper.orderByDesc(NewsSyncLogDO::getStartedAt);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 获取某数据源最近 N 条日志
     */
    default List<NewsSyncLogDO> selectRecentBySourceId(Long sourceId, int limit) {
        return selectList(new LambdaQueryWrapperX<NewsSyncLogDO>()
                .eq(NewsSyncLogDO::getSourceId, sourceId)
                .orderByDesc(NewsSyncLogDO::getStartedAt)
                .last("LIMIT " + limit));
    }
}
