# 知识库权限管理模型

## 一、权限模型概述

知识库模块采用**三层权限模型**，从外到内依次控制：**列表可见性 → 管理权限 → 内容访问**。三层模型独立运作，逐层收窄，共同构成完整的权限体系。

```
┌─────────────────────────────────────────────────────┐
│                    第一层：列表可见性                    │
│        KbVisibilityServiceImpl.filterVisible()         │
│    控制用户能看到哪些知识库（知识库列表页）                │
├─────────────────────────────────────────────────────┤
│                    第二层：管理权限                      │
│       LibraryServiceImpl.validateManagementPermission() │
│    控制用户能否对知识库执行增/删/改操作                   │
├─────────────────────────────────────────────────────┤
│                    第三层：内容访问                      │
│      DocumentController.validateProjectContentAccess()  │
│    控制用户能否查看项目成果库的文档内容                   │
└─────────────────────────────────────────────────────┘
```

**超级管理员/租户管理员绕过机制**：三层权限校验均在最前面判断用户是否为超级管理员或租户管理员，如是则直接放行，拥有所有权限。

---

## 二、核心数据表

### 2.1 表结构总览

| 表名 | 用途 | 关联字段 |
|------|------|----------|
| `kb_library` | 知识库主表 | `kb_level_id` → `kb_level_config.id`<br>`category_id` → `kb_category.id`<br>`owner_id` → 用户ID或部门ID<br>`is_project`：项目成果库标记 |
| `kb_level_config` | 层级配置（权限核心） | `visibility_rule`：可见规则<br>`owner_dim`：归属维度 |
| `kb_category` | 分类体系 | `kb_level_id` → `kb_level_config.id` |
| `kb_user_dept` | 用户↔部门关联 | `user_id` + `dept_id`<br>`role`：0=成员, 1=管理员 |
| `kb_share_dept` | 共享部门 | `kb_id` + `dept_id` |
| `kb_project_member` | 项目成员 | `kb_id` + `user_id` |
| `kb_document` | 文档记录 | `kb_id` → `kb_library.id` |

### 2.2 层级配置（kb_level_config）—— 权限控制的核心

层级配置定义了知识库的**可见规则（visibility_rule）**和**归属维度（owner_dim）**，是权限判断的入口。

| 字段 | 说明 |
|------|------|
| `level_code` | 层级编码，如 `personal`、`dept`、`company` |
| `level_name` | 层级名称，如"个人知识库"、"院级知识库" |
| `visibility_rule` | 可见规则：1=按所有者, 2=按归属部门, 3=全员, 5=指定部门列表, 6=知识库广场 |
| `owner_dim` | 归属维度：0=无, 1=用户, 2=部门 |
| `dept_scope` | 分类可见部门范围：NULL=全员, JSON数组如[101,102] |

### 2.3 知识库（kb_library）—— 权限判断的对象

| 字段 | 说明 |
|------|------|
| `kb_level_id` | 关联层级配置，决定该库的可见规则和管理方式 |
| `owner_id` | 所有者ID，根据`owner_dim`决定是用户ID还是部门ID |
| `is_public` | 是否公开到知识库广场 |
| `is_project` | 是否项目成果库（1=是，启用第三层内容访问控制） |

---

## 三、三层权限校验详解

### 第一层：列表可见性（KbVisibilityServiceImpl.filterVisible）

**作用**：在知识库列表页，过滤出当前用户可见的知识库。

**校验逻辑**：

```java
// 按 visibility_rule 分流
switch (visibilityRule) {
    case 1:  // 按所有者 → 仅创建者本人可见
        return ownerId == userId;
    case 2:  // 按归属部门 → 通过 kb_user_dept 判断用户是否属于该部门
        return userDeptIds.contains(ownerId);
    case 3:  // 全员 → 所有人可见
        return true;
    case 5:  // 指定部门列表 → 用户部门是否在 kb_share_dept 中
        return shareDeptMapper.count(kbId, userDeptId) > 0;
    case 6:  // 知识库广场 → 全员可见
        return true;
}
```

**支持一人多院**：预加载用户关联的所有部门ID（`kbUserDeptMapper.selectDeptIdsByUserId`），case 2 时逐一匹配。

