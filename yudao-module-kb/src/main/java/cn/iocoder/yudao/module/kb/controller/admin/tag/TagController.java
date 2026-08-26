package cn.iocoder.yudao.module.kb.controller.admin.tag;

import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import javax.validation.*;
import java.util.*;
import java.util.stream.Collectors;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.module.kb.controller.admin.tag.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.tag.TagDO;
import cn.iocoder.yudao.module.kb.service.tag.TagService;

@Tag(name = "管理后台 - 标签管理")
@RestController
@RequestMapping("/kb/tag")
@Validated
public class TagController {

    @Resource
    private TagService tagService;

    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建标签")
    @PreAuthorize("@ss.hasPermission('kb:tag:create')")
    public CommonResult<Long> createTag(@Valid @RequestBody TagSaveReqVO createReqVO) {
        return success(tagService.createTag(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新标签")
    @PreAuthorize("@ss.hasPermission('kb:tag:update')")
    public CommonResult<Boolean> updateTag(@Valid @RequestBody TagSaveReqVO updateReqVO) {
        tagService.updateTag(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除标签")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('kb:tag:delete')")
    public CommonResult<Boolean> deleteTag(@RequestParam("id") Long id) {
        tagService.deleteTag(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得标签")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('kb:tag:query')")
    public CommonResult<TagRespVO> getTag(@RequestParam("id") Long id) {
        TagDO tag = tagService.getTag(id);
        TagRespVO vo = BeanUtils.toBean(tag, TagRespVO.class);
        fillOwnerNicknames(Collections.singletonList(vo));
        return success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "获得标签分页")
    @PreAuthorize("@ss.hasPermission('kb:tag:query')")
    public CommonResult<PageResult<TagRespVO>> getTagPage(@Valid TagPageReqVO pageReqVO) {
        PageResult<TagDO> pageResult = tagService.getTagPage(pageReqVO);
        PageResult<TagRespVO> respPage = BeanUtils.toBean(pageResult, TagRespVO.class);
        fillOwnerNicknames(respPage.getList());
        return success(respPage);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "标签精简列表（C 端上传下拉，数据同源标签管理）")
    public CommonResult<List<TagRespVO>> getSimpleTagList(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "name", required = false) String name) {
        List<TagDO> list = tagService.getTagList(type);
        if (name != null && !name.isEmpty()) {
            list = list.stream()
                    .filter(t -> t.getName() != null && t.getName().contains(name))
                    .collect(Collectors.toList());
        }
        List<TagRespVO> vos = BeanUtils.toBean(list, TagRespVO.class);
        if (vos == null) {
            vos = Collections.emptyList();
        }
        fillOwnerNicknames(vos);
        return success(vos);
    }

    /**
     * 批量填充归属人昵称，避免 N+1 查询
     */
    private void fillOwnerNicknames(List<TagRespVO> list) {
        Set<Long> ownerIds = list.stream()
                .map(TagRespVO::getOwnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ownerIds.isEmpty()) {
            return;
        }
        Map<Long, AdminUserRespDTO> userMap;
        try {
            userMap = adminUserApi.getUserMap(ownerIds);
        } catch (Exception e) {
            return;
        }
        for (TagRespVO vo : list) {
            AdminUserRespDTO user = userMap.get(vo.getOwnerId());
            if (user != null) {
                vo.setOwnerNickname(user.getNickname());
            }
        }
    }

}