package cn.iocoder.yudao.module.kb.dal.mysql.news;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.news.NewsSourceDO;
import cn.iocoder.yudao.module.kb.controller.admin.news.vo.NewsSourcePageReqVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 新闻数据源 Mapper
 *
 * @author 吴皓
 */
@Mapper
public interface NewsSourceMapper extends BaseMapperX<NewsSourceDO> {

    /**
     * 分页查询（支持关键字搜索和启用状态过滤）
     */
    default PageResult<NewsSourceDO> selectPage(NewsSourcePageReqVO reqVO) {
        LambdaQueryWrapperX<NewsSourceDO> wrapper = new LambdaQueryWrapperX<NewsSourceDO>()
                .eqIfPresent(NewsSourceDO::getSyncEnabled, reqVO.getSyncEnabled());

        // 关键字搜索：name / dbHost / dbName 任一匹配
        if (reqVO.getSearch() != null && !reqVO.getSearch().isEmpty()) {
            wrapper.and(w -> w
                    .like(NewsSourceDO::getName, reqVO.getSearch())
                    .or()
                    .like(NewsSourceDO::getDbHost, reqVO.getSearch())
                    .or()
                    .like(NewsSourceDO::getDbName, reqVO.getSearch()));
        }

        wrapper.orderByDesc(NewsSourceDO::getCreateTime);
        return selectPage(reqVO, wrapper);
    }
}
