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
        项目成员管理 — 管理项目成果库的文档访问成员。项目成果库的内容（文档）仅对已添加的项目成员可见，非成员无法查看文档详情，实现项目级别的细粒度内容访问控制。
      </template>
    </el-alert>
    <ContentWrap>
    <!-- 知识库选择栏 -->
    <el-form :inline="true" class="mb-15px">
      <el-form-item label="项目成果库">
        <el-select
          v-model="currentKbId"
          placeholder="请选择项目成果库"
          filterable
          clearable
          class="!w-300px"
          @change="handleKbChange"
        >
          <el-option
            v-for="item in projectLibraries"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          plain
          :disabled="!currentKbId"
          @click="openAddDialog"
          v-hasPermi="['kb:project-member:update']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 添加成员
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 成员列表 -->
  <ContentWrap>
    <el-alert
      v-if="!currentKbId"
      title="请在上方选择一个项目成果库"
      type="info"
      :closable="false"
      class="mb-15px"
    />

    <el-table
      v-loading="loading"
      :data="list"
      :stripe="true"
      :show-overflow-tooltip="true"
      empty-text="暂无项目成员"
    >
      <el-table-column label="用户昵称" align="center" prop="nickname" min-width="120px">
        <template #default="{ row }">
          <span>{{ row.nickname || `用户${row.userId}` }}</span>
        </template>
      </el-table-column>
      <el-table-column label="用户ID" align="center" prop="userId" width="100px" />
      <el-table-column
        label="加入时间"
        align="center"
        prop="createTime"
        width="180px"
      >
        <template #default="{ row }">
          {{ row.createTime ? formatTime(row.createTime) : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" min-width="120px">
        <template #default="{ row }">
          <el-button
            link
            type="danger"
            @click="handleRemove(row)"
            v-hasPermi="['kb:project-member:delete']"
          >
            移除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <!-- 添加成员弹窗 -->
  <el-dialog v-model="addDialogVisible" title="添加项目成员" width="500px">
    <el-form :inline="true" class="mb-15px">
      <el-form-item>
        <el-input
          v-model="searchKeyword"
          placeholder="输入用户昵称搜索"
          clearable
          class="!w-260px"
          @keyup.enter="searchUsers"
        >
          <template #prefix>
            <Icon icon="ep:search" />
          </template>
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="searchUsers">搜索</el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-loading="searchLoading"
      :data="searchResults"
      :stripe="true"
      max-height="360px"
      empty-text="请搜索用户"
    >
      <el-table-column label="用户昵称" align="center" prop="nickname" min-width="120px" />
      <el-table-column label="用户ID" align="center" prop="id" width="100px" />
      <el-table-column label="操作" align="center" width="100px">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :disabled="isExistingMember(row.id)"
            @click="handleAdd(row)"
          >
            {{ isExistingMember(row.id) ? '已添加' : '添加' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
  </div>
</template>

<script setup lang="ts">
import request from '@/config/axios'
import { ProjectMemberApi, ProjectMember } from '@/api/kb/projectmember'
import { LibraryApi, Library } from '@/api/kb/library'

/** 项目成员管理 */
defineOptions({ name: 'KbProjectMember' })

const message = useMessage()
const { t } = useI18n()

/** 格式化时间 */
const formatTime = (time: any): string => {
  if (!time) return ''
  const d = new Date(time)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// ============ 知识库选择 ============
const currentKbId = ref<number>()
const projectLibraries = ref<any[]>([])

/** 加载项目成果库列表 */
const getProjectLibraries = async () => {
  const data = await LibraryApi.getLibraryPage({
    pageNo: 1,
    pageSize: 100,
    isProject: 1
  })
  projectLibraries.value = data.list || []
}

/** 切换知识库 */
const handleKbChange = () => {
  getList()
}

// ============ 成员列表 ============
const loading = ref(false)
const list = ref<ProjectMember[]>([])

/** 获取成员列表 */
const getList = async () => {
  if (!currentKbId.value) {
    list.value = []
    return
  }
  loading.value = true
  try {
    list.value = await ProjectMemberApi.getList(currentKbId.value)
  } finally {
    loading.value = false
  }
}

/** 移除成员 */
const handleRemove = async (row: ProjectMember) => {
  try {
    await message.confirm(
      `确认移除「${row.nickname || '用户' + row.userId}」的项目成员资格？\n移除后该用户将无法访问此项目成果库的文档内容。`
    )
    await ProjectMemberApi.removeMember(row.kbId, row.userId)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 检查是否已是成员 */
const isExistingMember = (userId: number): boolean => {
  return list.value.some((item) => item.userId === userId)
}

// ============ 添加成员弹窗 ============
const addDialogVisible = ref(false)
const searchKeyword = ref('')
const searchLoading = ref(false)
const searchResults = ref<any[]>([])

/** 打开添加弹窗 */
const openAddDialog = () => {
  addDialogVisible.value = true
  searchKeyword.value = ''
  searchResults.value = []
}

/** 搜索系统用户 */
const searchUsers = async () => {
  if (!searchKeyword.value.trim()) {
    message.warning('请输入用户昵称关键词')
    return
  }
  searchLoading.value = true
  try {
    const data = await request.get({
      url: `/system/user/page`,
      params: { pageNo: 1, pageSize: 50, nickname: searchKeyword.value }
    })
    searchResults.value = data.list || []
  } finally {
    searchLoading.value = false
  }
}

/** 添加成员 */
const handleAdd = async (row: any) => {
  try {
    await ProjectMemberApi.addMember(currentKbId.value!, row.id)
    message.success(t('common.createSuccess'))
    await getList()
  } catch {}
}

/** 初始化 */
onMounted(() => {
  getProjectLibraries()
})
</script>
