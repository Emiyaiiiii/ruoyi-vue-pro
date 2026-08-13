package cn.iocoder.yudao.module.kb.service.news;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.kb.controller.admin.news.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsRecordDO;

import java.util.*;

/**
 * 新闻记录 Service 接口
 *
 * @author 吴皓
 */
public interface NewsRecordService {

    /**
     * 获取新闻记录详情
     */
    NewsRecordDO getNewsRecord(Long id);

    /**
     * 分页查询新闻记录
     */
    PageResult<NewsRecordDO> getNewsRecordPage(NewsRecordPageReqVO pageReqVO);

    /**
     * 批量重试（failed → pending）
     */
    void batchRetry(List<Long> ids);

    /**
     * 批量删除
     */
    void batchDelete(List<Long> ids);

    /**
     * 获取所有不重复的频道列表
     */
    List<String> getChannels();

    /**
     * 获取全局统计信息
     */
    Map<String, Long> getStats();

    /**
     * 单条解析：触发向量处理任务
     *
     * @param id 新闻记录ID
     * @return 任务提交结果提示
     */
    String parseRecord(Long id);

    /**
     * 批量解析：批量触发向量处理任务
     *
     * @param ids 新闻记录ID列表
     * @return 任务提交结果提示
     */
    String batchParse(List<Long> ids);
}
