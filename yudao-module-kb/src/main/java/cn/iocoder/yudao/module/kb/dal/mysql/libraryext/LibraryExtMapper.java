package cn.iocoder.yudao.module.kb.dal.mysql.libraryext;

import java.util.*;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.libraryext.LibraryExtDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 知识库自定义字段值 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface LibraryExtMapper extends BaseMapperX<LibraryExtDO> {

    /** 查询指定知识库的全部自定义字段值 */
    default List<LibraryExtDO> selectListByKbId(Long kbId) {
        return selectList(new LambdaQueryWrapperX<LibraryExtDO>()
                .eq(LibraryExtDO::getKbId, kbId));
    }

    /** 批量查询多个知识库的自定义字段值 */
    default List<LibraryExtDO> selectListByKbIds(Collection<Long> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<LibraryExtDO>()
                .in(LibraryExtDO::getKbId, kbIds));
    }

    /**
     * 物理删除指定知识库的全部自定义字段值。
     *
     * 不能用 {@link #delete}（MyBatis-Plus 逻辑删除只置 deleted=1，物理行仍存在），
     * 否则 kb_library_ext 的 uk_kb_field(kb_id, field_key) 唯一键会与旧数据冲突，导致重复插入报错。
     */
    @Delete("DELETE FROM kb_library_ext WHERE kb_id = #{kbId}")
    int physicalDeleteByKbId(@Param("kbId") Long kbId);

}