### 第二层：管理权限（LibraryServiceImpl.validateManagementPermission）

**作用**：控制用户能否编辑、删除知识库。

**校验逻辑**：

```java
// 超级管理员/租户管理员 → 直接放行
if (isSuperAdmin || isTenantAdmin) return;

switch (visibilityRule) {
    case 1:  // 个人知识库 → 仅所有者本人可管理
        if (ownerId != userId) 拒绝;
    case 2:  // 院级/咨询评估
    case 3:  // 公司级 → 该部门管理员(role=1)可管理
        if (!kbUserDeptMapper.isAdmin(userId, ownerId)) 拒绝;
    // 其他规则不在此处校验（由 API 层权限控制）
}
```

### 第三层：内容访问（DocumentController.validateProjectContentAccess）

**作用**：控制项目成果库的文档内容访问，仅项目成员可查看文档。

**校验逻辑**：

```java
// 非项目成果库 → 无额外限制
if (isProject != 1) return;

// 超级管理员/租户管理员 → 直接放行
if (isSuperAdmin || isTenantAdmin) return;

// 项目成员校验
if (!projectMemberService.isMember(kbId, userId)) 拒绝;
```

### 三层校验汇总表

| 知识库类型 | visibility_rule | 第一层：可见性 | 第二层：管理权限 | 第三层：内容访问 |
|-----------|:---------------:|:--------------:|:----------------:|:----------------:|
| 个人知识库 | 1 | 仅创建者可见 | 仅创建者可管理 | — |
| 院级知识库 | 2 | 该院成员可见 | 该院管理员可管理 | — |
| 公司知识库 | 3 | 全员可见 | 公司管理员可管理 | — |
| 共享知识库 | 5 | 指定部门成员可见 | 不在此层校验 | — |
| 知识库广场 | 6 | 全员可见 | 不在此层校验 | — |
| 项目成果库 | 任意 | 按对应规则 | 按对应规则 | 仅项目成员可查看文档 |
| 超管/租户管理员 | — | 全部可见 | 全部可管理 | 全部可访问 |

---

## 四、六个管理页面及其关系

### 4.1 页面导航关系

```
知识库（一级菜单，父节点）
├── 知识库管理（kb/library）          ← 核心页面，管理增量
├── 分类管理（kb/category）           ← 分类体系，影响可见范围
├── 层级配置（kb/levelconfig）        ← 权限核心，定义可见规则
├── 共享部门管理（kb/sharedept）       ← 辅助配置，扩展可见范围
├── 文档管理（kb/document）           ← 文件管理，内容主体
├── 部门成员管理（kb/userdept）        ← 人员配置，设置管理员
└── 项目成员管理（kb/projectmember）   ← 项目控制，配置文档访问
```

### 4.2 各页面功能与权限作用

#### ① 知识库管理（library/index.vue）

| 项目 | 说明 |
|------|------|
| **功能** | 创建、编辑、删除、查询知识库 |
| **权限作用** | 知识库的增删改查入口，每个知识库关联一个层级配置 |
| **关键字段** | `kb_level_id`（关联层级配置）、`owner_id`（所有者）、`is_project`（项目标记） |
| **权限说明弹窗** | 管理所有知识库的基础信息，每类知识库的可见性和管理权限由关联的层级配置决定 |

#### ② 层级配置（levelconfig/index.vue）

| 项目 | 说明 |
|------|------|
| **功能** | 管理可见规则（visibility_rule）和归属维度（owner_dim） |
| **权限作用** | **权限控制的核心配置**，定义了知识库列表可见规则和管理权限归属维度 |
| **关键字段** | `visibility_rule`（1/2/3/5/6）、`owner_dim`（0/1/2）、`dept_scope` |
| **权限说明弹窗** | 每条层级配置定义了一类知识库的可见规则和归属维度 |

#### ③ 分类管理（category/index.vue）

| 项目 | 说明 |
|------|------|
| **功能** | 管理知识库分类体系（树形结构） |
| **权限作用** | 每个分类关联一个层级配置，创建知识库时选择分类即继承了该分类的层级配置，从而决定了该库的可见规则和管理方式 |
| **关键字段** | `kb_level_id`（关联层级配置） |
| **权限说明弹窗** | 分类用于组织知识库的层级结构，配合层级配置共同控制可见范围和管理权限 |

