package cn.iocoder.yudao.module.kb.service.folder;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.yudao.module.kb.controller.admin.folder.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.folder.FolderDO;

/**
 * 文档文件夹 Service 接口
 *
 * @author 吴皓
 */
public interface FolderService {

    /**
     * 创建文件夹
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createFolder(@Valid FolderSaveReqVO createReqVO);

    /**
     * 更新文件夹（重命名）
     *
     * @param updateReqVO 更新信息
     */
    void updateFolder(@Valid FolderSaveReqVO updateReqVO);

    /**
     * 删除文件夹
     *
     * @param id 编号
     */
    void deleteFolder(Long id);

    /**
     * 获得文件夹
     *
     * @param id 编号
     * @return 文件夹
     */
    FolderDO getFolder(Long id);

    /**
     * 获得指定知识库下的文件夹树
     *
     * @param kbId 知识库ID
     * @return 文件夹树列表
     */
    List<FolderDO> getFolderTree(Long kbId);

    /**
     * 按相对路径获取（不存在则逐级创建）目标文件夹，返回叶子文件夹ID。
     * 用于文件夹层级上传：将 webkitRelativePath 的目录部分映射到知识库文件夹树。
     *
     * @param kbId     知识库ID
     * @param parentId 起始父文件夹（null 或 0 表示根目录）
     * @param relPath  相对目录路径，以 '/' 分段，如 "a/b"（空则返回 parentId）
     * @return 叶子文件夹ID
     */
    Long getOrCreateFolderByPath(Long kbId, Long parentId, String relPath);

}