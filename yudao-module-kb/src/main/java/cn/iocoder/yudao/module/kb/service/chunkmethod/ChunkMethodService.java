package cn.iocoder.yudao.module.kb.service.chunkmethod;

import jakarta.validation.Valid;
import cn.iocoder.yudao.module.kb.controller.admin.chunkmethod.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.chunkmethod.ChunkMethodDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import java.util.List;

/**
 * 切片方法 Service 接口
 *
 * @author 吴皓
 */
public interface ChunkMethodService {

    /**
     * 创建切片方法
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createChunkMethod(@Valid ChunkMethodSaveReqVO createReqVO);

    /**
     * 更新切片方法
     *
     * @param updateReqVO 更新信息
     */
    void updateChunkMethod(@Valid ChunkMethodSaveReqVO updateReqVO);

    /**
     * 删除切片方法
     *
     * @param id 编号
     */
    void deleteChunkMethod(Long id);

    /**
     * 获得切片方法
     *
     * @param id 编号
     * @return 切片方法
     */
    ChunkMethodDO getChunkMethod(Long id);

    /**
     * 获得切片方法分页
     *
     * @param pageReqVO 分页查询
     * @return 切片方法分页
     */
    PageResult<ChunkMethodDO> getChunkMethodPage(ChunkMethodPageReqVO pageReqVO);

    /**
     * 测试切片方法
     *
     * @param reqVO 测试请求
     * @return 测试结果
     */
    ChunkMethodTestRespVO testChunkMethod(ChunkMethodTestReqVO reqVO);

    /**
     * 设置默认切片方法
     *
     * @param id 方法编号
     */
    void setDefaultChunkMethod(Long id);

    /**
     * 批量激活/停用切片方法
     *
     * @param ids 方法编号列表
     * @param isActive 是否激活
     * @return 操作影响的数量
     */
    Integer batchActivate(List<Long> ids, Boolean isActive);

    /**
     * 获得激活的切片方法精简列表（用于下拉选择等）
     *
     * @return 切片方法列表
     */
    List<ChunkMethodSimpleVO> getSimpleChunkMethodList();

}
