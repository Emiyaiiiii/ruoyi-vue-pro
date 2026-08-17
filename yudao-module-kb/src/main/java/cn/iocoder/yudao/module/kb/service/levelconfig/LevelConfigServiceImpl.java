package cn.iocoder.yudao.module.kb.service.levelconfig;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import cn.iocoder.yudao.module.kb.controller.admin.levelconfig.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.kb.dal.mysql.levelconfig.LevelConfigMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 知识库层级配置 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
public class LevelConfigServiceImpl implements LevelConfigService {

    @Resource
    private LevelConfigMapper levelConfigMapper;

    @Override
    public Long createLevelConfig(LevelConfigSaveReqVO createReqVO) {
        // 插入
        LevelConfigDO levelConfig = BeanUtils.toBean(createReqVO, LevelConfigDO.class);
        levelConfigMapper.insert(levelConfig);

        // 返回
        return levelConfig.getId();
    }

    @Override
    public void updateLevelConfig(LevelConfigSaveReqVO updateReqVO) {
        // 校验存在
        validateLevelConfigExists(updateReqVO.getId());
        // 更新
        LevelConfigDO updateObj = BeanUtils.toBean(updateReqVO, LevelConfigDO.class);
        levelConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteLevelConfig(Long id) {
        // 校验存在
        validateLevelConfigExists(id);
        // 删除
        levelConfigMapper.deleteById(id);
    }

    @Override
        public void deleteLevelConfigListByIds(List<Long> ids) {
        // 删除
        levelConfigMapper.deleteByIds(ids);
        }


    private void validateLevelConfigExists(Long id) {
        if (levelConfigMapper.selectById(id) == null) {
            throw exception(LEVEL_CONFIG_NOT_EXISTS);
        }
    }

    @Override
    public LevelConfigDO getLevelConfig(Long id) {
        return levelConfigMapper.selectById(id);
    }

    @Override
    public PageResult<LevelConfigDO> getLevelConfigPage(LevelConfigPageReqVO pageReqVO) {
        return levelConfigMapper.selectPage(pageReqVO);
    }

    @Override
    public List<LevelConfigDO> getSimpleLevelConfigList() {
        return levelConfigMapper.selectList();
    }

}