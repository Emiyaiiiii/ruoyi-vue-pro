package cn.iocoder.yudao.module.agent.dal.mysql.agentmcp;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.agent.dal.dataobject.agentmcp.AiAgentMcpDO;
import cn.iocoder.yudao.module.agent.controller.admin.agentmcp.vo.AgentMcpPageReqVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 智能体-MCP 绑定 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface AiAgentMcpMapper extends BaseMapperX<AiAgentMcpDO> {

    /**
     * 分页查询（支持按智能体过滤）
     */
    default PageResult<AiAgentMcpDO> selectPage(AgentMcpPageReqVO reqVO) {
        LambdaQueryWrapperX<AiAgentMcpDO> wrapper = new LambdaQueryWrapperX<AiAgentMcpDO>()
                .eqIfPresent(AiAgentMcpDO::getAgentId, reqVO.getAgentId())
                .eqIfPresent(AiAgentMcpDO::getMcpMetaId, reqVO.getMcpMetaId())
                .eqIfPresent(AiAgentMcpDO::getEnabled, reqVO.getEnabled());

        wrapper.orderByAsc(AiAgentMcpDO::getSortOrder)
               .orderByDesc(AiAgentMcpDO::getCreateTime);

        return selectPage(reqVO, wrapper);
    }

    /**
     * 查询某智能体的绑定列表
     */
    default List<AiAgentMcpDO> selectListByAgentId(Long agentId) {
        return selectList(new LambdaQueryWrapperX<AiAgentMcpDO>()
                .eq(AiAgentMcpDO::getAgentId, agentId)
                .orderByAsc(AiAgentMcpDO::getSortOrder));
    }

    /**
     * 查询某智能体启用的绑定列表
     */
    default List<AiAgentMcpDO> selectEnabledListByAgentId(Long agentId) {
        return selectList(new LambdaQueryWrapperX<AiAgentMcpDO>()
                .eq(AiAgentMcpDO::getAgentId, agentId)
                .eq(AiAgentMcpDO::getEnabled, 1)
                .orderByAsc(AiAgentMcpDO::getSortOrder));
    }

    /**
     * 校验重复绑定（同一智能体 + 同一 MCP）
     */
    default AiAgentMcpDO selectByAgentIdAndMcpMetaId(Long agentId, Long mcpMetaId) {
        return selectOne(new LambdaQueryWrapperX<AiAgentMcpDO>()
                .eq(AiAgentMcpDO::getAgentId, agentId)
                .eq(AiAgentMcpDO::getMcpMetaId, mcpMetaId));
    }

}
