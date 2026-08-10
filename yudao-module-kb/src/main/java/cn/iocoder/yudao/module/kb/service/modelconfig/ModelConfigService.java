package cn.iocoder.yudao.module.kb.service.modelconfig;

import javax.validation.Valid;
import cn.iocoder.yudao.module.kb.controller.admin.modelconfig.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.modelconfig.ModelConfigDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import java.util.List;

/**
 * 大模型配置 Service 接口
 *
 * @author 吴皓
 */
public interface ModelConfigService {

    /**
     * 创建大模型配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createModelConfig(@Valid ModelConfigSaveReqVO createReqVO);

    /**
     * 更新大模型配置
     *
     * @param updateReqVO 更新信息
     */
    void updateModelConfig(@Valid ModelConfigSaveReqVO updateReqVO);

    /**
     * 删除大模型配置
     *
     * @param id 编号
     */
    void deleteModelConfig(Long id);

    /**
     * 获得大模型配置
     *
     * @param id 编号
     * @return 大模型配置
     */
    ModelConfigDO getModelConfig(Long id);

    /**
     * 获得大模型配置分页
     *
     * @param pageReqVO 分页查询
     * @return 大模型配置分页
     */
    PageResult<ModelConfigDO> getModelConfigPage(ModelConfigPageReqVO pageReqVO);

    /**
     * 激活配置
     *
     * @param id 编号
     */
    void activateModelConfig(Long id);

    /**
     * 停用配置
     *
     * @param id 编号
     */
    void deactivateModelConfig(Long id);

    /**
     * 测试模型连接
     *
     * @param reqVO 测试请求
     * @return 测试结果
     */
    ModelConfigTestRespVO testModelConfig(ModelConfigTestReqVO reqVO);

    /**
     * 复制模型配置
     *
     * @param reqVO 复制请求
     * @return 新配置编号
     */
    Long copyModelConfig(ModelConfigCopyReqVO reqVO);

    /**
     * 设置默认配置（激活目标配置，停用其他所有配置）
     *
     * @param id 配置编号
     */
    void setDefaultModelConfig(Long id);

    /**
     * 批量操作（激活/停用/删除）
     *
     * @param reqVO 批量操作请求
     * @return 操作影响的数量
     */
    Integer batchOperation(ModelConfigBatchReqVO reqVO);

    /**
     * 获得模型配置统计信息
     *
     * @return 统计信息
     */
    ModelConfigStatisticsRespVO getStatistics();

    /**
     * 获得激活的模型配置精简列表（用于下拉选择等）
     *
     * @return 模型配置列表
     */
    List<ModelConfigSimpleVO> getSimpleModelConfigList();

}
