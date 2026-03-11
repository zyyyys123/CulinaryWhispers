import type { Result, Page } from '@/types/recipe'
import type { NotificationVO } from '@/types/notify'
import { http } from './http'

type BackendResult<T> = {
  code: number
  message: string
  data: T
}

type BackendNotificationVO = {
  id: number
  type: number
  targetType: number
  targetId: number
  content: string
  isRead: number
  createTime: string
  fromUserId: number
  fromNickname?: string
  fromAvatarUrl?: string
}

export const NotifyAPI = {
  list: async (params: { page: number; size: number; type?: number }): Promise<Result<Page<NotificationVO>>> => {
    const res = await http.get<BackendResult<Page<BackendNotificationVO>>>('/notify/list', { params })
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    const page = res.data.data
    return {
      code: 200,
      message: res.data.message,
      data: {
        ...page,
        records: (page.records ?? []).map(n => ({
          id: String(n.id),
          type: n.type,
          targetType: n.targetType,
          targetId: String(n.targetId),
          content: n.content,
          isRead: n.isRead === 1,
          createTime: n.createTime,
          fromUserId: String(n.fromUserId),
          fromNickname: n.fromNickname,
          fromAvatarUrl: n.fromAvatarUrl
        }))
      }
    }
  },

  unreadCount: async (type?: number): Promise<Result<number>> => {
    const res = await http.get<BackendResult<number>>('/notify/unread/count', { params: { type } })
    return { code: res.data.code, message: res.data.message, data: res.data.data }
  },

  markRead: async (id: string): Promise<Result<null>> => {
    const res = await http.post<BackendResult<null>>(`/notify/read/${id}`)
    return { code: res.data.code, message: res.data.message, data: null }
  },

  unreadCountsByType: async (): Promise<Result<Record<string, number>>> => {
    const res = await http.get<BackendResult<Record<string, number>>>('/notify/unread/counts')
    return { code: res.data.code, message: res.data.message, data: res.data.data as any }
  },

  markAllRead: async (type?: number): Promise<Result<null>> => {
    const res = await http.post<BackendResult<null>>('/notify/read/all', null, { params: { type } })
    return { code: res.data.code, message: res.data.message, data: null }
  }
}
