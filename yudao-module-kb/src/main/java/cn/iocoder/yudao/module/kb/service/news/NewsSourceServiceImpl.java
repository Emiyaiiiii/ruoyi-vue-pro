package cn.iocoder.yudao.module.kb.service.news;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;
import java.util.*;

import cn.iocoder.yudao.module.kb.controller.admin.news.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSourceDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSyncLogDO;
import cn.iocoder.yudao.module.kb.dal.mysql.news.NewsSourceMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.news.NewsRecordMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.news.NewsSyncLogMapper;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 新闻数据源 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Slf4j
public class NewsSourceServiceImpl implements NewsSourceService {

    @Resource
    private NewsSourceMapper newsSourceMapper;

    @Resource
    private NewsRecordMapper newsRecordMapper;

    @Resource
    private NewsSyncLogMapper newsSyncLogMapper;

    @Resource
    private NewsSyncService newsSyncService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNewsSource(NewsSourceSaveReqVO createReqVO) {
        NewsSourceDO source = BeanUtils.toBean(createReqVO, NewsSourceDO.class);
        if (source.getSyncEnabled() == null) source.setSyncEnabled(1);
        if (source.getSyncInterval() == null) source.setSyncInterval(3600);
        if (source.getDbPort() == null) source.setDbPort(3306);
        if (source.getIdField() == null || source.getIdField().isEmpty()) source.setIdField("id");
        if (source.getTitleField() == null || source.getTitleField().isEmpty()) source.setTitleField("doctitle");
        if (source.getContentField() == null || source.getContentField().isEmpty()) source.setContentField("doccontent");
        source.setTotalRecords(0);
        source.setProcessedRecords(0);
        source.setErrorCount(0);
        newsSourceMapper.insert(source);
        return source.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNewsSource(NewsSourceSaveReqVO updateReqVO) {
        NewsSourceDO existing = validateExists(updateReqVO.getId());
        NewsSourceDO updateObj = BeanUtils.toBean(updateReqVO, NewsSourceDO.class);
        newsSourceMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNewsSource(Long id) {
        validateExists(id);
        newsSourceMapper.deleteById(id);
    }

    @Override
    public NewsSourceDO getNewsSource(Long id) {
        return validateExists(id);
    }

    @Override
    public PageResult<NewsSourceDO> getNewsSourcePage(NewsSourcePageReqVO pageReqVO) {
        PageResult<NewsSourceDO> pageResult = newsSourceMapper.selectPage(pageReqVO);
        // 填充扩展统计字段
        for (NewsSourceDO source : pageResult.getList()) {
            source.setTotalRecords(newsRecordMapper.selectCount(
                    cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsRecordDO::getSourceId, source.getId()).intValue());
        }
        return pageResult;
    }

    @Override
    public Map<String, Object> getSourceStats(Long id) {
        validateExists(id);
        Map<String, Long> longStats = newsRecordMapper.selectStatsBySourceId(id);
        Map<String, Object> stats = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : longStats.entrySet()) {
            stats.put(entry.getKey(), entry.getValue());
        }
        return stats;
    }

    @Override
    public List<Map<String, Object>> getSourceSyncLogs(Long id, int limit) {
        validateExists(id);
        List<NewsSyncLogDO> logs = newsSyncLogMapper.selectRecentBySourceId(id, limit);
        List<Map<String, Object>> result = new ArrayList<>();
        for (NewsSyncLogDO log : logs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", log.getId());
            item.put("syncType", log.getSyncType());
            item.put("syncTypeDisplay", log.getSyncTypeDisplay());
            item.put("status", log.getStatus());
            item.put("statusDisplay", log.getStatusDisplay());
            item.put("totalFetched", log.getTotalFetched());
            item.put("newRecords", log.getNewRecords());
            item.put("updatedRecords", log.getUpdatedRecords());
            item.put("skippedRecords", log.getSkippedRecords());
            item.put("failedRecords", log.getFailedRecords());
            item.put("startedAt", log.getStartedAt());
            item.put("completedAt", log.getCompletedAt());
            item.put("errorMessage", log.getErrorMessage());
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> triggerSync(Long id, String syncType) {
        validateExists(id);
        return newsSyncService.syncSource(id, syncType != null ? syncType : "incremental");
    }

    // ========== 私有辅助 ==========

    private NewsSourceDO validateExists(Long id) {
        NewsSourceDO source = newsSourceMapper.selectById(id);
        if (source == null) {
            throw exception(NEWS_SOURCE_NOT_EXISTS);
        }
        return source;
    }
}
