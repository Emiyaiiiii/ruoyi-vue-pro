package cn.iocoder.yudao.module.kb.service.sharedept;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import cn.iocoder.yudao.module.kb.controller.admin.sharedept.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.sharedept.ShareDeptDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.kb.dal.mysql.sharedept.ShareDeptMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 知识库共享部门关联 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
public class ShareDeptServiceImpl implements ShareDeptService {

    @Resource
    private ShareDeptMapper shareDeptMapper;

    @Override
    public Long createShareDept(ShareDeptSaveReqVO createReqVO) {
        // 插入
        ShareDeptDO shareDept = BeanUtils.toBean(createReqVO, ShareDeptDO.class);
        shareDeptMapper.insert(shareDept);

        // 返回
        return shareDept.getId();
    }

    @Override
    public void updateShareDept(ShareDeptSaveReqVO updateReqVO) {
        // 校验存在
        validateShareDeptExists(updateReqVO.getId());
        // 更新
        ShareDeptDO updateObj = BeanUtils.toBean(updateReqVO, ShareDeptDO.class);
        shareDeptMapper.updateById(updateObj);
    }

    @Override
    public void deleteShareDept(Long id) {
        // 校验存在
        validateShareDeptExists(id);
        // 删除
        shareDeptMapper.deleteById(id);
    }

    @Override
        public void deleteShareDeptListByIds(List<Long> ids) {
        // 删除
        shareDeptMapper.deleteByIds(ids);
        }


    private void validateShareDeptExists(Long id) {
        if (shareDeptMapper.selectById(id) == null) {
            throw exception(SHARE_DEPT_NOT_EXISTS);
        }
    }

    @Override
    public ShareDeptDO getShareDept(Long id) {
        return shareDeptMapper.selectById(id);
    }

    @Override
    public PageResult<ShareDeptDO> getShareDeptPage(ShareDeptPageReqVO pageReqVO) {
        return shareDeptMapper.selectPage(pageReqVO);
    }

}