package cn.iocoder.yudao.module.kb.service.news;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

import cn.iocoder.yudao.module.kb.controller.admin.news.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSyncLogDO;
import cn.iocoder.yudao.module.kb.dal.mysql.news.NewsSyncLogMapper;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 新闻同步日志 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Slf4j
public class NewsSyncLogServiceImpl implements NewsSyncLogService {

    @Resource
    private NewsSyncLogMapper newsSyncLogMapper;

    @Resource
    private cn.iocoder.yudao.module.kb.dal.mysql.news.NewsSourceMapper newsSourceMapper;

    @Override
    public NewsSyncLogDO getNewsSyncLog(Long id) {
        NewsSyncLogDO log = newsSyncLogMapper.selectById(id);
        if (log == null) {
            throw exception(NEWS_SYNC_LOG_NOT_EXISTS);
        }
        // 填充数据源名称
        cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSourceDO source =
                newsSourceMapper.selectById(log.getSourceId());
        if (source != null) {
            log.setSourceName(source.getName());
        }
        return log;
    }

    @Override
    public PageResult<NewsSyncLogDO> getNewsSyncLogPage(NewsSyncLogPageReqVO pageReqVO) {
        PageResult<NewsSyncLogDO> pageResult = newsSyncLogMapper.selectPage(pageReqVO);
        // 批量填充数据源名称
        for (NewsSyncLogDO log : pageResult.getList()) {
            cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSourceDO source =
                    newsSourceMapper.selectById(log.getSourceId());
            if (source != null) {
                log.setSourceName(source.getName());
            }
        }
        return pageResult;
    }
}
