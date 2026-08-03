package cn.iocoder.yudao.module.kb.dal.mysql.sharedept;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.sharedept.ShareDeptDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.kb.controller.admin.sharedept.vo.*;

/**
 * 知识库共享部门关联 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface ShareDeptMapper extends BaseMapperX<ShareDeptDO> {

    default PageResult<ShareDeptDO> selectPage(ShareDeptPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ShareDeptDO>()
                .eqIfPresent(ShareDeptDO::getKbId, reqVO.getKbId())
                .eqIfPresent(ShareDeptDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(ShareDeptDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ShareDeptDO::getId));
    }

}