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

}
