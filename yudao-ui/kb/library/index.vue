<template>
  <el-alert
    class="mb-15px"
    type="info"
    :closable="false"
    title="知识库管理 — 管理所有知识库的基础信息。每类知识库的可见性和管理权限由关联的层级配置决定：个人库仅创建者可见，部门库按归属部门控制，院级库由院管理员管理，公司级库全员可见。项目成果库需额外配置项目成员才能访问文档内容。"
  />
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="90px"
    >
      <el-form-item label="知识库名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入知识库名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="所属分类" prop="categoryId">
        <el-select
          v-model="queryParams.categoryId"
          placeholder="请选择分类"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="cat in categoryList"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          class="!w-240px"
        >
          <el-option label="启用" :value="0" />
          <el-option label="禁用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['kb:library:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['kb:library:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
        <el-button
            type="danger"
            plain
            :disabled="isEmpty(checkedIds)"
            @click="handleDeleteBatch"
            v-hasPermi="['kb:library:delete']"
        >
          <Icon icon="ep:delete" class="mr-5px" /> 批量删除
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table
        row-key="id"
        v-loading="loading"
        :data="list"
        :stripe="true"
        :show-overflow-tooltip="true"
        @selection-change="handleRowCheckboxChange"
    >
    <el-table-column type="selection" width="55" />
      <el-table-column label="ID" align="center" prop="id" width="80px" />
      <el-table-column label="知识库名称" align="center" prop="name" />
      <el-table-column label="所属分类" align="center" width="140px">
        <template #default="scope">
          {{ categoryMap[scope.row.categoryId] || '未知分类' }}
        </template>
      </el-table-column>
      <el-table-column label="层级配置" align="center" width="120px">
        <template #default="scope">
          {{ levelConfigMap[scope.row.kbLevelId] || '未配置' }}
        </template>
      </el-table-column>
      <el-table-column label="所有者" align="center" width="120px">
        <template #default="scope">
          {{ ownerNameMap[scope.row.ownerId] || scope.row.ownerId || '无' }}
        </template>
      </el-table-column>
      <el-table-column label="描述" align="center" prop="description" min-width="200px" show-overflow-tooltip />
      <el-table-column label="文档数" align="center" prop="docCount" width="80px" />
      <el-table-column label="公开" align="center" width="80px">
        <template #default="scope">
          <el-tag :type="scope.row.isPublic === 1 ? 'success' : 'info'" size="small">
            {{ scope.row.isPublic === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80px">
        <template #default="scope">
          <el-tag :type="scope.row.status === 0 ? 'success' : 'danger'">
            {{ scope.row.status === 0 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180px"
      />
      <el-table-column label="操作" align="center" min-width="120px">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['kb:library:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['kb:library:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <LibraryForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { isEmpty } from '@/utils/is'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { LibraryApi, Library } from '@/api/kb/library'
import LibraryForm from './LibraryForm.vue'

/** 知识库 列表 */
defineOptions({ name: 'Library' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const list = ref<Library[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  categoryId: undefined,
  status: undefined,
  createTime: []
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中

// ========== 名称映射 ==========
const categoryList = ref<any[]>([])
const categoryMap = ref<Record<number, string>>({})
const levelConfigMap = ref<Record<number, string>>({})
const ownerNameMap = ref<Record<number, string>>({})

/** 加载名称映射数据 */
const loadNameMaps = async () => {
  try {
    const [categories, levelConfigs, depts] = await Promise.all([
      LibraryApi.getCategoryList(),
      LibraryApi.getLevelConfigSimpleList(),
      LibraryApi.getDeptSimpleList()
    ])

    // 分类映射
    categoryList.value = categories
    const cMap: Record<number, string> = {}
    categories.forEach(c => { cMap[c.id] = c.name })
    categoryMap.value = cMap

    // 层级配置映射
    const lcMap: Record<number, string> = {}
    levelConfigs.forEach(lc => { lcMap[lc.id] = lc.levelName })
    levelConfigMap.value = lcMap

    // 部门映射（所有者为部门时使用）
    const dMap: Record<number, string> = {}
    const flattenDepts = (items: any[]) => {
      items.forEach(d => {
        dMap[d.id] = d.name
        if (d.children) flattenDepts(d.children)
      })
    }
    flattenDepts(depts)
    ownerNameMap.value = dMap
  } catch (e) {
    console.error('加载名称映射失败', e)
  }
}

/** 更新所有者名称映射（从列表数据中提取用户所有者） */
const updateOwnerNameMap = (rows: Library[]) => {
  rows.forEach(row => {
    if (row.ownerId && !ownerNameMap.value[row.ownerId]) {
      // 暂时用 ID 占位，后续可通过搜索用户接口补充
      ownerNameMap.value[row.ownerId] = String(row.ownerId)
    }
  })
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await LibraryApi.getLibraryPage(queryParams)
    list.value = data.list
    total.value = data.total
    updateOwnerNameMap(data.list)
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await LibraryApi.deleteLibrary(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 批量删除知识库 */
const handleDeleteBatch = async () => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    await LibraryApi.deleteLibraryList(checkedIds.value);
    checkedIds.value = [];
    message.success(t('common.delSuccess'))
    await getList();
  } catch {}
}

const checkedIds = ref<number[]>([])
const handleRowCheckboxChange = (records: Library[]) => {
  checkedIds.value = records.map((item) => item.id!);
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await LibraryApi.exportLibrary(queryParams)
    download.excel(data, '知识库.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(() => {
  loadNameMaps()
  getList()
})
</script>