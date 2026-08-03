<script lang="ts" setup>
import type { Rule } from 'antdv-next/es/form';
import type { CategoryApi } from '#/api/kb/category';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { getDictOptions } from '@vben/hooks';
import { Tinymce as RichTextarea } from '#/components/tinymce';
import { ImageUpload, FileUpload } from "#/components/upload";
import { message, Tabs, Form, Input, TextArea, Select, RadioGroup, Radio, CheckboxGroup, Checkbox, DatePicker, TreeSelect } from 'antdv-next';

import { $t } from '#/locales';
import { getCategory, createCategory, updateCategory } from '#/api/kb/category';

const emit = defineEmits(['success']);

const formRef = ref();
const formData = ref<Partial<CategoryApi.Category>>({
        id: undefined,
        name: undefined,
        kbLevelId: undefined,
        sort: undefined,
        status: undefined,
});
const rules: Record<string, Rule[]> = {
        name: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }],
};
const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['知识库分类'])
    : $t('ui.actionTitle.create', ['知识库分类']);
});


/** 重置表单 */
function resetForm() {
  formData.value = {
            id: undefined,
            name: undefined,
            kbLevelId: undefined,
            sort: undefined,
            status: undefined,
  };
  formRef.value?.resetFields();
}


const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    await formRef.value?.validate();
        modalApi.lock();
    // 提交表单
    const data = formData.value as CategoryApi.Category;
        try {
      await (formData.value?.id ? updateCategory(data) : createCategory(data));
      // 关闭并提示
      await modalApi.close();
      emit('success');
      message.success({
        content: $t('ui.actionMessage.operationSuccess'),
      });
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      resetForm()
      return;
    }
    // 加载数据
    let data = modalApi.getData<CategoryApi.Category>();
    if (!data) {
      return;
    }
    if (data.id) {
      modalApi.lock();
      try {
        data = await getCategory(data.id);
      } finally {
        modalApi.unlock();
      }
    }
    formData.value = data;
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <Form
      ref="formRef"
      :model="formData"
      :rules="rules"
      :label-col="{ span: 5 }"
      :wrapper-col="{ span: 18 }"
    >
            <Form.Item label="分类名称" name="name">
              <Input v-model:value="formData.name" placeholder="请输入分类名称" />
            </Form.Item>
            <Form.Item label="层级配置ID" name="kbLevelId">
              <Input v-model:value="formData.kbLevelId" placeholder="请输入层级配置ID" />
            </Form.Item>
            <Form.Item label="排序" name="sort">
              <Input v-model:value="formData.sort" placeholder="请输入排序" />
            </Form.Item>
            <Form.Item label="状态" name="status">
              <RadioGroup v-model:value="formData.status">
                <Radio :value="0">启用</Radio>
                <Radio :value="1">禁用</Radio>
              </RadioGroup>
            </Form.Item>
    </Form>
      </Modal>
</template>
