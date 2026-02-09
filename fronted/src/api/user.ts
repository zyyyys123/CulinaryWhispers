import axios from 'axios'
import type { Result } from '@/types/recipe'
import type { UserProfileVO, UserStatsVO } from '@/types/user'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// Mock Delay
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

export const UserAPI = {
  // 获取个人资料
  getProfile: async (userId?: string): Promise<Result<UserProfileVO>> => {
    // return api.get('/user/profile', { params: { userId } })
    
    await delay(500)
    return {
      code: 200,
      message: 'success',
      data: {
        userId: userId || 'user_001',
        username: 'chef_gordon',
        nickname: 'Gordon Ramsay',
        avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
        signature: 'Cooking is my passion, and perfection is my goal.',
        totalSpend: 1250.00,
        isMasterChef: true,
        masterTitle: 'Michelin 3-Star Chef',
        bgImageUrl: 'https://images.unsplash.com/photo-1556910103-1c02745a30bf?w=1600&q=80'
      }
    }
  },

  // 获取成长统计
  getStats: async (userId?: string): Promise<Result<UserStatsVO>> => {
    // return api.get('/user/stats', { params: { userId } })

    await delay(600)
    return {
      code: 200,
      message: 'success',
      data: {
        userId: userId || 'user_001',
        level: 42,
        currentExp: 8500,
        nextLevelExp: 10000,
        followerCount: 1250000,
        followingCount: 120,
        likeCount: 5600000,
        recipeCount: 345,
        badges: [
          {
            id: 'badge_1',
            name: 'Master Chef',
            iconUrl: '/badges/chef-hat.png', // Placeholder
            description: 'Verified as a top-tier culinary expert.',
            isUnlocked: true,
            unlockedTime: '2023-10-15',
            modelUrl: '/models/badge_chef.glb' // Mock 3D model path
          },
          {
            id: 'badge_2',
            name: 'Trend Setter',
            iconUrl: '/badges/flame.png',
            description: 'Created a recipe that reached #1 trending.',
            isUnlocked: true,
            unlockedTime: '2024-01-20',
            modelUrl: '/models/badge_fire.glb'
          },
          {
            id: 'badge_3',
            name: 'Community Pillar',
            iconUrl: '/badges/heart.png',
            description: 'Received over 10,000 likes.',
            isUnlocked: true,
            modelUrl: '/models/badge_heart.glb'
          }
        ]
      }
    }
  }
}
