package cn.iocoder.yudao.module.kb.controller.admin.follow;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.kb.controller.admin.library.vo.LibraryRespVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.mysql.follow.FollowMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import cn.iocoder.yudao.module.kb.service.follow.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;

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
        return success(new PageResult<>(
                BeanUtils.toBean(page, LibraryRespVO.class), (long) total));
    }
}
