package cn.iocoder.yudao.module.kb.dal.mysql.wiki;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.wiki.WikiIssueDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Wiki 待修复问题 Mapper
 */
@Mapper
public interface WikiIssueMapper extends BaseMapperX<WikiIssueDO> {

    /**
     * 按知识库 + 可选 slug/status 查询问题（id 倒序，新的在前）
     */
    default List<WikiIssueDO> selectListByKb(Long kbId, String slug, String status) {
        return selectList(new LambdaQueryWrapperX<WikiIssueDO>()
                .eq(WikiIssueDO::getKbId, kbId)
                .eqIfPresent(WikiIssueDO::getSlug, slug)
                .eqIfPresent(WikiIssueDO::getStatus, status)
                .orderByDesc(WikiIssueDO::getId));
    }

    /**
     * 按知识库 + slug + issue_type + status=pending 查（幂等去重：同页同类型待处理只记一条）
     */
    default WikiIssueDO selectPendingByKbSlugType(Long kbId, String slug, String issueType) {
        return selectOne(new LambdaQueryWrapperX<WikiIssueDO>()
                .eq(WikiIssueDO::getKbId, kbId)
                .eq(WikiIssueDO::getSlug, slug)
                .eq(WikiIssueDO::getIssueType, issueType)
                .eq(WikiIssueDO::getStatus, "pending")
                .last("LIMIT 1"));
    }

    /**
     * 统计某知识库待处理问题数
     */
    default Long countPendingByKb(Long kbId) {
        return selectCount(new LambdaQueryWrapperX<WikiIssueDO>()
                .eq(WikiIssueDO::getKbId, kbId)
                .eq(WikiIssueDO::getStatus, "pending"));
    }

    /**
     * 物理删除某知识库全部问题
     */
    @Delete("DELETE FROM kb_wiki_issue WHERE kb_id = #{kbId} AND deleted = b'0'")
    int deletePhysicallyByKbId(Long kbId);
}