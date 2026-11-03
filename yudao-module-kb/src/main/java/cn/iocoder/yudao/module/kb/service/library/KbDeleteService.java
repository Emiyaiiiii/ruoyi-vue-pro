package cn.iocoder.yudao.module.kb.service.library;

import java.util.List;
import java.util.Map;

/**
 * 知识库异步删除 Service
 *
 * <p>删除知识库需要逐个文档清理向量/切片/图片/源文件/Wiki 引用（每篇文档多次远程调用），
 * 库大时会持续数分钟。这里把删除转为后台任务，前端轮询进度，避免请求超时与「无反馈」。</p>
 *
 * <p>删除仍然逐文档提交（不在一个大事务里），单个文档清理失败只记 warning，继续删其余文档。</p>
 */
public interface KbDeleteService {

    /**
     * 异步删除单个知识库，立即返回 `{taskId, status}`。
     */
    Map<String, Object> deleteLibraryAsync(Long kbId);

    /**
     * 异步批量删除知识库（逐个删除，进度按库计数）。
     */
    Map<String, Object> deleteLibraryListAsync(List<Long> ids);

    /**
     * 查询删除任务进度：{status, stage, progress, message, total, current, result, warnings}。
     */
    Map<String, Object> getTaskProgress(String taskId);
}
