package cn.iocoder.yudao.module.kb.service.news;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.kb.controller.admin.news.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSyncLogDO;

/**
 * 新闻同步日志 Service 接口
 *
 * @author 吴皓
 */
public interface NewsSyncLogService {

    /**
     * 获取同步日志详情
     */
    NewsSyncLogDO getNewsSyncLog(Long id);

    /**
     * 分页查询同步日志
     */
    PageResult<NewsSyncLogDO> getNewsSyncLogPage(NewsSyncLogPageReqVO pageReqVO);
}
