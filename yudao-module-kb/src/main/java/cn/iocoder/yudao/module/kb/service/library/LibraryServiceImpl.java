package cn.iocoder.yudao.module.kb.service.library;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.sharedept.ShareDeptDO;
import cn.iocoder.yudao.module.kb.dal.mysql.levelconfig.LevelConfigMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.sharedept.ShareDeptMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.userdept.KbUserDeptMapper;
import cn.iocoder.yudao.module.kb.service.projectmember.ProjectMemberService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.kb.controller.admin.library.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 知识库 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
public class LibraryServiceImpl implements LibraryService {

    @Resource
    private LibraryMapper libraryMapper;

    @Resource
    private ShareDeptMapper shareDeptMapper;

    @Resource
    private LevelConfigMapper levelConfigMapper;

    @Resource
    private KbUserDeptMapper kbUserDeptMapper;

    @Resource
    private ProjectMemberService projectMemberService;

    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLibrary(LibrarySaveReqVO createReqVO) {
        // 插入
        LibraryDO library = BeanUtils.toBean(createReqVO, LibraryDO.class);
        libraryMapper.insert(library);

        // 保存共享部门关联（仅共享知识库需要）
        if (createReqVO.getShareDeptIds() != null) {
            shareDeptMapper.delete(new LambdaQueryWrapper<ShareDeptDO>()
                    .eq(ShareDeptDO::getKbId, library.getId()));

            if (CollUtil.isNotEmpty(createReqVO.getShareDeptIds())) {
                List<ShareDeptDO> list = createReqVO.getShareDeptIds().stream()
                        .map(deptId -> new ShareDeptDO()
                                .setKbId(library.getId())
                                .setDeptId(deptId))
                        .collect(Collectors.toList());
                shareDeptMapper.insertBatch(list);
            }
        }
        return library.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLibrary(LibrarySaveReqVO updateReqVO) {
        // 校验存在
        LibraryDO library = validateLibraryExists(updateReqVO.getId());
        // 校验管理权限
        validateManagementPermission(library);

        // 更新
        LibraryDO updateObj = BeanUtils.toBean(updateReqVO, LibraryDO.class);
        libraryMapper.updateById(updateObj);

        // 保存共享部门关联
        if (updateReqVO.getShareDeptIds() != null) {
            shareDeptMapper.delete(new LambdaQueryWrapper<ShareDeptDO>()
                    .eq(ShareDeptDO::getKbId, updateObj.getId()));

            if (CollUtil.isNotEmpty(updateReqVO.getShareDeptIds())) {
                List<ShareDeptDO> list = updateReqVO.getShareDeptIds().stream()
                        .map(deptId -> new ShareDeptDO()
                                .setKbId(updateObj.getId())
                                .setDeptId(deptId))
                        .collect(Collectors.toList());
                shareDeptMapper.insertBatch(list);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLibrary(Long id) {
        // 校验存在
        LibraryDO library = validateLibraryExists(id);
        // 校验管理权限
        validateManagementPermission(library);

        // 删除关联的共享部门记录
        shareDeptMapper.delete(new LambdaQueryWrapper<ShareDeptDO>()
                .eq(ShareDeptDO::getKbId, id));
        // 删除关联的项目成员记录
        projectMemberService.removeAllByKbId(id);
        // 删除
        libraryMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLibraryListByIds(List<Long> ids) {
        // 先全部校验权限
        for (Long id : ids) {
            LibraryDO library = libraryMapper.selectById(id);
            if (library != null) {
                validateManagementPermission(library);
            }
        }
        // 再统一删除关联的共享部门记录和项目成员记录
        for (Long id : ids) {
            shareDeptMapper.delete(new LambdaQueryWrapper<ShareDeptDO>()
                    .eq(ShareDeptDO::getKbId, id));
            projectMemberService.removeAllByKbId(id);
        }
        // 最后批量删除知识库
        libraryMapper.deleteByIds(ids);
    }

    /**
     * 校验知识库管理权限（统一走 kb_user_dept 表）
     * 个人知识库(visibilityRule=1)：只有所有者本人可以管理
     * 院级/咨询评估(visibilityRule=2)：该部门管理员(role=1)可以管理
     * 公司级(visibilityRule=3)：公司部门管理员(role=1)可以管理
     */
    private void validateManagementPermission(LibraryDO library) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw exception(LIBRARY_PERMISSION_DENIED);
        }

        // 超级管理员/租户管理员 → 拥有所有管理权限
        if (securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode())) {
            return;
        }

        LevelConfigDO cfg = levelConfigMapper.selectById(library.getKbLevelId());
        if (cfg == null) return; // 配置不存在则不阻止（由 API 层权限控制）

        Integer rule = cfg.getVisibilityRule();
        if (rule == null) return;

        // 个人知识库 → 只有所有者可以管理
        if (rule == 1) {
            if (library.getOwnerId() == null || !library.getOwnerId().equals(userId)) {
                throw exception(LIBRARY_PERMISSION_DENIED);
            }
        }

        // 院级/咨询评估/公司级 → 查 kb_user_dept，用户须为该部门管理员(role=1)
        if (rule == 2 || rule == 3) {
            if (library.getOwnerId() == null
                    || !kbUserDeptMapper.isAdmin(userId, library.getOwnerId())) {
                throw exception(LIBRARY_PERMISSION_DENIED);
            }
        }
    }

    private LibraryDO validateLibraryExists(Long id) {
        LibraryDO library = libraryMapper.selectById(id);
        if (library == null) {
            throw exception(LIBRARY_NOT_EXISTS);
        }
        return library;
    }

    @Override
    public LibraryDO getLibrary(Long id) {
        return libraryMapper.selectById(id);
    }

    @Override
    public PageResult<LibraryDO> getLibraryPage(LibraryPageReqVO pageReqVO) {
        return libraryMapper.selectPage(pageReqVO);
    }

    @Override
    public void togglePublic(Long id) {
        LibraryDO library = libraryMapper.selectById(id);
        if (library == null) {
            throw exception(LIBRARY_NOT_EXISTS);
        }
        library.setIsPublic(library.getIsPublic() != null && library.getIsPublic() == 1 ? 0 : 1);
        libraryMapper.updateById(library);
    }

    @Override
    public PageResult<LibraryDO> getPublicPage(PageParam pageParam) {
        return libraryMapper.selectPublicPage(pageParam);
    }

    @Override
    public PageResult<LibraryDO> getMyPublicPage(PageParam pageParam, Long userId) {
        return libraryMapper.selectMyPublicPage(pageParam, userId);
    }

    @Override
    public List<LibraryDO> getSimpleLibraryList(Integer isProject) {
        return libraryMapper.selectSimpleList(isProject);
    }

}
