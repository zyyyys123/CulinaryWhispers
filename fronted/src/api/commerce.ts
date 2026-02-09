import axios from 'axios'
import type { Result, Page } from '@/types/recipe'
import type { ProductVO } from '@/types/commerce'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

export const CommerceAPI = {
  // 获取商品列表
  getList: async (params: { page: number; size: number }): Promise<Result<Page<ProductVO>>> => {
    await delay(600)
    
    // Mock Data
    const products: ProductVO[] = Array.from({ length: params.size }).map((_, i) => ({
      id: `prod_${i + 100}`,
      name: [
        'Premium Japanese Chef Knife',
        'Cast Iron Skillet',
        'Organic Saffron Spice',
        'Truffle Oil Set',
        'Sous Vide Precision Cooker'
      ][i % 5],
      description: 'Essential tool for every master chef.',
      price: [129.99, 45.00, 24.50, 38.00, 199.00][i % 5],
      originalPrice: [159.99, 60.00, 30.00, 45.00, 250.00][i % 5],
      coverUrl: [
        'https://images.unsplash.com/photo-1593618998160-e34015e67502?w=800&q=80',
        'https://images.unsplash.com/photo-1584269600464-37b1b58a9fe7?w=800&q=80',
        'https://images.unsplash.com/photo-1596040033229-a9821ebd058d?w=800&q=80',
        'https://images.unsplash.com/photo-1476124369491-e7addf5db371?w=800&q=80',
        'https://images.unsplash.com/photo-1585652684822-441619623b36?w=800&q=80'
      ][i % 5],
      images: [],
      stock: 50,
      categoryId: 'cat_1',
      rating: 4.8,
      reviewCount: 120
    }))

    return {
      code: 200,
      message: 'success',
      data: {
        records: products,
        total: 50,
        size: params.size,
        current: params.page,
        pages: 5
      }
    }
  }
}
