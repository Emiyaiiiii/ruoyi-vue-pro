package cn.iocoder.yudao.module.kb.dal.mysql.library;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import cn.iocoder.yudao.module.kb.controller.admin.library.vo.*;

/**
 * 知识库 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface LibraryMapper extends BaseMapperX<LibraryDO> {


    default PageResult<LibraryDO> selectPage(LibraryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LibraryDO>()
                .likeIfPresent(LibraryDO::getName, reqVO.getName())
                .eqIfPresent(LibraryDO::getCategoryId, reqVO.getCategoryId())
                .eqIfPresent(LibraryDO::getKbLevelId, reqVO.getKbLevelId())
                .eqIfPresent(LibraryDO::getOwnerId, reqVO.getOwnerId())
                .likeIfPresent(LibraryDO::getDescription, reqVO.getDescription())
                .eqIfPresent(LibraryDO::getCoverUrl, reqVO.getCoverUrl())
                .eqIfPresent(LibraryDO::getDocCount, reqVO.getDocCount())
                .eqIfPresent(LibraryDO::getStatus, reqVO.getStatus())
                .eqIfPresent(LibraryDO::getIsPublic, reqVO.getIsPublic())
                .eqIfPresent(LibraryDO::getIsProject, reqVO.getIsProject())
                .betweenIfPresent(LibraryDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(LibraryDO::getId));
    }

    /**
     * 查询广场公开知识库分页（isPublic=1 的所有知识库，不限制分类）
     */
    default PageResult<LibraryDO> selectPublicPage(PageParam pageParam, String name) {
        return selectPage(pageParam, new LambdaQueryWrapperX<LibraryDO>()
                .eq(LibraryDO::getIsPublic, 1)
                .eq(LibraryDO::getStatus, 0)
                .likeIfPresent(LibraryDO::getName, name)
                .orderByDesc(LibraryDO::getId));
    }

    /**
     * 查询用户公开的知识库分页
     */
    default PageResult<LibraryDO> selectMyPublicPage(PageParam pageParam, Long userId) {
        return selectPage(pageParam, new LambdaQueryWrapperX<LibraryDO>()
                .eq(LibraryDO::getIsPublic, 1)
                .eq(LibraryDO::getStatus, 0)
                .eq(LibraryDO::getOwnerId, userId)
                .orderByDesc(LibraryDO::getId));
    }

    /**
     * 查询知识库精简列表（用于下拉选择）
     *
     * @param isProject 是否项目成果库（可选，null=全部）
     * @return 知识库列表
     */
    default List<LibraryDO> selectSimpleList(Integer isProject) {
        return selectList(new LambdaQueryWrapperX<LibraryDO>()
                .eq(isProject != null, LibraryDO::getIsProject, isProject)
                .orderByDesc(LibraryDO::getId));
    }

    /**
     * 更新知识库文档数量
     * @param kbId  知识库ID
     * @param delta 变化量（正数增加，负数减少）
     */
    @Update("UPDATE kb_library SET doc_count = doc_count + #{delta} WHERE id = #{kbId}")
    void updateDocCount(@Param("kbId") Long kbId, @Param("delta") int delta);

}