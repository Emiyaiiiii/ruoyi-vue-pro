<script lang="ts" setup>
import type { CategoryApi } from '#/api/kb/category';

import { ref, h, reactive, onMounted, nextTick } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { getDictOptions } from '@vben/hooks';
import { useTableToolbar, VbenVxeTableToolbar } from '@vben/plugins/vxe-table';
import { cloneDeep, downloadFileFromBlobPart, formatDateTime, isEmpty } from '@vben/utils';
import { Alert, Button, Card, message, Tabs, Pagination, Form, RangePicker, DatePicker, Select, Input, Tag } from 'antdv-next';
import CategoryForm from './modules/form.vue';
import { Download, Plus, RefreshCw, Search, Trash2 } from '@vben/icons';
import { DictTag } from '#/components/dict-tag';
import { VxeColumn, VxeTable } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils/rangePickerProps';


import { $t } from '#/locales';
import { getCategoryPage, deleteCategory, deleteCategoryList, exportCategory } from '#/api/kb/category';


const loading = ref(true) // 列表的加载中
const list = ref<CategoryApi.Category[]>([]) // 列表的数据

const total = ref(0) // 列表的总页数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
                name: undefined,
                kbLevelId: undefined,
                sort: undefined,
                status: undefined,
                createTime: undefined,
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中

/** 查询列表 */
async function getList() {
  loading.value = true
  try {
    const params = cloneDeep(queryParams) as any;
                if (params.createTime && Array.isArray(params.createTime)) {
                  params.createTime = (params.createTime as string[]).join(',');
                }
              const data = await getCategoryPage(params)
        list.value = data.list
        total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  queryFormRef.value.resetFields()
  handleQuery()
}

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: CategoryForm,
  destroyOnClose: true,
});

/** 创建知识库分类 */
function handleCreate() {
  formModalApi.setData(null).open();
}

/** 编辑知识库分类 */
function handleEdit(row: CategoryApi.Category) {
  formModalApi.setData(row).open();
}


/** 删除知识库分类 */
async function handleDelete(row: CategoryApi.Category) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.id]),
    duration: 0,
  });
  try {
    await deleteCategory(row.id!);
    message.success($t('ui.actionMessage.deleteSuccess', [row.id]));
    await getList();
  } finally {
    hideLoading();
  }
}

/** 批量删除知识库分类 */
async function handleDeleteBatch() {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting'),
    duration: 0,
  });
  try {
    await deleteCategoryList(checkedIds.value);
    checkedIds.value = [];
    message.success($t('ui.actionMessage.deleteSuccess'));
    await getList();
  } finally {
    hideLoading();
  }
}

const checkedIds = ref<number[]>([])
function handleRowCheckboxChange({
  records,
}: {
  records: CategoryApi.Category[];
}) {
  checkedIds.value = records.map((item) => item.id!);
}

/** 导出表格 */
async function handleExport() {
try {
  exportLoading.value = true;
  const data = await exportCategory(queryParams);
  downloadFileFromBlobPart({ fileName: '知识库分类.xls', source: data });
}finally {
  exportLoading.value = false;
}
}



