package cn.iocoder.yudao.module.kb.service.sharedept;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.yudao.module.kb.controller.admin.sharedept.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.sharedept.ShareDeptDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 知识库共享部门关联 Service 接口
 *
 * @author 吴皓
 */
public interface ShareDeptService {

    /**
     * 创建知识库共享部门关联
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createShareDept(@Valid ShareDeptSaveReqVO createReqVO);

    /**
     * 更新知识库共享部门关联
     *
     * @param updateReqVO 更新信息
     */
    void updateShareDept(@Valid ShareDeptSaveReqVO updateReqVO);

    /**
     * 删除知识库共享部门关联
     *
     * @param id 编号
     */
    void deleteShareDept(Long id);

    /**
    * 批量删除知识库共享部门关联
    *
    * @param ids 编号
    */
    void deleteShareDeptListByIds(List<Long> ids);

    /**
     * 获得知识库共享部门关联
     *
     * @param id 编号
     * @return 知识库共享部门关联
     */
    ShareDeptDO getShareDept(Long id);

    /**
     * 获得知识库共享部门关联分页
     *
     * @param pageReqVO 分页查询
     * @return 知识库共享部门关联分页
     */
    PageResult<ShareDeptDO> getShareDeptPage(ShareDeptPageReqVO pageReqVO);

}