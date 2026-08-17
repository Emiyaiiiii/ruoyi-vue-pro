package cn.iocoder.yudao.module.agent.dal.mysql.skillmeta;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.agent.dal.dataobject.skillmeta.AiSkillMetaDO;
import cn.iocoder.yudao.module.agent.controller.admin.skillmeta.vo.SkillMetaPageReqVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 技能商店 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface AiSkillMetaMapper extends BaseMapperX<AiSkillMetaDO> {

    /**
     * 分页查询（支持来源/可见性/状态过滤和关键字搜索）
     */
    default PageResult<AiSkillMetaDO> selectPage(SkillMetaPageReqVO reqVO) {
        LambdaQueryWrapperX<AiSkillMetaDO> wrapper = new LambdaQueryWrapperX<AiSkillMetaDO>()
                .eqIfPresent(AiSkillMetaDO::getSource, reqVO.getSource())
                .eqIfPresent(AiSkillMetaDO::getVisibility, reqVO.getVisibility())
                .eqIfPresent(AiSkillMetaDO::getStatus, reqVO.getStatus());

        if (reqVO.getSearch() != null && !reqVO.getSearch().isEmpty()) {
            wrapper.and(w -> w
                    .like(AiSkillMetaDO::getSkillName, reqVO.getSearch())
                    .or()
                    .like(AiSkillMetaDO::getDisplayName, reqVO.getSearch())
                    .or()
                    .like(AiSkillMetaDO::getDescription, reqVO.getSearch()));
        }

        wrapper.orderByDesc(AiSkillMetaDO::getCreateTime);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 按 skillName 查询（唯一性校验）
     */
    default AiSkillMetaDO selectBySkillName(String skillName) {
        return selectOne(new LambdaQueryWrapperX<AiSkillMetaDO>()
                .eq(AiSkillMetaDO::getSkillName, skillName));
    }

    /**
     * 查询启用的技能列表
     */
    default List<AiSkillMetaDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<AiSkillMetaDO>()
                .eq(AiSkillMetaDO::getStatus, 1)
                .orderByDesc(AiSkillMetaDO::getCreateTime));
    }

    /**
     * 查询当前用户可见的技能列表（公开 + 自己的个人技能）
     */
    default List<AiSkillMetaDO> selectVisibleList(Long userId) {
        return selectList(new LambdaQueryWrapperX<AiSkillMetaDO>()
                .eq(AiSkillMetaDO::getStatus, 1)
                .and(w -> w
                        .eq(AiSkillMetaDO::getVisibility, 1) // 公开
                        .or()
                        .eq(AiSkillMetaDO::getOwnerUserId, userId)) // 自己的
                .orderByDesc(AiSkillMetaDO::getCreateTime));
    }

}
