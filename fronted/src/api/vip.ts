import type { Result } from '@/types/recipe'
import { http } from './http'

export type VipPlanVO = {
  level: number
  name: string
  costPoints: number
  durationDays: number
  benefits: string[]
}

type BackendResult<T> = {
  code: number
  message: string
  data: T
}

type BackendUserProfile = {
  userId: number
  vipLevel?: number
  vipExpireTime?: string
}

export const VipAPI = {
  plans: async (): Promise<Result<VipPlanVO[]>> => {
    const res = await http.get<BackendResult<VipPlanVO[]>>('/user/vip/plans')
    return { code: res.data.code, message: res.data.message, data: res.data.data }
  },

  exchange: async (level: number): Promise<Result<BackendUserProfile>> => {
    const res = await http.post<BackendResult<BackendUserProfile>>('/user/vip/exchange', null, { params: { level } })
    return { code: res.data.code, message: res.data.message, data: res.data.data }
  },

  me: async (): Promise<Result<BackendUserProfile>> => {
    const res = await http.get<BackendResult<BackendUserProfile>>('/user/vip/me')
    return { code: res.data.code, message: res.data.message, data: res.data.data }
  }
}

