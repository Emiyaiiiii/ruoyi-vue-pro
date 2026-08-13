package cn.iocoder.yudao.module.kb.dal.mysql.document;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.kb.controller.admin.document.vo.*;

/**
 * 知识库文件 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface DocumentMapper extends BaseMapperX<DocumentDO> {

    /**
     * 按知识库ID + 文件名查找文档（用于去重）
     */
    default DocumentDO selectByKbIdAndFileName(Long kbId, String fileName) {
        return selectOne(new LambdaQueryWrapperX<DocumentDO>()
                .eq(DocumentDO::getKbId, kbId)
                .eq(DocumentDO::getFileName, fileName));
    }

    default PageResult<DocumentDO> selectPage(DocumentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DocumentDO>()
                .eqIfPresent(DocumentDO::getKbId, reqVO.getKbId())
                .eqIfPresent(DocumentDO::getFolderId, reqVO.getFolderId())
                .likeIfPresent(DocumentDO::getFileName, reqVO.getFileName())
                .eqIfPresent(DocumentDO::getFileUrl, reqVO.getFileUrl())
                .eqIfPresent(DocumentDO::getFileType, reqVO.getFileType())
                .eqIfPresent(DocumentDO::getFileSize, reqVO.getFileSize())
                .eqIfPresent(DocumentDO::getFileConfigId, reqVO.getFileConfigId())
                .eqIfPresent(DocumentDO::getFilePath, reqVO.getFilePath())
                .eqIfPresent(DocumentDO::getDescription, reqVO.getDescription())
                .eqIfPresent(DocumentDO::getTags, reqVO.getTags())
                .eqIfPresent(DocumentDO::getDownloadCount, reqVO.getDownloadCount())
                .eqIfPresent(DocumentDO::getViewCount, reqVO.getViewCount())
                .eqIfPresent(DocumentDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(DocumentDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DocumentDO::getId));
    }

}