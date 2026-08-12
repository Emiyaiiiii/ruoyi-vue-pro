package cn.iocoder.yudao.module.kb.service.news;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;

import cn.iocoder.yudao.module.kb.controller.admin.news.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsRecordDO;
import cn.iocoder.yudao.module.kb.dal.mysql.news.NewsRecordMapper;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 新闻记录 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Slf4j
public class NewsRecordServiceImpl implements NewsRecordService {

    @Resource
    private NewsRecordMapper newsRecordMapper;

    @Resource
    private cn.iocoder.yudao.module.kb.dal.mysql.news.NewsSourceMapper newsSourceMapper;

    @Override
    public NewsRecordDO getNewsRecord(Long id) {
        NewsRecordDO record = validateExists(id);
        // 填充数据源名称
        cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSourceDO source =
                newsSourceMapper.selectById(record.getSourceId());
        if (source != null) {
            record.setSourceName(source.getName());
        }
        return record;
    }

    @Override
    public PageResult<NewsRecordDO> getNewsRecordPage(NewsRecordPageReqVO pageReqVO) {
        PageResult<NewsRecordDO> pageResult = newsRecordMapper.selectPage(pageReqVO);
        // 批量填充数据源名称
        for (NewsRecordDO record : pageResult.getList()) {
            cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSourceDO source =
                    newsSourceMapper.selectById(record.getSourceId());
            if (source != null) {
                record.setSourceName(source.getName());
            }
        }
        return pageResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRetry(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            NewsRecordDO record = newsRecordMapper.selectById(id);
            if (record != null && "failed".equals(record.getStatus())) {
                record.setStatus("pending");
                record.setErrorMessage(null);
                record.setRetryCount(0);
                newsRecordMapper.updateById(record);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            newsRecordMapper.deleteById(id);
        }
    }

    @Override
    public List<String> getChannels() {
        return newsRecordMapper.selectDistinctChannels();
    }

    @Override
    public Map<String, Long> getStats() {
        return newsRecordMapper.selectGlobalStats();
    }

    // ========== 私有辅助 ==========

    private NewsRecordDO validateExists(Long id) {
        NewsRecordDO record = newsRecordMapper.selectById(id);
        if (record == null) {
            throw exception(NEWS_RECORD_NOT_EXISTS);
        }
        return record;
    }
}
