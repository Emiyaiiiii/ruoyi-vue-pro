package cn.iocoder.yudao.module.kb.dal.mysql.folder;

import java.util.*;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.folder.FolderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档文件夹 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface FolderMapper extends BaseMapperX<FolderDO> {

    default List<FolderDO> selectByKbId(Long kbId) {
        return selectList(FolderDO::getKbId, kbId);
    }

    default List<FolderDO> selectByParentId(Long parentId) {
        return selectList(FolderDO::getParentId, parentId);
    }

    default FolderDO selectByParentIdAndName(Long kbId, Long parentId, String name) {
        return selectOne(FolderDO::getKbId, kbId, FolderDO::getParentId, parentId, FolderDO::getName, name);
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(FolderDO::getParentId, parentId);
    }

}