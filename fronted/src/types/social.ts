import type { UserProfileVO } from '@/types/user'

// 评论 VO
export interface CommentVO {
  id: string
  recipeId: string
  content: string
  parentId?: string
  author: UserProfileVO
  createTime: string
  likeCount: number
  isLiked: boolean
  replies?: CommentVO[]
}

// 关注关系 VO
export interface FollowVO {
  userId: string
  user: UserProfileVO
  createTime: string
  isMutual: boolean // 是否互相关注
  remarkName?: string
}
