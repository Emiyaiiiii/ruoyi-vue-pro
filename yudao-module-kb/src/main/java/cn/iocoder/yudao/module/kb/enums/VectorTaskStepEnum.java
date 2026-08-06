package cn.iocoder.yudao.module.kb.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 向量处理步骤枚举
 */
@Getter
@AllArgsConstructor
public enum VectorTaskStepEnum {

    OCR("ocr", "文档解析/OCR"),
    CHUNKING("chunking", "文本分块"),
    EMBEDDING("embedding", "向量生成"),
    STORING("storing", "向量存储");

    private final String step;
    private final String name;

    public static VectorTaskStepEnum valueOfStep(String step) {
        for (VectorTaskStepEnum e : values()) {
            if (e.getStep().equals(step)) {
                return e;
            }
        }
        return null;
    }
}
