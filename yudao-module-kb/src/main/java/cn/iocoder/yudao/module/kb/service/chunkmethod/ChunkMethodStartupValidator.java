package cn.iocoder.yudao.module.kb.service.chunkmethod;

import cn.iocoder.yudao.module.kb.dal.dataobject.chunkmethod.ChunkMethodDO;
import cn.iocoder.yudao.module.kb.dal.mysql.chunkmethod.ChunkMethodMapper;
import cn.iocoder.yudao.module.kb.framework.config.VectorTaskConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 切片方法注册表一致性校验（启动时）
 *
 * <p>方案 A 约定：`kb_chunk_method.code` 必须与 python-vector 切片器注册表的规范名一一对应
 * （注册表是类型实现的唯一权威，该表是业务配置）。本 Bean 启动后拉取 python `GET /api/v1/chunkers`
 * 的可用类型，对比启用方法的 code；不一致仅记日志告警、不阻断启动，避免与 python 服务耦合。</p>
 */
@Component
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class ChunkMethodStartupValidator implements ApplicationRunner {

    @Resource
    private ChunkMethodMapper chunkMethodMapper;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private VectorTaskConfig vectorTaskConfig;

    @Override
    public void run(ApplicationArguments args) {
        try {
            doValidate();
        } catch (Exception ex) {
            log.error("[startup] 切片方法注册表校验失败（不影响服务启动）：{}", ex.getMessage());
        }
    }

    private void doValidate() {
        // kb_chunk_method 为租户表，启动时无租户上下文：忽略租户过滤查全部启用方法做一致性校验
        List<ChunkMethodDO> activeMethods = cn.iocoder.yudao.framework.tenant.core.util.TenantUtils.executeIgnore(
                () -> chunkMethodMapper.selectActiveList());
        if (activeMethods.isEmpty()) {
            return;
        }
        Set<String> pythonTypes = fetchPythonChunkerTypes();
        if (pythonTypes == null) {
            log.warn("[startup] 无法获取 python 切片器注册表（服务不可达），跳过 code 一致性校验");
            return;
        }
        List<String> missing = activeMethods.stream()
                .map(ChunkMethodDO::getCode)
                .filter(code -> code != null && !pythonTypes.contains(code))
                .collect(Collectors.toList());
        if (missing.isEmpty()) {
            log.info("[startup] 切片方法注册表一致性校验通过：{} 个启用方法全部在 python 注册表中",
                    activeMethods.size());
            return;
        }
        log.warn("[startup] kb_chunk_method 存在 python 未实现的 code={}（启用中），"
                        + "请先在 python-vector 注册对应切片器或停用该方法；python 可用类型={}",
                missing, pythonTypes);
    }

    /**
     * 拉取 python 注册表快照；失败/异常返回 null。
     */
    private Set<String> fetchPythonChunkerTypes() {
        try {
            String url = vectorTaskConfig.getPythonServiceUrl() + "/api/v1/chunkers";
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
            if (resp.getBody() == null || !Integer.valueOf(0).equals(resp.getBody().get("code"))) {
                return null;
            }
            Object types = resp.getBody().get("types");
            if (!(types instanceof List)) {
                return null;
            }
            Set<String> result = new HashSet<>();
            for (Object o : (List<?>) types) {
                if (o != null) {
                    result.add(o.toString());
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("[startup] 调用 python /api/v1/chunkers 失败：{}", ex.getMessage());
            return null;
        }
    }
}
