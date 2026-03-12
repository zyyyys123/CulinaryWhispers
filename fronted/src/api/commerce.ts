import type { Result, Page } from '@/types/recipe'
import type { OrderVO, ProductVO } from '@/types/commerce'
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

type BackendOrderItem = {
  productId: number
  productTitle?: string
  count: number
  price: number
}

type BackendOrder = {
  id: number
  totalAmount: number
  status: number
  payTime?: string
  gmtCreate: string
  items?: BackendOrderItem[]
}

const coverByCategory: Record<number, string> = {
  1: '厨具',
  2: '食材',
  3: '调味',
  4: '课程',
  5: '周边'
}

const svgCover = (title: string, sub: string) => {
  const t = (title || '').trim()
  const s = (sub || '').trim()
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="600" height="600" viewBox="0 0 600 600">
<defs>
<linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
<stop offset="0" stop-color="#0b0b0b"/><stop offset="1" stop-color="#121212"/>
</linearGradient>
</defs>
<rect width="600" height="600" rx="32" fill="url(#g)"/>
<rect x="32" y="32" width="536" height="536" rx="26" fill="none" stroke="#1f2937" stroke-width="2"/>
<text x="300" y="285" text-anchor="middle" font-size="42" font-weight="700" fill="#22c55e" font-family="ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial">${t}</text>
<text x="300" y="345" text-anchor="middle" font-size="20" fill="#9ca3af" font-family="ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial">${s}</text>
</svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

const coverByProduct = (p: BackendProduct) => {
  const title = String(p.title ?? '').trim() || `商品 ${p.id}`
  const short = title.length > 18 ? `${title.slice(0, 18)}…` : title
  const cat = coverByCategory[p.categoryId] ?? '市集'
  return svgCover(short, `${cat} · #${p.id}`)
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
          coverUrl: coverByProduct(p) ?? coverByCategory[p.categoryId] ?? coverByCategory[1],
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
        coverUrl: coverByProduct(p) ?? coverByCategory[p.categoryId] ?? coverByCategory[1],
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
    const res = await http.post<BackendResult<number>>('/commerce/order/create', { productCounts: body })
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: '' }
    }
    return { code: 200, message: res.data.message, data: String(res.data.data ?? '') }
  },

  listMyOrders: async (params: { page: number; size: number; status?: number }): Promise<Result<Page<OrderVO>>> => {
    const res = await http.get<BackendResult<Page<BackendOrder>>>('/commerce/order/list', { params })
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    const page = res.data.data
    return {
      code: 200,
      message: res.data.message,
      data: {
        ...page,
        records: (page.records ?? []).map(o => ({
          id: String(o.id),
          totalAmount: Number(o.totalAmount ?? 0),
          status: Number(o.status ?? 0),
          payTime: o.payTime,
          createTime: o.gmtCreate,
          items: (o.items ?? []).map(it => ({
            productId: String(it.productId),
            productTitle: it.productTitle ?? `商品 ${it.productId}`,
            count: Number(it.count ?? 0),
            price: Number(it.price ?? 0)
          }))
        }))
      }
    }
  },

  getMyOrder: async (orderId: string): Promise<Result<OrderVO>> => {
    const res = await http.get<BackendResult<BackendOrder>>(`/commerce/order/${orderId}`)
    if (res.data.code !== 200) {
      return { code: res.data.code, message: res.data.message, data: null as any }
    }
    const o = res.data.data
    return {
      code: 200,
      message: res.data.message,
      data: {
        id: String(o.id),
        totalAmount: Number(o.totalAmount ?? 0),
        status: Number(o.status ?? 0),
        payTime: o.payTime,
        createTime: o.gmtCreate,
        items: (o.items ?? []).map(it => ({
          productId: String(it.productId),
          productTitle: it.productTitle ?? `商品 ${it.productId}`,
          count: Number(it.count ?? 0),
          price: Number(it.price ?? 0)
        }))
      }
    }
  },

  cancelOrder: async (orderId: string): Promise<Result<null>> => {
    const res = await http.post<BackendResult<null>>(`/commerce/order/${orderId}/cancel`)
    return { code: res.data.code, message: res.data.message, data: null }
  },

  deliverOrder: async (orderId: string): Promise<Result<null>> => {
    const res = await http.post<BackendResult<null>>(`/commerce/order/${orderId}/deliver`)
    return { code: res.data.code, message: res.data.message, data: null }
  },

  finishOrder: async (orderId: string): Promise<Result<null>> => {
    const res = await http.post<BackendResult<null>>(`/commerce/order/${orderId}/finish`)
    return { code: res.data.code, message: res.data.message, data: null }
  },

  paymentNotify: async (orderId: string): Promise<Result<null>> => {
    const res = await http.post<BackendResult<null>>('/commerce/payment/notify', null, { params: { orderId } })
    return { code: res.data.code, message: res.data.message, data: null }
  }
}
