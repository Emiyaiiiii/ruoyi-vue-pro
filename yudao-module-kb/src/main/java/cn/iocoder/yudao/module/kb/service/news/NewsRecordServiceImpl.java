package cn.iocoder.yudao.module.kb.service.news;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;
import java.util.*;

import cn.iocoder.yudao.module.kb.controller.admin.news.vo.*;
import cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo.VectorTaskSubmitReqVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsRecordDO;
import cn.iocoder.yudao.module.kb.dal.mysql.news.NewsRecordMapper;
import cn.iocoder.yudao.module.kb.service.vectortask.VectorTaskService;
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

    @Resource
    private VectorTaskService vectorTaskService;

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

    @Override
    public String parseRecord(Long id) {
        NewsRecordDO record = validateExists(id);

        if (record.getDocId() == null || record.getKbId() == null
                || record.getFileUrl() == null || record.getFileType() == null) {
            throw new RuntimeException("该新闻记录尚未同步到知识库，缺少关联文档信息，无法解析");
        }

        try {
            VectorTaskSubmitReqVO taskReqVO = new VectorTaskSubmitReqVO();
            taskReqVO.setDocId(record.getDocId());
            taskReqVO.setKbId(record.getKbId());
            taskReqVO.setFileUrl(record.getFileUrl());
            taskReqVO.setFileType(record.getFileType());
            vectorTaskService.submitTask(taskReqVO);
            log.info("[解析] 已触发向量处理任务: recordId={}, docId={}, kbId={}", id, record.getDocId(), record.getKbId());
            return "解析任务已提交";
        } catch (Exception e) {
            log.error("[解析] 触发向量处理任务失败: recordId={}, docId={}", id, record.getDocId(), e);
            throw new RuntimeException("解析失败: " + e.getMessage());
        }
    }

    @Override
    public String batchParse(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "没有需要解析的记录";
        }
        int successCount = 0;
        int failCount = 0;
        for (Long id : ids) {
            try {
                parseRecord(id);
                successCount++;
            } catch (Exception e) {
                log.warn("[批量解析] 单条失败: recordId={}, error={}", id, e.getMessage());
                failCount++;
            }
        }
        return String.format("解析完成: 成功 %d 条, 失败 %d 条", successCount, failCount);
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
