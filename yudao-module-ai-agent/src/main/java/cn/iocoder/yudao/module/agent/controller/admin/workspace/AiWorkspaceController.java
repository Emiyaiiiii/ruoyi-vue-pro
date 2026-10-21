package cn.iocoder.yudao.module.agent.controller.admin.workspace;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.agent.service.workspace.AiWorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 工作目录（透传 QwenPaw）
 *
 * <p>QwenPaw 是工作区文件与记忆文件的 source of truth。Java 端不做任何本地持久化，
 * 所有读写操作都通过 {@link AiWorkspaceService} 转发到 QwenPaw 的 agent 级接口。
 *
 * <p>除下载/上传外，接口均以 {@code agentId}（芋道智能体主键）定位智能体。
 *
 * @author 吴皓
 */
@Tag(name = "管理后台 - 工作目录（透传 QwenPaw）")
@RestController
@RequestMapping("/ai-agent/workspace")
@Validated
public class AiWorkspaceController {

    @Resource
    private AiWorkspaceService workspaceService;

    // ==================== 工作区文件 ====================

    @GetMapping("/tree")
    @Operation(summary = "列出智能体工作区目录树")
    @Parameter(name = "agentId", description = "智能体 ID（芋道 Java 主键）", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public CommonResult<Map<String, Object>> listTree(@RequestParam("agentId") Long agentId,
                                                      @RequestParam(value = "path", required = false) String path,
                                                      @RequestParam(value = "cursor", required = false) String cursor,
                                                      @RequestParam(value = "root", required = false) String root,
                                                      @RequestParam(value = "limit", required = false) Integer limit) {
        return success(workspaceService.listWorkspaceTree(agentId, path, cursor, root, limit));
    }

    @GetMapping("/file-metadata")
    @Operation(summary = "读取工作区文件元数据")
    @Parameter(name = "agentId", description = "智能体 ID（芋道 Java 主键）", required = true)
    @Parameter(name = "path", description = "文件相对路径", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public CommonResult<Map<String, Object>> fileMetadata(@RequestParam("agentId") Long agentId,
                                                          @RequestParam("path") String path,
                                                          @RequestParam(value = "root", required = false) String root) {
        return success(workspaceService.readWorkspaceFileMetadata(agentId, path, root));
    }

    @GetMapping("/file-content")
    @Operation(summary = "读取工作区文件内容（支持分页）")
    @Parameter(name = "agentId", description = "智能体 ID（芋道 Java 主键）", required = true)
    @Parameter(name = "path", description = "文件相对路径", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public CommonResult<Map<String, Object>> readFileContent(@RequestParam("agentId") Long agentId,
                                                             @RequestParam("path") String path,
                                                             @RequestParam(value = "root", required = false) String root,
                                                             @RequestParam(value = "offset", required = false) Integer offset,
                                                             @RequestParam(value = "limit", required = false) Integer limit) {
        return success(workspaceService.readWorkspaceFileContent(agentId, path, root, offset, limit));
    }

    @PutMapping("/file-content")
    @Operation(summary = "写入工作区文件内容（覆盖）",
            description = "请求体 {\"path\": ..., \"content\": ..., \"root\": ...}")
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:update')")
    public CommonResult<Map<String, Object>> writeFileContent(@RequestParam("agentId") Long agentId,
                                                              @RequestBody Map<String, Object> body) {
        String path = body.get("path") != null ? String.valueOf(body.get("path")) : null;
        String content = body.get("content") != null ? String.valueOf(body.get("content")) : null;
        String root = body.get("root") != null ? String.valueOf(body.get("root")) : null;
        return success(workspaceService.writeWorkspaceFileContent(agentId, path, content, root));
    }

    @GetMapping("/download")
    @Operation(summary = "下载工作区文件（转发 QwenPaw 文件字节流）")
    @Parameter(name = "agentId", description = "智能体 ID（芋道 Java 主键）", required = true)
    @Parameter(name = "path", description = "文件相对路径", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public ResponseEntity<byte[]> download(@RequestParam("agentId") Long agentId,
                                           @RequestParam("path") String path) {
        byte[] data = workspaceService.downloadWorkspaceFile(agentId, path);
        String fileName = path;
        int idx = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (idx >= 0) {
            fileName = path.substring(idx + 1);
        }
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件到工作区",
            description = "multipart 字段 file；query 携带 agentId、path、root")
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:update')")
    public CommonResult<Map<String, Object>> upload(@RequestParam("agentId") Long agentId,
                                                    @RequestParam("file") MultipartFile file,
                                                    @RequestParam(value = "path", required = false) String path,
                                                    @RequestParam(value = "root", required = false) String root) throws IOException {
        return success(workspaceService.uploadWorkspaceFile(agentId, file.getBytes(), file.getOriginalFilename(), path, root));
    }

    // ==================== 记忆文件 ====================

    @GetMapping("/memory")
    @Operation(summary = "列出智能体的记忆 Markdown 文件")
    @Parameter(name = "agentId", description = "智能体 ID（芋道 Java 主键）", required = true)
    @Parameter(name = "section", description = "记忆分区：daily / digest", required = false)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public CommonResult<List<Map<String, Object>>> listMemory(@RequestParam("agentId") Long agentId,
                                                               @RequestParam(value = "section", required = false) String section) {
        return success(workspaceService.listMemoryFiles(agentId, section));
    }

    @GetMapping("/memory/{mdPath}")
    @Operation(summary = "读取指定记忆 Markdown 文件内容")
    @Parameter(name = "agentId", description = "智能体 ID（芋道 Java 主键）", required = true)
    @Parameter(name = "mdPath", description = "记忆文件相对路径", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:query')")
    public CommonResult<Map<String, Object>> readMemory(@RequestParam("agentId") Long agentId,
                                                        @PathVariable("mdPath") String mdPath,
                                                        @RequestParam(value = "section", required = false) String section) {
        return success(workspaceService.readMemoryFile(agentId, mdPath, section));
    }

    @PutMapping("/memory/{mdPath}")
    @Operation(summary = "写入指定记忆 Markdown 文件内容（覆盖）",
            description = "query 携带 agentId、section；请求体 {\"content\": ...}")
    @Parameter(name = "agentId", description = "智能体 ID（芋道 Java 主键）", required = true)
    @Parameter(name = "mdPath", description = "记忆文件相对路径", required = true)
    @PreAuthorize("@ss.hasPermission('ai-agent:chat-session:update')")
    public CommonResult<Map<String, Object>> writeMemory(@RequestParam("agentId") Long agentId,
                                                         @PathVariable("mdPath") String mdPath,
                                                         @RequestParam(value = "section", required = false) String section,
                                                         @RequestBody Map<String, Object> body) {
        String content = body.get("content") != null ? String.valueOf(body.get("content")) : null;
        return success(workspaceService.writeMemoryFile(agentId, mdPath, content, section));
    }

}
