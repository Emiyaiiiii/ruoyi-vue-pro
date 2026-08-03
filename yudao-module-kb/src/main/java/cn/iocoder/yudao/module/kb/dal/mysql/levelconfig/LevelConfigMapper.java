package cn.iocoder.yudao.module.kb.dal.mysql.levelconfig;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.kb.controller.admin.levelconfig.vo.*;

/**
 * 知识库层级配置 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface LevelConfigMapper extends BaseMapperX<LevelConfigDO> {

    default PageResult<LevelConfigDO> selectPage(LevelConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LevelConfigDO>()
                .eqIfPresent(LevelConfigDO::getLevelCode, reqVO.getLevelCode())
                .likeIfPresent(LevelConfigDO::getLevelName, reqVO.getLevelName())
                .eqIfPresent(LevelConfigDO::getVisibilityRule, reqVO.getVisibilityRule())
                .eqIfPresent(LevelConfigDO::getOwnerDim, reqVO.getOwnerDim())
                .eqIfPresent(LevelConfigDO::getDeptScope, reqVO.getDeptScope())
                .eqIfPresent(LevelConfigDO::getSort, reqVO.getSort())
                .eqIfPresent(LevelConfigDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(LevelConfigDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(LevelConfigDO::getId));
    }

}