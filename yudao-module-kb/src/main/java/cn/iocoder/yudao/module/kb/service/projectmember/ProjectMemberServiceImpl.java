package cn.iocoder.yudao.module.kb.service.projectmember;

import cn.iocoder.yudao.module.kb.controller.admin.projectmember.vo.ProjectMemberRespVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.projectmember.ProjectMemberDO;
import cn.iocoder.yudao.module.kb.dal.mysql.projectmember.ProjectMemberMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库项目成员 Service 实现类
 * 合并系统用户昵称信息
 *
 * @author 吴皓
 */
@Service
@Validated
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberMapper projectMemberMapper;
    private final AdminUserApi adminUserApi;

    @Override
    public Long addMember(Long kbId, Long userId) {
        // 幂等：已存在则跳过
        if (projectMemberMapper.isMember(kbId, userId)) {
            return null;
        }
        ProjectMemberDO member = ProjectMemberDO.builder()
                .kbId(kbId)
                .userId(userId)
                .build();
        projectMemberMapper.insert(member);
        return member.getId();
    }

    @Override
    public void removeMember(Long kbId, Long userId) {
        projectMemberMapper.delete(new LambdaQueryWrapper<ProjectMemberDO>()
                .eq(ProjectMemberDO::getKbId, kbId)
                .eq(ProjectMemberDO::getUserId, userId));
    }

    @Override
    public boolean isMember(Long kbId, Long userId) {
        return projectMemberMapper.isMember(kbId, userId);
    }

    @Override
    public List<ProjectMemberRespVO> getByKbId(Long kbId) {
        // 1. 查询项目成员记录
        List<ProjectMemberDO> members = projectMemberMapper.selectByKbId(kbId);
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 批量查询系统用户信息，获取昵称
        Set<Long> userIds = members.stream()
                .map(ProjectMemberDO::getUserId)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);

        // 3. 合并：项目成员 + 系统用户昵称
        return members.stream().map(member -> {
            ProjectMemberRespVO vo = new ProjectMemberRespVO();
            vo.setId(member.getId());
            vo.setKbId(member.getKbId());
            vo.setUserId(member.getUserId());
            vo.setCreateTime(member.getCreateTime());

            AdminUserRespDTO user = userMap.get(member.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void removeAllByKbId(Long kbId) {
        projectMemberMapper.delete(new LambdaQueryWrapper<ProjectMemberDO>()
                .eq(ProjectMemberDO::getKbId, kbId));
    }

}
