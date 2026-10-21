package cn.iocoder.yudao.module.kb.dal.mysql.document;

import java.util.*;
import java.util.stream.Collectors;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.kb.dal.dataobject.document.DocumentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
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
                // 服务端注入的可见知识库过滤（未选知识库时按权限收敛，配合 inIfPresent 只在非空时生效）
                .inIfPresent(DocumentDO::getKbId, reqVO.getKbIds())
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

    /**
     * 贡献排行：按文档上传人（creator）聚合文档数，倒序取前 N。
     * 多租户条件由租户插件自动追加（kb_document 启用租户）。
     */
    @Select("SELECT creator AS creatorId, COUNT(*) AS docCount FROM kb_document " +
            "WHERE deleted = 0 AND creator IS NOT NULL GROUP BY creator ORDER BY docCount DESC LIMIT #{limit}")
    List<Map<String, Object>> selectContributionRank(int limit);

    /**
     * 按知识库批量汇总文件字节数（前端卡片 used_storage_mb 用）
     *
     * @param kbIds 知识库ID集合
     * @return kbId → 文件总字节数
     */
    default Map<Long, Long> selectStorageBytesMap(Collection<Long> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return selectList(new LambdaQueryWrapperX<DocumentDO>()
                .in(DocumentDO::getKbId, kbIds)).stream()
                .filter(d -> d.getFileSize() != null)
                .collect(Collectors.groupingBy(DocumentDO::getKbId,
                        Collectors.summingLong(DocumentDO::getFileSize)));
    }

    /**
     * 按知识库批量统计文档数（智库总览 doc_count 用）
     *
     * @param kbIds 知识库ID集合
     * @return 文档总数
     */
    default long selectCountByKbIds(Collection<Long> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) {
            return 0L;
        }
        return selectCount(new LambdaQueryWrapperX<DocumentDO>()
                .in(DocumentDO::getKbId, kbIds));
    }

}