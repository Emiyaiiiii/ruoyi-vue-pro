package cn.iocoder.yudao.module.kb.dal.mysql.folder;

import java.util.*;
import java.util.stream.Collectors;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.folder.FolderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档文件夹 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface FolderMapper extends BaseMapperX<FolderDO> {

    default List<FolderDO> selectByKbId(Long kbId) {
        return selectList(FolderDO::getKbId, kbId);
    }

    default List<FolderDO> selectByParentId(Long parentId) {
        return selectList(FolderDO::getParentId, parentId);
    }

    default FolderDO selectByParentIdAndName(Long kbId, Long parentId, String name) {
        return selectOne(FolderDO::getKbId, kbId, FolderDO::getParentId, parentId, FolderDO::getName, name);
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(FolderDO::getParentId, parentId);
    }

    /**
     * 删除某知识库下的全部文件夹（知识库删除时联动清理）
     */
    default void deleteByKbId(Long kbId) {
        delete(FolderDO::getKbId, kbId);
    }

    /**
     * 按知识库批量统计文件夹数量（前端卡片 folder_count 用，取自 kb_document_folder）。
     * 与 document_count（文档数，kb_library.doc_count）是两个不同指标。
     *
     * @param kbIds 知识库ID集合
     * @return kbId → 文件夹数量
     */
    default Map<Long, Long> selectFolderCountMap(Collection<Long> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 仅查 kb_id 一列做内存聚合，避免 N+1，也避免加载文件夹全字段
        return selectList(new LambdaQueryWrapperX<FolderDO>()
                .select(FolderDO::getKbId)
                .in(FolderDO::getKbId, kbIds)).stream()
                .collect(Collectors.groupingBy(FolderDO::getKbId, Collectors.counting()));
    }

}