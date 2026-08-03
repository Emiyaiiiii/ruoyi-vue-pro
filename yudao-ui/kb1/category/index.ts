import type { PageParam, PageResult } from '@vben/request';
import type { Dayjs } from 'dayjs';

import { requestClient } from '#/api/request';

export namespace CategoryApi {
    /** 知识库分类信息 */
  export interface Category {
            id: number; // 主键ID
            name?: string; // 分类名称
            kbLevelId: number; // 关联层级配置ID
            sort: number; // 排序
            status: number; // 状态: 0=启用, 1=禁用
      }
}

/** 查询知识库分类分页 */
export function getCategoryPage(params: PageParam) {
  return requestClient.get<PageResult<CategoryApi.Category>>('/kb/category/page', { params });
}

/** 查询知识库分类详情 */
export function getCategory(id: number) {
  return requestClient.get<CategoryApi.Category>(`/kb/category/get?id=${id}`);
}

/** 新增知识库分类 */
export function createCategory(data: CategoryApi.Category) {
  return requestClient.post('/kb/category/create', data);
}

/** 修改知识库分类 */
export function updateCategory(data: CategoryApi.Category) {
  return requestClient.put('/kb/category/update', data);
}

/** 删除知识库分类 */
export function deleteCategory(id: number) {
  return requestClient.delete(`/kb/category/delete?id=${id}`);
}

/** 批量删除知识库分类 */
export function deleteCategoryList(ids: number[]) {
  return requestClient.delete(`/kb/category/delete-list?ids=${ids.join(',')}`)
}

/** 导出知识库分类 */
export function exportCategory(params: any) {
  return requestClient.download('/kb/category/export-excel', { params });
}

