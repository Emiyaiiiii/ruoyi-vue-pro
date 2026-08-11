package cn.iocoder.yudao.module.kb.controller.admin.chunkmethod.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;

@Schema(description = "管理后台 - 切片方法 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ChunkMethodRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "方法名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "固定大小分块器")
    @ExcelProperty("方法名称")
    private String name;

    @Schema(description = "方法类型", example = "fixed_size")
    @ExcelProperty("方法类型")
    private String methodType;

    @Schema(description = "方法描述", example = "适用于通用文档，按指定字符数进行均匀分割")
    @ExcelProperty("方法描述")
    private String description;

    @Schema(description = "方法代码", example = "fixed_size")
    @ExcelProperty("方法代码")
    private String code;

    @Schema(description = "参数模板(JSON格式)")
    @ExcelProperty("参数模板")
    private String parametersTemplate;

    @Schema(description = "默认参数(JSON格式)")
    @ExcelProperty("默认参数")
    private String defaultParameters;

    @Schema(description = "处理器类全路径")
    @ExcelProperty("处理器类")
    private String handlerClass;

    @Schema(description = "是否启用", example = "1")
    @ExcelProperty("是否启用")
    private Integer isActive;

    @Schema(description = "是否默认切片方法", example = "0")
    @ExcelProperty("是否默认")
    private Integer isDefaultMethod;

    @Schema(description = "平均处理速度(千字/秒)", example = "5.0")
    @ExcelProperty("处理速度")
    private Double avgProcessingSpeed;

    @Schema(description = "内存占用(MB)", example = "50")
    @ExcelProperty("内存占用")
    private Integer memoryFootprint;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @ExcelProperty("更新时间")
    private LocalDateTime updateTime;

}
