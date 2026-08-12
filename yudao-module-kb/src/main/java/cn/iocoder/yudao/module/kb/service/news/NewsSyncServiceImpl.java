package cn.iocoder.yudao.module.kb.service.news;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSourceDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsRecordDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSyncLogDO;
import cn.iocoder.yudao.module.kb.dal.mysql.news.NewsSourceMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.news.NewsRecordMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.news.NewsSyncLogMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 新闻同步核心服务实现 — 使用 JDBC 直连外部数据库
 *
 * @author 吴皓
 */
@Service
@Slf4j
public class NewsSyncServiceImpl implements NewsSyncService {

    @Resource
    private NewsSourceMapper newsSourceMapper;

    @Resource
    private NewsRecordMapper newsRecordMapper;

    @Resource
    private NewsSyncLogMapper newsSyncLogMapper;

    /** 批量处理大小 */
    private static final int BATCH_SIZE = 100;

    @Override
    public Map<String, Object> syncSource(Long sourceId, String syncType) {
        // 1. 查询数据源
        NewsSourceDO source = newsSourceMapper.selectById(sourceId);
        if (source == null) {
            throw exception(NEWS_SOURCE_NOT_EXISTS);
        }

        // 2. 创建同步日志
        NewsSyncLogDO syncLog = new NewsSyncLogDO();
        syncLog.setSourceId(sourceId);
        syncLog.setSyncType(syncType != null ? syncType : "incremental");
        syncLog.setStatus("started");
        syncLog.setTotalFetched(0);
        syncLog.setNewRecords(0);
        syncLog.setUpdatedRecords(0);
        syncLog.setSkippedRecords(0);
        syncLog.setFailedRecords(0);
        syncLog.setStartedAt(LocalDateTime.now());
        newsSyncLogMapper.insert(syncLog);

        try {
            // 3. 更新日志状态为运行中
            syncLog.setStatus("running");
            newsSyncLogMapper.updateById(syncLog);

            // 4. 连接外部数据库并同步
            Map<String, Object> stats = doSync(source, syncType);

            // 5. 更新数据源统计
            source.setLastSyncTime(LocalDateTime.now());
            source.setTotalRecords((Integer) stats.getOrDefault("totalFetched", 0)
                    + source.getTotalRecords());
            source.setErrorCount((Integer) stats.getOrDefault("failedRecords", 0)
                    + source.getErrorCount());
            newsSourceMapper.updateById(source);

            // 6. 更新同步日志为完成
            syncLog.setStatus("completed");
            syncLog.setTotalFetched((Integer) stats.getOrDefault("totalFetched", 0));
            syncLog.setNewRecords((Integer) stats.getOrDefault("newRecords", 0));
            syncLog.setUpdatedRecords((Integer) stats.getOrDefault("updatedRecords", 0));
            syncLog.setSkippedRecords((Integer) stats.getOrDefault("skippedRecords", 0));
            syncLog.setFailedRecords((Integer) stats.getOrDefault("failedRecords", 0));
            syncLog.setCompletedAt(LocalDateTime.now());
            newsSyncLogMapper.updateById(syncLog);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("syncType", syncType);
            result.put("stats", stats);
            result.put("sourceName", source.getName());
            return result;

        } catch (Exception e) {
            log.error("同步数据源失败: sourceId={}, 错误: {}", sourceId, e.getMessage(), e);
            // 标记同步日志为失败
            syncLog.setStatus("failed");
            syncLog.setErrorMessage(e.getMessage() != null ? e.getMessage() : "未知错误");
            syncLog.setCompletedAt(LocalDateTime.now());
            newsSyncLogMapper.updateById(syncLog);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("syncType", syncType);
            result.put("sourceName", source.getName());
            return result;
        }
    }

