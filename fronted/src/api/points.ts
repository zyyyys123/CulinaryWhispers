import type { Result, Page } from '@/types/recipe'
import { http } from './http'

type BackendResult<T> = { code: number; message: string; data: T }

type BackendPointsRecord = {
  id: number
  userId: number
  type: number
  amount: number
  description?: string
  gmtCreate: string
}

export type PointsRecordVO = {
  id: string
  type: number
  amount: number
  description: string
  createTime: string
}

export const PointsAPI = {
  signIn: async (): Promise<Result<number>> => {
    const res = await http.post<BackendResult<number>>('/user/points/sign-in')
    return { code: res.data.code, message: res.data.message, data: Number(res.data.data ?? 0) }
  },

  history: async (params: { page: number; size: number }): Promise<Result<Page<PointsRecordVO>>> => {
    const res = await http.get<BackendResult<Page<BackendPointsRecord>>>('/user/points/history', { params })
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    const page = res.data.data
    return {
      code: 200,
      message: res.data.message,
      data: {
        ...page,
        records: (page.records ?? []).map(r => ({
          id: String(r.id),
          type: Number(r.type ?? 0),
          amount: Number(r.amount ?? 0),
          description: r.description ?? '',
          createTime: r.gmtCreate
        }))
      }
    }
  }
}

