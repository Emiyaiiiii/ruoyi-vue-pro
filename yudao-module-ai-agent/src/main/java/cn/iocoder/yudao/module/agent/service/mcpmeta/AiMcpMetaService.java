package cn.iocoder.yudao.module.agent.service.mcpmeta;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.agent.controller.admin.mcpmeta.vo.McpMetaPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.mcpmeta.vo.McpMetaSaveReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.mcpmeta.AiMcpMetaDO;

import java.util.List;

/**
 * MCP 商店 Service 接口
 *
 * @author 吴皓
 */
public interface AiMcpMetaService {

    /**
     * 创建 MCP 商店项
     */
    Long createMcpMeta(McpMetaSaveReqVO createReqVO);

    /**
     * 更新 MCP 商店项
     */
    void updateMcpMeta(McpMetaSaveReqVO updateReqVO);

    /**
     * 删除 MCP 商店项
     */
    void deleteMcpMeta(Long id);

    /**
     * 获取 MCP 商店项详情
     */
    AiMcpMetaDO getMcpMeta(Long id);

    /**
     * 分页查询 MCP 商店项
     */
    PageResult<AiMcpMetaDO> getMcpMetaPage(McpMetaPageReqVO pageReqVO);

    /**
     * 查询启用的商店项列表
     */
    List<AiMcpMetaDO> getEnabledMcpMetaList();

}
