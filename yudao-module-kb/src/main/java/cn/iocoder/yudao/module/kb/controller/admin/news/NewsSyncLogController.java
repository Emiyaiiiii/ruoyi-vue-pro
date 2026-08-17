package cn.iocoder.yudao.module.kb.controller.admin.news;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;

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

import cn.iocoder.yudao.module.kb.controller.admin.news.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSyncLogDO;
import cn.iocoder.yudao.module.kb.service.news.NewsSyncLogService;

@Tag(name = "管理后台 - 新闻同步日志")
@RestController
@RequestMapping("/kb/news-sync-log")
@Validated
public class NewsSyncLogController {

    @Resource
    private NewsSyncLogService newsSyncLogService;

    @GetMapping("/get")
    @Operation(summary = "获得同步日志详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('kb:news-sync-log:query')")
    public CommonResult<NewsSyncLogRespVO> getNewsSyncLog(@RequestParam("id") Long id) {
        NewsSyncLogDO log = newsSyncLogService.getNewsSyncLog(id);
        return success(convertToRespVO(log));
    }

    @GetMapping("/page")
    @Operation(summary = "获得同步日志分页")
    @PreAuthorize("@ss.hasPermission('kb:news-sync-log:query')")
    public CommonResult<PageResult<NewsSyncLogRespVO>> getNewsSyncLogPage(@Valid NewsSyncLogPageReqVO pageReqVO) {
        PageResult<NewsSyncLogDO> pageResult = newsSyncLogService.getNewsSyncLogPage(pageReqVO);
        List<NewsSyncLogRespVO> voList = new ArrayList<>();
        for (NewsSyncLogDO log : pageResult.getList()) {
            voList.add(convertToRespVO(log));
        }
        PageResult<NewsSyncLogRespVO> voResult = new PageResult<>();
        voResult.setList(voList);
        voResult.setTotal(pageResult.getTotal());
        return success(voResult);
    }

    // ==================== 私有辅助 ====================

    private NewsSyncLogRespVO convertToRespVO(NewsSyncLogDO log) {
        NewsSyncLogRespVO vo = BeanUtils.toBean(log, NewsSyncLogRespVO.class);
        vo.setSourceName(log.getSourceName());
        vo.setSyncTypeDisplay(log.getSyncTypeDisplay());
        vo.setStatusDisplay(log.getStatusDisplay());
        return vo;
    }
}
