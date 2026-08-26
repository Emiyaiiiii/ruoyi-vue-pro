package cn.iocoder.yudao.module.kb.controller.admin.projectmember;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.kb.controller.admin.projectmember.vo.ProjectMemberRespVO;
import cn.iocoder.yudao.module.kb.service.projectmember.ProjectMemberService;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 知识库项目成员 Controller
 * 管理项目成果库的成员（控制文档内容访问）
 *
 * @author 吴皓
 */
@Tag(name = "管理后台 - 知识库项目成员")
@RestController
@RequestMapping("/kb/project-member")
@Validated
public class ProjectMemberController {

    @Resource
    private ProjectMemberService projectMemberService;

    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @PostMapping("/add")
    @Operation(summary = "添加项目成员")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @Parameter(name = "userId", description = "用户ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:project-member:update')")
    public CommonResult<Long> addMember(@RequestParam("kbId") Long kbId,
                                        @RequestParam("userId") Long userId) {
        return success(projectMemberService.addMember(kbId, userId));
    }

    @DeleteMapping("/remove")
    @Operation(summary = "移除项目成员")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @Parameter(name = "userId", description = "用户ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:project-member:delete')")
    public CommonResult<Boolean> removeMember(@RequestParam("kbId") Long kbId,
                                              @RequestParam("userId") Long userId) {
        projectMemberService.removeMember(kbId, userId);
        return success(true);
    }

    @GetMapping("/check")
    @Operation(summary = "检查当前登录用户是否为项目成员（大屏/总览进入项目库时调用）")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    public CommonResult<Boolean> isMember(@RequestParam("kbId") Long kbId) {
        // 大屏/总览只传 kbId，按当前登录用户判断
        Long targetUserId = SecurityFrameworkUtils.getLoginUserId();
        if (targetUserId == null) {
            return success(false);
        }
        // 超管/租户管理员与文档接口一致，视为可访问全部项目库
        if (securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode())) {
            return success(true);
        }
        return success(projectMemberService.isMember(kbId, targetUserId));
    }

    @GetMapping("/list")
    @Operation(summary = "获取项目成员列表（含用户昵称）")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:project-member:query')")
    public CommonResult<List<ProjectMemberRespVO>> listMembers(@RequestParam("kbId") Long kbId) {
        return success(projectMemberService.getByKbId(kbId));
    }

}
