package cn.iocoder.yudao.module.kb.controller.admin.wiki;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.kb.controller.admin.wiki.vo.WikiGraphRespVO;
import cn.iocoder.yudao.module.kb.service.wiki.WikiPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - Wiki 图谱")
@RestController
@RequestMapping("/kb/wiki/graph")
@Validated
public class WikiGraphController {

    @Resource
    private WikiPageService wikiPageService;

    @GetMapping
    @Operation(summary = "Wiki 图谱（overview=全局 top-N；ego=以某页为中心 BFS 展开邻居）")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @Parameter(name = "mode", description = "overview/ego")
    @Parameter(name = "centerSlug", description = "ego 中心页 slug")
    @Parameter(name = "depth", description = "ego BFS 深度(1-3，默认1)")
    @Parameter(name = "types", description = "page_type 白名单，逗号分隔；空=不过滤")
    @Parameter(name = "limit", description = "overview 节点数限制(默认500，上限2000)")
    @PreAuthorize("@ss.hasPermission('kb:document:query')")
    public CommonResult<WikiGraphRespVO> graph(@RequestParam("kbId") Long kbId,
                                               @RequestParam(value = "mode", required = false, defaultValue = "overview") String mode,
                                               @RequestParam(value = "centerSlug", required = false) String centerSlug,
                                               @RequestParam(value = "depth", required = false) Integer depth,
                                               @RequestParam(value = "types", required = false) String types,
                                               @RequestParam(value = "limit", required = false) Integer limit) {
        return success(wikiPageService.graph(kbId, mode, centerSlug, depth, types, limit));
    }
}