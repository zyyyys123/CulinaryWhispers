import type { Result, Page, RecipePageVO, RecipeDetailVO, RecipePublishDTO } from '@/types/recipe'
import { http } from './http'

type BackendResult<T> = { code: number; message: string; data: T }

type BackendRecipePageVO = {
  id: number
  title: string
  description: string
  coverUrl: string
  difficulty: number
  timeCost: number
  score: number
  authorId: number
  authorName: string
  authorAvatar?: string
  viewCount: number
  likeCount: number
  collectCount: number
  gmtCreate: string
}

type BackendRecipeInfo = {
  id: number
  authorId: number
  title: string
  coverUrl: string
  videoUrl?: string
  description: string
  difficulty: number
  timeCost: number
  calories: number
  protein: number
  fat: number
  carbs: number
  score: number
  tags?: string
}

type BackendRecipeStats = {
  viewCount?: number
  likeCount?: number
  collectCount?: number
  shareCount?: number
}

type BackendRecipeStep = {
  stepNo: number
  desc: string
  imgUrl?: string
  videoUrl?: string
  timeCost?: number
  voiceUrl?: string
  isKeyStep?: boolean
}

type BackendRecipeDetailVO = {
  info: BackendRecipeInfo
  stats?: BackendRecipeStats
  steps?: BackendRecipeStep[]
  author?: { id: number; nickname: string; avatarUrl?: string }
}

const parseTags = (raw?: string): string[] => {
  if (!raw) return []
  try {
    const v = JSON.parse(raw)
    return Array.isArray(v) ? v.map(String) : []
  } catch {
    return []
  }
}

const mapPage = (page: Page<BackendRecipePageVO>, message: string): Result<Page<RecipePageVO>> => {
  return {
    code: 200,
    message,
    data: {
      ...page,
      records: (page.records ?? []).map(r => ({
        id: String(r.id),
        title: r.title,
        description: r.description,
        coverUrl: r.coverUrl,
        authorName: r.authorName,
        authorAvatar: r.authorAvatar ?? 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
        viewCount: Number(r.viewCount ?? 0),
        likeCount: Number(r.likeCount ?? 0),
        difficulty: Number(r.difficulty ?? 1),
        timeCost: Number(r.timeCost ?? 0),
        tags: []
      }))
    }
  }
}

export const RecipeAPI = {
  // 获取食谱列表
  getList: async (params: { page: number; size: number }): Promise<Result<Page<RecipePageVO>>> => {
    const res = await http.get<BackendResult<Page<BackendRecipePageVO>>>('/recipe/list', { params })
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    return mapPage(res.data.data, res.data.message)
  },

  recommend: async (params: { page: number; size: number }): Promise<Result<Page<RecipePageVO>>> => {
    const res = await http.get<BackendResult<Page<BackendRecipePageVO>>>('/recipe/recommend', { params })
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    return mapPage(res.data.data, res.data.message)
  },

  publish: async (payload: RecipePublishDTO): Promise<Result<string>> => {
    const res = await http.post<BackendResult<number>>('/recipe/publish', payload)
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: '' }
    }
    return { code: 200, message: res.data.message, data: String(res.data.data ?? '') }
  },

  // 获取详情
  getDetail: async (id: string): Promise<Result<RecipeDetailVO>> => {
    const res = await http.get<BackendResult<BackendRecipeDetailVO>>(`/recipe/${id}`)
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    const d = res.data.data
    const info = d.info
    const tags = parseTags(info.tags)
    const stats = d.stats ?? {}
    return {
      code: 200,
      message: res.data.message,
      data: {
        id: String(info.id),
        title: info.title,
        description: info.description,
        coverUrl: info.coverUrl,
        videoUrl: info.videoUrl,
        authorId: String(info.authorId),
        authorName: d.author?.nickname ?? 'Unknown',
        authorAvatar: d.author?.avatarUrl ?? 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
        difficulty: Number(info.difficulty ?? 1),
        timeCost: Number(info.timeCost ?? 0),
        calories: Number(info.calories ?? 0),
        protein: Number(info.protein ?? 0),
        fat: Number(info.fat ?? 0),
        carbs: Number(info.carbs ?? 0),
        score: Number(info.score ?? 0),
        likeCount: Number(stats.likeCount ?? 0),
        collectCount: Number(stats.collectCount ?? 0),
        steps: (d.steps ?? []).map(s => ({
          stepNo: s.stepNo,
          desc: s.desc,
          imgUrl: s.imgUrl,
          videoUrl: s.videoUrl,
          timeCost: s.timeCost,
          isKeyStep: Boolean(s.isKeyStep)
        })),
        tags,
        ingredients: []
      }
    }
  }
}
