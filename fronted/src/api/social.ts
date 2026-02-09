import axios from 'axios'
import type { Result, Page } from '@/types/recipe'
import type { CommentVO, FollowVO } from '@/types/social'
import type { UserProfileVO } from '@/types/user'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

// Mock User Helper
const mockUser = (name: string, seed: string): UserProfileVO => ({
  userId: `u_${seed}`,
  username: name.toLowerCase().replace(' ', '_'),
  nickname: name,
  avatarUrl: `https://api.dicebear.com/7.x/avataaars/svg?seed=${seed}`,
  totalSpend: 0,
  isMasterChef: Math.random() > 0.8
})

export const SocialAPI = {
  // 1. 互动
  interact: async (params: { targetType: number; targetId: string; actionType: number }): Promise<Result<void>> => {
    // return api.post('/social/interact', params)
    await delay(300)
    return { code: 200, message: 'success', data: undefined }
  },

  // 2. 评论
  getComments: async (params: { recipeId: string; page: number; size: number }): Promise<Result<Page<CommentVO>>> => {
    await delay(500)
    
    // Mock Comments
    const comments: CommentVO[] = Array.from({ length: params.size }).map((_, i) => ({
      id: `cmt_${i}`,
      recipeId: params.recipeId,
      content: [
        'Absolutely delicious! The texture was perfect.',
        'I substituted almond milk and it still worked great.',
        'Could you clarify step 3? I got a bit lost.',
        'This is now a staple in my household!',
        'Five stars! ⭐⭐⭐⭐⭐'
      ][i % 5],
      author: mockUser(['Alice', 'Bob', 'Charlie', 'David', 'Eve'][i % 5], `user_${i}`),
      createTime: '2 hours ago',
      likeCount: Math.floor(Math.random() * 50),
      isLiked: false
    }))

    return {
      code: 200,
      message: 'success',
      data: {
        records: comments,
        total: 25,
        size: params.size,
        current: params.page,
        pages: 3
      }
    }
  },

  postComment: async (params: { recipeId: string; content: string }): Promise<Result<string>> => {
    // return api.post('/social/comment', params)
    await delay(400)
    return { code: 200, message: 'success', data: 'new_comment_id' }
  },

  // 3. 关注
  follow: async (followingId: string): Promise<Result<void>> => {
    // return api.post(`/social/follow/${followingId}`)
    await delay(300)
    return { code: 200, message: 'success', data: undefined }
  },

  unfollow: async (followingId: string): Promise<Result<void>> => {
    // return api.post(`/social/unfollow/${followingId}`)
    await delay(300)
    return { code: 200, message: 'success', data: undefined }
  }
}
