package cn.iocoder.yudao.module.member.api.user;

import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.member.convert.user.MemberUserConvert;
import cn.iocoder.yudao.module.member.dal.dataobject.user.MemberUserDO;
import cn.iocoder.yudao.module.member.service.user.MemberUserService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.member.enums.ErrorCodeConstants.USER_MOBILE_NOT_EXISTS;

/**
 * 会员用户的 API 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MemberUserApiImpl implements MemberUserApi {

    @Resource
    private MemberUserService userService;

    @Resource
    private FileApi fileApi;

    @Override
    public MemberUserRespDTO getUser(Long id) {
        MemberUserDO user = userService.getUser(id);
        return resolveAvatar(MemberUserConvert.INSTANCE.convert2(user));
    }

    @Override
    public List<MemberUserRespDTO> getUserList(Collection<Long> ids) {
        return resolveAvatarList(MemberUserConvert.INSTANCE.convertList2(userService.getUserList(ids)));
    }

    @Override
    public List<MemberUserRespDTO> getUserListByNickname(String nickname) {
        return resolveAvatarList(MemberUserConvert.INSTANCE.convertList2(userService.getUserListByNickname(nickname)));
    }

    @Override
    public MemberUserRespDTO getUserByMobile(String mobile) {
        return resolveAvatar(MemberUserConvert.INSTANCE.convert2(userService.getUserByMobile(mobile)));
    }

    @Override
    public void validateUser(Long id) {
        MemberUserDO user = userService.getUser(id);
        if (user == null) {
            throw exception(USER_MOBILE_NOT_EXISTS);
        }
    }

    private MemberUserRespDTO resolveAvatar(MemberUserRespDTO user) {
        if (user != null) {
            user.setAvatar(fileApi.resolveUrl(user.getAvatar()));
        }
        return user;
    }

    private List<MemberUserRespDTO> resolveAvatarList(List<MemberUserRespDTO> users) {
        if (users != null) {
            users.forEach(this::resolveAvatar);
        }
        return users;
    }

}
