package cn.iocoder.yudao.module.agent.dal.mysql.mcpmeta;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.agent.dal.dataobject.mcpmeta.AiMcpMetaDO;
import cn.iocoder.yudao.module.agent.controller.admin.mcpmeta.vo.McpMetaPageReqVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MCP 商店 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface AiMcpMetaMapper extends BaseMapperX<AiMcpMetaDO> {

    /**
     * 分页查询（支持类型、状态过滤和关键字搜索）
     */
    default PageResult<AiMcpMetaDO> selectPage(McpMetaPageReqVO reqVO) {
        LambdaQueryWrapperX<AiMcpMetaDO> wrapper = new LambdaQueryWrapperX<AiMcpMetaDO>()
                .eqIfPresent(AiMcpMetaDO::getType, reqVO.getType())
                .eqIfPresent(AiMcpMetaDO::getTransport, reqVO.getTransport())
                .eqIfPresent(AiMcpMetaDO::getStatus, reqVO.getStatus());

        // 关键字搜索：name / code / description 任一匹配（OR 关系）
        if (reqVO.getSearch() != null && !reqVO.getSearch().isEmpty()) {
            wrapper.and(w -> w
                    .like(AiMcpMetaDO::getName, reqVO.getSearch())
                    .or()
                    .like(AiMcpMetaDO::getCode, reqVO.getSearch())
                    .or()
                    .like(AiMcpMetaDO::getDescription, reqVO.getSearch()));
        }

        wrapper.orderByAsc(AiMcpMetaDO::getSortOrder)
               .orderByDesc(AiMcpMetaDO::getCreateTime);

        return selectPage(reqVO, wrapper);
    }

    /**
     * 按编码查询（唯一性校验）
     */
    default AiMcpMetaDO selectByCode(String code) {
        return selectOne(new LambdaQueryWrapperX<AiMcpMetaDO>()
                .eq(AiMcpMetaDO::getCode, code));
    }

    /**
     * 查询启用的系统级 MCP 列表（商店展示）
     */
    default List<AiMcpMetaDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<AiMcpMetaDO>()
                .eq(AiMcpMetaDO::getStatus, 1)
                .orderByAsc(AiMcpMetaDO::getSortOrder));
    }

}
