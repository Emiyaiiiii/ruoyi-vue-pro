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
import java.util.Map;

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

    @PostMapping("/rebuild-links")
    @Operation(summary = "收尾重建：死链清理 + 交叉链接 + 入链重建 + index 兜底（纯算法，不含 LLM）")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:update')")
    public CommonResult<Map<String, Object>> rebuildLinks(@RequestParam("kbId") Long kbId) {
        return success(wikiPageService.rebuildLinks(kbId));
    }

    @GetMapping("/stats")
    @Operation(summary = "Wiki 健康统计：页数/链接数/孤立页/死链数/待处理问题数")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<Map<String, Object>> stats(@RequestParam("kbId") Long kbId) {
        return success(wikiPageService.stats(kbId));
    }

    @GetMapping("/batch/latest")
    @Operation(summary = "最近一次 Wiki 攒批状态（蒸馏进度 + 待处理文档数）")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<Map<String, Object>> batchLatest(@RequestParam("kbId") Long kbId) {
        return success(wikiPageService.batchLatest(kbId));
    }

    @GetMapping("/lint")
    @Operation(summary = "Wiki 质量检查：孤立页/死链/空内容/失效来源/缺失交叉链接 + 健康分")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<Map<String, Object>> lint(@RequestParam("kbId") Long kbId) {
        return success(wikiPageService.lint(kbId));
    }

    @PostMapping("/auto-fix")
    @Operation(summary = "自动修复可修项：死链拆文本 / 空内容归档 / 失效来源清理（纯算法，不含 LLM）")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:update')")
    public CommonResult<Map<String, Object>> autoFix(@RequestParam("kbId") Long kbId) {
        return success(wikiPageService.autoFix(kbId));
    }

    // ---- 独立目录实体（P1-1） ----

    @GetMapping("/folder-tree")
    @Operation(summary = "目录实体树（含每目录直接挂载页数）")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<List<Map<String, Object>>> folderTree(@RequestParam("kbId") Long kbId) {
        return success(wikiPageService.folderTree(kbId));
    }

    @PostMapping("/folder")
    @Operation(summary = "创建目录（名不得含分隔符；层级上限 3）")
    @PreAuthorize("@ss.hasPermission('kb:document:update')")
    public CommonResult<Map<String, Object>> createFolder(
            @RequestParam("kbId") Long kbId,
            @RequestParam("name") String name,
            @RequestParam(value = "parentId", required = false, defaultValue = "") String parentId) {
        return success(wikiPageService.createFolder(kbId, name, parentId));
    }

    @PutMapping("/folder/rename")
    @Operation(summary = "重命名目录（级联更新子树与页面派生路径）")
    @PreAuthorize("@ss.hasPermission('kb:document:update')")
    public CommonResult<Map<String, Object>> renameFolder(
            @RequestParam("kbId") Long kbId,
            @RequestParam("folderId") String folderId,
            @RequestParam("name") String name) {
        return success(wikiPageService.renameFolder(kbId, folderId, name));
    }

    @PutMapping("/folder/move")
    @Operation(summary = "移动目录到新父级（禁止移入自身子树）")
    @PreAuthorize("@ss.hasPermission('kb:document:update')")
    public CommonResult<Map<String, Object>> moveFolder(
            @RequestParam("kbId") Long kbId,
            @RequestParam("folderId") String folderId,
            @RequestParam(value = "parentId", required = false, defaultValue = "") String parentId) {
        return success(wikiPageService.moveFolder(kbId, folderId, parentId));
    }

    @DeleteMapping("/folder")
    @Operation(summary = "删除目录及整棵子树（子树内页面归位到根）")
    @PreAuthorize("@ss.hasPermission('kb:document:update')")
    public CommonResult<Map<String, Object>> deleteFolder(
            @RequestParam("kbId") Long kbId,
            @RequestParam("folderId") String folderId) {
        return success(wikiPageService.deleteFolder(kbId, folderId));
    }

    @PutMapping("/page/move")
    @Operation(summary = "移动页面到指定目录（folderId 为空表示根）")
    @PreAuthorize("@ss.hasPermission('kb:document:update')")
    public CommonResult<Map<String, Object>> movePage(
            @RequestParam("kbId") Long kbId,
            @RequestParam("slug") String slug,
            @RequestParam(value = "folderId", required = false, defaultValue = "") String folderId) {
        return success(wikiPageService.movePage(kbId, slug, folderId));
    }
}