package cn.iocoder.yudao.module.kb.controller.admin.news;

import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

import cn.iocoder.yudao.module.kb.controller.admin.news.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsRecordDO;
import cn.iocoder.yudao.module.kb.service.news.NewsRecordService;

@Tag(name = "管理后台 - 新闻记录")
@RestController
@RequestMapping("/kb/news-record")
@Validated
public class NewsRecordController {

    @Resource
    private NewsRecordService newsRecordService;

    @GetMapping("/get")
    @Operation(summary = "获得新闻记录详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('kb:news-record:query')")
    public CommonResult<NewsRecordRespVO> getNewsRecord(@RequestParam("id") Long id) {
        NewsRecordDO record = newsRecordService.getNewsRecord(id);
        return success(convertToRespVO(record));
    }

    @GetMapping("/page")
    @Operation(summary = "获得新闻记录分页")
    @PreAuthorize("@ss.hasPermission('kb:news-record:query')")
    public CommonResult<PageResult<NewsRecordRespVO>> getNewsRecordPage(@Valid NewsRecordPageReqVO pageReqVO) {
        PageResult<NewsRecordDO> pageResult = newsRecordService.getNewsRecordPage(pageReqVO);
        List<NewsRecordRespVO> voList = new ArrayList<>();
        for (NewsRecordDO record : pageResult.getList()) {
            voList.add(convertToRespVO(record));
        }
        PageResult<NewsRecordRespVO> voResult = new PageResult<>();
        voResult.setList(voList);
        voResult.setTotal(pageResult.getTotal());
        return success(voResult);
    }

    @PostMapping("/batch-retry")
    @Operation(summary = "批量重试新闻记录")
    @PreAuthorize("@ss.hasPermission('kb:news-record:batch')")
    public CommonResult<Boolean> batchRetry(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> idList = (List<Integer>) body.get("ids");
        if (idList == null || idList.isEmpty()) {
            return success(true);
        }
        List<Long> ids = new ArrayList<>();
        for (Integer i : idList) {
            ids.add(i.longValue());
        }
        newsRecordService.batchRetry(ids);
        return success(true);
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除新闻记录")
    @PreAuthorize("@ss.hasPermission('kb:news-record:batch')")
    public CommonResult<Boolean> batchDelete(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> idList = (List<Integer>) body.get("ids");
        if (idList == null || idList.isEmpty()) {
            return success(true);
        }
        List<Long> ids = new ArrayList<>();
        for (Integer i : idList) {
            ids.add(i.longValue());
        }
        newsRecordService.batchDelete(ids);
        return success(true);
    }

    @GetMapping("/channels")
    @Operation(summary = "获取所有不重复的频道列表")
    @PreAuthorize("@ss.hasPermission('kb:news-record:query')")
    public CommonResult<List<String>> getChannels() {
        return success(newsRecordService.getChannels());
    }

    @GetMapping("/stats")
    @Operation(summary = "获取全局统计信息")
    @PreAuthorize("@ss.hasPermission('kb:news-record:query')")
    public CommonResult<Map<String, Long>> getStats() {
        return success(newsRecordService.getStats());
    }

    // ========== 解析接口 — 暂未实现 ==========

    @PostMapping("/parse")
    @Operation(summary = "单条解析（暂未实现）")
    @PreAuthorize("@ss.hasPermission('kb:news-record:batch')")
    public CommonResult<Map<String, Object>> parse(@RequestBody Map<String, Object> body) {
        return success(buildNotImplementedResponse());
    }

    @PostMapping("/batch-parse")
    @Operation(summary = "批量解析（暂未实现）")
    @PreAuthorize("@ss.hasPermission('kb:news-record:batch')")
    public CommonResult<Map<String, Object>> batchParse(@RequestBody Map<String, Object> body) {
        return success(buildNotImplementedResponse());
    }

    // ==================== 私有辅助 ====================

    private NewsRecordRespVO convertToRespVO(NewsRecordDO record) {
        NewsRecordRespVO vo = BeanUtils.toBean(record, NewsRecordRespVO.class);
        vo.setStatusDisplay(record.getStatusDisplay());
        return vo;
    }

    private Map<String, Object> buildNotImplementedResponse() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "新闻解析功能暂未实现，敬请期待");
        result.put("notImplemented", true);
        return result;
    }
}
