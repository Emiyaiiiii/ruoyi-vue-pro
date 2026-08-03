import request from '@/config/axios'

/** 项目成员（含用户昵称） */
export interface ProjectMember {
  id?: number
  kbId: number
  userId: number
  nickname?: string
  createTime?: Date
}

// 项目成员管理 API
export const ProjectMemberApi = {
  // 获取项目成员列表（含用户昵称）
  getList: async (kbId: number): Promise<ProjectMember[]> => {
    return await request.get({ url: `/kb/project-member/list`, params: { kbId } })
  },

  // 添加项目成员
  addMember: async (kbId: number, userId: number) => {
    return await request.post({ url: `/kb/project-member/add`, params: { kbId, userId } })
  },

  // 移除项目成员
  removeMember: async (kbId: number, userId: number) => {
    return await request.delete({ url: `/kb/project-member/remove`, params: { kbId, userId } })
  },

  // 检查用户是否为项目成员
  isMember: async (kbId: number, userId: number): Promise<boolean> => {
    return await request.get({ url: `/kb/project-member/check`, params: { kbId, userId } })
  }
}
