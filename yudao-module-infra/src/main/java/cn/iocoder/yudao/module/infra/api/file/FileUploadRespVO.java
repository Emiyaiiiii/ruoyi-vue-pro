package cn.iocoder.yudao.module.infra.api.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "文件上传结果 Response VO")
@Data
public class FileUploadRespVO {

    @Schema(description = "文件访问 URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://test.yudao.iocoder.cn/xxx.png")
    private String url;

    @Schema(description = "文件存储路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "kb/20260803/xxx.png")
    private String path;

    @Schema(description = "文件配置编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long configId;

}