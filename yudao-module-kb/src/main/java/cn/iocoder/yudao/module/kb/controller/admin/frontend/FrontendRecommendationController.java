package cn.iocoder.yudao.module.kb.controller.admin.frontend;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendRecommendationListVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendRecommendationVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendResult;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 前端 C 端 - 推荐/首页兼容层
 *
 * <p>补齐首页两个缺失接口：
 * <ul>
 *   <li>{@code /recommendations/knowledge-bases/} —— 推荐知识库（复用广场公开库，冷启动式按文档数排序）</li>
 *   <li>{@code /agents/profiles/recommended-list/} —— 推荐智能体（Java 端暂无智能体档案，返回空数组，前端回退默认智能体）</li>
 * </ul>
 *
 * @author 吴皓
 */
@Tag(name = "前端 C 端 - 推荐/首页兼容层")
@RestController
public class FrontendRecommendationController {

    @Resource
    private LibraryMapper libraryMapper;

    /**
     * 推荐知识库（对应 /recommendations/knowledge-bases/?limit=6）
     *
     * <p>Python 端走四路召回 + 加权排序；Java 端暂无使用事件/关注热度数据，
     * 此处降级为冷启动逻辑：返回广场公开库，按文档数、id 倒序取前 limit 条。
     */
    @GetMapping({"/recommendations/knowledge-bases", "/recommendations/knowledge-bases/"})
    @Operation(summary = "前端推荐知识库列表")
    public FrontendResult<FrontendRecommendationListVO> recommendKnowledgeBases(
            @RequestParam(value = "limit", defaultValue = "6") Integer limit) {
        int capped = limit == null || limit < 1 ? 6 : Math.min(limit, 20);

        PageParam pageParam = new PageParam();
        pageParam.setPageNo(1);
        pageParam.setPageSize(capped);
        PageResult<LibraryDO> page = libraryMapper.selectPage(pageParam, new LambdaQueryWrapperX<LibraryDO>()
                .eq(LibraryDO::getIsPublic, 1)
                .eq(LibraryDO::getStatus, 0)
                .orderByDesc(LibraryDO::getDocCount)
                .orderByDesc(LibraryDO::getId));

        List<FrontendRecommendationVO> recommendations = page.getList().stream()
                .map(this::toRecommendation)
                .collect(Collectors.toList());

        FrontendRecommendationListVO data = new FrontendRecommendationListVO();
        data.setRecommendations(recommendations);
        data.setCount(recommendations.size());
        data.setElapsedMs(0);
        return FrontendResult.ok(data);
    }

    /**
     * 推荐智能体（对应 /agents/profiles/recommended-list/）
     *
     * <p>Java 端当前没有智能体档案（AgentProfile）实体，返回空数组；
     * 前端 getChatTypeList 会据此回退到默认智能体，不会报错。
     */
    @GetMapping({"/agents/profiles/recommended-list", "/agents/profiles/recommended-list/"})
    @Operation(summary = "前端推荐智能体列表")
    public FrontendResult<List<Object>> recommendedAgents() {
        return FrontendResult.ok(Collections.emptyList());
    }

    private FrontendRecommendationVO toRecommendation(LibraryDO lib) {
        FrontendRecommendationVO vo = new FrontendRecommendationVO();
        vo.setKbId(lib.getId());
        vo.setName(lib.getName());
        vo.setDescription(lib.getDescription() == null ? "" : lib.getDescription());
        vo.setDocumentsCount(lib.getDocCount() == null ? 0 : lib.getDocCount());
        vo.setFollowersCount(0);
        vo.setTags(Collections.emptyList());
        vo.setCategory("");
        vo.setIsPublic(lib.getIsPublic() != null && lib.getIsPublic() == 1);
        vo.setScore(0.5);
        vo.setReason("热门推荐");
        vo.setReasonDetail(Collections.singletonList("热门推荐"));
        return vo;
    }
}
