import type { Result } from '@/types/recipe'
import type { UserProfileVO, UserStatsVO } from '@/types/user'
import { http } from './http'

type BackendResult<T> = { code: number; message: string; data: T }

type BackendUserProfileVO = {
  id: number
  username: string
  nickname: string
  avatarUrl?: string
  mobile?: string
  email?: string
  gender?: number
  signature?: string
  country?: string
  province?: string
  city?: string
  job?: string
  cookAge?: number
  favoriteCuisine?: string
  tastePreference?: string
  dietaryRestrictions?: string
  interests?: string
  vipLevel?: number
  vipExpireTime?: string
  isMasterChef?: boolean
  masterTitle?: string
  bgImageUrl?: string
  totalSpend?: number
}

type BackendUserStatsVO = {
  userId: number
  level: number
  experience: number
  nextLevelExperience: number
  isMasterChef?: boolean
  masterTitle?: string
  badges?: string[]
  totalRecipes?: number
  totalLikesReceived?: number
  totalFans?: number
}

export const UserAPI = {
  register: async (payload: { username: string; password: string; nickname: string }): Promise<Result<string>> => {
    const res = await http.post<BackendResult<number>>('/user/register', payload)
    return { code: res.data.code, message: res.data.message, data: String(res.data.data ?? '') }
  },

  login: async (payload: { username: string; password: string }): Promise<Result<string>> => {
    const res = await http.post<BackendResult<string>>('/user/login', payload)
    if (res.data.code === 200 && res.data.data) {
      localStorage.setItem('cw_token', res.data.data)
    }
    return { code: res.data.code, message: res.data.message, data: res.data.data }
  },

  getProfile: async (): Promise<Result<UserProfileVO>> => {
    const res = await http.get<BackendResult<BackendUserProfileVO>>('/user/profile')
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    const d = res.data.data
    return {
      code: 200,
      message: res.data.message,
      data: {
        userId: String(d.id),
        username: d.username,
        nickname: d.nickname,
        avatarUrl: d.avatarUrl ?? 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
        mobile: d.mobile,
        email: d.email,
        gender: d.gender,
        signature: d.signature,
        country: d.country,
        province: d.province,
        city: d.city,
        job: d.job,
        cookAge: d.cookAge,
        favoriteCuisine: d.favoriteCuisine,
        tastePreference: d.tastePreference,
        dietaryRestrictions: d.dietaryRestrictions,
        interests: d.interests,
        vipLevel: d.vipLevel,
        vipExpireTime: d.vipExpireTime,
        totalSpend: Number(d.totalSpend ?? 0),
        isMasterChef: Boolean(d.isMasterChef),
        masterTitle: d.masterTitle,
        bgImageUrl: d.bgImageUrl
      }
    }
  },

  getProfileById: async (userId: string): Promise<Result<UserProfileVO>> => {
    const res = await http.get<BackendResult<BackendUserProfileVO>>(`/user/profile/${userId}`)
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    const d = res.data.data
    return {
      code: 200,
      message: res.data.message,
      data: {
        userId: String(d.id),
        username: d.username,
        nickname: d.nickname,
        avatarUrl: d.avatarUrl ?? 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
        mobile: d.mobile,
        email: d.email,
        gender: d.gender,
        signature: d.signature,
        country: d.country,
        province: d.province,
        city: d.city,
        job: d.job,
        cookAge: d.cookAge,
        favoriteCuisine: d.favoriteCuisine,
        tastePreference: d.tastePreference,
        dietaryRestrictions: d.dietaryRestrictions,
        interests: d.interests,
        vipLevel: d.vipLevel,
        vipExpireTime: d.vipExpireTime,
        totalSpend: Number(d.totalSpend ?? 0),
        isMasterChef: Boolean(d.isMasterChef),
        masterTitle: d.masterTitle,
        bgImageUrl: d.bgImageUrl
      }
    }
  },

  updateProfile: async (payload: Partial<UserProfileVO>): Promise<Result<void>> => {
    const body = {
      nickname: payload.nickname,
      avatarUrl: payload.avatarUrl,
      bgImageUrl: payload.bgImageUrl,
      gender: payload.gender,
      signature: payload.signature,
      city: payload.city,
      job: payload.job,
      cookAge: payload.cookAge,
      favoriteCuisine: payload.favoriteCuisine,
      tastePreference: payload.tastePreference,
      dietaryRestrictions: payload.dietaryRestrictions
    }
    const res = await http.put<BackendResult<null>>('/user/profile', body)
    return { code: res.data.code, message: res.data.message, data: undefined as any }
  },

  getStats: async (): Promise<Result<UserStatsVO>> => {
    const res = await http.get<BackendResult<BackendUserStatsVO>>('/user/stats')
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    const d = res.data.data
    const badgeList = (d.badges ?? []).map((name, idx) => ({
      id: `badge_${idx + 1}`,
      name,
      iconUrl: '/badges/chef-hat.png',
      description: name,
      isUnlocked: true
    }))
    return {
      code: 200,
      message: res.data.message,
      data: {
        userId: String(d.userId),
        level: d.level ?? 1,
        currentExp: d.experience ?? 0,
        nextLevelExp: d.nextLevelExperience ?? 0,
        badges: badgeList,
        followerCount: d.totalFans ?? 0,
        followingCount: 0,
        likeCount: d.totalLikesReceived ?? 0,
        recipeCount: d.totalRecipes ?? 0
      }
    }
  },

  getStatsById: async (userId: string): Promise<Result<UserStatsVO>> => {
    const res = await http.get<BackendResult<BackendUserStatsVO>>(`/user/stats/${userId}`)
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    const d = res.data.data
    const badgeList = (d.badges ?? []).map((name, idx) => ({
      id: `badge_${idx + 1}`,
      name,
      iconUrl: '/badges/chef-hat.png',
      description: name,
      isUnlocked: true
    }))
    return {
      code: 200,
      message: res.data.message,
      data: {
        userId: String(d.userId),
        level: d.level ?? 1,
        currentExp: d.experience ?? 0,
        nextLevelExp: d.nextLevelExperience ?? 0,
        badges: badgeList,
        followerCount: d.totalFans ?? 0,
        followingCount: 0,
        likeCount: d.totalLikesReceived ?? 0,
        recipeCount: d.totalRecipes ?? 0
      }
    }
  }
}
