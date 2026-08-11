package cn.iocoder.yudao.module.kb.controller.admin.chunkmethod.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 切片方法新增/修改 Request VO")
@Data
public class ChunkMethodSaveReqVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "方法名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "固定大小分块器")
    @NotEmpty(message = "方法名称不能为空")
    @Size(max = 100, message = "方法名称长度不能超过100个字符")
    private String name;

    @Schema(description = "方法类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "fixed_size")
    @NotEmpty(message = "方法类型不能为空")
    private String methodType;

    @Schema(description = "方法描述", example = "适用于通用文档，按指定字符数进行均匀分割")
    private String description;

    @Schema(description = "方法代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "fixed_size")
    @NotEmpty(message = "方法代码不能为空")
    @Size(max = 50, message = "方法代码长度不能超过50个字符")
    private String code;

    @Schema(description = "参数模板(JSON格式)", example = "{\"type\":\"object\",\"properties\":{\"chunk_size\":{\"type\":\"integer\"}}}")
    private String parametersTemplate;

    @Schema(description = "默认参数(JSON格式)", example = "{\"chunk_size\":1000,\"chunk_overlap\":200}")
    private String defaultParameters;

    @Schema(description = "处理器类全路径", example = "cn.iocoder.yudao.module.kb.service.chunk.FixedSizeChunker")
    private String handlerClass;

    @Schema(description = "是否启用", example = "1")
    private Integer isActive;

    @Schema(description = "是否默认切片方法", example = "0")
    private Integer isDefaultMethod;

    @Schema(description = "平均处理速度(千字/秒)", example = "5.0")
    private Double avgProcessingSpeed;

    @Schema(description = "内存占用(MB)", example = "50")
    private Integer memoryFootprint;

}
