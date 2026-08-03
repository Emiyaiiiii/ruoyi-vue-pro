package cn.iocoder.yudao.module.kb.service.document;

import java.util.*;
import javax.validation.*;
import cn.iocoder.yudao.module.kb.controller.admin.document.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文件 Service 接口
 *
 * @author 芋道源码
 */
public interface DocumentService {

    /**
     * 创建知识库文件
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDocument(@Valid DocumentSaveReqVO createReqVO);

    /**
     * 更新知识库文件
     *
     * @param updateReqVO 更新信息
     */
    void updateDocument(@Valid DocumentSaveReqVO updateReqVO);

    /**
     * 删除知识库文件
     *
     * @param id 编号
     */
    void deleteDocument(Long id);

    /**
    * 批量删除知识库文件
    *
    * @param ids 编号
    */
    void deleteDocumentListByIds(List<Long> ids);

    /**
     * 获得知识库文件
     *
     * @param id 编号
     * @return 知识库文件
     */
    DocumentDO getDocument(Long id);

    /**
     * 获得知识库文件分页
     *
     * @param pageReqVO 分页查询
     * @return 知识库文件分页
     */
    PageResult<DocumentDO> getDocumentPage(DocumentPageReqVO pageReqVO);

    /**
     * 上传文件并创建知识库文档记录
     *
     * @param file 上传的文件
     * @param kbId 所属知识库ID
     * @param description 文件描述（可选）
     * @param tags 标签（可选）
     * @return 文档ID
     */
    Long uploadAndCreate(MultipartFile file, Long kbId, String description, String tags);

}