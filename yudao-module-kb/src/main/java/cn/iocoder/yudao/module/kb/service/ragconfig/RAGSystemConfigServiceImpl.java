package cn.iocoder.yudao.module.kb.service.ragconfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;

import cn.iocoder.yudao.module.kb.controller.admin.ragconfig.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.ragconfig.RAGSystemConfigDO;
import cn.iocoder.yudao.module.kb.dal.mysql.ragconfig.RAGSystemConfigMapper;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * RAG系统配置 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
@Slf4j
public class RAGSystemConfigServiceImpl implements RAGSystemConfigService {

    @Resource
    private RAGSystemConfigMapper ragSystemConfigMapper;

    // ==================== 值类型常量 ====================
    private static final Set<String> VALID_VALUE_TYPES = new HashSet<>(Arrays.asList(
            "int", "float", "bool", "str", "json"
    ));

    private static final Set<String> VALID_MODULES = new HashSet<>(Arrays.asList(
            "retrieval", "rerank", "chunking", "llm", "cache", "batch", "conversation"
    ));

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRAGConfig(RAGConfigSaveReqVO createReqVO) {
        // 校验必填字段（创建时必须有值）
        if (createReqVO.getModule() == null || createReqVO.getModule().isEmpty()) {
            throw exception(RAG_CONFIG_MODULE_REQUIRED);
        }
        if (createReqVO.getKey() == null || createReqVO.getKey().isEmpty()) {
            throw exception(RAG_CONFIG_KEY_REQUIRED);
        }
        if (createReqVO.getValue() == null || createReqVO.getValue().isEmpty()) {
            throw exception(RAG_CONFIG_VALUE_REQUIRED);
        }
        if (createReqVO.getValueType() == null || createReqVO.getValueType().isEmpty()) {
            throw exception(RAG_CONFIG_VALUE_TYPE_REQUIRED);
        }
        // 校验值类型
        validateValueType(createReqVO.getValueType());
        // 校验值格式
        validateValueByType(createReqVO.getValue(), createReqVO.getValueType());
        // 校验 module + key 唯一性
        validateModuleKeyUnique(createReqVO.getModule(), createReqVO.getKey(), null);

        // 插入
        RAGSystemConfigDO config = BeanUtils.toBean(createReqVO, RAGSystemConfigDO.class);
        // 设置默认值
        if (config.getIsActive() == null) config.setIsActive(1);
        if (config.getSortOrder() == null) config.setSortOrder(0);

        ragSystemConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRAGConfig(RAGConfigSaveReqVO updateReqVO) {
        // 校验存在
        RAGSystemConfigDO existing = validateExists(updateReqVO.getId());

        // 校验值类型
        if (updateReqVO.getValueType() != null) {
            validateValueType(updateReqVO.getValueType());
        }
        // 校验值格式
        if (updateReqVO.getValue() != null) {
            String valueType = updateReqVO.getValueType() != null
                    ? updateReqVO.getValueType() : existing.getValueType();
            validateValueByType(updateReqVO.getValue(), valueType);
        }
        // 校验 module + key 唯一性（module/key 变更时）
        if (updateReqVO.getModule() != null || updateReqVO.getKey() != null) {
            String newModule = updateReqVO.getModule() != null ? updateReqVO.getModule() : existing.getModule();
            String newKey = updateReqVO.getKey() != null ? updateReqVO.getKey() : existing.getKey();
            validateModuleKeyUnique(newModule, newKey, updateReqVO.getId());
        }

        // 更新
        RAGSystemConfigDO updateObj = BeanUtils.toBean(updateReqVO, RAGSystemConfigDO.class);
        ragSystemConfigMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRAGConfig(Long id) {
        validateExists(id);
        ragSystemConfigMapper.deleteById(id);
    }

    @Override
    public RAGSystemConfigDO getRAGConfig(Long id) {
        RAGSystemConfigDO config = validateExists(id);
        config.computeTypedValue();
        return config;
    }

    @Override
    public PageResult<RAGSystemConfigDO> getRAGConfigPage(RAGConfigPageReqVO pageReqVO) {
        PageResult<RAGSystemConfigDO> pageResult = ragSystemConfigMapper.selectPage(pageReqVO);
        // 为每条记录计算 typedValue
        for (RAGSystemConfigDO config : pageResult.getList()) {
            config.computeTypedValue();
        }
        return pageResult;
    }

    @Override
    public Map<String, Object> getConfigByModule(String module) {
        List<RAGSystemConfigDO> configs = ragSystemConfigMapper.selectActiveByModule(module);
        Map<String, Object> result = new LinkedHashMap<>();
        for (RAGSystemConfigDO config : configs) {
            config.computeTypedValue();
            result.put(config.getKey(), config.getTypedValue());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchUpdate(List<Map<String, Object>> configs) {
        List<Map<String, Object>> updated = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();

        for (Map<String, Object> item : configs) {
            try {
                RAGSystemConfigDO config = null;

                // 通过 id 或 (module, key) 定位配置
                if (item.containsKey("id") && item.get("id") != null) {
                    Long id = Long.valueOf(item.get("id").toString());
                    config = ragSystemConfigMapper.selectById(id);
                } else if (item.containsKey("module") && item.containsKey("key")) {
                    String mod = (String) item.get("module");
                    String k = (String) item.get("key");
                    config = ragSystemConfigMapper.selectByModuleAndKey(mod, k);
                } else {
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put("item", item);
                    err.put("error", "缺少 id 或 (module, key)");
                    errors.add(err);
                    continue;
                }

                if (config == null) {
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put("item", item);
                    err.put("error", "配置不存在");
                    errors.add(err);
                    continue;
                }

                // 更新字段
                if (item.containsKey("value")) {
                    String newValue = item.get("value") != null ? item.get("value").toString() : null;
                    // 类型校验
                    if (newValue != null) {
                        validateValueByType(newValue, config.getValueType());
                    }
                    config.setValue(newValue);
                }
                if (item.containsKey("isActive")) {
                    config.setIsActive(Integer.valueOf(item.get("isActive").toString()));
                }
                if (item.containsKey("description")) {
                    config.setDescription((String) item.get("description"));
                }
                if (item.containsKey("valueType")) {
                    validateValueType((String) item.get("valueType"));
                    config.setValueType((String) item.get("valueType"));
                }
                if (item.containsKey("sortOrder")) {
                    config.setSortOrder(Integer.valueOf(item.get("sortOrder").toString()));
                }

                ragSystemConfigMapper.updateById(config);

                Map<String, Object> upd = new LinkedHashMap<>();
                upd.put("id", config.getId());
                upd.put("module", config.getModule());
                upd.put("key", config.getKey());
                upd.put("value", config.getValue());
                upd.put("isActive", config.getIsActive());
                updated.add(upd);

            } catch (Exception e) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("item", item);
                err.put("error", e.getMessage());
                errors.add(err);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", updated);
        result.put("errors", errors);
        result.put("totalUpdated", updated.size());
        result.put("totalErrors", errors.size());
        return result;
    }

    @Override
    public void refreshCache(String module, String key) {
        // 桩方法：当接入 Redis 后在此实现缓存刷新逻辑
        // 当前直接记录日志，配置读取时从数据库实时查询
        if (module != null && key != null) {
            log.info("刷新RAG配置缓存: module={}, key={}", module, key);
        } else if (module != null) {
            log.info("刷新RAG配置缓存: module={} (全部key)", module);
        } else {
            log.info("刷新RAG配置缓存: 全部模块");
        }
    }

    @Override
    public List<Map<String, Object>> getModules() {
        List<String> modules = ragSystemConfigMapper.selectDistinctModules();
        List<Map<String, Object>> result = new ArrayList<>();
        for (String m : modules) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", m);
            item.put("label", RAGSystemConfigDO.MODULE_DISPLAY_MAP.getOrDefault(m, m));
            // 统计该模块下的配置数
            List<RAGSystemConfigDO> configs = ragSystemConfigMapper.selectByModule(m);
            item.put("count", configs.size());
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> getStatistics() {
        List<RAGSystemConfigDO> all = ragSystemConfigMapper.selectList();
        int total = all.size();
        int active = 0;
        Map<String, Integer> byModule = new LinkedHashMap<>();

        for (RAGSystemConfigDO config : all) {
            if (config.getIsActive() != null && config.getIsActive() == 1) {
                active++;
            }
            String mod = config.getModule();
            byModule.merge(mod, 1, Integer::sum);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalConfigs", total);
        result.put("activeConfigs", active);
        result.put("byModule", byModule);
        return result;
    }

    // ==================== 私有辅助方法 ====================

    private RAGSystemConfigDO validateExists(Long id) {
        RAGSystemConfigDO config = ragSystemConfigMapper.selectById(id);
        if (config == null) {
            throw exception(RAG_CONFIG_NOT_EXISTS);
        }
        return config;
    }

    private void validateModuleKeyUnique(String module, String key, Long excludeId) {
        RAGSystemConfigDO existing = ragSystemConfigMapper.selectByModuleAndKey(module, key);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw exception(RAG_CONFIG_KEY_EXISTS);
        }
    }

    private void validateValueType(String valueType) {
        if (!VALID_VALUE_TYPES.contains(valueType)) {
            throw exception(RAG_CONFIG_VALUE_TYPE_ERROR);
        }
    }

    /**
     * 根据 valueType 校验 value 格式
     */
    private void validateValueByType(String value, String valueType) {
        if (value == null || valueType == null) return;
        try {
            switch (valueType) {
                case "int":
                    Integer.parseInt(value);
                    break;
                case "float":
                    Double.parseDouble(value);
                    break;
                case "bool":
                    String lower = value.toLowerCase();
                    if (!lower.equals("true") && !lower.equals("false")
                            && !lower.equals("1") && !lower.equals("0")
                            && !lower.equals("yes") && !lower.equals("no")) {
                        throw new IllegalArgumentException("无效的布尔值: " + value);
                    }
                    break;
                case "json":
                    new com.fasterxml.jackson.databind.ObjectMapper().readValue(value, Object.class);
                    break;
                case "str":
                    // 字符串总是有效
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            throw exception(RAG_CONFIG_VALUE_TYPE_ERROR);
        }
    }
}
