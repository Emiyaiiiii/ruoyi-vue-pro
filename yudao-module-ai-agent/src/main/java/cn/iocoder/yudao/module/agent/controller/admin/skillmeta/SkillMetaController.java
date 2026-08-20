package cn.iocoder.yudao.module.agent.controller.admin.skillmeta;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.agent.controller.admin.skillmeta.vo.SkillMetaPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.skillmeta.vo.SkillMetaRespVO;
import cn.iocoder.yudao.module.agent.controller.admin.skillmeta.vo.SkillMetaSaveReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.skillmeta.AiSkillMetaDO;
import cn.iocoder.yudao.module.agent.service.skillmeta.AiSkillMetaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - Skills 商店（QwenPaw 技能池元数据管理）
 *
 * @author 吴皓
 */
@Tag(name = "管理后台 - 技能商店")
@RestController
@RequestMapping("/ai-agent/skill-meta")
@Validated
@Slf4j
public class SkillMetaController {

    @Resource
    private AiSkillMetaService skillMetaService;

    @PostMapping("/create")
    @Operation(summary = "创建技能商店项（仅 Java 侧元数据）")
    @PreAuthorize("@ss.hasPermission('ai-agent:skill-meta:create')")
    public CommonResult<Long> createSkillMeta(@Valid @RequestBody SkillMetaSaveReqVO createReqVO) {
        return success(skillMetaService.createSkillMeta(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新技能商店项（icon/可见性/描述等）")
    @PreAuthorize("@ss.hasPermission('ai-agent:skill-meta:update')")
    public CommonResult<Boolean> updateSkillMeta(@Valid @RequestBody SkillMetaSaveReqVO updateReqVO) {
        skillMetaService.updateSkillMeta(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除技能商店项（同时从 QwenPaw 技能池删除）")
    @Parameter(name = "id", description = "技能商店项ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:skill-meta:delete')")
    public CommonResult<Boolean> deleteSkillMeta(@RequestParam("id") Long id) {
        skillMetaService.deleteSkillMeta(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得技能商店项详情")
    @Parameter(name = "id", description = "技能商店项ID", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:skill-meta:query')")
    public CommonResult<SkillMetaRespVO> getSkillMeta(@RequestParam("id") Long id) {
        AiSkillMetaDO meta = skillMetaService.getSkillMeta(id);
        return success(BeanUtils.toBean(meta, SkillMetaRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得技能商店分页（管理后台用）")
    @PreAuthorize("@ss.hasPermission('ai-agent:skill-meta:query')")
    public CommonResult<PageResult<SkillMetaRespVO>> getSkillMetaPage(@Valid SkillMetaPageReqVO pageReqVO) {
        PageResult<AiSkillMetaDO> pageResult = skillMetaService.getSkillMetaPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SkillMetaRespVO.class));
    }

    @GetMapping("/visible-list")
    @Operation(summary = "获得当前用户可见的技能列表（公开 + 自己的个人技能）")
    @PreAuthorize("@ss.hasPermission('ai-agent:skill-meta:query')")
    public CommonResult<List<SkillMetaRespVO>> getVisibleSkillMetaList() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        List<AiSkillMetaDO> list = skillMetaService.getVisibleSkillMetaList(userId);
        return success(BeanUtils.toBean(list, SkillMetaRespVO.class));
    }

    @PostMapping("/upload")
    @Operation(summary = "上传 zip 到 QwenPaw 技能池并创建 Java 侧元数据")
    @PreAuthorize("@ss.hasPermission('ai-agent:skill-meta:create')")
    public CommonResult<Long> uploadSkill(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "targetName", required = false) String targetName,
            @RequestParam(value = "displayName", required = false) String displayName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "icon", required = false) String icon,
            @RequestParam(value = "visibility", required = false) Integer visibility,
            @RequestParam(value = "tags", required = false) String tags) {
        try {
            Long userId = SecurityFrameworkUtils.getLoginUserId();
            Long id = skillMetaService.uploadSkillToPool(
                    file.getBytes(), file.getOriginalFilename(), targetName,
                    displayName, description, icon, visibility, userId, tags);
            return success(id);
        } catch (Exception e) {
            log.error("[uploadSkill] 上传失败", e);
            return CommonResult.error(500, "上传失败：" + e.getMessage());
        }
    }

}
