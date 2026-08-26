package cn.iocoder.yudao.module.kb.service.tag;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.kb.controller.admin.tag.vo.TagPageReqVO;
import cn.iocoder.yudao.module.kb.controller.admin.tag.vo.TagSaveReqVO;
import cn.iocoder.yudao.module.kb.dal.dataobject.tag.TagDO;

import java.util.List;

/**
 * 标签 Service 接口
 *
 * @author 吴皓
 */
public interface TagService {

    /**
     * 创建标签
     *
     * @return 标签编号
     */
    Long createTag(TagSaveReqVO reqVO);

    /**
     * 更新标签
     */
    void updateTag(TagSaveReqVO reqVO);

    /**
     * 删除标签
     */
    void deleteTag(Long id);

    /**
     * 获得标签
     */
    TagDO getTag(Long id);

    /**
     * 获得标签分页（含可见性过滤）
     */
    PageResult<TagDO> getTagPage(TagPageReqVO reqVO);

    /**
     * 获得当前用户可见的标签列表（管理员=全部，普通用户=本人+全局），用于 C 端上传下拉
     */
    List<TagDO> getTagList(String type);

}