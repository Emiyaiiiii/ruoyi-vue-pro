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

    @Resource
    private cn.iocoder.yudao.module.kb.framework.config.VectorTaskConfig vectorTaskConfig;

    @Resource
    private org.springframework.web.client.RestTemplate restTemplate;

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

        // 优先调用 python 真实切片服务；失败（服务未启/联网错）时回退本地模拟
        List<ChunkMethodTestRespVO.ChunkPreview> chunks = callPythonChunk(method, testText);
        boolean usingPython = chunks != null;
        if (chunks == null) {
            chunks = simulateChunking(method, testText);
        }

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
        result.setEngine(usingPython ? "python" : "local");

        return result;
    }

    /**
     * 调用 python 真实切片服务 POST /api/v1/chunk；线程内异常时返回 null 表示走本地模拟。
     */
    private List<ChunkMethodTestRespVO.ChunkPreview> callPythonChunk(ChunkMethodDO method, String testText) {
        try {
            String url = vectorTaskConfig.getPythonServiceUrl() + "/api/v1/chunk";

            Map<String, Object> params = new HashMap<>();
            try {
                String def = method.getDefaultParameters();
                if (def != null && !def.isBlank()) {
                    Map<String, Object> parsed = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseMap(def);
                    if (parsed != null) {
                        params.putAll(parsed);
                    }
                }
            } catch (Exception ignore) {
                // defaultParameters 不可解析则忽略
            }

            Map<String, Object> body = new HashMap<>();
            body.put("text", testText);
            body.put("strategy", method.getMethodType() != null ? method.getMethodType() : "fixed_size");
            body.put("parameters", params);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(body, headers);

            org.springframework.http.ResponseEntity<Map> resp = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.POST, entity, Map.class);

            if (resp.getBody() == null || !Integer.valueOf(0).equals(resp.getBody().get("code"))) {
                log.warn("[callPythonChunk] python 切片返回异常: resp={}", resp.getBody());
                return null;
            }
            Object chunksObj = resp.getBody().get("chunks");
            if (!(chunksObj instanceof java.util.List)) {
                return null;
            }
            List<ChunkMethodTestRespVO.ChunkPreview> list = new ArrayList<>();
            for (Object o : (java.util.List<?>) chunksObj) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) o;
                Object txt = m.get("text");
                String s = txt != null ? txt.toString() : "";
                ChunkMethodTestRespVO.ChunkPreview p = new ChunkMethodTestRespVO.ChunkPreview();
                p.setText(s);
                p.setSize(s.length());
                list.add(p);
            }
            return list;
        } catch (Exception e) {
            log.warn("[callPythonChunk] 调用 python 切片服务失败，回退本地模拟: {}", e.getMessage());
            return null;
        }
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

    @Override
    public String getDefaultImageStrategy() {
        ChunkMethodDO method = chunkMethodMapper.selectDefaultMethod();
        if (method == null || method.getDefaultParameters() == null) {
            return "";
        }
        Map<String, Object> params;
        try {
            params = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseMap(method.getDefaultParameters());
        } catch (Exception e) {
            params = null;
        }
        if (params == null) {
            return "";
        }
        Object v = params.get("image_strategy");
        return v == null ? "" : String.valueOf(v);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultImageStrategy(String imageStrategy) {
        ChunkMethodDO method = chunkMethodMapper.selectDefaultMethod();
        if (method == null) {
            throw exception(CHUNK_METHOD_NO_DEFAULT);
        }
        Map<String, Object> params = new HashMap<>();
        if (method.getDefaultParameters() != null && !method.getDefaultParameters().isBlank()) {
            try {
                Map<String, Object> parsed = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseMap(method.getDefaultParameters());
                if (parsed != null) {
                    params.putAll(parsed);
                }
            } catch (Exception ignore) {
                // defaultParameters 不可解析则丢弃原内容
            }
        }
        params.put("image_strategy", (imageStrategy == null || imageStrategy.isBlank()) ? "" : imageStrategy);
        method.setDefaultParameters(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(params));
        chunkMethodMapper.updateById(method);
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
