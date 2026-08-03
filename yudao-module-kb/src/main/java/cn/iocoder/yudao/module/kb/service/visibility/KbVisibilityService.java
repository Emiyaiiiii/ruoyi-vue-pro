package cn.iocoder.yudao.module.kb.service.visibility;

import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import java.util.List;

public interface KbVisibilityService {

    /**
     * 过滤出当前用户可见的知识库列表
     * @param allLibs    全部知识库
     * @param userId     当前用户ID
     * @param userDeptId 当前用户部门ID
     */
    List<LibraryDO> filterVisible(List<LibraryDO> allLibs, Long userId, Long userDeptId);
}