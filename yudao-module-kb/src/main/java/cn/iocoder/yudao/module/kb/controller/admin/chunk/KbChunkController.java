package cn.iocoder.yudao.module.kb.controller.admin.chunk;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.kb.controller.admin.chunk.vo.ChunkItemVO;
import cn.iocoder.yudao.module.kb.controller.admin.chunk.vo.ChunkRevisionRespVO;
import cn.iocoder.yudao.module.kb.controller.admin.chunk.vo.ChunkRevertReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.chunk.vo.ChunkUpdateReqVO;
import cn.iocoder.yudao.module.kb.service.chunk.KbChunkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 切片编辑与修订")
@RestController
@RequestMapping("/kb/chunk")
@Validated
public class KbChunkController {

    @Resource
    private KbChunkService kbChunkService;

    @GetMapping("/list")
    @Operation(summary = "列出文档全部切片（含当前版本号，供乐观锁）")
    @Parameter(name = "docId", description = "文档ID", required = true)
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<List<ChunkItemVO>> listChunks(@RequestParam("docId") Long docId,
                                                      @RequestParam("kbId") Long kbId) {
        return success(kbChunkService.listChunks(docId, kbId));
    }

    @PutMapping("/update")
    @Operation(summary = "编辑切片（乐观锁 + 重嵌入双写 + 落修订）")
    @PreAuthorize("@ss.hasPermission('kb:document:update')")
    public CommonResult<Integer> updateChunk(@Valid @RequestBody ChunkUpdateReqVO reqVO) {
        return success(kbChunkService.updateChunk(reqVO));
    }

    @GetMapping("/revisions")
    @Operation(summary = "查询切片修订历史（倒序）")
    @Parameter(name = "docId", description = "文档ID", required = true)
    @Parameter(name = "chunkIndex", description = "切片序号", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<List<ChunkRevisionRespVO>> listRevisions(@RequestParam("docId") Long docId,
                                                                 @RequestParam("chunkIndex") Integer chunkIndex) {
        return success(kbChunkService.listRevisions(docId, chunkIndex));
    }

    @PutMapping("/revert")
    @Operation(summary = "回滚切片到指定版本")
    @PreAuthorize("@ss.hasPermission('kb:document:update')")
    public CommonResult<Integer> revertChunk(@Valid @RequestBody ChunkRevertReqVO reqVO) {
        return success(kbChunkService.revertChunk(reqVO));
    }
}
