<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useWindowSize } from '@vueuse/core'
import { gsap } from 'gsap'
import { RecipeAPI } from '@/api/recipe'
import type { RecipePageVO } from '@/types/recipe'
import RecipeCard from './RecipeCard.vue'
import RecipeSkeleton from '@/components/feedback/RecipeSkeleton.vue'

// State
const recipes = ref<RecipePageVO[]>([])
const loading = ref(false)
const errorMessage = ref('')
const page = ref(1)
const hasMore = ref(true)

// Masonry Logic
const { width } = useWindowSize()
const columnCount = computed(() => {
  const w = width.value || (typeof window !== 'undefined' ? window.innerWidth : 1024)
  if (w < 640) return 1
  if (w < 1024) return 2
  return 3
})

const columns = computed(() => {
  const cols: RecipePageVO[][] = Array.from({ length: columnCount.value }, () => [])
  recipes.value.forEach((recipe, i) => {
    cols[i % columnCount.value].push(recipe)
  })
  return cols
})

// Data Fetching
const fetchRecipes = async () => {
  if (loading.value || !hasMore.value) return
  
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await RecipeAPI.recommend({ page: page.value, size: 6 }) // 6 items per batch
    if (res.code === 200) {
      const newRecipes = res.data?.records ?? []
      if (newRecipes.length === 0) {
        hasMore.value = false
      } else {
        recipes.value.push(...newRecipes)
        page.value++
        
        // Animate new items
        nextTick(() => {
          animateEntrance()
        })
      }
    } else {
      errorMessage.value = res.message || '加载失败，请稍后重试'
    }
  } catch {
    errorMessage.value = '加载失败，请检查网络或稍后重试'
  } finally {
    loading.value = false
  }
}

// GSAP Animation
const animateEntrance = () => {
  gsap.fromTo('.recipe-card.new-item', 
    { 
      y: 50, 
      opacity: 0 
    },
    { 
      y: 0, 
      opacity: 1, 
      duration: 0.8, 
      stagger: 0.1, 
      ease: 'power3.out',
      onComplete: () => {
        // Remove class to prevent re-animating
        document.querySelectorAll('.recipe-card.new-item').forEach(el => {
          el.classList.remove('new-item')
        })
      }
    }
  )
}

onMounted(() => {
  fetchRecipes()
})
</script>

<template>
  <div class="recipe-feed w-full">
    <div v-if="errorMessage" class="mb-6 rounded-2xl border border-white/10 bg-black/20 px-6 py-4 flex items-center justify-between gap-4">
      <div class="text-gray-300 text-sm tracking-wider">{{ errorMessage }}</div>
      <button
        @click="fetchRecipes"
        class="px-5 py-2 rounded-full bg-primary text-black font-bold text-sm"
      >
        Retry
      </button>
    </div>

    <!-- Masonry Grid -->
    <div class="flex gap-6 items-start">
      <div 
        v-for="(col, colIndex) in columns" 
        :key="colIndex" 
        class="flex-1 flex flex-col gap-6"
      >
        <RecipeCard 
          v-for="recipe in col" 
          :key="recipe.id" 
          :data="recipe"
          class="new-item" 
        />
      </div>
    </div>

    <!-- Loading State / Skeletons -->
    <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-6">
      <RecipeSkeleton v-for="i in 3" :key="i" />
    </div>
    
    <div v-if="!loading && !errorMessage && recipes.length === 0" class="text-center mt-12 text-gray-600 text-sm tracking-widest uppercase">
      No Recipes
    </div>

    <!-- Load More Trigger (Simple Button for now, IntersectionObserver later) -->
    <div v-if="!loading && !errorMessage && hasMore" class="flex justify-center mt-12">
      <button 
        @click="fetchRecipes"
        class="group relative px-8 py-3 bg-transparent border border-gray-700 hover:border-primary text-gray-300 hover:text-primary transition-colors duration-300 rounded-full overflow-hidden"
      >
        <span class="relative z-10 text-sm tracking-widest uppercase">Load More</span>
        <div class="absolute inset-0 bg-primary/10 transform scale-x-0 group-hover:scale-x-100 transition-transform origin-left duration-500"></div>
      </button>
    </div>
    
    <div v-if="!loading && !errorMessage && !hasMore" class="text-center mt-12 text-gray-600 text-sm tracking-widest uppercase">
      End of Collection
    </div>
  </div>
</template>