/** 初始化 */
const { hiddenSearchBar, tableToolbarRef, tableRef } = useTableToolbar();
onMounted(() => {
  getList();
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="getList" />

    <Alert
      type="info"
      show-icon
      banner
      message="分类管理 — 管理知识库的分类体系。分类用于组织知识库的层级结构，配合层级配置共同控制知识库的可见范围和管理权限。"
    />

    <Card v-if="!hiddenSearchBar" class="mb-4">
      <!-- 搜索工作栏 -->
      <Form
          :model="queryParams"
          ref="queryFormRef"
          layout="inline"
      >
                    <Form.Item label="分类名称" name="name">
                      <Input
                          v-model:value="queryParams.name"
                          placeholder="请输入分类名称"
                          allowClear
                          @pressEnter="handleQuery"
                           class="w-full"
                      />
                    </Form.Item>
                    <Form.Item label="层级配置ID" name="kbLevelId">
                      <Input
                          v-model:value="queryParams.kbLevelId"
                          placeholder="请输入层级配置ID"
                          allowClear
                          @pressEnter="handleQuery"
                           class="w-full"
                      />
                    </Form.Item>
                    <Form.Item label="排序" name="sort">
                      <Input
                          v-model:value="queryParams.sort"
                          placeholder="请输入排序"
                          allowClear
                          @pressEnter="handleQuery"
                           class="w-full"
                      />
                    </Form.Item>
                    <Form.Item label="状态" name="status">
                      <Select
                          v-model:value="queryParams.status"
                          placeholder="请选择状态"
                          allowClear
                           class="w-full"
                      >
                            <SelectOption label="启用" :value="0" />
                            <SelectOption label="禁用" :value="1" />
                      </Select>
                    </Form.Item>
                        <Form.Item label="创建时间" name="createTime">
                          <RangePicker
                              v-model:value="queryParams.createTime"
                              v-bind="getRangePickerDefaultProps()"
                              class="w-full"
                          />
                        </Form.Item>
        <Form.Item>
          <Button class="ml-2" @click="resetQuery"> 重置 </Button>
          <Button class="ml-2" @click="handleQuery" type="primary">
            搜索
          </Button>
        </Form.Item>
      </Form>
    </Card>

    <!-- 列表 -->
    <Card title="知识库分类">
      <template #extra>
        <VbenVxeTableToolbar
            ref="tableToolbarRef"
            v-model:hidden-search="hiddenSearchBar"
        >
          <Button
              class="ml-2"
              :icon="h(Plus)"
              type="primary"
              @click="handleCreate"
              v-access:code="['kb:category:create']"
          >
            {{ $t('ui.actionTitle.create', ['知识库分类']) }}
          </Button>
          <Button
              :icon="h(Download)"
              type="primary"
              class="ml-2"
              :loading="exportLoading"
              @click="handleExport"
              v-access:code="['kb:category:export']"
          >
            {{ $t('ui.actionTitle.export') }}
          </Button>
          <Button
              :icon="h(Trash2)"
              type="primary"
              danger
              class="ml-2"
              :disabled="isEmpty(checkedIds)"
              @click="handleDeleteBatch"
              v-access:code="['kb:category:delete']"
          >
            批量删除
          </Button>
        </VbenVxeTableToolbar>
      </template>
      <VxeTable
          ref="tableRef"
          :data="list"
          show-overflow
          :loading="loading"
          @checkboxAll="handleRowCheckboxChange"
          @checkboxChange="handleRowCheckboxChange"
      >
        <VxeColumn type="checkbox" width="40" />
                              <VxeColumn field="id" title="ID" align="center" width="80" />
                    <VxeColumn field="name" title="分类名称" align="center" />
                    <VxeColumn field="kbLevelId" title="层级配置ID" align="center" />
                    <VxeColumn field="sort" title="排序" align="center" />
                    <VxeColumn field="status" title="状态" align="center">
                      <template #default="{row}">
                        <Tag :color="row.status === 0 ? 'green' : 'red'">
                          {{ row.status === 0 ? '启用' : '禁用' }}
                        </Tag>
                      </template>
                    </VxeColumn>
                    <VxeColumn field="createTime" title="创建时间" align="center">
                      <template #default="{row}">
                        {{formatDateTime(row.createTime)}}
                      </template>
                    </VxeColumn>
        <VxeColumn field="operation" title="操作" align="center">
          <template #default="{row}">
            <Button
                size="small"
                type="link"
                @click="handleEdit(row)"
                v-access:code="['kb:category:update']"
            >
              {{ $t('ui.actionTitle.edit') }}
            </Button>
            <Button
                size="small"
                type="link"
                danger
                class="ml-2"
                @click="handleDelete(row)"
                v-access:code="['kb:category:delete']"
            >
              {{ $t('ui.actionTitle.delete') }}
            </Button>
          </template>
        </VxeColumn>
      </VxeTable>
      <!-- 分页 -->
      <div class="mt-2 flex justify-end">
        <Pagination
            :total="total"
            v-model:current="queryParams.pageNo"
            v-model:page-size="queryParams.pageSize"
            show-size-changer
            @change="getList"
        />
      </div>
    </Card>
  </Page>
</template>
