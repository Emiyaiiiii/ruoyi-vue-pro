import request from '@/config/axios'

/** 知识库文件信息 */
export interface Document {
  id: number
  kbId: number
  fileName: string
  fileUrl: string
  fileType: string
  fileSize: number
  fileConfigId?: number
  filePath?: string
  description?: string
  tags?: string
  downloadCount: number
  viewCount: number
  status: number
  createTime: string
}

// 知识库文件 API
export const DocumentApi = {
  // 查询知识库文件分页
  getDocumentPage: async (params: any) => {
    return await request.get({ url: `/kb/document/page`, params })
  },

  // 查询知识库文件详情
  getDocument: async (id: number) => {
    return await request.get({ url: `/kb/document/get?id=` + id })
  },

  // 上传文件到知识库
  uploadDocument: async (data: FormData) => {
    return await request.post({ url: `/kb/document/upload`, data, headersType: 'multipart/form-data' })
  },

  // 更新知识库文件
  updateDocument: async (data: Document) => {
    return await request.put({ url: `/kb/document/update`, data })
  },

  // 删除知识库文件
  deleteDocument: async (id: number) => {
    return await request.delete({ url: `/kb/document/delete?id=` + id })
  },

  // 批量删除知识库文件
  deleteDocumentList: async (ids: number[]) => {
    return await request.delete({ url: `/kb/document/delete-list?ids=${ids.join(',')}` })
  },

  // 导出知识库文件 Excel
  exportDocument: async (params: any) => {
    return await request.download({ url: `/kb/document/export-excel`, params })
  }
}
