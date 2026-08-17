package cn.iocoder.yudao.module.kb.service.levelconfig;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.yudao.module.kb.controller.admin.levelconfig.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 知识库层级配置 Service 接口
 *
 * @author 吴皓
 */
public interface LevelConfigService {

    /**
     * 创建知识库层级配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createLevelConfig(@Valid LevelConfigSaveReqVO createReqVO);

    /**
     * 更新知识库层级配置
     *
     * @param updateReqVO 更新信息
     */
    void updateLevelConfig(@Valid LevelConfigSaveReqVO updateReqVO);

    /**
     * 删除知识库层级配置
     *
     * @param id 编号
     */
    void deleteLevelConfig(Long id);

    /**
    * 批量删除知识库层级配置
    *
    * @param ids 编号
    */
    void deleteLevelConfigListByIds(List<Long> ids);

    /**
     * 获得知识库层级配置
     *
     * @param id 编号
     * @return 知识库层级配置
     */
    LevelConfigDO getLevelConfig(Long id);

    /**
     * 获得知识库层级配置分页
     *
     * @param pageReqVO 分页查询
     * @return 知识库层级配置分页
     */
    PageResult<LevelConfigDO> getLevelConfigPage(LevelConfigPageReqVO pageReqVO);

    /**
     * 获得知识库层级配置精简列表（用于下拉选择）
     *
     * @return 层级配置精简列表
     */
    List<LevelConfigDO> getSimpleLevelConfigList();

}