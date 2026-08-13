package cn.iocoder.yudao.module.kb.controller.admin.news;

import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import javax.validation.*;
import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.module.kb.controller.admin.news.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSourceDO;
import cn.iocoder.yudao.module.kb.service.news.NewsSourceService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;

@Tag(name = "管理后台 - 新闻数据源")
@RestController
@RequestMapping("/kb/news-source")
@Validated
public class NewsSourceController {

    @Resource
    private NewsSourceService newsSourceService;

    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建新闻数据源")
    @PreAuthorize("@ss.hasPermission('kb:news-source:create')")
    public CommonResult<Long> createNewsSource(@Valid @RequestBody NewsSourceSaveReqVO createReqVO) {
        return success(newsSourceService.createNewsSource(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新新闻数据源")
    @PreAuthorize("@ss.hasPermission('kb:news-source:update')")
    public CommonResult<Boolean> updateNewsSource(@Valid @RequestBody NewsSourceSaveReqVO updateReqVO) {
        newsSourceService.updateNewsSource(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除新闻数据源")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('kb:news-source:delete')")
    public CommonResult<Boolean> deleteNewsSource(@RequestParam("id") Long id) {
        newsSourceService.deleteNewsSource(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得新闻数据源")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('kb:news-source:query')")
    public CommonResult<NewsSourceRespVO> getNewsSource(@RequestParam("id") Long id) {
        NewsSourceDO source = newsSourceService.getNewsSource(id);
        return success(convertToRespVO(source));
    }

    @GetMapping("/page")
    @Operation(summary = "获得新闻数据源分页")
    @PreAuthorize("@ss.hasPermission('kb:news-source:query')")
    public CommonResult<PageResult<NewsSourceRespVO>> getNewsSourcePage(@Valid NewsSourcePageReqVO pageReqVO) {
        PageResult<NewsSourceDO> pageResult = newsSourceService.getNewsSourcePage(pageReqVO);
        List<NewsSourceRespVO> voList = new ArrayList<>();
        for (NewsSourceDO source : pageResult.getList()) {
            voList.add(convertToRespVO(source));
        }
        PageResult<NewsSourceRespVO> voResult = new PageResult<>();
        voResult.setList(voList);
        voResult.setTotal(pageResult.getTotal());
        return success(voResult);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取数据源统计信息")
    @Parameter(name = "id", description = "数据源编号", required = true)
    @PreAuthorize("@ss.hasPermission('kb:news-source:query')")
    public CommonResult<Map<String, Object>> getSourceStats(@RequestParam("id") Long id) {
        return success(newsSourceService.getSourceStats(id));
    }

    @GetMapping("/sync-logs")
    @Operation(summary = "获取数据源最近同步日志")
    @Parameter(name = "id", description = "数据源编号", required = true)
    @PreAuthorize("@ss.hasPermission('kb:news-source:query')")
    public CommonResult<List<Map<String, Object>>> getSourceSyncLogs(
            @RequestParam("id") Long id,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return success(newsSourceService.getSourceSyncLogs(id, limit));
    }

    @PostMapping("/trigger-sync")
    @Operation(summary = "手动触发数据源同步")
    @PreAuthorize("@ss.hasPermission('kb:news-source:update')")
    public CommonResult<Map<String, Object>> triggerSync(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") != null ? Long.valueOf(body.get("id").toString()) : null;
        String syncType = body.get("syncType") != null ? (String) body.get("syncType") : "manual";
        if (id == null) {
            return success(Collections.emptyMap());
        }
        return success(newsSourceService.triggerSync(id, syncType));
    }

    // ==================== 私有辅助 ====================

    private NewsSourceRespVO convertToRespVO(NewsSourceDO source) {
        NewsSourceRespVO vo = BeanUtils.toBean(source, NewsSourceRespVO.class);
        // 填充部门名称
        if (source.getDbDept() != null) {
            DeptRespDTO dept = deptApi.getDept(source.getDbDept());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }
        return vo;
    }
}
