package cn.iocoder.yudao.module.kb.controller.admin.wiki;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiPageRespVO;
import cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiPageSaveReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiRevisionRespVO;
import cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiRevertReqVO;
import cn.iocoder.yudao.module.kb.service.wiki.WikiPageService;
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

@Tag(name = "管理后台 - Wiki 页面")
@RestController
@RequestMapping("/kb/wiki")
@Validated
public class WikiPageController {

    @Resource
    private WikiPageService wikiPageService;

    @GetMapping("/list")
    @Operation(summary = "列出某知识库全部 Wiki 页面")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<List<WikiPageRespVO>> list(@RequestParam("kbId") Long kbId) {
        return success(wikiPageService.listByKb(kbId));
    }

    @GetMapping("/folder/list")
    @Operation(summary = "列出某知识库全部目录树（含各目录挂载页数）")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<List<cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiFolderRespVO>> folderList(
            @RequestParam("kbId") Long kbId) {
        return success(wikiPageService.listFolders(kbId));
    }

    @GetMapping("/index")
    @Operation(summary = "知识库 index 聚合（intro + 按 page_type 分组目录）")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiIndexRespVO> index(
            @RequestParam("kbId") Long kbId) {
        return success(wikiPageService.getIndex(kbId));
    }

    @GetMapping("/issues")
    @Operation(summary = "列出知识库待修复问题（可按 slug/status 过滤）")
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<List<cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiIssueRespVO>> issues(
            @RequestParam("kbId") Long kbId,
            @RequestParam(value = "slug", required = false) String slug,
            @RequestParam(value = "status", required = false) String status) {
        return success(wikiPageService.listIssues(kbId, slug, status));
    }

    @PutMapping("/issue/status")
    @Operation(summary = "更新问题状态（pending/ignored/resolved）——处理或标记误报")
    @PreAuthorize("@ss.hasPermission('kb:document:update')")
    public CommonResult<Boolean> updateIssueStatus(@RequestParam("kbId") Long kbId,
                                                   @RequestParam("slug") String slug,
                                                   @RequestParam("issueType") String issueType,
                                                   @RequestParam("status") String status) {
        wikiPageService.updateIssueStatus(kbId, slug, issueType, status);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "Wiki 页面详情")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @Parameter(name = "slug", description = "页面 slug", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<WikiPageRespVO> get(@RequestParam("kbId") Long kbId,
                                            @RequestParam("slug") String slug) {
        return success(wikiPageService.getPage(kbId, slug));
    }

    @PostMapping("/save")
    @Operation(summary = "保存 Wiki 页面（新增/人工编辑，乐观锁防并发）")
    @PreAuthorize("@ss.hasPermission('kb:document:update')")
    public CommonResult<Boolean> save(@Valid @RequestBody WikiPageSaveReqVO reqVO) {
        wikiPageService.savePage(reqVO);
        return success(true);
    }

    @PutMapping("/revert")
    @Operation(summary = "回滚 Wiki 页面到指定版本")
    @PreAuthorize("@ss.hasPermission('kb:document:update')")
    public CommonResult<Integer> revert(@Valid @RequestBody WikiRevertReqVO reqVO) {
        return success(wikiPageService.revertPage(reqVO));
    }

    @GetMapping("/revisions")
    @Operation(summary = "查询 Wiki 页面修订历史（倒序）")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @Parameter(name = "slug", description = "页面 slug", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<List<WikiRevisionRespVO>> revisions(@RequestParam("kbId") Long kbId,
                                                            @RequestParam("slug") String slug) {
        return success(wikiPageService.listRevisions(kbId, slug));
    }
}