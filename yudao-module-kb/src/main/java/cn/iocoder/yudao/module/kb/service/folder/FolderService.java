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

}