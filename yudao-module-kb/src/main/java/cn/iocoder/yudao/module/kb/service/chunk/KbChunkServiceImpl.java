package cn.iocoder.yudao.module.kb.service.chunk;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.kb.controller.admin.chunk.vo.ChunkItemVO;
import cn.iocoder.yudao.module.kb.controller.admin.chunk.vo.ChunkRevisionRespVO;
import cn.iocoder.yudao.module.kb.controller.admin.chunk.vo.ChunkRevertReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.chunk.vo.ChunkUpdateReqVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.chunk.KbChunkRevisionDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.module.kb.dal.mysql.chunk.KbChunkRevisionMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.document.DocumentMapper;
import cn.iocoder.yudao.module.kb.framework.config.VectorTaskConfig;
import cn.iocoder.yudao.module.kb.service.library.LibraryService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 切片编辑与修订 Service 实现（#7）
 *
 * <p>对齐 WeKnora chunk editing：revision 快照（变更前内容）+ 乐观锁冲突检测 +
 * 回滚 = 取历史快照应用为当前。存储/向量操作经 python-vector 执行（复用现有双写一致性）。</p>
 */
@Service
@Validated
@Slf4j
public class KbChunkServiceImpl implements KbChunkService {

    /** 编辑内容长度上限（与 python-vector 端保持一致） */
    private static final int MAX_CONTENT_LENGTH = 20000;

    /**
     * 解析 python-vector 返回（snake_case 键）为前端 VO（camelCase）。
     * 注意不能用共享 JsonUtils（默认 camelCase），否则 chunk_index→chunkIndex 等映射全部为 null。
     */
    private static final ObjectMapper SNAKE_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            // python-vector 会持续新增字段（如原文定位 page_start/bboxes）；
            // 忽略未知键，避免新增字段直接把切片列表接口打成 500
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Resource
    private KbChunkRevisionMapper kbChunkRevisionMapper;
    @Resource
    private DocumentMapper documentMapper;
    @Resource
    private LibraryService libraryService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private RestTemplate chunkRestTemplate;
    @Resource
    private VectorTaskConfig vectorTaskConfig;

