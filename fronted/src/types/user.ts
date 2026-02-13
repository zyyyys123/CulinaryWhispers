// 用户基础信息 VO
export interface UserProfileVO {
  userId: string
  username: string
  nickname: string
  avatarUrl: string
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

  totalSpend?: number
  isMasterChef: boolean // 认证大厨标识
  masterTitle?: string
  bgImageUrl?: string // 个人主页背景图
}

// 用户统计/成长数据 VO
export interface UserStatsVO {
  userId: string
  level: number
  currentExp: number
  nextLevelExp: number
  badges: BadgeVO[]
  // 社交统计
  followerCount: number
  followingCount: number
  likeCount: number
  recipeCount: number
}

// 勋章 VO
export interface BadgeVO {
  id: string
  name: string
  iconUrl: string
  description: string
  isUnlocked: boolean
  unlockedTime?: string
  modelUrl?: string // 3D模型地址 (glb/gltf)
}
