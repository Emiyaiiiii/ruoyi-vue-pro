package cn.iocoder.yudao.module.kb.service.libraryext;

import java.util.*;

/**
 * 知识库自定义字段值 Service 接口
 *
 * @author 吴皓
 */
public interface LibraryExtService {

    /**
     * 覆盖保存知识库的自定义字段值（先删后插）
     *
     * @param kbId   知识库ID
     * @param values 字段key → 字段值
     */
    void replaceExtValues(Long kbId, Map<String, String> values);

    /**
     * 删除知识库的全部自定义字段值
     *
     * @param kbId 知识库ID
     */
    void removeAllByKbId(Long kbId);

    /**
     * 获得知识库的自定义字段值
     *
     * @param kbId 知识库ID
     * @return 字段key → 字段值
     */
    Map<String, String> getExtValues(Long kbId);

    /**
     * 批量获得多个知识库的自定义字段值
     *
     * @param kbIds 知识库ID集合
     * @return 知识库ID → (字段key → 字段值)
     */
    Map<Long, Map<String, String>> getExtValuesMap(Collection<Long> kbIds);

}
