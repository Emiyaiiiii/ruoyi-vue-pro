package cn.iocoder.yudao.module.kb.controller.admin.frontend;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendResult;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.mysql.document.DocumentMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import cn.iocoder.yudao.module.kb.service.follow.FollowService;
import cn.iocoder.yudao.module.kb.service.library.rule.VisibilityContext;
import cn.iocoder.yudao.module.kb.service.library.rule.VisibilityRuleEngine;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 前端 C 端 - 智库统计兼容层
 *
 * <p>将 Python 端 {@code /statistics/home-summary/}、{@code /statistics/knowledge-statistics/}
 * 与 {@code /statistics/knowledge-statistics/stream/}（贡献排行）映射到 Java kb 模块的 MySQL 聚合。
 *
 * @author 吴皓
 */
@Tag(name = "前端 C 端 - 智库统计兼容层")
@RestController
public class FrontendStatisticsController {

    @Resource
    private LibraryMapper libraryMapper;

    @Resource
    private DocumentMapper documentMapper;

    @Resource
    private FollowService followService;

    @Resource
    private VisibilityRuleEngine visibilityRuleEngine;

    @Resource
    private AdminUserApi adminUserApi;

    /**
     * 首页知识库总览（对应 /statistics/home-summary/）。
     * 返回 data：{kb_count, kb_private, kb_public, kb_follow_count, doc_count, doc_size_gb, doc_size_text}。
     * 说明：doc_count / doc_size_text / doc_size_gb 统计范围 = 该用户创建（owner_id=自己, status=0）的全部知识库下的文档。
     */
    @GetMapping({"/statistics/home-summary/", "/statistics/home-summary"})
    @Operation(summary = "前端首页知识库总览统计")
    public FrontendResult<Map<String, Object>> homeSummary(
            @RequestParam(value = "time_range", defaultValue = "7d") String timeRange) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();

        // 统计范围同“智库总数”：我创建的知识库（status=0）
        List<LibraryDO> myLibs = selectMyLibraries(userId);
        long myTotal = myLibs.size();
        long myPublic = myLibs.stream()
                .filter(l -> l.getIsPublic() != null && l.getIsPublic() == 1)
                .count();
        long myPrivate = Math.max(0, myTotal - myPublic);
        int followCount = followService.getFollowedKbIds(userId).size();

