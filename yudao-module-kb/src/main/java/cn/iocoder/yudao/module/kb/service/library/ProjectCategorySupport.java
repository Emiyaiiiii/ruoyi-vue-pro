package cn.iocoder.yudao.module.kb.service.library;

import cn.iocoder.yudao.module.kb.dal.dataobject.category.CategoryDO;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 判断分类是否属于「院级/公司知识库 → 项目成果库」。
 * <p>
 * 命中后，该分类下创建的知识库应自动标记为项目库（{@code is_project=1}），纳入项目成员管理。
 */
public final class ProjectCategorySupport {

    private static final Long ROOT_ID = 0L;
    private static final int MAX_DEPTH = 32;

    private ProjectCategorySupport() {
    }

    public static boolean isOutcomeName(String name) {
        return name != null && name.contains("项目成果");
    }

    public static boolean isInstituteOrCompanyName(String name) {
        if (name == null) {
            return false;
        }
        return name.contains("院级")
                || name.contains("公司知识库")
                || (name.contains("公司") && name.contains("知识库"));
    }

    public static boolean isMarkedProject(Integer isProject) {
        return isProject != null && isProject == 1;
    }

    /**
     * 当前分类或其祖先被标为项目成果库，或名称路径为「院级/公司 + 项目成果」。
     */
    public static boolean isProjectCategory(CategoryDO current, Function<Long, CategoryDO> loader) {
        if (current == null) {
            return false;
        }
        List<String> names = new ArrayList<>();
        CategoryDO node = current;
        int guard = 0;
        while (node != null && guard++ < MAX_DEPTH) {
            if (isMarkedProject(node.getIsProject())) {
                return true;
            }
            names.add(node.getName());
            Long parentId = node.getParentId();
            if (parentId == null || ROOT_ID.equals(parentId)) {
                break;
            }
            node = loader.apply(parentId);
        }
        return names.stream().anyMatch(ProjectCategorySupport::isOutcomeName)
                && names.stream().anyMatch(ProjectCategorySupport::isInstituteOrCompanyName);
    }

    /**
     * 新建/编辑分类时：名称含「项目成果」且挂在院级/公司下，或父链已是项目成果库。
     */
    public static boolean shouldAutoMark(String name, Long parentId, Function<Long, CategoryDO> loader) {
        if (parentId != null && !ROOT_ID.equals(parentId)) {
            CategoryDO parent = loader.apply(parentId);
            if (isProjectCategory(parent, loader)) {
                return true;
            }
        }
        if (!isOutcomeName(name)) {
            return false;
        }
        List<String> names = new ArrayList<>();
        names.add(name);
        CategoryDO node = (parentId != null && !ROOT_ID.equals(parentId)) ? loader.apply(parentId) : null;
        int guard = 0;
        while (node != null && guard++ < MAX_DEPTH) {
            if (isMarkedProject(node.getIsProject())) {
                return true;
            }
            names.add(node.getName());
            Long pid = node.getParentId();
            if (pid == null || ROOT_ID.equals(pid)) {
                break;
            }
            node = loader.apply(pid);
        }
        return names.stream().anyMatch(ProjectCategorySupport::isInstituteOrCompanyName);
    }
}
