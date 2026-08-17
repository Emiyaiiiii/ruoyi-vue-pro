package cn.iocoder.yudao.module.kb.service.chunkmethod;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;
import java.util.*;

import cn.iocoder.yudao.module.kb.controller.admin.chunkmethod.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.chunkmethod.ChunkMethodDO;
import cn.iocoder.yudao.module.kb.dal.mysql.chunkmethod.ChunkMethodMapper;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 切片方法 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
@Slf4j
public class ChunkMethodServiceImpl implements ChunkMethodService {

    @Resource
    private ChunkMethodMapper chunkMethodMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createChunkMethod(ChunkMethodSaveReqVO createReqVO) {
        // 校验Code唯一性
        validateCodeUnique(createReqVO.getCode(), null);

        // 插入
        ChunkMethodDO chunkMethod = BeanUtils.toBean(createReqVO, ChunkMethodDO.class);
        // 设置默认值
        if (chunkMethod.getIsActive() == null) chunkMethod.setIsActive(1);
        if (chunkMethod.getIsDefaultMethod() == null) chunkMethod.setIsDefaultMethod(0);
        if (chunkMethod.getAvgProcessingSpeed() == null) chunkMethod.setAvgProcessingSpeed(1.0);
        if (chunkMethod.getMemoryFootprint() == null) chunkMethod.setMemoryFootprint(100);
        if (chunkMethod.getParametersTemplate() == null) chunkMethod.setParametersTemplate("{}");
        if (chunkMethod.getDefaultParameters() == null) chunkMethod.setDefaultParameters("{}");

        // 如果设为默认方法，取消其他默认
        if (chunkMethod.getIsDefaultMethod() == 1) {
            clearExistingDefault();
        }

        chunkMethodMapper.insert(chunkMethod);
        return chunkMethod.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChunkMethod(ChunkMethodSaveReqVO updateReqVO) {
        // 校验存在
        ChunkMethodDO existing = validateExists(updateReqVO.getId());

        // 校验Code唯一性
        validateCodeUnique(updateReqVO.getCode(), updateReqVO.getId());

        // 更新
        ChunkMethodDO updateObj = BeanUtils.toBean(updateReqVO, ChunkMethodDO.class);

        // 如果设为默认方法，取消其他默认
        if (updateObj.getIsDefaultMethod() != null && updateObj.getIsDefaultMethod() == 1
                && (existing.getIsDefaultMethod() == null || existing.getIsDefaultMethod() == 0)) {
            clearExistingDefault();
        }

        chunkMethodMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChunkMethod(Long id) {
        ChunkMethodDO method = validateExists(id);

        // 检查是否为默认方法
        if (method.getIsDefaultMethod() != null && method.getIsDefaultMethod() == 1) {
            throw exception(CHUNK_METHOD_DEFAULT_DELETE);
        }

        chunkMethodMapper.deleteById(id);
    }

    @Override
    public ChunkMethodDO getChunkMethod(Long id) {
        return validateExists(id);
    }

    @Override
    public PageResult<ChunkMethodDO> getChunkMethodPage(ChunkMethodPageReqVO pageReqVO) {
        return chunkMethodMapper.selectPage(pageReqVO);
    }

    @Override
    public ChunkMethodTestRespVO testChunkMethod(ChunkMethodTestReqVO reqVO) {
        ChunkMethodDO method = validateExists(reqVO.getId());
        String testText = reqVO.getTestText();

        long startTime = System.nanoTime();

        // 模拟切片处理：按指定规则切分文本
        List<ChunkMethodTestRespVO.ChunkPreview> chunks = simulateChunking(method, testText);

        long elapsed = System.nanoTime() - startTime;
        double processingTimeSeconds = elapsed / 1_000_000_000.0;
        int textLength = testText.length();
        double speed = textLength / Math.max(processingTimeSeconds, 0.001);
        double avgSize = chunks.isEmpty() ? 0 : chunks.stream().mapToInt(ChunkMethodTestRespVO.ChunkPreview::getSize).average().orElse(0);

        ChunkMethodTestRespVO result = new ChunkMethodTestRespVO();
        result.setMethodId(method.getId());
        result.setMethodName(method.getName());
        result.setTestTextLength(textLength);
        result.setChunkCount(chunks.size());
        result.setProcessingTimeSeconds(Math.round(processingTimeSeconds * 1000.0) / 1000.0);
        result.setProcessingSpeedCharsPerSecond(Math.round(speed * 100.0) / 100.0);
        result.setAvgChunkSize(Math.round(avgSize * 100.0) / 100.0);
        result.setChunksPreview(chunks.subList(0, Math.min(chunks.size(), 3)));

        return result;
    }

    /**
     * 模拟切片处理
     */
    private List<ChunkMethodTestRespVO.ChunkPreview> simulateChunking(ChunkMethodDO method, String text) {
        List<ChunkMethodTestRespVO.ChunkPreview> chunks = new ArrayList<>();

        // 尝试从默认参数中读取 chunk_size
        int chunkSize = 1000;
        int chunkOverlap = 200;
        try {
            if (method.getDefaultParameters() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> params = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(method.getDefaultParameters(), Map.class);
                if (params.containsKey("chunk_size")) {
                    chunkSize = ((Number) params.get("chunk_size")).intValue();
                }
                if (params.containsKey("chunk_overlap")) {
                    chunkOverlap = ((Number) params.get("chunk_overlap")).intValue();
                }
            }
        } catch (Exception e) {
            log.warn("解析默认参数失败，使用默认值: chunkSize=1000", e);
        }

        int step = Math.max(chunkSize - chunkOverlap, 1);
        int pos = 0;
        while (pos < text.length()) {
            int end = Math.min(pos + chunkSize, text.length());
            String chunkText = text.substring(pos, end);
            ChunkMethodTestRespVO.ChunkPreview preview = new ChunkMethodTestRespVO.ChunkPreview();
            preview.setText(chunkText);
            preview.setSize(chunkText.length());
            chunks.add(preview);
            pos += step;
        }

        return chunks;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultChunkMethod(Long id) {
        ChunkMethodDO method = validateExists(id);

        if (method.getIsActive() == null || method.getIsActive() == 0) {
            throw exception(CHUNK_METHOD_NOT_EXISTS);
        }

        // 取消现有默认方法
        clearExistingDefault();

        // 设置新的默认方法
        method.setIsDefaultMethod(1);
        chunkMethodMapper.updateById(method);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchActivate(List<Long> ids, Boolean isActive) {
        int count = 0;
        for (Long id : ids) {
            ChunkMethodDO method = chunkMethodMapper.selectById(id);
            if (method == null) continue;

            int targetStatus = isActive ? 1 : 0;
            if (method.getIsActive() != null && method.getIsActive().equals(targetStatus)) continue;

            // 如果停用且当前是默认方法，不允许停用
            if (!isActive && method.getIsDefaultMethod() != null && method.getIsDefaultMethod() == 1) {
                log.warn("不能停用默认切片方法: id={}", id);
                continue;
            }

            method.setIsActive(targetStatus);
            chunkMethodMapper.updateById(method);
            count++;
        }
        return count;
    }

    @Override
    public List<ChunkMethodSimpleVO> getSimpleChunkMethodList() {
        List<ChunkMethodDO> list = chunkMethodMapper.selectActiveList();
        return BeanUtils.toBean(list, ChunkMethodSimpleVO.class);
    }

    // ==================== 私有辅助方法 ====================

    private ChunkMethodDO validateExists(Long id) {
        ChunkMethodDO method = chunkMethodMapper.selectById(id);
        if (method == null) {
            throw exception(CHUNK_METHOD_NOT_EXISTS);
        }
        return method;
    }

    private void validateCodeUnique(String code, Long excludeId) {
        ChunkMethodDO existing = chunkMethodMapper.selectByCode(code);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw exception(CHUNK_METHOD_CODE_EXISTS);
        }
    }

    /**
     * 清除现有的默认方法
     */
    private void clearExistingDefault() {
        ChunkMethodDO existingDefault = chunkMethodMapper.selectDefaultMethod();
        if (existingDefault != null) {
            existingDefault.setIsDefaultMethod(0);
            chunkMethodMapper.updateById(existingDefault);
        }
    }

}