#### ④ 共享部门管理（sharedept/index.vue）

| 项目 | 说明 |
|------|------|
| **功能** | 管理知识库与部门的共享关联 |
| **权限作用** | 当层级配置的可见规则为 `visibility_rule=5`（指定部门列表）时，在此配置哪些部门可以访问该知识库 |
| **关键字段** | `kb_id`（知识库ID）、`dept_id`（共享部门ID） |
| **权限说明弹窗** | 通过配置共享部门，可将部门级知识库扩展可见到其他部门，实现跨部门知识共享 |

#### ⑤ 文档管理（document/index.vue）

| 项目 | 说明 |
|------|------|
| **功能** | 上传、下载、编辑、删除知识库文档 |
| **权限作用** | 文档上传后自动存储到芋道文件管理系统，项目成果库的文档仅对项目成员可见（第三层内容访问控制在此生效） |
| **关键字段** | `kb_id`（关联知识库）、`file_url`（芋道文件访问地址） |
| **权限说明弹窗** | 上传文件后自动存储到芋道文件管理系统，项目成果库的文档仅对项目成员可见 |

#### ⑥ 部门成员管理（userdept/index.vue）

| 项目 | 说明 |
|------|------|
| **功能** | 管理各部门（院/中心/公司）的知识库管理员和成员角色 |
| **权限作用** | **第二层管理权限的数据来源**：通过 `kb_user_dept` 表设置 `role=1` 的用户即为该部门管理员，可管理该部门下的知识库 |
| **关键字段** | `user_id`、`dept_id`、`role`（0=成员, 1=管理员） |
| **页面布局** | 左部门树 + 右成员列表，成员自动同步芋道系统部门用户 |
| **权限说明弹窗** | 成员列表自动同步芋道系统部门用户，管理员可在此设置哪些用户拥有部门级知识库的管理权限 |

#### ⑦ 项目成员管理（projectmember/index.vue）

| 项目 | 说明 |
|------|------|
| **功能** | 管理项目成果库的文档访问成员 |
| **权限作用** | **第三层内容访问的数据来源**：通过 `kb_project_member` 表控制项目成果库的文档可见范围 |
| **关键字段** | `kb_id`（知识库ID）、`user_id`（成员用户ID） |
| **权限说明弹窗** | 项目成果库的内容（文档）仅对已添加的项目成员可见，非成员无法查看文档详情 |

### 4.3 页面数据流向关系

```
层级配置 (kb_level_config)
    │ 定义 visibility_rule / owner_dim
    ▼
分类 (kb_category) ────→ 知识库 (kb_library) ────→ 文档 (kb_document)
    │ 关联 kb_level_id      │ 关联 kb_level_id       │ 关联 kb_id
    │                        │ 关联 category_id
    │                        │ 标记 is_project
    ▼                        ▼                        ▼
部门成员 (kb_user_dept)    共享部门 (kb_share_dept)  项目成员 (kb_project_member)
    │ 控制管理权限            │ 扩展可见范围             │ 控制文档访问
    │ role=1=管理员           │ visibility_rule=5 时    │ is_project=1 时生效
    ▼
芋道系统部门 (system_dept)  ← 部门成员管理页面自动同步
```

---

## 五、权限边界说明

### 5.1 知识库分类（category）与权限的关系

- 分类本身**不直接控制权限**，但它通过关联的 `kb_level_id` 间接决定了知识库的可见规则和管理方式
- 创建知识库时选择分类，该库自动继承分类的层级配置
- 修改分类的层级配置不会影响已有知识库（知识库在创建时已确定 `kb_level_id`）

### 5.2 共享部门（share_dept）的使用场景

- `visibility_rule=5` 时需配合使用
- 本质是实现"跨部门可见"的扩展机制
- 当前系统未使用该规则（所有分类均配置为 1/2/3 规则）

### 5.3 项目成果库（is_project）的特殊性

- `is_project=1` 是一个**叠加层**，不影响原有的可见规则和管理权限
- 在原有的三层权限之上，额外增加第四层：文档内容访问控制
- 超级管理员/租户管理员不受项目成员限制，可查看所有项目成果库的文档

### 5.4 角色与权限对应关系

