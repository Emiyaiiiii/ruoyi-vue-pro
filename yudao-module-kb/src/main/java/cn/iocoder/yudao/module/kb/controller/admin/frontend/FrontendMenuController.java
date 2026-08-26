package cn.iocoder.yudao.module.kb.controller.admin.frontend;

import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendMenuNodeVO;
import cn.iocoder.yudao.module.kb.controller.admin.frontend.vo.FrontendResult;
import cn.iocoder.yudao.module.kb.dal.dataobject.category.CategoryDO;
import cn.iocoder.yudao.module.kb.dal.dataobject.levelconfig.LevelConfigDO;
import cn.iocoder.yudao.module.kb.dal.mysql.levelconfig.LevelConfigMapper;
import cn.iocoder.yudao.module.kb.service.category.CategoryService;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.service.permission.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 前端 C 端 - 导航菜单兼容层
 *
 * <p>左侧导航菜单（{@code /system/system_menu/front_tree/}）来自 system_menu；
 * 知识库分类树（{@code /knowledge/basestypes/front_tree/}）来自后台「分类管理」（kb_category），
 * 与知识库大屏（kb/screen）保持一致。
 *
 * <p>前端 Layout.getKnowledgeTreeData 会把「知识库」菜单的子项替换为
 * [分类树 children] + [知识库广场 / 我创建的 / 我加入的]，这三项只需出现在
 * system_menu（菜单树）中，由前端合并逻辑追加，分类树里不要再包含它们。
 *
 * @author 吴皓
 */
@Tag(name = "前端 C 端 - 导航菜单兼容层")
@RestController
public class FrontendMenuController {

    /** 前端导航菜单根目录（system_menu 中「知识库菜单管理」的 path，管理后台路由要求以 / 开头） */
    private static final String MENU_ROOT_PATH = "/kb-menu";

    @Resource
    private MenuService menuService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private LevelConfigMapper levelConfigMapper;

    /**
     * 前端左侧导航菜单树（对应 /system/system_menu/front_tree/）
     *
     * <p>返回「知识库菜单管理」根目录下的子节点作为顶级导航（首页/个人中心/知识库/笔记/智能体广场/一张图）。
     * 知识库节点下保留 knowledge-hub / my-public / my-follows，供前端合并分类树时追加到末尾。
     */
    @GetMapping({"/system/system_menu/front_tree", "/system/system_menu/front_tree/"})
    @Operation(summary = "前端左侧导航菜单树")
    public FrontendResult<List<FrontendMenuNodeVO>> frontTree() {
        List<MenuDO> menus = menuService.getMenuList();
        MenuDO root = findNode(menus, MENU_ROOT_PATH);
        Long rootId = root != null ? root.getId() : MenuDO.ID_ROOT;
        List<FrontendMenuNodeVO> tree = buildTree(menus, rootId, m -> true);
        return FrontendResult.ok(tree);
    }

    /**
     * 前端知识库分类树（对应 /knowledge/basestypes/front_tree/）
     *
     * <p>返回「知识库」节点，其子分类来自后台「分类管理」（kb_category），与知识库大屏一致。
     * 分类节点 value/id 使用分类主键，前端据此拼接路由 /knowledge-base/{categoryId} 并作为
     * category 查询参数，后端按分类主键走 LibraryService.getLibraryPage（含可见性过滤）。
     * knowledge-hub（知识库广场）不在此处返回，由前端合并逻辑从 system_menu 追加。
     */
    @GetMapping({"/knowledge/basestypes/front_tree", "/knowledge/basestypes/front_tree/"})
    @Operation(summary = "前端知识库分类树")
    public FrontendResult<List<FrontendMenuNodeVO>> knowledgeFrontTree() {
        // 读取当前登录用户可见的分类（超管/租户管理员可见全部）
        List<CategoryDO> categories = categoryService.listCategoriesForUser(null);

        FrontendMenuNodeVO kb = new FrontendMenuNodeVO();
        kb.setValue("knowledge-base");
        kb.setLabel("知识库");
        kb.setStatus(true);
        kb.setVisible(true);
        kb.setChildren(buildCategoryTree(categories, CategoryDO.PARENT_ID_ROOT));
        return FrontendResult.ok(Collections.singletonList(kb));
    }

