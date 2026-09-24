package cn.iocoder.yudao.module.kb.dal.mysql.wiki;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.wiki.WikiPageRevisionDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Wiki 页面修订历史 Mapper
 *
 * <p>页面正文在 ES(kb_wiki_all)，本表仅作修订快照纯载体，由 (kb_id, slug, revision) 唯一幂等
 * （复用 BaseMapperX.insert，并发重复写由调用方捕获 DuplicateKeyException 忽略；
 * 租户字段由 MyBatis 租户插件自动填充）。</p>
 */
@Mapper
public interface WikiPageRevisionMapper extends BaseMapperX<WikiPageRevisionDO> {

    /**
     * 列出某(KB,slug)页面的全部修订（按版本降序，供修订抽屉）
     */
    default List<WikiPageRevisionDO> selectListByKbSlug(Long kbId, String slug) {
        return selectList(new LambdaQueryWrapperX<WikiPageRevisionDO>()
                .eq(WikiPageRevisionDO::getKbId, kbId)
                .eq(WikiPageRevisionDO::getSlug, slug)
                .orderByDesc(WikiPageRevisionDO::getRevision));
    }

    /**
     * 按修订号查某(KB,slug)页面的版本快照（回滚用）
     */
    default WikiPageRevisionDO selectByKbSlugRevision(Long kbId, String slug, Integer revision) {
        return selectOne(new LambdaQueryWrapperX<WikiPageRevisionDO>()
                .eq(WikiPageRevisionDO::getKbId, kbId)
                .eq(WikiPageRevisionDO::getSlug, slug)
                .eq(WikiPageRevisionDO::getRevision, revision));
    }

    /**
     * 物理删除某知识库全部修订（知识库删除时联动；页面已不在 kb_wiki_page，按 kb_id 直删）
     */
    @Delete("DELETE FROM kb_wiki_revision WHERE kb_id = #{kbId} AND deleted = b'0'")
    int deletePhysicallyByKbId(Long kbId);
}