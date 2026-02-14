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

const coverByCategory: Record<number, string> = {
  1: 'https://www.themealdb.com/images/ingredients/Butter.png',
  2: 'https://www.themealdb.com/images/ingredients/Chicken.png',
  3: 'https://www.themealdb.com/images/ingredients/Salt.png',
  4: 'https://www.themealdb.com/images/ingredients/Eggs.png',
  5: 'https://www.themealdb.com/images/ingredients/Milk.png'
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
          coverUrl: coverByCategory[p.categoryId] ?? coverByCategory[1],
          images: [],
          stock: Number(p.stock ?? 0),
          categoryId: String(p.categoryId ?? ''),
          rating: 0,
          reviewCount: 0
        }))
      }
    }
  },

  getProduct: async (productId: string): Promise<Result<ProductVO>> => {
    const res = await http.get<BackendResult<BackendProduct>>(`/commerce/product/${productId}`)
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    const p = res.data.data
    return {
      code: 200,
      message: res.data.message,
      data: {
        id: String(p.id),
        name: p.title,
        description: p.description,
        price: Number(p.price ?? 0),
        coverUrl: coverByCategory[p.categoryId] ?? coverByCategory[1],
        images: [],
        stock: Number(p.stock ?? 0),
        categoryId: String(p.categoryId ?? ''),
        rating: 0,
        reviewCount: 0
      }
    }
  },

  createOrder: async (productCounts: Record<string, number>): Promise<Result<string>> => {
    const body: Record<number, number> = {}
    Object.entries(productCounts).forEach(([k, v]) => {
      const id = Number(k)
      if (Number.isFinite(id) && v > 0) body[id] = v
    })
    const res = await http.post<BackendResult<number>>('/commerce/order/create', body)
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: '' }
    }
    return { code: 200, message: res.data.message, data: String(res.data.data ?? '') }
  },

  paymentNotify: async (orderId: string): Promise<Result<null>> => {
    const res = await http.post<BackendResult<null>>('/commerce/payment/notify', null, { params: { orderId } })
    return { code: res.data.code, message: res.data.message, data: null }
  }
}
