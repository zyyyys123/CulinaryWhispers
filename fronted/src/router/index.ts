import { createRouter, createWebHistory } from 'vue-router'

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
      path: '/user/profile',
      name: 'user-profile',
      component: () => import('@/modules/user/UserProfile.vue')
    },
    {
      path: '/market',
      name: 'market',
      component: () => import('@/modules/commerce/MarketPage.vue')
    }
  ]
})

export default router
