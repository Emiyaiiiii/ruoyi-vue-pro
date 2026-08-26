package cn.iocoder.yudao.module.kb.controller.admin.frontend;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendResult;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendUserSimpleVO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 前端 C 端用户搜索，对齐 Python {@code /auth/users/search/?q=}。
 * <p>
 * 仅返回启用用户的 id/昵称，供项目成员多选，不改系统用户权限模型。
 */
@Tag(name = "前端 C 端 - 用户搜索")
@RestController
public class FrontendUserController {

    private static final int SEARCH_LIMIT = 20;

    @Resource
    private AdminUserApi adminUserApi;

    @GetMapping({"/auth/users/search", "/auth/users/search/"})
    @Operation(summary = "按昵称模糊搜索用户（项目成员多选）")
    public FrontendResult<List<FrontendUserSimpleVO>> search(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "ids", required = false) String ids) {
        String keyword = firstNotBlank(q, search, nickname);
        List<AdminUserRespDTO> users = new ArrayList<>();
        if (StrUtil.isNotBlank(ids)) {
            List<Long> idList = Arrays.stream(ids.split("[,，]"))
                    .map(String::trim)
                    .filter(s -> s.matches("\\d+"))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            if (!idList.isEmpty()) {
                users.addAll(adminUserApi.getUserList(idList));
            }
        }
        if (StrUtil.isNotBlank(keyword)) {
            List<AdminUserRespDTO> matched = adminUserApi.getUserListByNickname(keyword.trim());
            if (matched != null) {
                users.addAll(matched);
            }
        }
        Map<Long, FrontendUserSimpleVO> uniq = new LinkedHashMap<>();
        boolean idLookupOnly = StrUtil.isNotBlank(ids) && StrUtil.isBlank(keyword);
        for (AdminUserRespDTO user : users) {
            if (user == null || user.getId() == null) {
                continue;
            }
            if (!idLookupOnly && user.getStatus() != null
                    && !CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus())) {
                continue;
            }
            uniq.putIfAbsent(user.getId(), toVO(user));
            if (!idLookupOnly && uniq.size() >= SEARCH_LIMIT) {
                break;
            }
        }
        return FrontendResult.ok(new ArrayList<>(uniq.values()));
    }

    private static String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private FrontendUserSimpleVO toVO(AdminUserRespDTO user) {
        FrontendUserSimpleVO vo = new FrontendUserSimpleVO();
        vo.setId(String.valueOf(user.getId()));
        vo.setNickname(user.getNickname());
        vo.setName(user.getNickname());
        vo.setDeptId(user.getDeptId());
        return vo;
    }
}
