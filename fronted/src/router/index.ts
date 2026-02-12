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
      path: '/market',
      name: 'market',
      component: () => import('@/modules/commerce/MarketPage.vue')
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

router.beforeEach(to => {
  if (to.name === 'login' || to.name === 'register') return true
  const auth = useAuthStore()
  if (to.meta?.requiresAuth && !auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
