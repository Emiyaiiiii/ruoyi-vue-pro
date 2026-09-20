package cn.iocoder.yudao.module.kb.controller.admin.follow;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.kb.controller.admin.library.vo.LibraryRespVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.mysql.follow.FollowMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import cn.iocoder.yudao.module.kb.service.follow.FollowService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 知识库关注")
@RestController
@RequestMapping("/kb/follow")
@Validated
public class FollowController {

    @Resource
    private FollowService followService;

    @Resource
    private LibraryMapper libraryMapper;

    @PostMapping("/{kbId}")
    @Operation(summary = "关注知识库")
    @PreAuthorize("@ss.hasPermission('kb:follow:create')")
    public CommonResult<Boolean> follow(@PathVariable Long kbId) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        followService.follow(kbId, userId);
        return success(true);
    }

    @DeleteMapping("/{kbId}")
    @Operation(summary = "取消关注")
    @PreAuthorize("@ss.hasPermission('kb:follow:delete')")
    public CommonResult<Boolean> unfollow(@PathVariable Long kbId) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        followService.unfollow(kbId, userId);
        return success(true);
    }

    @GetMapping("/check/{kbId}")
    @Operation(summary = "是否已关注")
    @PreAuthorize("@ss.hasPermission('kb:follow:query')")
    public CommonResult<Boolean> isFollowing(@PathVariable Long kbId) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return success(followService.isFollowing(kbId, userId));
    }

    @GetMapping("/my-page")
    @Operation(summary = "我关注的知识库分页列表")
    @PreAuthorize("@ss.hasPermission('kb:follow:query')")
    public CommonResult<PageResult<LibraryRespVO>> getMyFollowedPage(@Valid PageParam pageParam) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        // 获取关注的知识库ID列表
        List<Long> kbIds = followService.getFollowedKbIds(userId);
        if (kbIds.isEmpty()) {
            return success(new PageResult<>(Collections.emptyList(), 0L));
        }
        // 根据ID列表批量查询知识库（避免 N+1 查询）
        List<LibraryDO> list = libraryMapper.selectBatchIds(kbIds);
        // 手动分页
        int total = list.size();
        int fromIndex = (pageParam.getPageNo() - 1) * pageParam.getPageSize();
        int toIndex = Math.min(fromIndex + pageParam.getPageSize(), total);
        if (fromIndex >= total) {
            return success(new PageResult<>(Collections.emptyList(), (long) total));
        }
        List<LibraryDO> page = list.subList(fromIndex, toIndex);
        List<LibraryRespVO> voList = page.stream().map(lib -> {
            LibraryRespVO vo = new LibraryRespVO();
            // processConfig：DO 为 JSON 字符串、VO 为 Map，hutool convertField 无法转换，显式忽略后反序列化填充
            BeanUtil.copyProperties(lib, vo, CopyOptions.create().setIgnoreProperties("processConfig"));
            if (StrUtil.isNotBlank(lib.getProcessConfig())) {
                Map<String, Object> config = JsonUtils.parseMap(lib.getProcessConfig());
                if (config != null) {
                    vo.setProcessConfig(config);
                }
            }
            return vo;
        }).collect(Collectors.toList());
        return success(new PageResult<>(voList, (long) total));
    }
}
