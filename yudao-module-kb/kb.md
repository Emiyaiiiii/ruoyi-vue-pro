## 一、表关系详解

整个知识库模块有 9 张表，分为三个层次：

### 1. 配置层（2 张表）

| 表 | 作用 | 关键字段 |
|---|---|---|
| `kb_level_config` | 定义知识库的**层级模板**，决定可见规则和管理归属 | `visibilityRule`(1/2/3/5), `ownerDim`(1用户/2部门), `deptScope`(JSON数组) |
| `kb_category` | 定义**分类树**，每个分类绑定一个层级配置 | `kb_level_id`, `parent_id`(树形结构), `is_project`(项目成果库分类) |

**关系**：`kb_category.kb_level_id` → `kb_level_config.id`，一个分类对应一个层级配置。

### 2. 核心层（1 张表）

| 表 | 作用 | 关键字段 |
|---|---|---|
| `kb_library` | 知识库主表，存储所有知识库实例 | `category_id`, `kb_level_id`, `owner_id`, `is_project`, `is_public` |

**关系**：
- `kb_library.kb_level_id` → `kb_level_config.id`（继承层级配置的规则）
- `kb_library.category_id` → `kb_category.id`（归属分类）
- `owner_id` 的含义由层级配置的 `ownerDim` 决定（用户ID 或 部门ID）

### 3. 关联层（6 张表）

| 表 | 作用 | 关联方式 |
|---|---|---|
| `kb_user_dept` | 用户-部门-角色映射（芋道体系外的补充） | 独立表，通过 `userId` / `deptId` / `role`(0成员/1管理员) 查询 |
| `kb_share_dept` | 指定部门可见的知识库共享列表 | `kb_id` → `kb_library.id`，用于 `visibilityRule=5` |
| `kb_project_member` | 项目知识库的成员列表 | `kb_id` → `kb_library.id`，`user_id` 为项目成员 |
| `kb_folder` | 知识库内的文件夹 | `kb_id` → `kb_library.id`，`parent_id` 自引用树形 |
| `kb_document` | 知识库内的文档 | `kb_id` → `kb_library.id`，`folder_id` → `kb_folder.id` |
| `kb_follow` | 用户关注的知识库 | `kb_id` → `kb_library.id`，`user_id` |

---

## 二、权限控制三层模型

### 第 1 层：分类可见性（`CategoryServiceImpl.listCategoriesForUser`）

**控制什么**：用户能看到哪些分类节点。

**判断逻辑**：
1. 获取用户所有部门 ID（`system_user.dept_id` + `kb_user_dept` 中的扩展部门）
2. 计算祖先部门链（如规划处 → 水利院 → 公司总部）
3. 遍历所有分类，读取其 `kb_level_id` 对应的 `deptScope`
4. 若 `deptScope` 为空 → 全员可见；否则检查用户部门是否在允许列表中
5. 超管/租户管理员 → 跳过过滤，返回全部

**关键代码位置**：`CategoryServiceImpl.java` 第 47-103 行

### 第 2 层：知识库可见性 + 管理权限（`LibraryServiceImpl`）

分为两个子层：

**A. 列表可见性（`filterVisible`，第 255-315 行）**
- `visibilityRule=1`（个人）：仅所有者可见
- `visibilityRule=2`（院级/咨询评估）：`ownerId` 在用户部门+祖先部门范围内可见
- `visibilityRule=3`（公司）：全员可见
- `visibilityRule=5`（指定部门）：用户的任一部门在 `kb_share_dept` 共享列表中
- **特殊处理**：公司级项目库视图（`isCompanyLevelProjectView`）时，所有 `isProject=1` 的知识库都可见，实现项目聚合

**B. 管理权限（`validateManagementPermission`，第 172-243 行）**
- 超管/租户管理员 → 直接放行
- `rule=1`：仅所有者本人
- `rule=2/3`：`ownerId` 部门的管理员（精确匹配 + 祖先部门向上查找）
- `rule=5`：按 `ownerDim` 决定——`ownerDim=1` 仅所有者，`ownerDim=2` 该部门管理员

**应用范围**：`createLibrary`、`updateLibrary`、`deleteLibrary`、`togglePublic`、`canManage` 全部调用此方法。

### 第 3 层：内容级访问控制

**控制什么**：项目知识库（`isProject=1`）的文档内容。

**判断逻辑**：检查当前用户是否在 `kb_project_member` 表中。非成员只能看到知识库基本信息，无法查看文件夹和文档内容。

**应用位置**：前端 `overview/index.vue` 中检查 `isProjectMember`，后端 `DocumentController` 和 `FolderController` 中也有对应校验。

**自动纳入项目成员管理**：
- 分类可配置 `is_project=1`（分类管理里的「项目成果库」开关）
- 院级知识库 / 公司知识库下名称含「项目成果」的分类会自动打标，无需手工勾选
- 大屏、总览、知识库管理里在这些分类下创建的库，`createLibrary` 会强制 `is_project=1`，创建人自动成为首个项目成员
- 打开「项目成员管理」时会回填同分类下尚未打标的旧库
- 新建/编辑时自定义字段 type=member（或 `member_ids`）会追加写入 `kb_project_member`，只增不删；不在 `kb_library` 加成员列
- C 端模糊搜人：`GET /auth/users/search/?q=`

---

## 三、关键设计决策

| 决策 | 说明 |
|---|---|
| **祖先部门链** | `getDeptAncestorIds()` 向上遍历部门树，确保水利院管理员能管理规划处的库 |
| **权限统一入口** | 所有管理操作都走 `validateManagementPermission`，便于维护 |
| **可见性在 Service 层过滤** | `filterVisible` 在分页查询后过滤，而非 SQL 层，因为规则复杂且涉及多表 |
| **kb_user_dept 补充体系** | 芋道 `system_user` 只有一个 `dept_id`，`kb_user_dept` 支持一人多部门 |
| **项目成员不进知识库主表** | 成员关系只写 `kb_project_member`；分类自定义字段 type=member 仅作展示和创建时的录入入口 |
| **院级默认二级部门** | C 端无部门下拉；`createLibrary` 在 visibilityRule=2 且 ownerDim=2 时，把空/当前用户 ownerId 改成用户部门链第二级 |

如果你对某个具体环节还想深入了解，可以告诉我。