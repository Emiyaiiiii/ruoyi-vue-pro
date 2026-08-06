package cn.iocoder.yudao.module.kb.service.document;

import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.FileUploadRespVO;
import cn.iocoder.yudao.module.kb.controller.admin.vectortask.vo.VectorTaskSubmitReqVO;
import cn.iocoder.yudao.module.kb.service.vectortask.VectorTaskService;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import cn.iocoder.yudao.module.kb.controller.admin.document.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.library.LibraryDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.kb.dal.mysql.document.DocumentMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 知识库文件 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    @Resource
    private DocumentMapper documentMapper;

    @Resource
    private LibraryMapper libraryMapper;

    @Resource
    private FileApi fileApi;

    @Resource
    private VectorTaskService vectorTaskService;

    @Override
    public Long createDocument(DocumentSaveReqVO createReqVO) {
        // 1. 校验知识库存在
        LibraryDO library = libraryMapper.selectById(createReqVO.getKbId());
        if (library == null) {
            throw exception(LIBRARY_NOT_EXISTS);
        }

        // 2. 写入文件记录
        DocumentDO document = BeanUtils.toBean(createReqVO, DocumentDO.class);
        document.setDownloadCount(0);
        document.setViewCount(0);
        documentMapper.insert(document);

        // 3. 更新知识库的文档数量
        libraryMapper.updateDocCount(createReqVO.getKbId(), 1);

        return document.getId();
    }

    @Override
    public void updateDocument(DocumentSaveReqVO updateReqVO) {
        // 校验存在
        validateDocumentExists(updateReqVO.getId());
        // 更新
        DocumentDO updateObj = BeanUtils.toBean(updateReqVO, DocumentDO.class);
        documentMapper.updateById(updateObj);
    }

    @Override
    public void deleteDocument(Long id) {
        // 校验存在
        DocumentDO document = validateDocumentExists(id);
        // 删除
        documentMapper.deleteById(id);
        // 更新知识库的文档数量
        libraryMapper.updateDocCount(document.getKbId(), -1);
    }

    @Override
        public void deleteDocumentListByIds(List<Long> ids) {
        for (Long id : ids) {
            DocumentDO document = documentMapper.selectById(id);
            if (document != null) {
                libraryMapper.updateDocCount(document.getKbId(), -1);
            }
        }
        // 删除
        documentMapper.deleteByIds(ids);
        }


    private DocumentDO validateDocumentExists(Long id) {
        DocumentDO document = documentMapper.selectById(id);
        if (document == null) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
        return document;
    }

    @Override
    public DocumentDO getDocument(Long id) {
        return documentMapper.selectById(id);
    }

    @Override
    public PageResult<DocumentDO> getDocumentPage(DocumentPageReqVO pageReqVO) {
        return documentMapper.selectPage(pageReqVO);
    }

    @Override
    public Long uploadAndCreate(MultipartFile file, Long kbId, String description, String tags) {
        return uploadAndCreate(file, kbId, null, description, tags);
    }

    @Override
    public Long uploadAndCreate(MultipartFile file, Long kbId, Long folderId, String description, String tags) {
        // 1. 校验知识库存在
        LibraryDO library = libraryMapper.selectById(kbId);
        if (library == null) {
            throw exception(LIBRARY_NOT_EXISTS);
        }

        // 2. 上传文件到芋道文件存储
        byte[] content;
        try {
            content = file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("读取文件内容失败", e);
        }
        String fileName = file.getOriginalFilename();
        // 使用 createFileDetail 获取完整文件信息（含 URL、路径、配置编号）
        FileUploadRespVO uploadResp = fileApi.createFileDetail(content, fileName, "kb", file.getContentType());

        // 3. 创建文档记录
        DocumentDO document = new DocumentDO();
        document.setKbId(kbId);
        document.setFolderId(folderId != null ? folderId : 0L);
        document.setFileName(fileName);
        document.setFileUrl(uploadResp.getUrl());
        document.setFilePath(uploadResp.getPath());
        document.setFileConfigId(uploadResp.getConfigId());
        document.setFileType(StrUtil.isNotBlank(fileName) ? FileUtil.extName(fileName) : null);
        document.setFileSize((long) content.length);
        document.setDescription(description);
        document.setTags(tags);
        document.setDownloadCount(0);
        document.setViewCount(0);
        document.setStatus(0);
        documentMapper.insert(document);

        // 4. 更新知识库的文档数量
        libraryMapper.updateDocCount(kbId, 1);

        // 5. 自动触发向量处理任务
        try {
            VectorTaskSubmitReqVO taskReqVO = new VectorTaskSubmitReqVO();
            taskReqVO.setDocId(document.getId());
            taskReqVO.setKbId(kbId);
            taskReqVO.setFileUrl(uploadResp.getUrl());
            taskReqVO.setFileType(document.getFileType());
            vectorTaskService.submitTask(taskReqVO);
            log.info("[uploadAndCreate] 已触发向量处理任务: docId={}, taskId={}", document.getId());
        } catch (Exception e) {
            log.error("[uploadAndCreate] 触发向量处理任务失败: docId={}", document.getId(), e);
            // 不抛出异常，避免影响文件上传主流程
        }

        return document.getId();
    }

}
