package cn.iocoder.yudao.module.kb.service.news;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.FileUploadRespVO;
import cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo.VectorTaskSubmitReqVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.category.CategoryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSourceDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsRecordDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSyncLogDO;
import cn.iocoder.yudao.module.kb.dal.mysql.category.CategoryMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.document.DocumentMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import cn.iocoder.yudao.module.kb.service.vectortask.VectorTaskService;
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

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private LibraryMapper libraryMapper;

    @Resource
    private DocumentMapper documentMapper;

    @Resource
    private FileApi fileApi;

    @Resource
    private VectorTaskService vectorTaskService;

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

            // 5.5 同步新闻内容到知识库（生成 txt 文件并上传）
            try {
                syncToKnowledgeBase(source);
            } catch (Exception kbEx) {
                log.warn("同步到知识库失败（不影响新闻记录同步）: sourceId={}, error={}",
                        sourceId, kbEx.getMessage());
            }

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

    // ==================== 知识库同步 ====================

    /**
     * 将新闻内容同步到知识库（生成 txt 文件并上传）
     * <p>
     * 去重策略：
     * - 知识库去重：categoryId + name("新闻库") + ownerId(deptId)
     * - 文件去重：kbId + fileName
     * <p>
     * dbDept 为空时跳过，不影响新闻记录同步主流程。
     */
    private void syncToKnowledgeBase(NewsSourceDO source) {
        Long deptId = source.getDbDept();
        if (deptId == null) {
            log.info("[KB同步] 数据源 [{}] 未设置所属部门，跳过知识库同步", source.getName());
            return;
        }

        // 1. 查找「院级知识库 > 院级新闻库」分类
        CategoryDO newsCategory = findNewsCategory();
        if (newsCategory == null) {
            log.warn("[KB同步] 未找到「院级知识库 > 院级新闻库」分类，跳过");
            return;
        }

        // 2. 查找或创建知识库「新闻库」
        LibraryDO kb = findOrCreateNewsLibrary(newsCategory, deptId);
        if (kb == null) {
            log.error("[KB同步] 创建/查找知识库失败，跳过文件上传");
            return;
        }

        // 3. 遍历该数据源下所有新闻记录，生成 txt 并上传
        List<NewsRecordDO> records = newsRecordMapper.selectList(
                new LambdaQueryWrapperX<NewsRecordDO>()
                        .eq(NewsRecordDO::getSourceId, source.getId()));

        int uploadedCount = 0;
        int skippedCount = 0;

        for (NewsRecordDO record : records) {
            if (StrUtil.isBlank(record.getExternalContent())) {
                continue;
            }

            String fileName = sanitizeFileName(record.getExternalTitle()) + ".txt";
            // 标题为空或过滤后只剩 ".txt" → 用 externalId 兜底
            if (fileName.length() <= 4) {
                fileName = record.getExternalId() + ".txt";
            }

            // 文件去重检查
            DocumentDO existingDoc = documentMapper.selectByKbIdAndFileName(kb.getId(), fileName);
            if (existingDoc != null) {
                skippedCount++;
                continue;
            }

            try {
                byte[] content = record.getExternalContent().getBytes(StandardCharsets.UTF_8);
                FileUploadRespVO uploadResp = fileApi.createFileDetail(content, fileName, "kb", "text/plain");

                DocumentDO doc = new DocumentDO();
                doc.setKbId(kb.getId());
                doc.setFolderId(0L);
                doc.setFileName(fileName);
                doc.setFileUrl(uploadResp.getUrl());
                doc.setFilePath(uploadResp.getPath());
                doc.setFileConfigId(uploadResp.getConfigId());
                doc.setFileType("txt");
                doc.setFileSize((long) content.length);
                doc.setDescription(null);
                doc.setTags(null);
                doc.setDownloadCount(0);
                doc.setViewCount(0);
                doc.setStatus(0);
                documentMapper.insert(doc);

                // 自动触发向量处理任务
                try {
                    VectorTaskSubmitReqVO taskReqVO = new VectorTaskSubmitReqVO();
                    taskReqVO.setDocId(doc.getId());
                    taskReqVO.setKbId(kb.getId());
                    taskReqVO.setFileUrl(uploadResp.getUrl());
                    taskReqVO.setFileType(doc.getFileType());
                    vectorTaskService.submitTask(taskReqVO);
                    log.info("[KB同步] 已触发向量处理任务: docId={}, kbId={}", doc.getId(), kb.getId());
                } catch (Exception e) {
                    log.error("[KB同步] 触发向量处理任务失败: docId={}", doc.getId(), e);
                }

                // 回写文档信息到新闻记录
                record.setDocId(doc.getId());
                record.setKbId(kb.getId());
                record.setFileUrl(uploadResp.getUrl());
                record.setFileType("txt");
                newsRecordMapper.updateById(record);

                uploadedCount++;
            } catch (Exception e) {
                log.warn("[KB同步] 上传文件失败: fileName={}, error={}", fileName, e.getMessage());
            }
        }

        // 更新知识库文档数量
        if (uploadedCount > 0) {
            libraryMapper.updateDocCount(kb.getId(), uploadedCount);
        }

        log.info("[KB同步] 完成: sourceId={}, kbId={}, deptId={}, 上传={}, 跳过={}",
                source.getId(), kb.getId(), deptId, uploadedCount, skippedCount);
    }

    /**
     * 查找「院级知识库 > 院级新闻库」分类
     */
    private CategoryDO findNewsCategory() {
        List<CategoryDO> allCategories = categoryMapper.selectList();
        if (allCategories.isEmpty()) {
            return null;
        }

        // 构建 parentId → children 映射
        Map<Long, List<CategoryDO>> childrenMap = new HashMap<>();
        for (CategoryDO cat : allCategories) {
            childrenMap.computeIfAbsent(cat.getParentId(), k -> new ArrayList<>()).add(cat);
        }

        // 查找「院级知识库」
        CategoryDO deptLevel = findByName(allCategories, "院级知识库");
        if (deptLevel == null) {
            log.debug("[KB同步] 未找到「院级知识库」分类");
            return null;
        }

        // 查找其子分类「院级新闻库」
        List<CategoryDO> children = childrenMap.getOrDefault(deptLevel.getId(), Collections.emptyList());
        CategoryDO newsCategory = findByName(children, "院级新闻库");
        if (newsCategory == null) {
            log.debug("[KB同步] 未找到「院级知识库 > 院级新闻库」子分类");
        }
        return newsCategory;
    }

    /**
     * 在列表中按名称查找分类（精确匹配）
     */
    private CategoryDO findByName(List<CategoryDO> categories, String name) {
        for (CategoryDO cat : categories) {
            if (name.equals(cat.getName())) {
                return cat;
            }
        }
        return null;
    }

    /**
     * 查找或创建知识库「新闻库」
     * 去重键：categoryId + name + ownerId
     */
    private LibraryDO findOrCreateNewsLibrary(CategoryDO category, Long deptId) {
        // 检查是否已存在
        LibraryDO existing = libraryMapper.selectOne(
                new LambdaQueryWrapperX<LibraryDO>()
                        .eq(LibraryDO::getCategoryId, category.getId())
                        .eq(LibraryDO::getName, "新闻库")
                        .eq(LibraryDO::getOwnerId, deptId));

        if (existing != null) {
            log.info("[KB同步] 知识库已存在: id={}, name=新闻库, deptId={}", existing.getId(), deptId);
            return existing;
        }

        // 创建新知识库
        LibraryDO kb = new LibraryDO();
        kb.setName("新闻库");
        kb.setCategoryId(category.getId());
        kb.setKbLevelId(category.getKbLevelId());
        kb.setOwnerId(deptId);
        kb.setDocCount(0);
        kb.setStatus(0);       // 启用
        kb.setIsPublic(0);     // 不公开到广场
        kb.setIsProject(0);    // 非项目成果库
        libraryMapper.insert(kb);

        log.info("[KB同步] 已创建知识库: id={}, name=新闻库, categoryId={}, deptId={}",
                kb.getId(), category.getId(), deptId);
        return kb;
    }

    /**
     * 文件名安全处理：移除文件系统不允许的字符
     */
    private String sanitizeFileName(String title) {
        if (StrUtil.isBlank(title)) {
            return "untitled";
        }
        // 移除 Windows/Unix 文件名非法字符：/ \ : * ? " < > |
        return title.replaceAll("[/\\\\:*?\"<>|]", "").trim();
    }
}
