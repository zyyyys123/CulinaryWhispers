// 通用响应结构
export interface Result<T> {
  code: number
  message: string
  data: T
}

// 分页结构
export interface Page<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 食谱列表项 VO
export interface RecipePageVO {
  id: string // JS 中大数处理，建议 ID 用 string
  title: string
  description: string
  coverUrl: string
  authorName: string
  authorAvatar: string
  viewCount: number
  likeCount: number
  difficulty: number // 1-5
  timeCost: number // 分钟
  tags: string[]
}

// 食谱步骤
export interface RecipeStep {
  stepNo: number
  desc: string
  imgUrl?: string
  videoUrl?: string
  timeCost?: number
  isKeyStep: boolean
}

// 食谱详情 VO
export interface RecipeDetailVO {
  id: string
  title: string
  description: string
  coverUrl: string
  videoUrl?: string
  authorId: string
  authorName: string
  authorAvatar: string
  difficulty: number
  timeCost: number
  calories: number
  protein: number
  fat: number
  carbs: number
  score: number
  steps: RecipeStep[]
  tags: string[]
  ingredients: Array<{ name: string; amount: string }>
}
