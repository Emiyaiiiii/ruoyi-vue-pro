package cn.iocoder.yudao.module.kb.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 向量处理任务状态枚举
 */
@Getter
@AllArgsConstructor
public enum VectorTaskStatusEnum {

    PENDING(0, "待提交"),
    PROCESSING(1, "处理中"),
    COMPLETED(2, "已完成"),
    FAILED(3, "失败"),
    SUBMIT_FAILED(4, "提交失败"),
    TIMEOUT(5, "超时"),
    CANCELLED(6, "已取消");

    /**
     * 判断是否为终态（不可再被覆盖的状态）
     */
    public static boolean isTerminal(Integer status) {
        return COMPLETED.getStatus().equals(status)
                || FAILED.getStatus().equals(status)
                || SUBMIT_FAILED.getStatus().equals(status)
                || TIMEOUT.getStatus().equals(status)
                || CANCELLED.getStatus().equals(status);
    }

    private final Integer status;
    private final String name;

    public static VectorTaskStatusEnum valueOf(Integer status) {
        for (VectorTaskStatusEnum e : values()) {
            if (e.getStatus().equals(status)) {
                return e;
            }
        }
        return null;
    }
}
