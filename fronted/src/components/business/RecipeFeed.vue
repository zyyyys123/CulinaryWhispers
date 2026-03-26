<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useWindowSize } from '@vueuse/core'
import { gsap } from 'gsap'
import { RecipeAPI } from '@/api/recipe'
import type { RecipePageVO } from '@/types/recipe'
import RecipeCard from './RecipeCard.vue'
import RecipeSkeleton from '@/components/feedback/RecipeSkeleton.vue'
import CwErrorState from '@/components/feedback/CwErrorState.vue'
import CwEmptyState from '@/components/feedback/CwEmptyState.vue'
import CwListFooter from '@/components/feedback/CwListFooter.vue'

const props = defineProps<{
  authorId?: string
}>()

// State
const recipes = ref<RecipePageVO[]>([])
const loading = ref(false)
const errorMessage = ref('')
const page = ref(1)
const hasMore = ref(true)
const isAnimationEnabled = ref(true) // 控制动画的开关
const loadMoreTrigger = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

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
    // 强制每次只加载 6 个，严格控制初次加载和后续加载的数量
    const pageSize = 6
    const res = props.authorId
      ? await RecipeAPI.getList({ page: page.value, size: pageSize, authorId: Number(props.authorId) })
      : await RecipeAPI.recommend({ page: page.value, size: pageSize })
    if (res.code === 200) {
      const newRecipes = res.data?.records ?? []
      if (newRecipes.length === 0) {
        hasMore.value = false
      } else {
        recipes.value.push(...newRecipes)
        page.value++
        
        // 只有在数据量少于 pageSize 时才判定没有更多了
        if (newRecipes.length < pageSize) {
          hasMore.value = false
        }

        // 动画开关检查
        if (isAnimationEnabled.value) {
          nextTick(() => {
            animateEntrance()
          })
        }
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
  if (!isAnimationEnabled.value) return

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
        document.querySelectorAll('.recipe-card.new-item').forEach(el => {
          el.classList.remove('new-item')
        })
      }
    }
  )
}

const toggleAnimation = () => {
  isAnimationEnabled.value = !isAnimationEnabled.value
}

// 自动滚动监听：只有用户开始滚动且触发 trigger 时才加载下一页
const setupObserver = () => {
  if (observer) observer.disconnect()
  
  observer = new IntersectionObserver((entries) => {
    // entries[0].isIntersecting 表示 trigger 进入了视口
    if (entries[0].isIntersecting && !loading.value && hasMore.value && recipes.value.length > 0) {
      fetchRecipes()
    }
  }, {
    rootMargin: '100px', // 距离底部 100px 时开始加载，避免加载过早
    threshold: 0.1
  })

  if (loadMoreTrigger.value) {
    observer.observe(loadMoreTrigger.value)
  }
}

onMounted(() => {
  fetchRecipes() // 初次加载 6 个
  setupObserver()
})

onUnmounted(() => {
  if (observer) observer.disconnect()
})
</script>

<template>
  <div class="recipe-feed w-full">
    <!-- 顶部控制栏 -->
    <div class="flex justify-end mb-4">
      <button 
        class="flex items-center gap-2 px-3 py-1.5 text-sm font-medium rounded-full transition-all duration-200"
        :class="[
          isAnimationEnabled 
            ? 'bg-primary/10 text-primary hover:bg-primary/20' 
            : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
        ]"
        @click="toggleAnimation"
      >
        <span class="relative flex h-2 w-2">
          <span 
            v-if="isAnimationEnabled"
            class="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"
          ></span>
          <span 
            class="relative inline-flex rounded-full h-2 w-2"
            :class="isAnimationEnabled ? 'bg-primary' : 'bg-gray-400'"
          ></span>
        </span>
        {{ isAnimationEnabled ? '列表动画已开启' : '列表动画已暂停' }}
      </button>
    </div>

    <CwErrorState v-if="errorMessage" class="mb-6" :message="errorMessage" action-label="重试" @action="fetchRecipes" />

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
    
    <CwEmptyState
      v-if="!loading && !errorMessage && recipes.length === 0"
      title="暂无食谱"
      description="试试搜索或换个关键词。"
      action-label="重新加载"
      @action="fetchRecipes"
    />

    <!-- Load More Trigger (Infinite Scroll) -->
    <div ref="loadMoreTrigger" class="h-20 w-full mt-4 flex items-center justify-center">
      <CwListFooter
        v-if="!errorMessage && recipes.length > 0"
        :loading="loading"
        :hasMore="hasMore"
        load-more-label="查看更多菜谱"
        loading-label="正在为您寻觅美食…"
        end-label="已经到底啦，去发布一个吧！"
        @loadMore="fetchRecipes"
      />
    </div>
  </div>
</template>
