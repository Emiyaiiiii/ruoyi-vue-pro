package cn.iocoder.yudao.module.kb.service.category;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.userdept.KbUserDeptDO;
import cn.iocoder.yudao.module.kb.dal.mysql.levelconfig.LevelConfigMapper;
import cn.iocoder.yudao.module.kb.service.userdept.KbUserDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.kb.controller.admin.category.vo.*;
import cn.iocoder.yudao.module.kb.dal.dataobject.category.CategoryDO;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.kb.dal.mysql.category.CategoryMapper;
import cn.iocoder.yudao.module.kb.dal.mysql.library.LibraryMapper;
import cn.iocoder.yudao.module.kb.service.library.ProjectCategorySupport;
import org.springframework.transaction.annotation.Transactional;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.kb.enums.ErrorCodeConstants.*;

/**
 * 知识库分类 Service 实现类
 *
 * @author 吴皓
 */
@Service
@Validated
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final LevelConfigMapper levelConfigMapper;
    private final KbUserDeptService kbUserDeptService;
    private final SecurityFrameworkService securityFrameworkService;
    private final LibraryMapper libraryMapper;

    @Override
    public List<CategoryDO> listCategoriesForUser(Long userDeptId) {
        // 获取当前用户的所有部门ID（支持一人多院）
        Set<Long> userDeptIds;
        if (userDeptId != null) {
            // 显式传了部门ID → 仅该部门
            userDeptIds = Collections.singleton(userDeptId);
        } else {
            // 自动从当前登录用户获取
            Long userId = SecurityFrameworkUtils.getLoginUserId();
            if (userId != null) {
                userDeptIds = kbUserDeptService.getDeptIdsByUserId(userId);
            } else {
                userDeptIds = Collections.emptySet();
            }
        }

        List<CategoryDO> all = categoryMapper.selectList();

        // 超级管理员/租户管理员 → 跳过 deptScope 过滤，可见所有分类
        if (securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode())) {
            return all;
        }

        // 加载层级配置
        Map<Long, LevelConfigDO> configMap = levelConfigMapper.selectList()
                .stream()
                .collect(Collectors.toMap(LevelConfigDO::getId, Function.identity()));

        // 预计算用户可见的部门范围 = 用户所属部门 + 所有祖先部门
        Set<Long> visibleDeptIds = new HashSet<>(userDeptIds);
        for (Long deptId : userDeptIds) {
            visibleDeptIds.addAll(kbUserDeptService.getDeptAncestorIds(deptId));
        }

        return all.stream()
                .filter(cat -> {
                    LevelConfigDO cfg = configMap.get(cat.getKbLevelId());
                    if (cfg == null) return true;

                    String deptScope = cfg.getDeptScope();
                    // dept_scope 为空 → 所有人都能看到这个分类
                    if (StrUtil.isBlank(deptScope)) return true;

                    // dept_scope 有值 → 用户任一部门（含祖先部门）在允许列表里即可
                    // 兼容两种格式：JSON 数组 "[1,2]" 或纯数字 "123"
                    List<Long> allowDeptIds;
                    if (deptScope.trim().startsWith("[")) {
                        allowDeptIds = JsonUtils.parseArray(deptScope, Long.class);
                    } else {
                        allowDeptIds = Collections.singletonList(Long.parseLong(deptScope.trim()));
                    }
                    return allowDeptIds.stream().anyMatch(visibleDeptIds::contains);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Long createCategory(CategorySaveReqVO createReqVO) {
        // 校验父分类ID: 0=顶级分类的有效性
        validateParentCategory(null, createReqVO.getParentId());
        // 校验分类名称的唯一性
        validateCategoryNameUnique(null, createReqVO.getParentId(), createReqVO.getName());
        applyAutoProjectCategoryFlag(createReqVO);

        // 插入
        CategoryDO category = BeanUtils.toBean(createReqVO, CategoryDO.class);
        categoryMapper.insert(category);

        // 返回
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(CategorySaveReqVO updateReqVO) {
        // 校验存在
        validateCategoryExists(updateReqVO.getId());
        // 校验父分类ID: 0=顶级分类的有效性
        validateParentCategory(updateReqVO.getId(), updateReqVO.getParentId());
        // 校验分类名称的唯一性
        validateCategoryNameUnique(updateReqVO.getId(), updateReqVO.getParentId(), updateReqVO.getName());
        applyAutoProjectCategoryFlag(updateReqVO);

        // 更新
        CategoryDO updateObj = BeanUtils.toBean(updateReqVO, CategoryDO.class);
        categoryMapper.updateById(updateObj);

        // 标为项目成果库后，把该分类及子分类下已有知识库一并纳入项目成员管理
        if (ProjectCategorySupport.isMarkedProject(updateReqVO.getIsProject())) {
            libraryMapper.updateIsProjectByCategoryIds(collectSelfAndDescendantIds(updateReqVO.getId()), 1);
        }
    }

    @Override
    public void deleteCategory(Long id) {
        // 校验存在
        validateCategoryExists(id);
        // 校验是否有子知识库分类
        if (categoryMapper.selectCountByParentId(id) > 0) {
            throw exception(CATEGORY_EXITS_CHILDREN);
        }
        // 删除
        categoryMapper.deleteById(id);
    }


    private void validateCategoryExists(Long id) {
        if (categoryMapper.selectById(id) == null) {
            throw exception(CATEGORY_NOT_EXISTS);
        }
    }

    private void validateParentCategory(Long id, Long parentId) {
        if (parentId == null || CategoryDO.PARENT_ID_ROOT.equals(parentId)) {
            return;
        }
        // 1. 不能设置自己为父知识库分类
        if (Objects.equals(id, parentId)) {
            throw exception(CATEGORY_PARENT_ERROR);
        }
        // 2. 父知识库分类不存在
        CategoryDO parentCategory = categoryMapper.selectById(parentId);
        if (parentCategory == null) {
            throw exception(CATEGORY_PARENT_NOT_EXITS);
        }
        // 3. 递归校验父知识库分类，如果父知识库分类是自己的子知识库分类，则报错，避免形成环路
        if (id == null) { // id 为空，说明新增，不需要考虑环路
            return;
        }
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            // 3.1 校验环路
            parentId = parentCategory.getParentId();
            if (Objects.equals(id, parentId)) {
                throw exception(CATEGORY_PARENT_IS_CHILD);
            }
            // 3.2 继续递归下一级父知识库分类
            if (parentId == null || CategoryDO.PARENT_ID_ROOT.equals(parentId)) {
                break;
            }
            parentCategory = categoryMapper.selectById(parentId);
            if (parentCategory == null) {
                break;
            }
        }
    }

    private void validateCategoryNameUnique(Long id, Long parentId, String name) {
        CategoryDO category = categoryMapper.selectByParentIdAndName(parentId, name);
        if (category == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的知识库分类
        if (id == null) {
            throw exception(CATEGORY_NAME_DUPLICATE);
        }
        if (!Objects.equals(category.getId(), id)) {
            throw exception(CATEGORY_NAME_DUPLICATE);
        }
    }

    @Override
    public CategoryDO getCategory(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public List<CategoryDO> getCategoryList(CategoryListReqVO listReqVO) {
        return categoryMapper.selectList(listReqVO);
    }

    /**
     * 院级/公司下的「项目成果」分类自动打上项目库标记，无需手工勾选。
     */
    private void applyAutoProjectCategoryFlag(CategorySaveReqVO reqVO) {
        if (ProjectCategorySupport.isMarkedProject(reqVO.getIsProject())) {
            return;
        }
        if (ProjectCategorySupport.shouldAutoMark(reqVO.getName(), reqVO.getParentId(),
                categoryMapper::selectById)) {
            reqVO.setIsProject(1);
        } else if (reqVO.getIsProject() == null) {
            reqVO.setIsProject(0);
        }
    }

    private Set<Long> collectSelfAndDescendantIds(Long categoryId) {
        List<CategoryDO> all = categoryMapper.selectList();
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (CategoryDO cat : all) {
            if (cat.getParentId() == null) {
                continue;
            }
            childrenMap.computeIfAbsent(cat.getParentId(), k -> new ArrayList<>()).add(cat.getId());
        }
        Set<Long> ids = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(categoryId);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            if (id == null || !ids.add(id)) {
                continue;
            }
            List<Long> children = childrenMap.get(id);
            if (children != null) {
                queue.addAll(children);
            }
        }
        return ids;
    }

}
