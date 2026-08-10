package cn.iocoder.yudao.module.kb.dal.mysql.modelconfig;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.modelconfig.ModelConfigDO;
import cn.iocoder.yudao.module.kb.controller.admin.modelconfig.vo.*;
import org.apache.ibatis.annotations.Mapper;

/**
 * 大模型配置 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface ModelConfigMapper extends BaseMapperX<ModelConfigDO> {

    /**
     * 分页查询（支持多条件过滤和关键字搜索）
     */
    default PageResult<ModelConfigDO> selectPage(ModelConfigPageReqVO reqVO) {
        LambdaQueryWrapperX<ModelConfigDO> wrapper = new LambdaQueryWrapperX<ModelConfigDO>()
                .eqIfPresent(ModelConfigDO::getDeploy, reqVO.getDeploy())
                .eqIfPresent(ModelConfigDO::getIsActive, reqVO.getIsActive())
                .eqIfPresent(ModelConfigDO::getPlatform, reqVO.getPlatform());

        // 关键字搜索：uid / name / description 任一匹配（OR 关系）
        if (reqVO.getSearch() != null && !reqVO.getSearch().isEmpty()) {
            wrapper.and(w -> w
                    .like(ModelConfigDO::getUid, reqVO.getSearch())
                    .or()
                    .like(ModelConfigDO::getName, reqVO.getSearch())
                    .or()
                    .like(ModelConfigDO::getDescription, reqVO.getSearch()));
        }

        wrapper.orderByDesc(ModelConfigDO::getIsPinned)
               .orderByAsc(ModelConfigDO::getSortOrder)
               .orderByDesc(ModelConfigDO::getUpdateTime);

        return selectPage(reqVO, wrapper);
    }

    /**
     * 获取所有激活的模型配置（用于下拉选择等）
     */
    default List<ModelConfigDO> selectActiveList() {
        return selectList(new LambdaQueryWrapperX<ModelConfigDO>()
                .eq(ModelConfigDO::getIsActive, 1)
                .orderByDesc(ModelConfigDO::getIsPinned)
                .orderByAsc(ModelConfigDO::getSortOrder)
                .orderByDesc(ModelConfigDO::getUpdateTime));
    }

    /**
     * 根据UID查询
     */
    default ModelConfigDO selectByUid(String uid) {
        return selectOne(new LambdaQueryWrapperX<ModelConfigDO>()
                .eq(ModelConfigDO::getUid, uid));
    }
}
