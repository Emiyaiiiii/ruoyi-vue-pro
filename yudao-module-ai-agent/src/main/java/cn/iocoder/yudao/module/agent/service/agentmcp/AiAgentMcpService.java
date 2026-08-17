package cn.iocoder.yudao.module.agent.service.agentmcp;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.agent.controller.admin.agentmcp.vo.AgentMcpPageReqVO;
import cn.iocoder.yudao.module.agent.controller.admin.agentmcp.vo.AgentMcpRespVO;
import cn.iocoder.yudao.module.agent.controller.admin.agentmcp.vo.AgentMcpSaveReqVO;
import cn.iocoder.yudao.module.agent.dal.dataobject.agentmcp.AiAgentMcpDO;

import java.util.List;

/**
 * 智能体-MCP 绑定 Service 接口
 *
 * @author 吴皓
 */
public interface AiAgentMcpService {

    /**
     * 创建绑定（同时下发到 QwenPaw）
     */
    Long createAgentMcp(AgentMcpSaveReqVO createReqVO);

    /**
     * 更新绑定（重新下发到 QwenPaw）
     */
    void updateAgentMcp(AgentMcpSaveReqVO updateReqVO);

    /**
     * 删除绑定（同时删除 QwenPaw MCP client）
     */
    void deleteAgentMcp(Long id);

    /**
     * 启停绑定（重新下发到 QwenPaw）
     */
    void toggleAgentMcp(Long id);

    /**
     * 获取绑定详情
     */
    AiAgentMcpDO getAgentMcp(Long id);

    /**
     * 分页查询绑定
     */
    PageResult<AgentMcpRespVO> getAgentMcpPage(AgentMcpPageReqVO pageReqVO);

    /**
     * 查询某智能体的绑定列表
     */
    List<AgentMcpRespVO> getAgentMcpListByAgentId(Long agentId);

}
