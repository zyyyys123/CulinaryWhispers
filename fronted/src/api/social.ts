import type { Result, Page } from '@/types/recipe'
import type { CommentVO, FollowVO } from '@/types/social'
import type { UserProfileVO } from '@/types/user'
import { http } from './http'

type BackendResult<T> = { code: number; message: string; data: T }

type BackendComment = {
  id: number
  userId: number
  targetId: number
  recipeId?: number
  content: string
  parentId: number
  likeCount: number
  gmtCreate: string
}

type BackendFollow = {
  id: number
  userId: number
  followingId: number
  gmtCreate: string
}

type BackendInteractionStatusVO = {
  liked?: boolean
  collected?: boolean
}

const fallbackUser = (userId: string): UserProfileVO => ({
  userId,
  username: `user_${userId}`,
  nickname: `User ${userId}`,
  avatarUrl: `https://api.dicebear.com/7.x/avataaars/svg?seed=${userId}`,
  totalSpend: 0,
  isMasterChef: false
})

export const SocialAPI = {
  // 1. 互动
  interact: async (params: { targetType: number; targetId: string; actionType: number }): Promise<Result<void>> => {
    const res = await http.post<BackendResult<void>>(
      '/social/interact',
      null,
      { params: { targetType: params.targetType, targetId: params.targetId, actionType: params.actionType } }
    )
    return { code: res.data.code, message: res.data.message, data: undefined }
  },

  getInteractionStatus: async (params: { targetType: number; targetId: string }): Promise<Result<{ liked: boolean; collected: boolean }>> => {
    const res = await http.get<BackendResult<BackendInteractionStatusVO>>('/social/interact/status', {
      params: { targetType: params.targetType, targetId: params.targetId }
    })
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    const d = res.data.data
    return { code: 200, message: res.data.message, data: { liked: Boolean(d.liked), collected: Boolean(d.collected) } }
  },

  // 2. 评论
  getComments: async (params: { recipeId: string; page: number; size: number }): Promise<Result<Page<CommentVO>>> => {
    const res = await http.get<BackendResult<Page<BackendComment>>>('/social/comment/list', {
      params: { recipeId: params.recipeId, page: params.page, size: params.size }
    })
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    const page = res.data.data
    return {
      code: 200,
      message: res.data.message,
      data: {
        ...page,
        records: (page.records ?? []).map(c => ({
          id: String(c.id),
          recipeId: String(c.recipeId ?? params.recipeId),
          content: c.content,
          parentId: c.parentId ? String(c.parentId) : undefined,
          author: fallbackUser(String(c.userId)),
          createTime: c.gmtCreate,
          likeCount: c.likeCount ?? 0,
          isLiked: false
        }))
      }
    }
  },

  postComment: async (params: { recipeId: string; content: string; parentId?: string }): Promise<Result<string>> => {
    const res = await http.post<BackendResult<number>>(
      '/social/comment',
      null,
      { params: { recipeId: params.recipeId, content: params.content, parentId: params.parentId } }
    )
    return { code: res.data.code, message: res.data.message, data: String(res.data.data ?? '') }
  },

  // 3. 关注
  follow: async (followingId: string): Promise<Result<void>> => {
    const res = await http.post<BackendResult<void>>(`/social/follow/${followingId}`)
    return { code: res.data.code, message: res.data.message, data: undefined }
  },

  unfollow: async (followingId: string): Promise<Result<void>> => {
    const res = await http.post<BackendResult<void>>(`/social/unfollow/${followingId}`)
    return { code: res.data.code, message: res.data.message, data: undefined }
  },

  getFollowers: async (params: { page: number; size: number }): Promise<Result<Page<FollowVO>>> => {
    const res = await http.get<BackendResult<Page<BackendFollow>>>('/social/followers', { params })
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    const page = res.data.data
    return {
      code: 200,
      message: res.data.message,
      data: {
        ...page,
        records: (page.records ?? []).map(f => ({
          userId: String(f.followingId),
          user: fallbackUser(String(f.followingId)),
          createTime: f.gmtCreate,
          isMutual: false
        }))
      }
    }
  },

  getFollowing: async (params: { page: number; size: number }): Promise<Result<Page<FollowVO>>> => {
    const res = await http.get<BackendResult<Page<BackendFollow>>>('/social/following', { params })
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    const page = res.data.data
    return {
      code: 200,
      message: res.data.message,
      data: {
        ...page,
        records: (page.records ?? []).map(f => ({
          userId: String(f.followingId),
          user: fallbackUser(String(f.followingId)),
          createTime: f.gmtCreate,
          isMutual: false
        }))
      }
    }
  }
}
