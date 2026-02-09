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
