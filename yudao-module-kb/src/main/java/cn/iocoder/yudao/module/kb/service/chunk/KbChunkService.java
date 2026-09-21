package cn.iocoder.yudao.module.kb.service.chunk;

import cn.iocoder.yudao.module.kb.controller.admin.chunk.vo.ChunkItemVO;
import cn.iocoder.yudao.module.kb.controller.admin.chunk.vo.ChunkRevisionRespVO;
import cn.iocoder.yudao.module.kb.controller.admin.chunk.vo.ChunkRevertReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.chunk.vo.ChunkUpdateReqVO;

import java.util.List;

/**
 * 切片编辑与修订 Service（#7）
 */
public interface KbChunkService {

    /**
     * 列出文档全部切片（含当前版本号，供前端乐观锁）
     */
    List<ChunkItemVO> listChunks(Long docId, Long kbId);

    /**
     * 编辑切片：乐观锁校验 → python 重嵌入双写 → 落修订快照
     *
     * @return 新版本号
     */
    Integer updateChunk(ChunkUpdateReqVO reqVO);

    /**
     * 查询切片修订历史（倒序）
     */
    List<ChunkRevisionRespVO> listRevisions(Long docId, Integer chunkIndex);

    /**
     * 回滚切片到指定版本
     *
     * @return 新版本号
     */
    Integer revertChunk(ChunkRevertReqVO reqVO);
}
