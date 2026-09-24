package cn.iocoder.yudao.module.kb.dal.mysql.wiki;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.wiki.WikiFolderDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Wiki 目录树 Mapper
 *
 * <p>目录删除走【物理删除】（唯一键 (kb_id, parent_id, name) 不含 deleted，
 * 物理删后同名目录才可重建），删除用真实 @Delete SQL 绕过 BaseDO 全局逻辑删除。</p>
 */
@Mapper
public interface WikiFolderMapper extends BaseMapperX<WikiFolderDO> {

    /**
     * 按知识库列全部目录（含子目录，供重建树/路径缓存）
     */
    default List<WikiFolderDO> selectListByKb(Long kbId) {
        return selectList(new LambdaQueryWrapperX<WikiFolderDO>()
                .eq(WikiFolderDO::getKbId, kbId)
                .orderByAsc(WikiFolderDO::getDepth)
                .orderByAsc(WikiFolderDO::getSortOrder)
                .orderByAsc(WikiFolderDO::getId));
    }

    /**
     * 按父目录列直接子目录（前端树展开用）
     */
    default List<WikiFolderDO> selectListByParent(Long kbId, Long parentId) {
        return selectList(new LambdaQueryWrapperX<WikiFolderDO>()
                .eq(WikiFolderDO::getKbId, kbId)
                .eq(WikiFolderDO::getParentId, parentId == null ? 0L : parentId)
                .orderByAsc(WikiFolderDO::getSortOrder)
                .orderByAsc(WikiFolderDO::getId));
    }

    /**
     * 按父目录 + 名称查（FindOrCreateFolderPath 用）
     */
    default WikiFolderDO selectByKbParentName(Long kbId, Long parentId, String name) {
        return selectOne(new LambdaQueryWrapperX<WikiFolderDO>()
                .eq(WikiFolderDO::getKbId, kbId)
                .eq(WikiFolderDO::getParentId, parentId == null ? 0L : parentId)
                .eq(WikiFolderDO::getName, name));
    }

    /**
     * 物理删除某知识库全部目录
     */
    @Delete("DELETE FROM kb_wiki_folder WHERE kb_id = #{kbId} AND deleted = b'0'")
    int deletePhysicallyByKbId(Long kbId);
}
