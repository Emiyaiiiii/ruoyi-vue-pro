package cn.iocoder.yudao.module.kb.controller.admin.ragconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.idev.excel.annotation.ExcelProperty;

@Schema(description = "管理后台 - RAG配置 Response VO")
@Data
public class RAGConfigRespVO {

    @Schema(description = "主键ID", example = "1")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "所属模块", example = "retrieval")
    @ExcelProperty("模块")
    private String module;

    @Schema(description = "所属模块显示名", example = "检索模块")
    @ExcelProperty("模块显示名")
    private String moduleDisplay;

    @Schema(description = "配置键名", example = "top_k")
    @ExcelProperty("键名")
    private String key;

    @Schema(description = "配置值(字符串)", example = "30")
    @ExcelProperty("配置值")
    private String value;

    @Schema(description = "值类型", example = "int")
    @ExcelProperty("值类型")
    private String valueType;

    @Schema(description = "值类型显示名", example = "整数")
    @ExcelProperty("值类型显示名")
    private String valueTypeDisplay;

    @Schema(description = "类型转换后的值", example = "30")
    private Object typedValue;

    @Schema(description = "配置说明", example = "最终返回给 LLM 的文档片段数量")
    @ExcelProperty("说明")
    private String description;

    @Schema(description = "是否启用", example = "1")
    @ExcelProperty("状态")
    private Integer isActive;

    @Schema(description = "排序", example = "1")
    @ExcelProperty("排序")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @ExcelProperty("更新时间")
    private LocalDateTime updateTime;

}
