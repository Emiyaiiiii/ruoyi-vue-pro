package cn.iocoder.yudao.module.agent.service.workspace;

import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.agent.dal.dataobject.agent.AiAgentDO;
import cn.iocoder.yudao.module.agent.dal.mysql.agent.AiAgentMapper;
import cn.iocoder.yudao.module.agent.framework.config.QwenPawClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.agent.enums.ErrorCodeConstants.AGENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.agent.enums.ErrorCodeConstants.QWENPAW_AGENT_NOT_FOUND;

/**
 * 智能体工作目录 Service 实现（透传模式）
 *
 * <p>本实现不维护任何本地文件/记忆表：所有数据走 QwenPaw，Java 端只做
 * （1）智能体存在性校验；（2）按 agent 作用域转发。
 *
 * @author 吴皓
 */
@Service
@Validated
@Slf4j
public class AiWorkspaceServiceImpl implements AiWorkspaceService {

    @Resource
    private QwenPawClient qwenPawClient;
    @Resource
    private AiAgentMapper agentMapper;

    @Override
    public Map<String, Object> listWorkspaceTree(Long agentId, String path, String cursor, String root, Integer limit) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.listWorkspaceTree(agent.getQwenpawAgentId(), path, cursor, root, limit);
    }

    @Override
    public Map<String, Object> readWorkspaceFileMetadata(Long agentId, String path, String root) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.readWorkspaceFileMetadata(agent.getQwenpawAgentId(), path, root);
    }

    @Override
    public Map<String, Object> readWorkspaceFileContent(Long agentId, String path, String root, Integer offset, Integer limit) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.readWorkspaceFileContent(agent.getQwenpawAgentId(), path, root, offset, limit);
    }

    @Override
    public Map<String, Object> writeWorkspaceFileContent(Long agentId, String path, String content, String root) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.writeWorkspaceFileContent(agent.getQwenpawAgentId(), path, content, root);
    }

    @Override
    public byte[] downloadWorkspaceFile(Long agentId, String path) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.downloadWorkspaceFile(agent.getQwenpawAgentId(), path);
    }

    @Override
    public Map<String, Object> uploadWorkspaceFile(Long agentId, byte[] data, String fileName, String path, String root) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.uploadWorkspaceFile(agent.getQwenpawAgentId(), data, fileName, path, root);
    }

    @Override
    public List<Map<String, Object>> listMemoryFiles(Long agentId, String section) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.listMemoryFiles(agent.getQwenpawAgentId(), section);
    }

    @Override
    public Map<String, Object> readMemoryFile(Long agentId, String mdPath, String section) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.readMemoryFile(agent.getQwenpawAgentId(), mdPath, section);
    }

    @Override
    public Map<String, Object> writeMemoryFile(Long agentId, String mdPath, String content, String section) {
        AiAgentDO agent = validateAgent(agentId);
        return qwenPawClient.writeMemoryFile(agent.getQwenpawAgentId(), mdPath, content, section);
    }

    /**
     * 校验智能体存在并返回 DO（命中芋道租户隔离）
     */
    private AiAgentDO validateAgent(Long agentId) {
        AiAgentDO agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw ServiceExceptionUtil.exception(AGENT_NOT_EXISTS);
        }
        if (agent.getQwenpawAgentId() == null || agent.getQwenpawAgentId().isEmpty()) {
            throw ServiceExceptionUtil.exception(QWENPAW_AGENT_NOT_FOUND);
        }
        return agent;
    }

}
