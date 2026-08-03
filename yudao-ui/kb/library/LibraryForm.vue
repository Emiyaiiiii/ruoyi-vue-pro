<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="formLoading"
    >
      <el-form-item label="知识库名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入知识库名称" />
      </el-form-item>

      <!-- 所属分类：选择分类后自动确定层级配置 -->
      <el-form-item label="所属分类" prop="categoryId">
        <el-select
          v-model="formData.categoryId"
          placeholder="请选择分类"
          filterable
          style="width: 100%"
          @change="handleCategoryChange"
        >
          <el-option
            v-for="cat in categoryList"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          />
        </el-select>
      </el-form-item>

      <!-- 层级配置：自动带出，只读展示 -->
      <el-form-item label="层级配置" prop="kbLevelId">
        <el-input
          :model-value="levelConfigName"
          placeholder="选择分类后自动带出"
          :disabled="true"
        />
      </el-form-item>

      <!-- 所有者：根据层级配置的 ownerDim 动态切换 -->
      <el-form-item
        v-if="ownerDim === 1"
        label="所有者（用户）"
        prop="ownerId"
      >
        <el-select
          v-model="formData.ownerId"
          placeholder="搜索用户昵称"
          filterable
          remote
          reserve-keyword
          :remote-method="searchUser"
          :loading="userSearchLoading"
          style="width: 100%"
          clearable
        >
          <el-option
            v-for="user in userList"
            :key="user.id"
            :label="user.nickname + (user.deptName ? ' (' + user.deptName + ')' : '')"
            :value="user.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        v-else-if="ownerDim === 2"
        label="所有者（部门）"
        prop="ownerId"
      >
        <el-select
          v-model="formData.ownerId"
          placeholder="请选择部门"
          filterable
          style="width: 100%"
          clearable
        >
          <el-option
            v-for="dept in deptList"
            :key="dept.id"
            :label="dept.name"
            :value="dept.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="描述" prop="description">
        <Editor v-model="formData.description" height="150px" />
      </el-form-item>
      <el-form-item label="封面图片URL" prop="coverUrl">
        <el-input v-model="formData.coverUrl" placeholder="请输入封面图片URL" />
      </el-form-item>
      <el-form-item label="公开到广场" prop="isPublic">
        <el-radio-group v-model="formData.isPublic">
          <el-radio :value="0">不公开</el-radio>
          <el-radio :value="1">公开</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="项目成果库" prop="isProject">
        <el-radio-group v-model="formData.isProject">
          <el-radio :value="0">否</el-radio>
          <el-radio :value="1">是</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :value="0">启用</el-radio>
          <el-radio :value="1">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { LibraryApi, CategorySimple, LevelConfigSimple, UserSimpleVO } from '@/api/kb/library'

/** 知识库 表单 */
defineOptions({ name: 'LibraryForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  name: undefined,
  categoryId: undefined,
  kbLevelId: undefined,
  ownerId: undefined,
  description: undefined,
  coverUrl: undefined,
  docCount: undefined,
  status: 0,
  isPublic: 0,
  isProject: 0
})
const formRules = reactive({
  name: [{ required: true, message: '知识库名称不能为空', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择所属分类', trigger: 'change' }]
})
const formRef = ref() // 表单 Ref

// ========== 分类选择 ==========
const categoryList = ref<CategorySimple[]>([])
const levelConfigMap = ref<Record<number, LevelConfigSimple>>({})

/** 当前层级配置的 ownerDim：0=无, 1=用户, 2=部门 */
const ownerDim = ref(0)
/** 层级配置名称（只读展示） */
const levelConfigName = ref('')

// ========== 用户搜索 ==========
const userList = ref<UserSimpleVO[]>([])
const userSearchLoading = ref(false)

// ========== 部门列表 ==========
const deptList = ref<any[]>([])

/** 加载分类列表和层级配置 */
const loadCategoryAndLevelConfig = async () => {
  try {
    // 并行加载分类和层级配置
    const [categories, levelConfigs] = await Promise.all([
      LibraryApi.getCategoryList(),
      LibraryApi.getLevelConfigSimpleList()
    ])
    categoryList.value = categories
    // 建立层级配置映射
    const map: Record<number, LevelConfigSimple> = {}
    levelConfigs.forEach(lc => { map[lc.id] = lc })
    levelConfigMap.value = map

    // 预加载部门列表
    deptList.value = await LibraryApi.getDeptSimpleList()
  } catch (e) {
    console.error('加载分类/层级配置失败', e)
  }
}

/** 分类变更：自动更新层级配置 */
const handleCategoryChange = (categoryId: number) => {
  const cat = categoryList.value.find(c => c.id === categoryId)
  if (cat && cat.kbLevelId) {
    formData.value.kbLevelId = cat.kbLevelId
    const lc = levelConfigMap.value[cat.kbLevelId]
    if (lc) {
      ownerDim.value = lc.ownerDim || 0
      levelConfigName.value = lc.levelName
    } else {
      ownerDim.value = 0
      levelConfigName.value = '已关联（ID: ' + cat.kbLevelId + '）'
    }
    // 切换 ownerDim 时清空之前选择的所有者
    formData.value.ownerId = undefined
  } else {
    formData.value.kbLevelId = undefined
    ownerDim.value = 0
    levelConfigName.value = ''
    formData.value.ownerId = undefined
  }
}

/** 远程搜索用户 */
const searchUser = async (query: string) => {
  if (!query || query.trim() === '') {
    userList.value = []
    return
  }
  userSearchLoading.value = true
  try {
    const res = await LibraryApi.searchUserByNickname(query.trim())
    userList.value = res || []
  } catch (e) {
    console.error('搜索用户失败', e)
    userList.value = []
  } finally {
    userSearchLoading.value = false
  }
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  // 加载分类和层级配置数据
  await loadCategoryAndLevelConfig()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      const data = await LibraryApi.getLibrary(id)
      formData.value = data
      // 根据已有数据恢复 ownerDim 和 levelConfigName
      if (data.kbLevelId) {
        const lc = levelConfigMap.value[data.kbLevelId]
        if (lc) {
          ownerDim.value = lc.ownerDim || 0
          levelConfigName.value = lc.levelName
        }
      }
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  await formRef.value.validate()
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value as any
    if (formType.value === 'create') {
      await LibraryApi.createLibrary(data)
      message.success(t('common.createSuccess'))
    } else {
      await LibraryApi.updateLibrary(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined,
    name: undefined,
    categoryId: undefined,
    kbLevelId: undefined,
    ownerId: undefined,
    description: undefined,
    coverUrl: undefined,
    docCount: undefined,
    status: 0,
    isPublic: 0,
    isProject: 0
  }
  ownerDim.value = 0
  levelConfigName.value = ''
  userList.value = []
  formRef.value?.resetFields()
}
</script>