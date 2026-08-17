package cn.iocoder.yudao.module.kb.service.library;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.yudao.module.kb.controller.admin.library.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 知识库 Service 接口
 *
 * @author 吴皓
 */
public interface LibraryService {

    /**
     * 创建知识库
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createLibrary(@Valid LibrarySaveReqVO createReqVO);

    /**
     * 更新知识库
     *
     * @param updateReqVO 更新信息
     */
    void updateLibrary(@Valid LibrarySaveReqVO updateReqVO);

    /**
     * 删除知识库
     *
     * @param id 编号
     */
    void deleteLibrary(Long id);

    /**
    * 批量删除知识库
    *
    * @param ids 编号
    */
    void deleteLibraryListByIds(List<Long> ids);

    /**
     * 获得知识库
     *
     * @param id 编号
     * @return 知识库
     */
    LibraryDO getLibrary(Long id);

    /**
     * 获得知识库分页
     *
     * @param pageReqVO 分页查询
     * @return 知识库分页
     */
    PageResult<LibraryDO> getLibraryPage(LibraryPageReqVO pageReqVO);

    /**
     * 切换知识库公开状态
     *
     * @param id 知识库编号
     */
    void togglePublic(Long id);

    /**
     * 获得广场公开知识库分页
     */
    PageResult<LibraryDO> getPublicPage(PageParam pageParam);

    /**
     * 获得我的公开知识库分页
     */
    PageResult<LibraryDO> getMyPublicPage(PageParam pageParam, Long userId);

    /**
     * 获得知识库精简列表（用于下拉选择）
     *
     * @param isProject 是否项目成果库（可选，null=全部）
     * @return 知识库精简列表
     */
    List<LibraryDO> getSimpleLibraryList(Integer isProject);

    /**
     * 检查当前用户是否有该知识库的管理权限（增删改）
     *
     * @param kbId 知识库ID
     * @return true=有管理权限
     */
    boolean canManage(Long kbId);
}