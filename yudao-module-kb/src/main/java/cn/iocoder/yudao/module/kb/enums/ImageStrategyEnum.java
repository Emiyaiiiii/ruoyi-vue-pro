package cn.iocoder.yudao.module.kb.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 图片处理方案（平级策略，非升级层级）。
 *
 * <p>知识库可显式指定以下任一方案：任务下发的 chunking_config.image_strategy
 * 即按下发方案执行，不再依赖当前激活模型的布尔标志隐式推断。
 *
 * <pre>
 *   NONE       纯文本：图片不单独处理，无上下文孤立图并入最近文本块
 *   OCR        OCR 文字：提取图中文字作文本入库，普通 LLM 可答
 *   VL_SUMMARY VL 总结：VL-LLM 看图片总结语义入库，普通 LLM 可答
 *   VISION     视觉召回：embedding-vl 编码图片像素，语义召回图片，问答必须 VL-LLM
 * </pre>
 */
@Getter
@AllArgsConstructor
public enum ImageStrategyEnum {

    NONE("none", "纯文本"),
    OCR("ocr", "OCR文字"),
    VL_SUMMARY("vl_summary", "VL总结"),
    VISION("vision", "视觉召回");

    private final String code;
    private final String name;

    public static ImageStrategyEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ImageStrategyEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}