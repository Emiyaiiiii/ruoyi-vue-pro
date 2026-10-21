package cn.iocoder.yudao.module.agent.service.workspace;

import java.util.List;
import java.util.Map;

/**
 * 智能体工作目录 Service 接口（透传模式）
 *
 * <p>QwenPaw 是工作区文件与记忆文件的 source of truth。Java 端不做任何本地持久化，
 * 所有读写操作都先校验智能体存在性，再通过 {@link cn.iocoder.yudao.module.agent.framework.config.QwenPawClient}
 * 转发到 QwenPaw 的 agent 级接口。
 *
 * @author 吴皓
 */
public interface AiWorkspaceService {

    /**
     * 列出智能体工作区目录树
     *
     * @param agentId 芋道智能体主键 ID
     * @param path    目录相对路径（相对 root），可为 null
     * @param cursor  分页游标，可为 null
     * @param root    根目录标识（如 project），可为 null
     * @param limit   每页数量，可为 null
     */
    Map<String, Object> listWorkspaceTree(Long agentId, String path, String cursor, String root, Integer limit);

    /**
     * 读取工作区文件元数据
     *
     * @param agentId 芋道智能体主键 ID
     * @param path    文件相对路径
     * @param root    根目录标识，可为 null
     */
    Map<String, Object> readWorkspaceFileMetadata(Long agentId, String path, String root);

    /**
     * 读取工作区文件内容（支持分页）
     *
     * @param agentId 芋道智能体主键 ID
     * @param path    文件相对路径
     * @param root    根目录标识，可为 null
     * @param offset  起始偏移量，可为 null
     * @param limit   读取长度，可为 null
     */
    Map<String, Object> readWorkspaceFileContent(Long agentId, String path, String root, Integer offset, Integer limit);

    /**
     * 写入工作区文件内容（覆盖）
     *
     * @param agentId 芋道智能体主键 ID
     * @param path    文件相对路径
     * @param content 文件内容
     * @param root    根目录标识，可为 null
     */
    Map<String, Object> writeWorkspaceFileContent(Long agentId, String path, String content, String root);

    /**
     * 下载工作区文件字节流
     *
     * @param agentId 芋道智能体主键 ID
     * @param path    文件相对路径
     */
    byte[] downloadWorkspaceFile(Long agentId, String path);

    /**
     * 上传文件到工作区
     *
     * @param agentId  芋道智能体主键 ID
     * @param data     文件字节
     * @param fileName 文件名
     * @param path     目标相对路径，可为 null
     * @param root     根目录标识，可为 null
     */
    Map<String, Object> uploadWorkspaceFile(Long agentId, byte[] data, String fileName, String path, String root);

    /**
     * 列出智能体的记忆 Markdown 文件
     *
     * @param agentId 芋道智能体主键 ID
     * @param section 记忆分区：daily / digest，可为 null
     */
    List<Map<String, Object>> listMemoryFiles(Long agentId, String section);

    /**
     * 读取指定记忆 Markdown 文件内容
     *
     * @param agentId 芋道智能体主键 ID
     * @param mdPath  记忆文件相对路径
     * @param section 记忆分区：daily / digest，可为 null
     */
    Map<String, Object> readMemoryFile(Long agentId, String mdPath, String section);

    /**
     * 写入指定记忆 Markdown 文件内容（覆盖）
     *
     * @param agentId 芋道智能体主键 ID
     * @param mdPath  记忆文件相对路径
     * @param content 记忆文件内容
     * @param section 记忆分区：daily / digest，可为 null
     */
    Map<String, Object> writeMemoryFile(Long agentId, String mdPath, String content, String section);

}
