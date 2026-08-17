package cn.iocoder.yudao.module.kb.service.category;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.yudao.module.kb.controller.admin.category.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.category.CategoryDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 知识库分类 Service 接口
 *
 * @author 吴皓
 */
public interface CategoryService {

    List<CategoryDO> listCategoriesForUser(Long userDeptId);

    /**
     * 创建知识库分类
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCategory(@Valid CategorySaveReqVO createReqVO);

    /**
     * 更新知识库分类
     *
     * @param updateReqVO 更新信息
     */
    void updateCategory(@Valid CategorySaveReqVO updateReqVO);

    /**
     * 删除知识库分类
     *
     * @param id 编号
     */
    void deleteCategory(Long id);


    /**
     * 获得知识库分类
     *
     * @param id 编号
     * @return 知识库分类
     */
    CategoryDO getCategory(Long id);

    /**
     * 获得知识库分类列表
     *
     * @param listReqVO 查询条件
     * @return 知识库分类列表
     */
    List<CategoryDO> getCategoryList(CategoryListReqVO listReqVO);

}