        // 文档总数 + 存储占用：我创建的知识库下所有文档
        List<Long> myKbIds = myLibs.stream().map(LibraryDO::getId).collect(Collectors.toList());
        long docCount = documentMapper.selectCountByKbIds(myKbIds);
        long docBytes = documentMapper.selectStorageBytesMap(myKbIds).values().stream()
                .mapToLong(Long::longValue).sum();
        double docSizeGb = Math.round(docBytes / (1024.0 * 1024 * 1024) * 100) / 100.0;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("kb_count", myTotal);
        data.put("kb_private", myPrivate);
        data.put("kb_public", myPublic);
        data.put("kb_follow_count", (long) followCount);
        data.put("doc_count", docCount);
        data.put("doc_size_gb", docSizeGb);
        // 存储占用自适应单位（KB/MB/GB），避免小体量四舍五入后显示为 0
        data.put("doc_size_text", formatSize(docBytes));
        return FrontendResult.ok(data);
    }

    /**
     * 个人智库统计（对应 /statistics/knowledge-statistics/）。
     * 返回 data：{overview: {knowledge_bases:{total,personal,shared,public}, usagePercent, usedGB, totalGB, newFollowers}}。
     * 口径：total=与我相关（我创建的+他人创建且对我可见）；personal=我创建的；
     * shared=平台内所有公开库总数（含自己的公开库）；public=我共享的（我创建的公开库）。
     */
    @GetMapping({"/statistics/knowledge-statistics/", "/statistics/knowledge-statistics"})
    @Operation(summary = "前端个人智库统计")
    public FrontendResult<Map<String, Object>> knowledgeStatistics(
            @RequestParam(value = "time_range", defaultValue = "7d") String timeRange) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();

        // 个人知识库 = 我创建的知识库
        long personal = selectMyLibraries(userId).size();
        // 我共享的 = 我创建的公开库
        long myPublic = libraryMapper.selectCount(new LambdaQueryWrapperX<LibraryDO>()
                .eq(LibraryDO::getOwnerId, userId)
                .eq(LibraryDO::getStatus, 0)
                .eq(LibraryDO::getIsPublic, 1));
        // 共享知识库 = 平台内所有公开库总数（含自己的公开库）
        long shared = libraryMapper.selectCount(new LambdaQueryWrapperX<LibraryDO>()
                .eq(LibraryDO::getStatus, 0)
                .eq(LibraryDO::getIsPublic, 1));
        // 与我相关 = 我创建的 + 他人创建且对我可见的库（他人公开/部门共享），
        // 不直接用 shared 相加，避免自己的公开库被重复计入
        VisibilityContext ctx = visibilityRuleEngine.buildContext(userId);
        long othersVisible = visibilityRuleEngine.filterVisible(ctx, libraryMapper.selectSimpleList(null)).stream()
                .filter(l -> l.getOwnerId() == null || !l.getOwnerId().equals(userId))
                .count();
        long total = personal + othersVisible;

        Map<String, Object> kb = new LinkedHashMap<>();
        kb.put("total", total);
        kb.put("personal", personal);
        kb.put("shared", shared);
        kb.put("public", myPublic);

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("knowledge_bases", kb);
        // 存储配额：ruoyi 侧暂未实现配额管理，返回 0 占位（前端按 0% 渲染）
        overview.put("usagePercent", 0);
        overview.put("usedGB", 0);
        overview.put("totalGB", 0);
        overview.put("newFollowers", 0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overview", overview);
        return FrontendResult.ok(data);
    }

    /**
     * 贡献排行（对应 /statistics/knowledge-statistics/stream/）。
     * 前端按行解析 data: 前缀，一次性输出 data:{"contribution_rank":[...]} 后关闭连接。
     * 排行项：{user_id, name, avatar, doc_count, total_points, hotCount, likeCount, status}。
     */
    @GetMapping({"/statistics/knowledge-statistics/stream/", "/statistics/knowledge-statistics/stream"})
    @Operation(summary = "前端智库贡献排行（SSE）")
    public SseEmitter contributionRankStream(
            @RequestParam(value = "time_range", defaultValue = "7d") String timeRange) {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            List<Map<String, Object>> rank = buildContributionRank();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("contribution_rank", rank);
            emitter.send(SseEmitter.event().data(payload));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }
        emitter.complete();
        return emitter;
    }

    // ---------- 内部实现 ----------

    /** 查询我创建的知识库（status=0） */
    private List<LibraryDO> selectMyLibraries(Long userId) {
        return libraryMapper.selectList(new LambdaQueryWrapperX<LibraryDO>()
                .eq(LibraryDO::getOwnerId, userId)
                .eq(LibraryDO::getStatus, 0));
    }

    /** 字节数自适应格式化（B/KB/MB/GB），避免小体量按 GB 四舍五入后显示为 0 */
    private String formatSize(long bytes) {
        if (bytes <= 0) {
            return "0B";
        }
        if (bytes < 1024) {
            return bytes + "B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1fKB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("%.1fMB", mb);
        }
        return String.format("%.2fGB", mb / 1024.0);
    }

    /** 按文档上传人聚合贡献排行，补齐用户昵称/头像 */
    private List<Map<String, Object>> buildContributionRank() {
        List<Map<String, Object>> rows = documentMapper.selectContributionRank(50);
        List<Map<String, Object>> rank = new ArrayList<>(rows.size());

        Set<Long> userIds = rows.stream()
                .map(r -> parseLong(r.get("creatorId")))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            try {
                userMap = adminUserApi.getUserMap(userIds);
            } catch (Exception ignored) {
            }
        }

        for (Map<String, Object> row : rows) {
            Long creatorId = parseLong(row.get("creatorId"));
            long docCount = row.get("docCount") == null ? 0 : Long.parseLong(String.valueOf(row.get("docCount")));
            AdminUserRespDTO user = creatorId != null ? userMap.get(creatorId) : null;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("user_id", creatorId);
            item.put("name", user != null ? user.getNickname() : "");
            item.put("avatar", user != null ? user.getAvatar() : "");
            item.put("doc_count", docCount);
            // 积分/热度/点赞：ruoyi 侧暂无对应统计，返回 0 占位
            item.put("total_points", 0);
            item.put("hotCount", 0);
            item.put("likeCount", 0);
            item.put("status", "none");
            rank.add(item);
        }
        return rank;
    }

    private Long parseLong(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Exception ignored) {
            return null;
        }
    }
}