    @Override
    public Map<String, Object> syncAllEnabledSources(String syncType) {
        List<NewsSourceDO> enabledSources = newsSourceMapper.selectList(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<NewsSourceDO>()
                        .eq(NewsSourceDO::getSyncEnabled, 1));

        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (NewsSourceDO source : enabledSources) {
            try {
                Map<String, Object> result = syncSource(source.getId(), syncType);
                results.add(result);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                log.error("同步数据源异常: sourceId={}, 错误: {}", source.getId(), e.getMessage());
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("sourceId", source.getId());
                err.put("sourceName", source.getName());
                err.put("success", false);
                err.put("error", e.getMessage());
                results.add(err);
                failCount++;
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalSources", enabledSources.size());
        summary.put("successCount", successCount);
        summary.put("failCount", failCount);
        summary.put("results", results);
        return summary;
    }

    // ==================== 核心同步逻辑 ====================

    /**
     * 执行实际的数据同步
     */
    @SuppressWarnings("java:S2077") // SQL 由表名拼接，无法使用 PreparedStatement 参数化表名
    private Map<String, Object> doSync(NewsSourceDO source, String syncType) throws Exception {
        int totalFetched = 0;
        int newRecords = 0;
        int updatedRecords = 0;
        int skippedRecords = 0;
        int failedRecords = 0;

        // 1. 构建 JDBC 连接
        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai",
                source.getDbHost(), source.getDbPort(), source.getDbName());

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            // 2. 加载驱动并连接
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(jdbcUrl, source.getDbUser(), source.getDbPassword());
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(Integer.MIN_VALUE); // MySQL 流式读取，防止大表 OOM

            // 3. 构建 SELECT SQL
            String selectSql = buildSelectSQL(source, syncType);
            log.info("开始同步数据源 [{}], SQL: {}", source.getName(), selectSql);
            rs = stmt.executeQuery(selectSql);

            // 4. 遍历外部表数据，批量处理
            List<NewsRecordDO> batch = new ArrayList<>(BATCH_SIZE);

            while (rs.next()) {
                totalFetched++;

                try {
                    // 按字段映射读取数据
                    String externalId = getStringField(rs, source.getIdField(), "external_id");
                    if (externalId == null || externalId.isEmpty()) {
                        skippedRecords++;
                        continue;
                    }

                    String title = getStringField(rs, source.getTitleField(), "title");
                    String content = getStringField(rs, source.getContentField(), "content");
                    String channel = getStringField(rs, source.getChannelField(), "channel");
                    String time = getStringField(rs, source.getTimeField(), "time");
                    String url = getStringField(rs, source.getUrlField(), "url");
                    String crdept = getStringField(rs, source.getCrdeptField(), "crdept");
                    String cruser = getStringField(rs, source.getCruserField(), "cruser");

                    // 检查是否已存在（按 source_id + external_id 唯一键）
                    NewsRecordDO existing = newsRecordMapper.selectBySourceAndExternalId(source.getId(), externalId);

                    if (existing != null) {
                        // 已存在：根据同步类型决定跳过或更新
                        if ("full".equals(syncType)) {
                            boolean changed = false;
                            if (title != null && !title.equals(existing.getExternalTitle())) {
                                existing.setExternalTitle(title);
                                changed = true;
                            }
                            if (content != null && !content.equals(existing.getExternalContent())) {
                                existing.setExternalContent(content);
                                changed = true;
                            }
                            if (changed) {
                                existing.setStatus("pending");
                                existing.setErrorMessage(null);
                                existing.setRetryCount(0);
                                newsRecordMapper.updateById(existing);
                                updatedRecords++;
                            } else {
                                skippedRecords++;
                            }
                        } else {
                            skippedRecords++;
                        }
                    } else {
                        // 检查是否之前被逻辑删除过（绕过 @TableLogic 过滤）
                        NewsRecordDO deletedRecord = newsRecordMapper.selectBySourceAndExternalIdIncludeDeleted(
                                source.getId(), externalId);
                        if (deletedRecord != null) {
                            // 恢复已删除的记录：先恢复（deleted=0），再更新字段
                            newsRecordMapper.restoreDeletedRecord(deletedRecord.getId());
                            // 全量同步时更新外部字段
                            boolean needUpdate = false;
                            if ("full".equals(syncType)) {
                                if (title != null) { deletedRecord.setExternalTitle(title); needUpdate = true; }
                                if (content != null) { deletedRecord.setExternalContent(content); needUpdate = true; }
                                if (channel != null) { deletedRecord.setExternalChannel(channel); needUpdate = true; }
                                if (time != null) { deletedRecord.setExternalTime(time); needUpdate = true; }
                                if (url != null) { deletedRecord.setExternalUrl(url); needUpdate = true; }
                                if (crdept != null) { deletedRecord.setExternalCrdept(crdept); needUpdate = true; }
                                if (cruser != null) { deletedRecord.setExternalCruser(cruser); needUpdate = true; }
                            }
                            if (needUpdate) {
                                newsRecordMapper.updateById(deletedRecord);
                            }
                            newRecords++;
                        } else {
                            // 全新记录
                            NewsRecordDO record = new NewsRecordDO();
                            record.setSourceId(source.getId());
                            record.setExternalId(externalId);
                            record.setExternalTitle(title);
                            record.setExternalContent(content);
                            record.setExternalChannel(channel);
                            record.setExternalTime(time);
                            record.setExternalUrl(url);
                            record.setExternalCrdept(crdept);
                            record.setExternalCruser(cruser);
                            record.setStatus("pending");
                            record.setRetryCount(0);

                            newsRecordMapper.insert(record);
                            newRecords++;
                        }
                    }

                } catch (Exception e) {
                    log.warn("处理外部记录失败: sourceId={}, 行号={}, 错误: {}",
                            source.getId(), totalFetched, e.getMessage());
                    failedRecords++;
                }
            }

        } finally {
            // 关闭资源
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalFetched", totalFetched);
        stats.put("newRecords", newRecords);
        stats.put("updatedRecords", updatedRecords);
        stats.put("skippedRecords", skippedRecords);
        stats.put("failedRecords", failedRecords);
        return stats;
    }

    /**
     * 构建外部表查询 SQL
     */
    private String buildSelectSQL(NewsSourceDO source, String syncType) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM `").append(source.getTableName()).append("`");

        // 增量同步时，按 last_sync_time 过滤
        if ("incremental".equals(syncType) && source.getLastSyncTime() != null
                && source.getTimeField() != null && !source.getTimeField().isEmpty()) {
            sql.append(" WHERE `").append(source.getTimeField()).append("` > '")
                    .append(source.getLastSyncTime().toString().replace("T", " "))
                    .append("'");
        }

        // 按时间字段排序
        if (source.getTimeField() != null && !source.getTimeField().isEmpty()) {
            sql.append(" ORDER BY `").append(source.getTimeField()).append("` DESC");
        }

        return sql.toString();
    }

    /**
     * 从 ResultSet 安全读取字段值（字段名可能为 null）
     */
    private String getStringField(ResultSet rs, String fieldName, String fallbackLabel) {
        if (fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        try {
            String value = rs.getString(fieldName);
            return value;
        } catch (SQLException e) {
            // 字段不存在，记录警告
            log.debug("字段 [{}] 不存在于外部表中", fieldName);
            return null;
        }
    }
}
