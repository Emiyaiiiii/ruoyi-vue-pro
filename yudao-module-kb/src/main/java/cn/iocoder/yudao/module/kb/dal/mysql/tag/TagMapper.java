package cn.iocoder.yudao.module.kb.dal.mysql.tag;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.tag.TagDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface TagMapper extends BaseMapperX<TagDO> {

    /**
     * 按名称 + 归属查询标签（用于作用域内名称唯一校验）。
     *
     * <p>MySQL 中 {@code owner_id IS NULL} 与任意值都不相等（NULL 不等于 NULL），
     * 因此全局标签与个人标签需分别使用 {@code isNull} / {@code eq} 查询，等价于
     * Python 端 {@code (name, owner)} 唯一约束的序列化器校验逻辑。
     */
    default TagDO selectByNameAndOwner(String name, Long ownerId) {
        if (ownerId == null) {
            return selectOne(new LambdaQueryWrapperX<TagDO>()
                    .eq(TagDO::getName, name)
                    .isNull(TagDO::getOwnerId));
        }
        return selectOne(new LambdaQueryWrapperX<TagDO>()
                .eq(TagDO::getName, name)
                .eq(TagDO::getOwnerId, ownerId));
    }

}