import request from '@/config/axios'
import type { Dayjs } from 'dayjs';

/** 知识库信息 */
export interface Library {
          id: number; // 主键ID
          name?: string; // 知识库名称
          categoryId?: number; // 分类ID
          kbLevelId: number; // 关联层级配置ID
          ownerId: number; // 所有者ID: 用户或部门, 取决于层级配置的owner_dim
          description: string; // 描述
          coverUrl: string; // 封面图片URL
          docCount: number; // 文档数量
          status: number; // 状态: 0=启用, 1=禁用
          isPublic: number; // 是否公开到广场: 0=否, 1=是
          isProject: number; // 是否项目成果库: 0=否, 1=是
  }

/** 知识库分类精简信息 */
export interface CategorySimple {
  id: number
  name: string
  kbLevelId: number
  parentId: number
  status: number
}

/** 层级配置精简信息 */
export interface LevelConfigSimple {
  id: number
  levelCode: string
  levelName: string
  visibilityRule: number
  ownerDim: number
}

/** 用户精简信息 */
export interface UserSimpleVO {
  id: number
  nickname: string
  avatar?: string
  sex?: number
  deptId?: number
  deptName?: string
}

// 知识库 API
export const LibraryApi = {
  // 查询知识库分页
  getLibraryPage: async (params: any) => {
    return await request.get({ url: `/kb/library/page`, params })
  },

  // 查询知识库详情
  getLibrary: async (id: number) => {
    return await request.get({ url: `/kb/library/get?id=` + id })
  },

  // 新增知识库
  createLibrary: async (data: Library) => {
    return await request.post({ url: `/kb/library/create`, data })
  },

  // 修改知识库
  updateLibrary: async (data: Library) => {
    return await request.put({ url: `/kb/library/update`, data })
  },

  // 删除知识库
  deleteLibrary: async (id: number) => {
    return await request.delete({ url: `/kb/library/delete?id=` + id })
  },

  /** 批量删除知识库 */
  deleteLibraryList: async (ids: number[]) => {
    return await request.delete({ url: `/kb/library/delete-list?ids=${ids.join(',')}` })
  },

  // 导出知识库 Excel
  exportLibrary: async (params) => {
    return await request.download({ url: `/kb/library/export-excel`, params })
  },

  // ========== 辅助接口 ==========

  /** 获取所有分类列表（用于下拉选择） */
  getCategoryList: async (): Promise<CategorySimple[]> => {
    return await request.get({ url: `/kb/category/list` })
  },

  /** 获取层级配置精简列表（用于下拉选择） */
  getLevelConfigSimpleList: async (): Promise<LevelConfigSimple[]> => {
    return await request.get({ url: `/kb/level-config/simple-list` })
  },

  /** 按昵称/用户名搜索用户（用于选择所有者） */
  searchUserByNickname: async (nickname: string): Promise<UserSimpleVO[]> => {
    return await request.get({ url: `/system/user/list-by-nickname`, params: { nickname } })
  },

  /** 获取部门精简列表（用于选择部门所有者） */
  getDeptSimpleList: async (): Promise<any[]> => {
    return await request.get({ url: `/system/dept/list` })
  }
}