package cn.iocoder.yudao.module.kb.dal.mysql.ragconfig;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.ragconfig.RAGSystemConfigDO;
import cn.iocoder.yudao.module.kb.controller.admin.ragconfig.vo.*;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG系统配置 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface RAGSystemConfigMapper extends BaseMapperX<RAGSystemConfigDO> {

    /**
     * 分页查询（支持模块、状态过滤和关键字搜索）
     */
    default PageResult<RAGSystemConfigDO> selectPage(RAGConfigPageReqVO reqVO) {
        LambdaQueryWrapperX<RAGSystemConfigDO> wrapper = new LambdaQueryWrapperX<RAGSystemConfigDO>()
                .eqIfPresent(RAGSystemConfigDO::getModule, reqVO.getModule())
                .eqIfPresent(RAGSystemConfigDO::getIsActive, reqVO.getIsActive());

        // 关键字搜索：key / description 任一匹配（OR 关系）
        if (reqVO.getSearch() != null && !reqVO.getSearch().isEmpty()) {
            wrapper.and(w -> w
                    .like(RAGSystemConfigDO::getKey, reqVO.getSearch())
                    .or()
                    .like(RAGSystemConfigDO::getDescription, reqVO.getSearch()));
        }

        wrapper.orderByAsc(RAGSystemConfigDO::getSortOrder)
               .orderByAsc(RAGSystemConfigDO::getModule)
               .orderByAsc(RAGSystemConfigDO::getKey);

        return selectPage(reqVO, wrapper);
    }

    /**
     * 按模块查询激活的配置列表
     */
    default List<RAGSystemConfigDO> selectActiveByModule(String module) {
        return selectList(new LambdaQueryWrapperX<RAGSystemConfigDO>()
                .eq(RAGSystemConfigDO::getModule, module)
                .eq(RAGSystemConfigDO::getIsActive, 1)
                .orderByAsc(RAGSystemConfigDO::getSortOrder)
                .orderByAsc(RAGSystemConfigDO::getKey));
    }

    /**
     * 按模块查询所有配置（不过滤激活状态）
     */
    default List<RAGSystemConfigDO> selectByModule(String module) {
        return selectList(new LambdaQueryWrapperX<RAGSystemConfigDO>()
                .eq(RAGSystemConfigDO::getModule, module)
                .orderByAsc(RAGSystemConfigDO::getSortOrder)
                .orderByAsc(RAGSystemConfigDO::getKey));
    }

    /**
     * 根据 module + key 查询唯一的配置
     */
    default RAGSystemConfigDO selectByModuleAndKey(String module, String key) {
        return selectOne(new LambdaQueryWrapperX<RAGSystemConfigDO>()
                .eq(RAGSystemConfigDO::getModule, module)
                .eq(RAGSystemConfigDO::getKey, key));
    }

    /**
     * 获取所有不重复的模块列表
     */
    default List<String> selectDistinctModules() {
        List<RAGSystemConfigDO> all = selectList();
        Set<String> modules = new LinkedHashSet<>();
        for (RAGSystemConfigDO config : all) {
            modules.add(config.getModule());
        }
        return new ArrayList<>(modules);
    }
}
