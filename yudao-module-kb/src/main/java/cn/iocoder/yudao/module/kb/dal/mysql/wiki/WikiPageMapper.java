package cn.iocoder.yudao.module.kb.dal.mysql.wiki;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.wiki.WikiPageDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Wiki 页面 Mapper
 *
 * <p>页面删除走【物理删除】（唯一键 (kb_id, slug) 不含 deleted，物理删后 Slug 才可重建同名），
 * 因此删除用真实 @Delete SQL 绕过 BaseDO 全局逻辑删除。</p>
 */
@Mapper
public interface WikiPageMapper extends BaseMapperX<WikiPageDO> {

    /**
     * 按 knowledge_base_id + slug 查页面（一个 KB 内 slug 唯一）
     */
    default WikiPageDO selectByKbSlug(Long kbId, String slug) {
        return selectOne(new LambdaQueryWrapperX<WikiPageDO>()
                .eq(WikiPageDO::getKbId, kbId)
                .eq(WikiPageDO::getSlug, slug));
    }

    /**
     * 物理删除单个页面
     */
    @Delete("DELETE FROM kb_wiki_page WHERE id = #{id} AND deleted = b'0'")
    int deletePhysicallyById(Long id);

    /**
     * 物理删除某知识库全部页面
     */
    @Delete("DELETE FROM kb_wiki_page WHERE kb_id = #{kbId} AND deleted = b'0'")
    int deletePhysicallyByKbId(Long kbId);

    /**
     * 列某知识库的全部页面（按 sort_order + id 稳定排序）
     */
    default List<WikiPageDO> selectListByKb(Long kbId) {
        return selectList(new LambdaQueryWrapperX<WikiPageDO>()
                .eq(WikiPageDO::getKbId, kbId)
                .orderByAsc(WikiPageDO::getSortOrder)
                .orderByAsc(WikiPageDO::getId));
    }

    /**
     * 按 slug 集合批量查（linkify/反链解析用）
     */
    default List<WikiPageDO> selectListBySlugs(Long kbId, List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<WikiPageDO>()
                .eq(WikiPageDO::getKbId, kbId)
                .in(WikiPageDO::getSlug, slugs));
    }

    /**
     * 按来源文档(source_refs 含 docKey)查询引用该文档的页面（文档删除/retract 反查）
     * @param docKey 形如 "kbId|docTitle"
     */
    default List<WikiPageDO> selectBySourceRefContains(String docKey) {
        return selectList(new LambdaQueryWrapperX<WikiPageDO>()
                .like(WikiPageDO::getSourceRefs, docKey));
    }
}