import request from '@/config/axios'

/** 部门成员（合并系统用户 + kb_user_dept 角色） */
export interface DeptMember {
  id?: number // kb_user_dept 主键ID（无记录时为 null）
  userId: number // 用户ID
  deptId: number // 部门ID
  nickname?: string // 用户昵称（来自系统用户表）
  role: number // 角色: 0=成员, 1=管理员
  createTime?: Date // 创建时间
}

/** 系统部门信息 */
export interface DeptVO {
  id: number
  name: string
  parentId: number
  sort: number
  status: number
  children?: DeptVO[]
}

// 部门成员管理 API
export const UserDeptApi = {
  // 获取部门下所有成员（合并系统用户 + 角色）
  getListByDept: async (deptId: number): Promise<DeptMember[]> => {
    return await request.get({ url: `/kb/user-dept/list-by-dept`, params: { deptId } })
  },

  // 添加成员（设置角色为成员）
  addMember: async (userId: number, deptId: number) => {
    return await request.post({ url: `/kb/user-dept/add-member`, params: { userId, deptId } })
  },

  // 添加管理员（设置角色为管理员）
  addAdmin: async (userId: number, deptId: number) => {
    return await request.post({ url: `/kb/user-dept/add-admin`, params: { userId, deptId } })
  },

  // 移除用户角色记录（用户仍保留在系统部门中）
  remove: async (userId: number, deptId: number) => {
    return await request.delete({ url: `/kb/user-dept/remove`, params: { userId, deptId } })
  },

  // 设置用户角色（成员/管理员切换）
  setRole: async (userId: number, deptId: number, role: number) => {
    return await request.put({ url: `/kb/user-dept/set-role`, params: { userId, deptId, role } })
  }
}

// 系统部门 API（用于获取部门树）
export const DeptApi = {
  // 获取部门列表（用于构建树）
  getSimpleDeptList: async (): Promise<DeptVO[]> => {
    return await request.get({ url: `/system/dept/list` })
  }
}
