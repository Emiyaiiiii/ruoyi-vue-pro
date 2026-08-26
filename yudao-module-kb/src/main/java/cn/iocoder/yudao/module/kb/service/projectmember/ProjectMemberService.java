package cn.iocoder.yudao.module.kb.service.projectmember;

import cn.iocoder.yudao.module.kb.controller.admin.projectmember.vo.ProjectMemberRespVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.projectmember.ProjectMemberDO;

import java.util.List;

/**
 * 知识库项目成员 Service 接口
 *
 * @author 吴皓
 */
public interface ProjectMemberService {

    /**
     * 添加项目成员
     */
    Long addMember(Long kbId, Long userId);

    /**
     * 批量添加项目成员（已存在则跳过）
     */
    void addMembers(Long kbId, java.util.Collection<Long> userIds);

    /**
     * 移除项目成员
     */
    void removeMember(Long kbId, Long userId);

    /**
     * 检查用户是否为指定项目的成员
     */
    boolean isMember(Long kbId, Long userId);

    /**
     * 获取知识库的所有项目成员（合并系统用户昵称）
     */
    List<ProjectMemberRespVO> getByKbId(Long kbId);

    /**
     * 删除知识库的所有项目成员（知识库删除时调用）
     */
    void removeAllByKbId(Long kbId);

}
