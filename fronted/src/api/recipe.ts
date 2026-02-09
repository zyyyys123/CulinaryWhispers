import axios from 'axios'
import type { Result, Page, RecipePageVO, RecipeDetailVO } from '@/types/recipe'

// 基础配置：后续可从 .env 读取 VITE_API_BASE_URL
const api = axios.create({
  baseURL: '/api', // 配合 Vite Proxy
  timeout: 10000,
})

// Mock 数据生成器 (仅开发阶段使用)
const mockRecipes = (count: number): RecipePageVO[] => {
  return Array.from({ length: count }).map((_, i) => ({
    id: `rcp_${i + 1000}`,
    title: [
      'Truffle Mushroom Risotto', 
      'Classic Beef Wellington', 
      'Matcha Lava Cake', 
      'Saffron Seafood Paella',
      'Sous-vide Salmon'
    ][i % 5],
    description: 'A delicate balance of flavors...',
    coverUrl: [
      'https://images.unsplash.com/photo-1476124369491-e7addf5db371?w=800&q=80',
      'https://images.unsplash.com/photo-1600891964092-4316c288032e?w=800&q=80',
      'https://images.unsplash.com/photo-1546549032-9571cd6b27df?w=800&q=80',
      'https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=800&q=80',
      'https://images.unsplash.com/photo-1467003909585-2f8a7270028d?w=800&q=80'
    ][i % 5],
    authorName: 'Chef Gordon',
    authorAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
    viewCount: 12500 + i * 100,
    likeCount: 340 + i * 10,
    difficulty: (i % 3) + 1,
    timeCost: 45 + i * 5,
    tags: ['French', 'Dinner', 'Low Carb'].slice(0, (i % 3) + 1)
  }))
}

// 模拟延迟
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

export const RecipeAPI = {
  // 获取食谱列表
  getList: async (params: { page: number; size: number }): Promise<Result<Page<RecipePageVO>>> => {
    // TODO: Switch to real API when backend is ready
    // return api.get('/recipe/list', { params })
    
    await delay(800) // 模拟网络延迟
    const list = mockRecipes(params.size)
    return {
      code: 200,
      message: 'success',
      data: {
        records: list,
        total: 100,
        size: params.size,
        current: params.page,
        pages: 10
      }
    }
  },

  // 获取详情
  getDetail: async (id: string): Promise<Result<RecipeDetailVO>> => {
    // return api.get(`/recipe/${id}`)
    
    await delay(600)
    return {
      code: 200,
      message: 'success',
      data: {
        id,
        title: 'Truffle Mushroom Risotto',
        description: 'A creamy, rich Italian classic enhanced with the earthy aroma of black truffles.',
        coverUrl: 'https://images.unsplash.com/photo-1476124369491-e7addf5db371?w=800&q=80',
        authorId: 'user_001',
        authorName: 'Chef Gordon',
        authorAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
        difficulty: 3,
        timeCost: 45,
        calories: 450,
        protein: 12,
        fat: 18,
        carbs: 55,
        score: 4.8,
        tags: ['Italian', 'Dinner', 'Vegetarian'],
        ingredients: [
          { name: 'Arborio Rice', amount: '300g' },
          { name: 'Black Truffle', amount: '10g' },
          { name: 'Parmesan Cheese', amount: '50g' }
        ],
        steps: [
          { stepNo: 1, desc: 'Prepare the broth and keep it warm.', isKeyStep: false },
          { stepNo: 2, desc: 'Toast the rice with butter and shallots.', isKeyStep: true },
          { stepNo: 3, desc: 'Slowly add broth while stirring constantly.', isKeyStep: true }
        ]
      }
    }
  }
}
