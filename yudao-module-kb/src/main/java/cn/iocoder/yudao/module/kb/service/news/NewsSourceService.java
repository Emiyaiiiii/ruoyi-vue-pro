package cn.iocoder.yudao.module.kb.service.news;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.kb.controller.admin.news.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSourceDO;

import java.util.*;

/**
 * 新闻数据源 Service 接口
 *
 * @author 吴皓
 */
public interface NewsSourceService {

    /**
     * 创建新闻数据源
     */
    Long createNewsSource(NewsSourceSaveReqVO createReqVO);

    /**
     * 更新新闻数据源
     */
    void updateNewsSource(NewsSourceSaveReqVO updateReqVO);

    /**
     * 删除新闻数据源
     */
    void deleteNewsSource(Long id);

    /**
     * 获取新闻数据源详情
     */
    NewsSourceDO getNewsSource(Long id);

    /**
     * 分页查询新闻数据源
     */
    PageResult<NewsSourceDO> getNewsSourcePage(NewsSourcePageReqVO pageReqVO);

    /**
     * 获取数据源统计信息
     */
    Map<String, Object> getSourceStats(Long id);

    /**
     * 获取数据源最近同步日志
     */
    List<Map<String, Object>> getSourceSyncLogs(Long id, int limit);

    /**
     * 触发指定数据源的同步
     */
    Map<String, Object> triggerSync(Long id, String syncType);
}
