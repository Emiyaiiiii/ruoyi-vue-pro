package cn.iocoder.yudao.module.kb.controller.admin.frontend;

import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendResult;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendTagVO;
import cn.iocoder.yudao.module.kb.controller.admin.tag.vo.TagSaveReqVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.tag.TagDO;
import cn.iocoder.yudao.module.kb.service.tag.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 前端 C 端 - 标签兼容层
 *
 * <p>将 Python 端 {@code /knowledge/tags/} 接口映射到 Java kb 标签服务，返回前端约定的
 * 扁平结构 {@code {code:0, data:[...], total:N}}。供 C 端「标签管理」页与「上传文件」标签下拉联动使用。
 *
 * @author 吴皓
 */
@Tag(name = "前端 C 端 - 标签兼容层")
@RestController
@RequestMapping("/knowledge/tags")
public class FrontendTagController {

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private TagService tagService;

    /**
     * 标签列表（对应 /knowledge/tags/，上传下拉使用 type=document 过滤）
     */
    @GetMapping({"", "/"})
    @Operation(summary = "前端标签列表")
    public FrontendResult<List<FrontendTagVO>> list(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "page_size", defaultValue = "100") Integer pageSize,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "search", required = false) String search) {
        List<TagDO> all = tagService.getTagList(type);
        if (search != null && !search.isEmpty()) {
            all = all.stream()
                    .filter(t -> t.getName() != null && t.getName().contains(search))
                    .collect(Collectors.toList());
        }

        int total = all.size();
        int pageNo = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 100 : pageSize;
        int from = (pageNo - 1) * size;
        int to = Math.min(from + size, total);
        List<FrontendTagVO> slice = from >= total
                ? Collections.emptyList()
                : all.subList(from, to).stream().map(this::toVO).collect(Collectors.toList());
        return FrontendResult.okList(slice, (long) total);
    }

    /**
     * 创建标签（对应 /knowledge/tags/，C 端默认创建个人标签）
     */
    @PostMapping({"", "/"})
    @Operation(summary = "前端创建标签")
    public FrontendResult<Long> create(@RequestBody TagSaveReqVO req) {
        return FrontendResult.ok(tagService.createTag(req));
    }

    /**
     * 更新标签（对应 /knowledge/tags/{id}/）
     */
    @PatchMapping({"/{id}", "/{id}/"})
    @Operation(summary = "前端更新标签")
    public FrontendResult<Boolean> update(@PathVariable("id") Long id, @RequestBody TagSaveReqVO req) {
        req.setId(id);
        tagService.updateTag(req);
        return FrontendResult.ok(true);
    }

    /**
     * 删除标签（对应 /knowledge/tags/{id}/）
     */
    @DeleteMapping({"/{id}", "/{id}/"})
    @Operation(summary = "前端删除标签")
    public FrontendResult<Boolean> delete(@PathVariable("id") Long id) {
        tagService.deleteTag(id);
        return FrontendResult.ok(true);
    }

    private FrontendTagVO toVO(TagDO tag) {
        FrontendTagVO vo = new FrontendTagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setColor(tag.getColor());
        vo.setType(tag.getType());
        vo.setOwner(tag.getOwnerId());
        vo.setIsGlobal(tag.getOwnerId() == null);
        vo.setCreatedAt(format(tag.getCreateTime()));
        return vo;
    }

    private String format(LocalDateTime time) {
        return time == null ? null : time.format(DATETIME);
    }

}