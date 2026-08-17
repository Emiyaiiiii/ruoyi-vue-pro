package cn.iocoder.yudao.module.agent.service.tokenusage;

import cn.iocoder.yudao.module.agent.framework.config.QwenPawClient;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * Token 用量统计 Service 实现
 *
 * @author 吴皓
 */
@Service
public class AiTokenUsageServiceImpl implements AiTokenUsageService {

    @Resource
    private QwenPawClient qwenPawClient;

    @Override
    public Map<String, Object> getTokenUsage(String startDate, String endDate, String model, String provider) {
        return qwenPawClient.getTokenUsage(startDate, endDate, model, provider);
    }

    @Override
    public List<Map<String, Object>> getTokenUsageDetails(String startDate, String endDate, String model, String provider) {
        return qwenPawClient.getTokenUsageDetails(startDate, endDate, model, provider);
    }

}