    // ---------- 菜单树 ----------

    private MenuDO findNode(List<MenuDO> menus, String path) {
        return menus.stream()
                .filter(m -> path.equals(m.getPath()))
                .findFirst()
                .orElse(null);
    }

    private List<FrontendMenuNodeVO> buildTree(List<MenuDO> menus, Long parentId, Predicate<MenuDO> filter) {
        return menus.stream()
                .filter(m -> Objects.equals(m.getParentId(), parentId))
                .filter(filter)
                .sorted(Comparator.comparing(m -> m.getSort() == null ? 0 : m.getSort()))
                .map(m -> toNode(m, menus))
                .collect(Collectors.toList());
    }

    private FrontendMenuNodeVO toNode(MenuDO m) {
        FrontendMenuNodeVO vo = new FrontendMenuNodeVO();
        vo.setId(m.getId());
        vo.setLabel(m.getName());
        // system_menu.path 以 / 开头（管理后台动态路由要求），前端 C 端 value 是去 / 的 slug
        vo.setValue(stripSlash(m.getPath()));
        vo.setIcon(m.getIcon());
        vo.setSort(m.getSort());
        vo.setType(m.getType());
        vo.setPath(m.getPath());
        // status: CommonStatusEnum 0=启用；前端按布尔值「可用」理解
        vo.setStatus(m.getStatus() == null || m.getStatus() == 0);
        vo.setVisible(m.getVisible() == null || m.getVisible());
        vo.setChildren(Collections.emptyList());
        return vo;
    }

    private FrontendMenuNodeVO toNode(MenuDO m, List<MenuDO> menus) {
        FrontendMenuNodeVO vo = toNode(m);
        vo.setChildren(buildTree(menus, m.getId(), child -> true));
        return vo;
    }

    /** 去掉 path 开头的 /，得到前端 C 端 value（slug） */
    private String stripSlash(String path) {
        if (path == null) {
            return null;
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }

    // ---------- 分类树（来自 kb_category） ----------

    /** 按 parentId 递归构建分类树，节点 value/id 使用分类主键，附带 columnConfig 供动态表头使用 */
    private List<FrontendMenuNodeVO> buildCategoryTree(List<CategoryDO> categories, Long parentId) {
        return categories.stream()
                .filter(c -> Objects.equals(c.getParentId(), parentId))
                // 广场入口由前端从 system_menu 追加，分类树里不再返回「知识库广场」
                .filter(c -> !"知识库广场".equals(c.getName()))
                .sorted(Comparator.comparing(c -> c.getSort() == null ? 0 : c.getSort()))
                .map(c -> toCategoryNode(c, categories))
                .collect(Collectors.toList());
    }

    private FrontendMenuNodeVO toCategoryNode(CategoryDO c, List<CategoryDO> all) {
        FrontendMenuNodeVO vo = new FrontendMenuNodeVO();
        vo.setId(c.getId());
        vo.setLabel(c.getName());
        // value 使用分类主键字符串，前端据此拼接 /knowledge-base/{categoryId} 并作为 category 参数
        vo.setValue(String.valueOf(c.getId()));
        vo.setStatus(c.getStatus() == null || c.getStatus() == 0);
        vo.setVisible(true);
        vo.setColumnConfig(c.getColumnConfig());
        vo.setIsProject(c.getIsProject());
        if (c.getKbLevelId() != null) {
            LevelConfigDO level = levelConfigMapper.selectById(c.getKbLevelId());
            if (level != null) {
                vo.setVisibilityRule(level.getVisibilityRule());
            }
        }
        vo.setChildren(buildCategoryTree(all, c.getId()));
        return vo;
    }
}