| 角色 | 列表可见范围 | 管理权限范围 | 项目管理范围 |
|------|-------------|-------------|-------------|
| 超级管理员 | 全部知识库 | 全部知识库 | 全部项目成果库 |
| 租户管理员 | 全部知识库 | 全部知识库 | 全部项目成果库 |
| 部门管理员 | 所在部门知识库 | 所在部门知识库 | 无（需单独添加为项目成员） |
| 部门成员 | 所在部门知识库 | 仅个人知识库 | 无（需单独添加为项目成员） |
| 普通用户 | 仅个人知识库+公司级 | 仅个人知识库 | 无（需单独添加为项目成员） |

---

## 六、数据库表字段说明

### 6.1 kb_library（知识库）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| name | VARCHAR | 知识库名称 |
| category_id | BIGINT | 分类ID → kb_category.id |
| kb_level_id | BIGINT | 层级配置ID → kb_level_config.id |
| owner_id | BIGINT | 所有者ID（用户或部门） |
| description | TEXT | 描述 |
| cover_url | VARCHAR | 封面图片URL |
| doc_count | INT | 文档数量 |
| status | TINYINT | 状态：0=启用, 1=禁用 |
| is_public | TINYINT | 是否公开到广场：0=否, 1=是 |
| is_project | TINYINT | 是否项目成果库：0=否, 1=是 |

### 6.2 kb_level_config（层级配置）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| level_code | VARCHAR | 层级编码 |
| level_name | VARCHAR | 层级名称 |
| visibility_rule | TINYINT | 可见规则：1=按所有者, 2=按归属部门, 3=全员, 5=指定部门列表, 6=知识库广场 |
| owner_dim | TINYINT | 归属维度：0=无, 1=用户, 2=部门 |
| dept_scope | VARCHAR | 分类可见部门范围 |
| sort | INT | 排序 |
| status | TINYINT | 状态：0=启用, 1=禁用 |

### 6.3 kb_user_dept（用户部门关联）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| user_id | BIGINT | 用户ID |
| dept_id | BIGINT | 部门ID（院/公司/中心） |
| role | TINYINT | 角色：0=成员, 1=管理员 |
| 唯一约束 | uk_user_dept | (user_id, dept_id) |

### 6.4 kb_project_member（项目成员）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| kb_id | BIGINT | 知识库ID |
| user_id | BIGINT | 项目成员用户ID |
| 唯一约束 | uk_kb_user | (kb_id, user_id) |

### 6.5 kb_share_dept（共享部门）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| kb_id | BIGINT | 知识库ID |
| dept_id | BIGINT | 共享目标部门ID |

### 6.6 kb_category（分类）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| name | VARCHAR | 分类名称 |
| kb_level_id | BIGINT | 关联层级配置ID |
| parent_id | BIGINT | 父分类ID（0=顶级） |
| sort | INT | 排序 |
| status | TINYINT | 状态：0=启用, 1=禁用 |

### 6.7 kb_document（文档）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| kb_id | BIGINT | 所属知识库ID |
| file_name | VARCHAR | 文件名称 |
| file_url | VARCHAR | 文件访问URL（芋道文件管理返回） |
| file_type | VARCHAR | 文件类型 |
| file_size | BIGINT | 文件大小（字节） |
| file_config_id | BIGINT | 芋道文件配置ID |
| file_path | VARCHAR | 文件存储路径 |
| description | TEXT | 文件描述 |
| tags | VARCHAR | 标签（逗号分隔） |
| download_count | INT | 下载次数 |
| view_count | INT | 查看次数 |
| status | TINYINT | 状态：0=正常, 1=禁用 |

---

## 七、关键代码路径

| 权限层 | 文件 | 关键方法 |
|--------|------|----------|
| 列表可见性 | `KbVisibilityServiceImpl.java` | `filterVisible()` |
| 管理权限 | `LibraryServiceImpl.java` | `validateManagementPermission()` |
| 内容访问 | `DocumentController.java` | `validateProjectContentAccess()` |
| 部门关联查询 | `KbUserDeptMapper.java` | `selectDeptIdsByUserId()`, `isAdmin()` |
| 项目成员查询 | `ProjectMemberServiceImpl.java` | `isMember()` |