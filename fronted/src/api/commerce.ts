import type { Result, Page } from '@/types/recipe'
import type { ProductVO } from '@/types/commerce'
import { http } from './http'

type BackendResult<T> = { code: number; message: string; data: T }

type BackendProduct = {
  id: number
  title: string
  description: string
  price: number
  stock: number
  categoryId: number
}

export const CommerceAPI = {
  // 获取商品列表
  getList: async (params: { page: number; size: number; keyword?: string; categoryId?: number }): Promise<Result<Page<ProductVO>>> => {
    const res = await http.get<BackendResult<Page<BackendProduct>>>('/commerce/product/list', { params })
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    const page = res.data.data
    return {
      code: 200,
      message: res.data.message,
      data: {
        ...page,
        records: (page.records ?? []).map(p => ({
          id: String(p.id),
          name: p.title,
          description: p.description,
          price: Number(p.price ?? 0),
          coverUrl: 'https://images.unsplash.com/photo-1593618998160-e34015e67502?w=800&q=80',
          images: [],
          stock: Number(p.stock ?? 0),
          categoryId: String(p.categoryId ?? ''),
          rating: 0,
          reviewCount: 0
        }))
      }
    }
  }
}
