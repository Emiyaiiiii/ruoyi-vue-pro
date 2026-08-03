import request from '@/config/axios'
import type { Dayjs } from 'dayjs';

/** 知识库层级配置信息 */
export interface LevelConfig {
          id: number; // 主键ID
          levelCode?: string; // 层级编码
          levelName?: string; // 层级名称
          visibilityRule?: number; // 可见规则: 1=按所有者, 2=按归属部门, 3=全员, 5=指定部门列表
          ownerDim: number; // 归属维度: 0=无, 1=用户, 2=部门
          deptScope: string; // 分类可见部门范围: NULL=全员可见, JSON数组[101,102]=仅指定部门
          sort: number; // 排序
          status: number; // 状态: 0=启用, 1=禁用
  }

// 知识库层级配置 API
export const LevelConfigApi = {
  // 查询知识库层级配置分页
  getLevelConfigPage: async (params: any) => {
    return await request.get({ url: `/kb/level-config/page`, params })
  },

  // 查询知识库层级配置详情
  getLevelConfig: async (id: number) => {
    return await request.get({ url: `/kb/level-config/get?id=` + id })
  },

  // 新增知识库层级配置
  createLevelConfig: async (data: LevelConfig) => {
    return await request.post({ url: `/kb/level-config/create`, data })
  },

  // 修改知识库层级配置
  updateLevelConfig: async (data: LevelConfig) => {
    return await request.put({ url: `/kb/level-config/update`, data })
  },

  // 删除知识库层级配置
  deleteLevelConfig: async (id: number) => {
    return await request.delete({ url: `/kb/level-config/delete?id=` + id })
  },

  /** 批量删除知识库层级配置 */
  deleteLevelConfigList: async (ids: number[]) => {
    return await request.delete({ url: `/kb/level-config/delete-list?ids=${ids.join(',')}` })
  },

  // 导出知识库层级配置 Excel
  exportLevelConfig: async (params) => {
    return await request.download({ url: `/kb/level-config/export-excel`, params })
  }
}