package cn.iocoder.yudao.module.kb.controller.admin.imagestrategy;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.kb.service.chunkmethod.ChunkMethodService;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 图片处理方案：全局默认方案读写（载体为默认切片方法 default_parameters.image_strategy）
 */
@Tag(name = "管理后台 - 图片处理方案")
@RestController
@RequestMapping("/kb/image-strategy")
@Validated
public class ImageStrategyController {

    @Resource
    private ChunkMethodService chunkMethodService;

    @GetMapping("/default")
    @Operation(summary = "获取全局默认图片处理方案")
    @PreAuthorize("@ss.hasPermission('kb:model-config:query')")
    public CommonResult<String> getDefault() {
        return success(chunkMethodService.getDefaultImageStrategy());
    }

    @PostMapping("/default")
    @Operation(summary = "设置全局默认图片处理方案")
    @PreAuthorize("@ss.hasPermission('kb:model-config:update')")
    public CommonResult<Boolean> setDefault(@RequestParam("imageStrategy") String imageStrategy) {
        chunkMethodService.setDefaultImageStrategy(imageStrategy);
        return success(true);
    }

}