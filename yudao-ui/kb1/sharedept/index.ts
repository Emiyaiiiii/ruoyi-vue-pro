import request from '@/config/axios'
import type { Dayjs } from 'dayjs';

/** 知识库共享部门关联信息 */
export interface ShareDept {
          id: number; // 主键ID
          kbId?: number; // 知识库ID
          deptId?: number; // 共享目标部门ID
  }

// 知识库共享部门关联 API
export const ShareDeptApi = {
  // 查询知识库共享部门关联分页
  getShareDeptPage: async (params: any) => {
    return await request.get({ url: `/kb/share-dept/page`, params })
  },

  // 查询知识库共享部门关联详情
  getShareDept: async (id: number) => {
    return await request.get({ url: `/kb/share-dept/get?id=` + id })
  },

  // 新增知识库共享部门关联
  createShareDept: async (data: ShareDept) => {
    return await request.post({ url: `/kb/share-dept/create`, data })
  },

  // 修改知识库共享部门关联
  updateShareDept: async (data: ShareDept) => {
    return await request.put({ url: `/kb/share-dept/update`, data })
  },

  // 删除知识库共享部门关联
  deleteShareDept: async (id: number) => {
    return await request.delete({ url: `/kb/share-dept/delete?id=` + id })
  },

  /** 批量删除知识库共享部门关联 */
  deleteShareDeptList: async (ids: number[]) => {
    return await request.delete({ url: `/kb/share-dept/delete-list?ids=${ids.join(',')}` })
  },

  // 导出知识库共享部门关联 Excel
  exportShareDept: async (params) => {
    return await request.download({ url: `/kb/share-dept/export-excel`, params })
  }
}