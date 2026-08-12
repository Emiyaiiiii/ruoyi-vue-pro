package cn.iocoder.yudao.module.kb.dal.mysql.news;

import java.util.*;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsRecordDO;
import cn.iocoder.yudao.module.kb.controller.admin.news.vo.NewsRecordPageReqVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 新闻记录 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface NewsRecordMapper extends BaseMapperX<NewsRecordDO> {

    /**
     * 分页查询（支持搜索/数据源/状态/频道过滤）
     */
    default PageResult<NewsRecordDO> selectPage(NewsRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<NewsRecordDO> wrapper = new LambdaQueryWrapperX<NewsRecordDO>()
                .eqIfPresent(NewsRecordDO::getSourceId, reqVO.getSourceId())
                .eqIfPresent(NewsRecordDO::getStatus, reqVO.getStatus())
                .eqIfPresent(NewsRecordDO::getExternalChannel, reqVO.getExternalChannel());

        // 关键字搜索：标题/内容/外部ID 任一匹配
        if (reqVO.getSearch() != null && !reqVO.getSearch().isEmpty()) {
            wrapper.and(w -> w
                    .like(NewsRecordDO::getExternalTitle, reqVO.getSearch())
                    .or()
                    .like(NewsRecordDO::getExternalContent, reqVO.getSearch())
                    .or()
                    .like(NewsRecordDO::getExternalId, reqVO.getSearch()));
        }

        wrapper.orderByDesc(NewsRecordDO::getCreateTime);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 获取所有不重复的频道列表
     */
    default List<String> selectDistinctChannels() {
        List<NewsRecordDO> all = selectList();
        Set<String> channels = new LinkedHashSet<>();
        for (NewsRecordDO record : all) {
            if (record.getExternalChannel() != null && !record.getExternalChannel().isEmpty()) {
                channels.add(record.getExternalChannel());
            }
        }
        return new ArrayList<>(channels);
    }

    /**
     * 统计数据源下各状态的记录数
     */
    default Map<String, Long> selectStatsBySourceId(Long sourceId) {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", selectCount(NewsRecordDO::getSourceId, sourceId));
        stats.put("pending", selectCount(new LambdaQueryWrapperX<NewsRecordDO>()
                .eq(NewsRecordDO::getSourceId, sourceId)
                .eq(NewsRecordDO::getStatus, "pending")));
        stats.put("completed", selectCount(new LambdaQueryWrapperX<NewsRecordDO>()
                .eq(NewsRecordDO::getSourceId, sourceId)
                .eq(NewsRecordDO::getStatus, "completed")));
        stats.put("failed", selectCount(new LambdaQueryWrapperX<NewsRecordDO>()
                .eq(NewsRecordDO::getSourceId, sourceId)
                .eq(NewsRecordDO::getStatus, "failed")));
        stats.put("skipped", selectCount(new LambdaQueryWrapperX<NewsRecordDO>()
                .eq(NewsRecordDO::getSourceId, sourceId)
                .eq(NewsRecordDO::getStatus, "skipped")));
        return stats;
    }

    /**
     * 全局统计各状态记录数
     */
    default Map<String, Long> selectGlobalStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", selectCount());
        stats.put("pending", selectCount(NewsRecordDO::getStatus, "pending"));
        stats.put("completed", selectCount(NewsRecordDO::getStatus, "completed"));
        stats.put("failed", selectCount(NewsRecordDO::getStatus, "failed"));
        stats.put("skipped", selectCount(NewsRecordDO::getStatus, "skipped"));
        return stats;
    }

    /**
     * 按 source_id + external_id 查找已有记录（自动过滤逻辑删除）
     */
    default NewsRecordDO selectBySourceAndExternalId(Long sourceId, String externalId) {
        return selectOne(new LambdaQueryWrapperX<NewsRecordDO>()
                .eq(NewsRecordDO::getSourceId, sourceId)
                .eq(NewsRecordDO::getExternalId, externalId));
    }

    /**
     * 按 source_id + external_id 查找已有记录（包括已逻辑删除的）
     * 绕过 @TableLogic 过滤，用于同步时恢复已删除记录
     */
    @Select("SELECT * FROM kb_news_record WHERE source_id = #{sourceId} AND external_id = #{externalId} LIMIT 1")
    NewsRecordDO selectBySourceAndExternalIdIncludeDeleted(@Param("sourceId") Long sourceId, @Param("externalId") String externalId);

    /**
     * 恢复已逻辑删除的记录（设置 deleted=0，重置状态为 pending）
     */
    @Update("UPDATE kb_news_record SET deleted = 0, status = 'pending', error_message = NULL, retry_count = 0 WHERE id = #{id}")
    int restoreDeletedRecord(@Param("id") Long id);
}
