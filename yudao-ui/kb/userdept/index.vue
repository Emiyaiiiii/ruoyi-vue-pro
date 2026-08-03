<template>
  <div>
    <!-- 权限说明 -->
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb-15px"
    >
      <template #title>
        部门成员管理 — 管理各部门（院/中心/公司）的知识库管理员和成员角色。成员列表自动同步芋道系统部门用户，管理员可在此设置哪些用户拥有部门级知识库的管理权限。
      </template>
    </el-alert>
    <div class="flex">
    <!-- 左侧：部门树 -->
    <el-card class="!w-280px flex-shrink-0 mr-15px">
      <template #header>
        <div class="flex items-center justify-between">
          <span>部门列表</span>
          <el-button link type="primary" @click="refreshDeptTree">
            <Icon icon="ep:refresh" />
          </el-button>
        </div>
      </template>
      <el-input
        v-model="deptKeyword"
        placeholder="搜索部门名称"
        clearable
        class="mb-10px"
      >
        <template #prefix>
          <Icon icon="ep:search" />
        </template>
      </el-input>
      <el-tree
        ref="deptTreeRef"
        :data="deptTree"
        :props="{ label: 'name', children: 'children' }"
        :expand-on-click-node="false"
        default-expand-all
        node-key="id"
        highlight-current
        @node-click="handleDeptClick"
      >
        <template #default="{ node, data }">
          <span class="custom-tree-node">
            <span>{{ node.label }}</span>
            <span v-if="data.status === 1" class="text-gray-400 text-xs ml-5px">（已停用）</span>
          </span>
        </template>
      </el-tree>
    </el-card>

    <!-- 右侧：成员列表 -->
    <el-card class="flex-1">
      <template #header>
        <div class="flex items-center justify-between">
          <span>
            {{ currentDeptName ? `${currentDeptName} - 成员管理` : '部门成员管理' }}
            <el-tag v-if="currentDeptId" size="small" class="ml-10px">
              共 {{ list.length }} 人
            </el-tag>
          </span>
        </div>
      </template>

      <!-- 提示信息 -->
      <el-alert
        v-if="!currentDeptId"
        title="请在左侧选择一个部门"
        type="info"
        :closable="false"
        class="mb-15px"
      />

      <!-- 成员表格 -->
      <el-table
        v-loading="loading"
        :data="filteredlist"
        :stripe="true"
        :show-overflow-tooltip="true"
        empty-text="该部门下暂无成员"
      >
        <el-table-column label="用户昵称" align="center" prop="nickname" min-width="120px">
          <template #default="{ row }">
            <span>{{ row.nickname || `用户${row.userId}` }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户ID" align="center" prop="userId" width="100px" />
        <el-table-column label="角色" align="center" width="120px">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'danger' : 'info'" size="small">
              {{ row.role === 1 ? '管理员' : '成员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="角色设置时间"
          align="center"
          prop="createTime"
          width="180px"
        >
          <template #default="{ row }">
            {{ row.createTime ? formatTime(row.createTime) : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" min-width="180px">
          <template #default="{ row }">
            <el-button
              v-if="row.role !== 1"
              link
              type="danger"
              @click="handleSetRole(row, 1)"
              v-hasPermi="['kb:user-dept:update']"
            >
              设为管理员
            </el-button>
            <el-button
              v-if="row.role === 1"
              link
              type="info"
              @click="handleSetRole(row, 0)"
              v-hasPermi="['kb:user-dept:update']"
            >
              取消管理员
            </el-button>
            <el-button
              v-if="row.id"
              link
              type="warning"
              @click="handleRemove(row)"
              v-hasPermi="['kb:user-dept:delete']"
            >
              移除角色
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { UserDeptApi, DeptApi, DeptMember, DeptVO } from '@/api/kb/userdept'

/** 部门成员管理 */
defineOptions({ name: 'KbUserDept' })

const message = useMessage()
const { t } = useI18n()

/** 将扁平部门列表转为树结构 */
const buildDeptTree = (list: DeptVO[]): DeptVO[] => {
  const map = new Map<number, DeptVO>()
  const roots: DeptVO[] = []
  // 先建索引
  list.forEach((item) => {
    map.set(item.id, { ...item, children: [] })
  })
  // 再建树
  list.forEach((item) => {
    const node = map.get(item.id)!
    if (item.parentId && map.has(item.parentId)) {
      map.get(item.parentId)!.children!.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

/** 格式化时间 */
const formatTime = (time: any): string => {
  if (!time) return ''
  const d = new Date(time)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// ============ 部门树 ============
const deptTreeRef = ref()
const deptTree = ref<DeptVO[]>([])
const deptKeyword = ref('')
const currentDeptId = ref<number>()
const currentDeptName = ref('')

/** 加载部门树 */
const getDeptTree = async () => {
  const data = await DeptApi.getSimpleDeptList()
  deptTree.value = buildDeptTree(data)
}

/** 刷新部门树 */
const refreshDeptTree = () => {
  getDeptTree()
}

/** 点击部门节点 */
const handleDeptClick = (data: DeptVO) => {
  currentDeptId.value = data.id
  currentDeptName.value = data.name
  getList()
}

// ============ 成员列表 ============
const loading = ref(false)
const list = ref<DeptMember[]>([])

/** 搜索过滤 */
const filteredlist = computed(() => {
  if (!deptKeyword.value) return list.value
  const kw = deptKeyword.value.toLowerCase()
  return list.value.filter(
    (item) =>
      item.nickname?.toLowerCase().includes(kw) || String(item.userId).includes(kw)
  )
})

/** 获取成员列表 */
const getList = async () => {
  if (!currentDeptId.value) return
  loading.value = true
  try {
    list.value = await UserDeptApi.getListByDept(currentDeptId.value)
  } finally {
    loading.value = false
  }
}

/** 设置角色 */
const handleSetRole = async (row: DeptMember, role: number) => {
  try {
    await message.confirm(
      `确认将「${row.nickname || '用户' + row.userId}」设为${role === 1 ? '管理员' : '成员'}？`
    )
    await UserDeptApi.setRole(row.userId, row.deptId, role)
    message.success(t('common.updateSuccess'))
    await getList()
  } catch {}
}

/** 移除角色记录 */
const handleRemove = async (row: DeptMember) => {
  try {
    await message.confirm(
      `确认移除「${row.nickname || '用户' + row.userId}」的角色记录？\n移除后该用户仍保留在系统部门中，但不再拥有知识库管理权限。`
    )
    await UserDeptApi.remove(row.userId, row.deptId)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 初始化 */
onMounted(() => {
  getDeptTree()
})
</script>

<style scoped lang="scss">
.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  font-size: 14px;
}
</style>
