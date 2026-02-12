import type { Result, Page } from '@/types/recipe'
import { http } from './http'

type BackendResult<T> = { code: number; message: string; data: T }

type BackendRecipeDocument = {
  id: number
  title: string
  description: string
  tags?: string[]
  authorName?: string
  difficulty?: number
  timeCost?: number
  score?: number
}

export type RecipeSearchItem = {
  id: string
  title: string
  description: string
  tags: string[]
  authorName: string
  difficulty: number
  timeCost: number
  score: number
}

export const SearchAPI = {
  searchRecipe: async (params: { keyword: string; page: number; size: number }): Promise<Result<Page<RecipeSearchItem>>> => {
    const res = await http.get<BackendResult<Page<BackendRecipeDocument>>>('/search/recipe', { params })
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
          title: r.title ?? '',
          description: r.description ?? '',
          tags: (r.tags ?? []).map(String),
          authorName: r.authorName ?? 'Unknown',
          difficulty: Number(r.difficulty ?? 1),
          timeCost: Number(r.timeCost ?? 0),
          score: Number(r.score ?? 0)
        }))
      }
    }
  },

  searchPersonalized: async (params: { keyword?: string; page: number; size: number }): Promise<Result<Page<RecipeSearchItem>>> => {
    const res = await http.get<BackendResult<Page<BackendRecipeDocument>>>('/search/personalized', { params })
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
          title: r.title ?? '',
          description: r.description ?? '',
          tags: (r.tags ?? []).map(String),
          authorName: r.authorName ?? 'Unknown',
          difficulty: Number(r.difficulty ?? 1),
          timeCost: Number(r.timeCost ?? 0),
          score: Number(r.score ?? 0)
        }))
      }
    }
  }
}

