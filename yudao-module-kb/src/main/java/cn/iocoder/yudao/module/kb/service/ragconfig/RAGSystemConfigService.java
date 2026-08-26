package cn.iocoder.yudao.module.kb.service.ragconfig;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.kb.controller.admin.ragconfig.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.ragconfig.RAGSystemConfigDO;

import java.util.*;

/**
 * RAG系统配置 Service 接口
 *
 * @author 吴皓
 */
public interface RAGSystemConfigService {

    /**
     * 创建RAG配置
     */
    Long createRAGConfig(RAGConfigSaveReqVO createReqVO);

    /**
     * 更新RAG配置
     */
    void updateRAGConfig(RAGConfigSaveReqVO updateReqVO);

    /**
     * 删除RAG配置
     */
    void deleteRAGConfig(Long id);

    /**
     * 获取RAG配置详情
     */
    RAGSystemConfigDO getRAGConfig(Long id);

    /**
     * 分页查询RAG配置
     */
    PageResult<RAGSystemConfigDO> getRAGConfigPage(RAGConfigPageReqVO pageReqVO);

    /**
     * 按模块获取配置键值对（仅激活的配置）
     */
    Map<String, Object> getConfigByModule(String module);

    /**
     * 批量更新配置
     */
    Map<String, Object> batchUpdate(List<Map<String, Object>> configs);

    /**
     * 刷新配置缓存
     */
    void refreshCache(String module, String key);

    /**
     * 获取模块列表及计数
     */
    List<Map<String, Object>> getModules();

    /**
     * 获取统计信息
     */
    Map<String, Object> getStatistics();

    /**
     * 启动预热：遍历各租户激活的 RAG 配置，经 RabbitMQ 全量重推给 python-vector。
     * 解决 RAG/rerank 配置触发式推送导致的服务重启后 Redis 缓存为空的问题。
     */
    void publishAllToVector();

}
