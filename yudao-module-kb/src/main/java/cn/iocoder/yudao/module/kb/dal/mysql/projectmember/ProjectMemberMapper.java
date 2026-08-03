package cn.iocoder.yudao.module.kb.dal.mysql.projectmember;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.projectmember.ProjectMemberDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 知识库项目成员 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface ProjectMemberMapper extends BaseMapperX<ProjectMemberDO> {

    /**
     * 检查用户是否为指定知识库的项目成员
     */
    default boolean isMember(Long kbId, Long userId) {
        return selectCount(new LambdaQueryWrapperX<ProjectMemberDO>()
                .eq(ProjectMemberDO::getKbId, kbId)
                .eq(ProjectMemberDO::getUserId, userId)) > 0;
    }

    /**
     * 获取知识库的所有项目成员
     */
    default List<ProjectMemberDO> selectByKbId(Long kbId) {
        return selectList(new LambdaQueryWrapperX<ProjectMemberDO>()
                .eq(ProjectMemberDO::getKbId, kbId));
    }

}
