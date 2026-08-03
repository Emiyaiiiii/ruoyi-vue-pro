package cn.iocoder.yudao.module.kb.service.folder;

import cn.iocoder.yudao.module.kb.dal.mysql.folder.FolderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.kb.controller.admin.folder.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.folder.FolderDO;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 文档文件夹 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private final FolderMapper folderMapper;

    @Override
    public Long createFolder(FolderSaveReqVO createReqVO) {
        // 校验同层级下名称唯一
        Long parentId = createReqVO.getParentId() != null ? createReqVO.getParentId() : FolderDO.PARENT_ID_ROOT;
        validateFolderNameUnique(null, createReqVO.getKbId(), parentId, createReqVO.getName());

        // 插入
        FolderDO folder = BeanUtils.toBean(createReqVO, FolderDO.class);
        folder.setParentId(parentId);
        folder.setSort(createReqVO.getSort() != null ? createReqVO.getSort() : 0);
        folderMapper.insert(folder);
        return folder.getId();
    }

    @Override
    public void updateFolder(FolderSaveReqVO updateReqVO) {
        // 校验存在
        FolderDO folder = validateFolderExists(updateReqVO.getId());
        // 校验同层级下名称唯一
        Long parentId = updateReqVO.getParentId() != null ? updateReqVO.getParentId() : folder.getParentId();
        validateFolderNameUnique(updateReqVO.getId(), folder.getKbId(), parentId, updateReqVO.getName());

        // 更新
        FolderDO updateObj = BeanUtils.toBean(updateReqVO, FolderDO.class);
        folderMapper.updateById(updateObj);
    }

    @Override
    public void deleteFolder(Long id) {
        // 校验存在
        validateFolderExists(id);
        // 校验是否有子文件夹
        if (folderMapper.selectCountByParentId(id) > 0) {
            throw exception(FOLDER_EXITS_CHILDREN);
        }
        // 删除
        folderMapper.deleteById(id);
    }

    @Override
    public FolderDO getFolder(Long id) {
        return folderMapper.selectById(id);
    }

    @Override
    public List<FolderDO> getFolderTree(Long kbId) {
        List<FolderDO> all = folderMapper.selectByKbId(kbId);
        // 构建树形结构
        return buildTree(all, FolderDO.PARENT_ID_ROOT);
    }

    /**
     * 递归构建文件夹树
     */
    private List<FolderDO> buildTree(List<FolderDO> all, Long parentId) {
        List<FolderDO> children = all.stream()
                .filter(f -> Objects.equals(f.getParentId(), parentId))
                .sorted(Comparator.comparingInt(FolderDO::getSort))
                .collect(Collectors.toList());
        for (FolderDO child : children) {
            child.setChildren(buildTree(all, child.getId()));
        }
        return children;
    }

    private FolderDO validateFolderExists(Long id) {
        FolderDO folder = folderMapper.selectById(id);
        if (folder == null) {
            throw exception(FOLDER_NOT_EXISTS);
        }
        return folder;
    }

    private void validateFolderNameUnique(Long id, Long kbId, Long parentId, String name) {
        FolderDO folder = folderMapper.selectByParentIdAndName(kbId, parentId, name);
        if (folder == null) {
            return;
        }
        if (id == null) {
            throw exception(FOLDER_NAME_DUPLICATE);
        }
        if (!Objects.equals(folder.getId(), id)) {
            throw exception(FOLDER_NAME_DUPLICATE);
        }
    }

}