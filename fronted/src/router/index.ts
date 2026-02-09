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
    }
  ]
})

export default router
