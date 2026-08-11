package cn.iocoder.yudao.module.kb.dal.mysql.chunkmethod;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.chunkmethod.ChunkMethodDO;
import cn.iocoder.yudao.module.kb.controller.admin.chunkmethod.vo.*;
import org.apache.ibatis.annotations.Mapper;

/**
 * 切片方法 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface ChunkMethodMapper extends BaseMapperX<ChunkMethodDO> {

    /**
     * 分页查询（支持多条件过滤和关键字搜索）
     */
    default PageResult<ChunkMethodDO> selectPage(ChunkMethodPageReqVO reqVO) {
        LambdaQueryWrapperX<ChunkMethodDO> wrapper = new LambdaQueryWrapperX<ChunkMethodDO>()
                .eqIfPresent(ChunkMethodDO::getMethodType, reqVO.getMethodType())
                .eqIfPresent(ChunkMethodDO::getIsActive, reqVO.getIsActive());

        // 关键字搜索：name / code / description 任一匹配（OR 关系）
        if (reqVO.getSearch() != null && !reqVO.getSearch().isEmpty()) {
            wrapper.and(w -> w
                    .like(ChunkMethodDO::getName, reqVO.getSearch())
                    .or()
                    .like(ChunkMethodDO::getCode, reqVO.getSearch())
                    .or()
                    .like(ChunkMethodDO::getDescription, reqVO.getSearch()));
        }

        wrapper.orderByAsc(ChunkMethodDO::getMethodType)
               .orderByAsc(ChunkMethodDO::getName);

        return selectPage(reqVO, wrapper);
    }

    /**
     * 获取所有启用的切片方法（用于下拉选择等）
     */
    default List<ChunkMethodDO> selectActiveList() {
        return selectList(new LambdaQueryWrapperX<ChunkMethodDO>()
                .eq(ChunkMethodDO::getIsActive, 1)
                .orderByAsc(ChunkMethodDO::getMethodType)
                .orderByAsc(ChunkMethodDO::getName));
    }

    /**
     * 根据Code查询
     */
    default ChunkMethodDO selectByCode(String code) {
        return selectOne(new LambdaQueryWrapperX<ChunkMethodDO>()
                .eq(ChunkMethodDO::getCode, code));
    }

    /**
     * 获取默认切片方法
     */
    default ChunkMethodDO selectDefaultMethod() {
        return selectOne(new LambdaQueryWrapperX<ChunkMethodDO>()
                .eq(ChunkMethodDO::getIsDefaultMethod, 1)
                .eq(ChunkMethodDO::getIsActive, 1));
    }
}
