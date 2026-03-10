import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/modules/home/HomePage.vue')
    },
    {
      path: '/recipe/:id',
      name: 'recipe-detail',
      component: () => import('@/modules/recipe/RecipeDetail.vue')
    },
    {
      path: '/recipe/publish',
      name: 'recipe-publish',
      component: () => import('@/modules/recipe/RecipePublish.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/user/profile',
      name: 'user-profile',
      component: () => import('@/modules/user/UserProfile.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/user/profile/setup',
      name: 'user-profile-setup',
      component: () => import('@/modules/user/ProfileSetupPage.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/user/:id',
      name: 'user-public',
      component: () => import('@/modules/user/UserProfile.vue')
    },
    {
      path: '/market',
      name: 'market',
      component: () => import('@/modules/commerce/MarketPage.vue')
    },
    {
      path: '/search',
      name: 'search',
      component: () => import('@/modules/search/SearchPage.vue')
    },
    {
      path: '/social',
      name: 'social',
      component: () => import('@/modules/social/SocialHubPage.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/vip',
      name: 'vip',
      component: () => import('@/modules/user/VipCenterPage.vue')
    },
    {
      path: '/points',
      name: 'points',
      component: () => import('@/modules/user/PointsPage.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/data-lab',
      name: 'data-lab',
      component: () => import('@/modules/lab/DataLabPage.vue')
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('@/modules/admin/AdminConsolePage.vue'),
      meta: { requiresAuth: true, requiresAdmin: true }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/modules/auth/LoginPage.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/modules/auth/RegisterPage.vue')
    }
  ]
})

router.beforeEach(async to => {
  const auth = useAuthStore()
  const storedToken = localStorage.getItem('cw_token')
  if (auth.token && !storedToken) {
    auth.clear()
  }
  if (to.name === 'login' || to.name === 'register') {
    if (auth.token) {
      if (!auth.profile) {
        await auth.loadProfile()
      }
      if (auth.profile) {
        return auth.profile.isAdmin ? { name: 'admin' } : { name: 'home' }
      }
    }
    return true
  }
  if (to.meta?.requiresAuth && !auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta?.requiresAdmin) {
    if (!auth.profile && auth.token) {
      await auth.loadProfile()
    }
    if (!auth.profile?.isAdmin) {
      return { name: 'home' }
    }
  }
  return true
})

export default router
