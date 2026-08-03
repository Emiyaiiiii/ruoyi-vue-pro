package cn.iocoder.yudao.module.kb.controller.admin.userdept;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.kb.controller.admin.userdept.vo.DeptMemberRespVO;
import cn.iocoder.yudao.module.kb.service.userdept.KbUserDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 知识库用户部门关联 Controller
 * 管理部门（院/公司/中心）的成员和管理员
 * 列表数据来源于芋道系统用户表，角色信息来源于 kb_user_dept
 *
 * @author 吴皓
 */
@Tag(name = "管理后台 - 知识库用户部门关联")
@RestController
@RequestMapping("/kb/user-dept")
@Validated
public class KbUserDeptController {

    @Resource
    private KbUserDeptService kbUserDeptService;

    @PostMapping("/add-member")
    @Operation(summary = "添加成员")
    @Parameter(name = "userId", description = "用户ID", required = true)
    @Parameter(name = "deptId", description = "部门ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:user-dept:update')")
    public CommonResult<Long> addMember(@RequestParam("userId") Long userId,
                                        @RequestParam("deptId") Long deptId) {
        return success(kbUserDeptService.addMember(userId, deptId));
    }

    @PostMapping("/add-admin")
    @Operation(summary = "添加管理员")
    @Parameter(name = "userId", description = "用户ID", required = true)
    @Parameter(name = "deptId", description = "部门ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:user-dept:update')")
    public CommonResult<Long> addAdmin(@RequestParam("userId") Long userId,
                                       @RequestParam("deptId") Long deptId) {
        return success(kbUserDeptService.addAdmin(userId, deptId));
    }

    @DeleteMapping("/remove")
    @Operation(summary = "移除用户角色记录（用户仍保留在系统部门中）")
    @Parameter(name = "userId", description = "用户ID", required = true)
    @Parameter(name = "deptId", description = "部门ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:user-dept:delete')")
    public CommonResult<Boolean> remove(@RequestParam("userId") Long userId,
                                        @RequestParam("deptId") Long deptId) {
        kbUserDeptService.remove(userId, deptId);
        return success(true);
    }

    @PutMapping("/set-role")
    @Operation(summary = "设置用户角色（成员/管理员切换）")
    @Parameter(name = "userId", description = "用户ID", required = true)
    @Parameter(name = "deptId", description = "部门ID", required = true)
    @Parameter(name = "role", description = "角色: 0=成员, 1=管理员", required = true)
    @PreAuthorize("@ss.hasPermission('kb:user-dept:update')")
    public CommonResult<Boolean> setRole(@RequestParam("userId") Long userId,
                                         @RequestParam("deptId") Long deptId,
                                         @RequestParam("role") Integer role) {
        kbUserDeptService.setRole(userId, deptId, role);
        return success(true);
    }

    @GetMapping("/list-by-dept")
    @Operation(summary = "获取部门下所有成员（合并系统用户 + 角色）")
    @Parameter(name = "deptId", description = "部门ID", required = true)
    @PreAuthorize("@ss.hasPermission('kb:user-dept:query')")
    public CommonResult<List<DeptMemberRespVO>> listByDept(@RequestParam("deptId") Long deptId) {
        return success(kbUserDeptService.getByDeptId(deptId));
    }

}
