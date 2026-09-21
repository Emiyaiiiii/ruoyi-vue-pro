package cn.iocoder.yudao.module.kb.dal.mysql.chunk;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.chunk.KbChunkRevisionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface KbChunkRevisionMapper extends BaseMapperX<KbChunkRevisionDO> {

    default KbChunkRevisionDO selectMaxRevision(String chunkId) {
        return selectOne(new LambdaQueryWrapperX<KbChunkRevisionDO>()
                .eq(KbChunkRevisionDO::getChunkId, chunkId)
                .orderByDesc(KbChunkRevisionDO::getRevision)
                .last("LIMIT 1"));
    }

    default List<KbChunkRevisionDO> selectListByChunkId(String chunkId) {
        return selectList(new LambdaQueryWrapperX<KbChunkRevisionDO>()
                .eq(KbChunkRevisionDO::getChunkId, chunkId)
                .orderByDesc(KbChunkRevisionDO::getRevision));
    }

    /**
     * 按修订号查询某版本快照（回滚用）
     */
    default KbChunkRevisionDO selectByRevision(String chunkId, Integer revision) {
        return selectOne(new LambdaQueryWrapperX<KbChunkRevisionDO>()
                .eq(KbChunkRevisionDO::getChunkId, chunkId)
                .eq(KbChunkRevisionDO::getRevision, revision));
    }

    /**
     * 删除某文档的全部修订记录（文档删除时联动清理）
     */
    default void deleteByDocumentId(Long documentId) {
        delete(new LambdaQueryWrapperX<KbChunkRevisionDO>()
                .eq(KbChunkRevisionDO::getDocumentId, documentId));
    }

    /**
     * 删除某知识库的全部修订记录（知识库删除时联动清理）
     */
    default void deleteByKbId(Long kbId) {
        delete(new LambdaQueryWrapperX<KbChunkRevisionDO>()
                .eq(KbChunkRevisionDO::getKbId, kbId));
    }
}
