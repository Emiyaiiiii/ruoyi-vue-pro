package cn.iocoder.yudao.module.agent.service.tokenusage;

import java.util.List;
import java.util.Map;

/**
 * Token 用量统计 Service
 *
 * <p>透传 QwenPaw 的全局 token-usage 统计接口（汇总 + 明细）。
 *
 * @author 吴皓
 */
public interface AiTokenUsageService {

    /**
     * 获得 Token 用量汇总
     *
     * @param startDate 起始日期 YYYY-MM-DD，可为 null
     * @param endDate   结束日期 YYYY-MM-DD，可为 null
     * @param model     按模型名过滤，可为 null
     * @param provider  按 provider 过滤，可为 null
     * @return total_prompt_tokens / total_completion_tokens / total_calls / by_model / by_date
     */
    Map<String, Object> getTokenUsage(String startDate, String endDate, String model, String provider);

    /**
     * 获得 Token 用量明细
     *
     * @return 按日期×provider×model 一行的记录数组
     */
    List<Map<String, Object>> getTokenUsageDetails(String startDate, String endDate, String model, String provider);

}
