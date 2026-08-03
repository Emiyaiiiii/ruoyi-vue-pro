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
        文档管理 — 管理知识库中的文档文件。上传文件后自动存储到芋道文件管理系统，并在知识库中创建文档记录。项目成果库的文档仅对项目成员可见。
      </template>
    </el-alert>

    <!-- 知识库选择 + 搜索栏 -->
    <ContentWrap>
      <el-form
        class="-mb-15px"
        :model="queryParams"
        ref="queryFormRef"
        :inline="true"
        label-width="90px"
      >
        <el-form-item label="知识库" prop="kbId">
          <el-select
            v-model="queryParams.kbId"
            placeholder="请选择知识库"
            filterable
            clearable
            class="!w-240px"
            @change="handleQuery"
          >
            <el-option
              v-for="item in libraryList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="文件名称" prop="fileName">
          <el-input
            v-model="queryParams.fileName"
            placeholder="请输入文件名称"
            clearable
            @keyup.enter="handleQuery"
            class="!w-240px"
          />
        </el-form-item>
        <el-form-item label="文件类型" prop="fileType">
          <el-input
            v-model="queryParams.fileType"
            placeholder="如 pdf/docx"
            clearable
            @keyup.enter="handleQuery"
            class="!w-180px"
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
          <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
          <el-button
            type="primary"
            plain
            :disabled="!queryParams.kbId"
            @click="openUploadDialog"
            v-hasPermi="['kb:document:create']"
          >
            <Icon icon="ep:upload" class="mr-5px" /> 上传文件
          </el-button>
          <el-button
            type="success"
            plain
            @click="handleExport"
            :loading="exportLoading"
            v-hasPermi="['kb:document:export']"
          >
            <Icon icon="ep:download" class="mr-5px" /> 导出
          </el-button>
          <el-button
            type="danger"
            plain
            :disabled="isEmpty(checkedIds)"
            @click="handleDeleteBatch"
            v-hasPermi="['kb:document:delete']"
          >
            <Icon icon="ep:delete" class="mr-5px" /> 批量删除
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <!-- 列表 -->
    <ContentWrap>
      <el-alert
        v-if="!queryParams.kbId"
        title="请在上方选择一个知识库"
        type="info"
        :closable="false"
        class="mb-15px"
      />
      <el-table
        row-key="id"
        v-loading="loading"
        :data="list"
        :stripe="true"
        :show-overflow-tooltip="true"
        empty-text="暂无文档"
        @selection-change="handleRowCheckboxChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="ID" align="center" prop="id" width="80px" />
        <el-table-column label="文件名称" align="center" prop="fileName" min-width="200px" show-overflow-tooltip />
        <el-table-column label="文件类型" align="center" prop="fileType" width="100px">
          <template #default="{ row }">
            <el-tag size="small" :type="getFileTagType(row.fileType)">
              {{ row.fileType || '—' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文件大小" align="center" prop="fileSize" width="120px">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="下载次数" align="center" prop="downloadCount" width="100px" />
        <el-table-column label="查看次数" align="center" prop="viewCount" width="100px" />
        <el-table-column label="标签" align="center" prop="tags" width="150px" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.tags">
              <el-tag
                v-for="tag in row.tags.split(',')"
                :key="tag"
                size="small"
                class="mr-3px"
              >
                {{ tag.trim() }}
              </el-tag>
            </span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status" width="80px">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="上传时间"
          align="center"
          prop="createTime"
          :formatter="dateFormatter"
          width="180px"
        />
        <el-table-column label="操作" align="center" min-width="160px" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="handleDownload(row)"
            >
              下载
            </el-button>
            <el-button
              link
              type="primary"
              @click="handleEdit(row)"
              v-hasPermi="['kb:document:update']"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              @click="handleDelete(row.id)"
              v-hasPermi="['kb:document:delete']"
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

    <!-- 上传文件弹窗 -->
    <el-dialog v-model="uploadDialogVisible" title="上传文件到知识库" width="600px">
      <el-form :model="uploadForm" label-width="100px" ref="uploadFormRef">
        <el-form-item label="目标知识库">
          <el-tag type="info">{{ selectedLibraryName }}</el-tag>
        </el-form-item>
        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-exceed="handleExceed"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            drag
            accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.jpg,.jpeg,.png,.gif,.zip,.rar"
          >
            <Icon icon="ep:upload-filled" class="el-icon--upload" />
            <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">
                支持 PDF、Word、Excel、PPT、图片、压缩包等格式
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="文件描述">
          <el-input
            v-model="uploadForm.description"
            type="textarea"
            :rows="2"
            placeholder="请输入文件描述（可选）"
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-input
            v-model="uploadForm.tags"
            placeholder="多个标签用逗号分隔（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取 消</el-button>
        <el-button
          type="primary"
          :loading="uploading"
          :disabled="!uploadForm.file"
          @click="handleUpload"
        >
          确认上传
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑文件弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑文档信息" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="文件名称">
          <el-input v-model="editForm.fileName" placeholder="请输入文件名称" />
        </el-form-item>
        <el-form-item label="文件描述">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="2"
            placeholder="请输入文件描述"
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="editForm.tags" placeholder="多个标签用逗号分隔" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEditSubmit">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { isEmpty } from '@/utils/is'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { DocumentApi, Document } from '@/api/kb/document'
import { LibraryApi, Library } from '@/api/kb/library'

/** 知识库文档管理 */
defineOptions({ name: 'KbDocument' })

const message = useMessage()
const { t } = useI18n()

// ============ 知识库列表 ============
const libraryList = ref<Library[]>([])

/** 加载知识库列表 */
const getLibraryList = async () => {
  const data = await LibraryApi.getLibraryPage({ pageNo: 1, pageSize: 100 })
  libraryList.value = data.list || []
}

/** 当前选择的知识库名称 */
const selectedLibraryName = computed(() => {
  const lib = libraryList.value.find((item) => item.id === queryParams.kbId)
  return lib?.name || ''
})

// ============ 文档列表 ============
const loading = ref(false)
const list = ref<Document[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  kbId: undefined as number | undefined,
  fileName: undefined as string | undefined,
  fileType: undefined as string | undefined
})
const queryFormRef = ref()
const exportLoading = ref(false)
const checkedIds = ref<number[]>([])

/** 查询列表 */
const getList = async () => {
  if (!queryParams.kbId) {
    list.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const data = await DocumentApi.getDocumentPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

/** 行选中 */
const handleRowCheckboxChange = (records: Document[]) => {
  checkedIds.value = records.map((item) => item.id!)
}

// ============ 文件大小格式化 ============
const formatFileSize = (size: number): string => {
  if (!size) return '—'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(1) + ' MB'
  return (size / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}

/** 文件类型标签颜色 */
const getFileTagType = (type: string): string => {
  if (!type) return 'info'
  const t = type.toLowerCase()
  if (['pdf'].includes(t)) return 'danger'
  if (['doc', 'docx'].includes(t)) return 'primary'
  if (['xls', 'xlsx'].includes(t)) return 'success'
  if (['ppt', 'pptx'].includes(t)) return 'warning'
  if (['jpg', 'jpeg', 'png', 'gif'].includes(t)) return 'info'
  return 'info'
}

// ============ 下载 ============
const handleDownload = (row: Document) => {
  if (row.fileUrl) {
    window.open(row.fileUrl, '_blank')
  }
}

// ============ 删除 ============
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await DocumentApi.deleteDocument(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 批量删除 */
const handleDeleteBatch = async () => {
  try {
    await message.delConfirm()
    await DocumentApi.deleteDocumentList(checkedIds.value)
    checkedIds.value = []
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

// ============ 导出 ============
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await DocumentApi.exportDocument(queryParams)
    download.excel(data, '知识库文件.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

// ============ 上传文件 ============
const uploadDialogVisible = ref(false)
const uploading = ref(false)
const uploadRef = ref()
const uploadForm = reactive({
  file: null as File | null,
  description: '',
  tags: ''
})

/** 打开上传弹窗 */
const openUploadDialog = () => {
  uploadDialogVisible.value = true
  uploadForm.file = null
  uploadForm.description = ''
  uploadForm.tags = ''
  nextTick(() => {
    uploadRef.value?.clearFiles()
  })
}

/** 文件选择变化 */
const handleFileChange = (file: any) => {
  uploadForm.file = file.raw
}

/** 文件移除 */
const handleFileRemove = () => {
  uploadForm.file = null
}

/** 超出限制 */
const handleExceed = () => {
  message.warning('只能上传一个文件，请先移除已选文件')
}

/** 确认上传 */
const handleUpload = async () => {
  if (!uploadForm.file) {
    message.warning('请先选择文件')
    return
  }
  if (!queryParams.kbId) {
    message.warning('请先选择知识库')
    return
  }

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadForm.file)
    formData.append('kbId', String(queryParams.kbId))
    if (uploadForm.description) {
      formData.append('description', uploadForm.description)
    }
    if (uploadForm.tags) {
      formData.append('tags', uploadForm.tags)
    }
    await DocumentApi.uploadDocument(formData)
    message.success('文件上传成功')
    uploadDialogVisible.value = false
    await getList()
  } catch {
  } finally {
    uploading.value = false
  }
}

// ============ 编辑文档 ============
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive({
  id: undefined as number | undefined,
  fileName: '',
  description: '',
  tags: '',
  status: 0
})

/** 打开编辑弹窗 */
const handleEdit = (row: Document) => {
  editForm.id = row.id
  editForm.fileName = row.fileName
  editForm.description = row.description || ''
  editForm.tags = row.tags || ''
  editForm.status = row.status
  editDialogVisible.value = true
}

/** 提交编辑 */
const handleEditSubmit = async () => {
  editLoading.value = true
  try {
    await DocumentApi.updateDocument({
      id: editForm.id,
      kbId: queryParams.kbId!,
      fileName: editForm.fileName,
      fileUrl: '',
      fileType: '',
      fileSize: 0,
      description: editForm.description,
      tags: editForm.tags,
      status: editForm.status,
      downloadCount: 0,
      viewCount: 0
    } as Document)
    message.success(t('common.updateSuccess'))
    editDialogVisible.value = false
    await getList()
  } catch {
  } finally {
    editLoading.value = false
  }
}

/** 初始化 */
onMounted(() => {
  getLibraryList()
})
</script>
