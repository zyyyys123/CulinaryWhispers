// 商品 VO
export interface ProductVO {
  id: string
  name: string
  description: string
  price: number
  originalPrice?: number
  coverUrl: string
  images: string[]
  stock: number
  categoryId: string
  rating: number
  reviewCount: number
}

// 购物车项
export interface CartItem {
  productId: string
  product: ProductVO
  quantity: number
}

export interface OrderItemVO {
  productId: string
  productTitle: string
  count: number
  price: number
}

export interface OrderVO {
  id: string
  totalAmount: number
  status: number
  payTime?: string
  createTime: string
  items: OrderItemVO[]
}
