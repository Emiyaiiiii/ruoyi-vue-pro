package cn.iocoder.yudao.module.kb.service.libraryext;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.kb.dal.dataobject.libraryext.LibraryExtDO;
import cn.iocoder.yudao.module.kb.dal.mysql.libraryext.LibraryExtMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 知识库自定义字段值 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
@RequiredArgsConstructor
public class LibraryExtServiceImpl implements LibraryExtService {

    private final LibraryExtMapper libraryExtMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceExtValues(Long kbId, Map<String, String> values) {
        // 先物理删除，再逐条插入（不能用逻辑删除，否则 uk_kb_field 唯一键会与旧数据冲突）
        libraryExtMapper.physicalDeleteByKbId(kbId);
        if (CollUtil.isEmpty(values)) {
            return;
        }
        values.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .forEach(e -> libraryExtMapper.insert(new LibraryExtDO()
                        .setKbId(kbId)
                        .setFieldKey(e.getKey())
                        .setFieldValue(e.getValue())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAllByKbId(Long kbId) {
        libraryExtMapper.physicalDeleteByKbId(kbId);
    }

    @Override
    public Map<String, String> getExtValues(Long kbId) {
        List<LibraryExtDO> list = libraryExtMapper.selectListByKbId(kbId);
        Map<String, String> map = new HashMap<>();
        list.forEach(item -> map.put(item.getFieldKey(), item.getFieldValue()));
        return map;
    }

    @Override
    public Map<Long, Map<String, String>> getExtValuesMap(Collection<Long> kbIds) {
        Map<Long, Map<String, String>> result = new HashMap<>();
        if (CollUtil.isEmpty(kbIds)) {
            return result;
        }
        List<LibraryExtDO> list = libraryExtMapper.selectListByKbIds(kbIds);
        for (LibraryExtDO item : list) {
            result.computeIfAbsent(item.getKbId(), k -> new HashMap<>())
                    .put(item.getFieldKey(), item.getFieldValue());
        }
        return result;
    }

}