    @Override
    public List<ChunkItemVO> listChunks(Long docId, Long kbId) {
        // 查看切片：文档内容访问校验（对齐 DocumentController 的项目成果库内容访问）
        DocumentDO document = validateDocView(docId, kbId);
        Map<String, Object> resp = pythonGet("/api/v1/documents/" + docId + "/chunks?kb_id=" + kbId);
        List<ChunkItemVO> chunks = new ArrayList<>();
        Object rawChunks = resp == null ? null : resp.get("chunks");
        if (rawChunks instanceof List) {
            for (Object o : (List<?>) rawChunks) {
                if (!(o instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                ChunkItemVO vo = SNAKE_MAPPER.convertValue(o, ChunkItemVO.class);
                if (vo != null) {
                    // 补当前版本号（乐观锁用）：无修订记录=0
                    vo.setRevision(currentRevision(vo.getChunkId()));
                    chunks.add(vo);
                }
            }
        }
        // 兜底：docId 归属由 python 侧校验，此处仅记录
        log.info("[listChunks] docId={} kbId={} chunks={}", docId, kbId, chunks.size());
        return chunks;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateChunk(ChunkUpdateReqVO reqVO) {
        String chunkId = chunkId(reqVO.getDocumentId(), reqVO.getChunkIndex());
        DocumentDO document = validateDoc(reqVO.getDocumentId(), reqVO.getKbId());
        // 1. 乐观锁：当前版本号须与前端一致
        int maxRev = currentRevision(chunkId);
        if (maxRev != reqVO.getExpectedRevision()) {
            throw exception(CHUNK_REVISION_CONFLICT);
        }
        if (StrUtil.isBlank(reqVO.getContent()) || reqVO.getContent().length() > MAX_CONTENT_LENGTH) {
            throw exception(CHUNK_CANNOT_EDIT);
        }
        // 2. 调 python 重嵌入 + 双写（失败抛异常，不落修订）。
        //    模型配置不再随请求下发：python 端从 kb:process:config:{kb_id}.model.embedding 读取
        Map<String, Object> body = new HashMap<>();
        body.put("content", reqVO.getContent());
        body.put("kb_id", reqVO.getKbId());
        pythonPut("/api/v1/chunks/" + chunkId, body);
        // 3. 落修订快照（变更后内容）
        try {
            insertRevision(chunkId, reqVO, document, reqVO.getContent(), "edit");
        } catch (DuplicateKeyException e) {
            log.warn("[updateChunk] 修订快照并发冲突 chunkId={}", chunkId);
            throw exception(CHUNK_REVISION_CONFLICT);
        }
        return maxRev + 1;
    }

    @Override
    public List<ChunkRevisionRespVO> listRevisions(Long docId, Integer chunkIndex) {
        validateDoc(docId, null);
        String chunkId = chunkId(docId, chunkIndex);
        List<KbChunkRevisionDO> list = kbChunkRevisionMapper.selectListByChunkId(chunkId);
        List<ChunkRevisionRespVO> result = new ArrayList<>();
        for (KbChunkRevisionDO rev : list) {
            ChunkRevisionRespVO vo = new ChunkRevisionRespVO();
            vo.setId(rev.getId());
            vo.setChunkId(rev.getChunkId());
            vo.setRevision(rev.getRevision());
            vo.setContent(rev.getContent());
            vo.setContentHash(rev.getContentHash());
            vo.setEditorId(rev.getEditorId());
            vo.setEditorName(rev.getEditorName());
            vo.setChangeType(rev.getChangeType());
            vo.setCreateTime(rev.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer revertChunk(ChunkRevertReqVO reqVO) {
        String chunkId = chunkId(reqVO.getDocumentId(), reqVO.getChunkIndex());
        validateDoc(reqVO.getDocumentId(), null);
        // 1. 乐观锁
        int maxRev = currentRevision(chunkId);
        if (maxRev != reqVO.getExpectedRevision()) {
            throw exception(CHUNK_REVISION_CONFLICT);
        }
        // 2. 读目标版本快照（其内容即回滚后的目标内容）
        KbChunkRevisionDO target = kbChunkRevisionMapper.selectByRevision(chunkId, reqVO.getTargetRevision());
        if (target == null) {
            throw exception(CHUNK_REVISION_CONFLICT);
        }
        // 3. 调 python 应用旧内容（模型配置由 python 端从 KB 缓存读取）
        Map<String, Object> body = new HashMap<>();
        body.put("content", target.getContent());
        body.put("kb_id", reqVO.getKbId() != null ? reqVO.getKbId() : target.getKbId());
        pythonPut("/api/v1/chunks/" + chunkId, body);
        // 4. 落修订快照（change_type=revert，内容=回滚到的目标内容）
        try {
            DocumentDO document = documentMapper.selectById(reqVO.getDocumentId());
            insertRevision(chunkId, reqVO.getDocumentId(), document == null ? null : document.getKbId(),
                    target.getContent(), "revert");
        } catch (DuplicateKeyException e) {
            log.warn("[revertChunk] 修订快照并发冲突 chunkId={}", chunkId);
            throw exception(CHUNK_REVISION_CONFLICT);
        }
        return maxRev + 1;
    }

    // ==================== 私有辅助 ====================

    /**
     * 查看校验：文档存在 + 项目成果库内容访问（对齐 DocumentController.validateProjectContentAccess）
     */
    private DocumentDO validateDocView(Long docId, Long kbId) {
        DocumentDO document = documentMapper.selectById(docId);
        if (document == null) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
        if (kbId != null && !kbId.equals(document.getKbId())) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
        // 项目成果库内容访问校验
        var library = libraryService.getLibrary(document.getKbId());
        if (library != null && Integer.valueOf(1).equals(library.getIsProject())
                && !libraryService.canOpenProjectLibrary(document.getKbId())) {
            throw exception(DOCUMENT_PERMISSION_DENIED);
        }
        return document;
    }

    /**
     * 编辑/回滚校验：文档存在 + 知识库管理权限
     */
    private DocumentDO validateDoc(Long docId, Long kbId) {
        DocumentDO document = documentMapper.selectById(docId);
        if (document == null) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
        if (kbId != null && !kbId.equals(document.getKbId())) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
        if (!libraryService.canManage(document.getKbId())) {
            throw exception(LIBRARY_PERMISSION_DENIED);
        }
        return document;
    }

    private String chunkId(Long docId, Integer chunkIndex) {
        return docId + "_" + chunkIndex;
    }

    /** 当前版本号：MAX(revision)，无修订记录=0 */
    private int currentRevision(String chunkId) {
        KbChunkRevisionDO max = kbChunkRevisionMapper.selectMaxRevision(chunkId);
        return max == null ? 0 : max.getRevision();
    }

    private void insertRevision(String chunkId, ChunkUpdateReqVO reqVO, DocumentDO document,
                                String snapshotContent, String changeType) {
        insertRevision(chunkId, reqVO.getDocumentId(), reqVO.getKbId(), snapshotContent, changeType);
    }

    private void insertRevision(String chunkId, Long docId, Long kbId, String snapshotContent, String changeType) {
        KbChunkRevisionDO revision = new KbChunkRevisionDO();
        revision.setChunkId(chunkId);
        revision.setDocumentId(docId);
        revision.setKbId(kbId);
        revision.setRevision(currentRevision(chunkId) + 1);
        revision.setContent(snapshotContent);
        revision.setContentHash(DigestUtil.sha256Hex(snapshotContent));
        revision.setChangeType(changeType);
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        if (user != null) {
            revision.setEditorId(user.getId());
            try {
                AdminUserRespDTO sysUser = adminUserApi.getUser(user.getId());
                revision.setEditorName(sysUser != null ? sysUser.getNickname() : "");
            } catch (Exception e) {
                revision.setEditorName("");
            }
        }
        kbChunkRevisionMapper.insert(revision);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> pythonGet(String url) {
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    vectorTaskConfig.getPythonServiceUrl() + url, HttpMethod.GET, null, Map.class);
            return resp.getBody();
        } catch (Exception e) {
            log.error("[pythonGet] 调用 python 失败 url={}, error={}", url, e.getMessage());
            throw exception(CHUNK_NOT_EXISTS);
        }
    }

    private void pythonPut(String url, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> resp = chunkRestTemplate.exchange(
                    vectorTaskConfig.getPythonServiceUrl() + url, HttpMethod.PUT, entity, Map.class);
            if (resp.getBody() == null || !Integer.valueOf(0).equals(resp.getBody().get("code"))) {
                String detail = resp.getBody() == null ? "empty" : String.valueOf(resp.getBody().get("detail"));
                log.error("[pythonPut] python 返回失败 url={}, resp={}", url, resp.getBody());
                if (StrUtil.contains(detail, "父块") || StrUtil.contains(detail, "图片")) {
                    throw exception(CHUNK_CANNOT_EDIT);
                }
                throw exception(CHUNK_NOT_EXISTS);
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("[pythonPut] python HTTP 错误 url={}, status={}, body={}", url, e.getStatusCode(),
                    e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 400) {
                throw exception(CHUNK_CANNOT_EDIT);
            }
            if (e.getStatusCode().value() == 404) {
                throw exception(CHUNK_NOT_EXISTS);
            }
            throw exception(CHUNK_NOT_EXISTS);
        } catch (RuntimeException e) {
            // 上面 throw exception 抛的 ServiceException 也会到这里，需原样抛出避免误吞
            if (e instanceof cn.iocoder.yudao.framework.common.exception.ServiceException) {
                throw e;
            }
            log.error("[pythonPut] 调用 python 失败 url={}, error={}", url, e.getMessage());
            throw exception(CHUNK_NOT_EXISTS);
        }
    }
}
