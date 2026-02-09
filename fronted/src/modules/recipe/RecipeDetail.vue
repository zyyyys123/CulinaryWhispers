<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { RecipeAPI } from '@/api/recipe'
import type { RecipeDetailVO } from '@/types/recipe'
import AIAssistantOrb from '@/components/visual/AIAssistantOrb.vue'
import { NIcon, NRate } from 'naive-ui'

gsap.registerPlugin(ScrollTrigger)

const route = useRoute()
const router = useRouter()
const recipe = ref<RecipeDetailVO | null>(null)
const loading = ref(true)
const isZenMode = ref(false)

// Mock Data Fetch
const fetchDetail = async () => {
  const id = route.params.id as string
  const res = await RecipeAPI.getDetail(id)
  if (res.code === 200) {
    recipe.value = res.data
    // After data loaded, trigger animations
    setTimeout(() => initAnimations(), 100)
  }
  loading.value = false
}

// GSAP Animations
const initAnimations = () => {
  // Parallax Header Image
  gsap.to('.hero-bg', {
    yPercent: 30,
    ease: 'none',
    scrollTrigger: {
      trigger: '.hero-header',
      start: 'top top',
      end: 'bottom top',
      scrub: true
    }
  })

  // Staggered Content Entrance
  gsap.from('.content-block', {
    y: 30,
    opacity: 0,
    duration: 0.8,
    stagger: 0.1,
    scrollTrigger: {
      trigger: '.recipe-content',
      start: 'top 80%'
    }
  })
}

// Zen Mode Toggle
const toggleZenMode = () => {
  isZenMode.value = !isZenMode.value
  if (isZenMode.value) {
    document.body.style.overflow = 'hidden' // Prevent scrolling
  } else {
    document.body.style.overflow = ''
  }
}

onMounted(() => {
  fetchDetail()
})

onUnmounted(() => {
  document.body.style.overflow = '' // Cleanup
  ScrollTrigger.getAll().forEach(t => t.kill())
})
</script>

<template>
  <div v-if="loading" class="min-h-screen bg-dark-bg flex items-center justify-center">
    <div class="text-primary animate-pulse">Loading Recipe...</div>
  </div>

  <div v-else-if="recipe" class="recipe-detail bg-dark-bg min-h-screen text-white relative">
    
    <!-- 1. Hero Header (Video/Image Parallax) -->
    <header class="hero-header relative h-[80vh] overflow-hidden">
      <!-- Background (Parallax Target) -->
      <div class="hero-bg absolute inset-0 w-full h-[120%] -top-[10%]">
        <img :src="recipe.coverUrl" class="w-full h-full object-cover filter brightness-75" />
        <div class="absolute inset-0 bg-gradient-to-t from-dark-bg via-transparent to-transparent"></div>
      </div>

      <!-- Hero Content -->
      <div class="absolute bottom-0 left-0 w-full p-8 md:p-20 z-10">
        <div class="max-w-4xl mx-auto">
          <!-- Tags -->
          <div class="flex gap-3 mb-4">
            <span v-for="tag in recipe.tags" :key="tag" class="px-3 py-1 bg-white/10 backdrop-blur rounded-full text-xs uppercase tracking-widest text-primary border border-white/10">
              {{ tag }}
            </span>
          </div>
          
          <!-- Title -->
          <h1 class="text-5xl md:text-7xl font-serif mb-6 leading-tight shadow-black drop-shadow-lg">
            {{ recipe.title }}
          </h1>

          <!-- Meta Row -->
          <div class="flex items-center gap-8 text-sm md:text-base font-medium text-gray-200">
            <div class="flex items-center gap-2">
              <img :src="recipe.authorAvatar" class="w-8 h-8 rounded-full border border-primary/50" />
              <span>By {{ recipe.authorName }}</span>
            </div>
            <div class="flex items-center gap-2">
              <NRate readonly :default-value="recipe.score" size="small" allow-half />
              <span>{{ recipe.score }}</span>
            </div>
            <div>{{ recipe.timeCost }} mins</div>
            <div>{{ recipe.calories }} kcal</div>
          </div>
        </div>
      </div>
      
      <!-- Back Button -->
      <button @click="router.back()" class="absolute top-8 left-8 z-20 w-10 h-10 flex items-center justify-center rounded-full bg-black/20 backdrop-blur hover:bg-primary hover:text-black transition-all">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
      </button>
    </header>

    <!-- 2. Main Content -->
    <main class="recipe-content max-w-4xl mx-auto px-6 py-20 grid grid-cols-1 md:grid-cols-[1fr_300px] gap-12">
      
      <!-- Left Column: Description & Steps -->
      <div class="space-y-12">
        <!-- Description -->
        <section class="content-block">
          <p class="text-xl text-gray-300 font-serif leading-relaxed italic border-l-2 border-primary pl-6">
            "{{ recipe.description }}"
          </p>
        </section>

        <!-- Ingredients (Mobile Only view, hidden on Desktop if needed, but here we show inline) -->
        <section class="content-block md:hidden">
          <h3 class="text-2xl font-serif text-primary mb-6">Ingredients</h3>
          <ul class="space-y-3">
            <li v-for="(ing, i) in recipe.ingredients" :key="i" class="flex justify-between border-b border-gray-800 pb-2">
              <span>{{ ing.name }}</span>
              <span class="text-gray-400">{{ ing.amount }}</span>
            </li>
          </ul>
        </section>

        <!-- Steps -->
        <section class="content-block">
          <div class="flex items-center justify-between mb-8">
            <h3 class="text-3xl font-serif text-primary">Instructions</h3>
            <button @click="toggleZenMode" class="flex items-center gap-2 px-4 py-2 bg-gray-800 rounded-full hover:bg-primary hover:text-black transition-colors">
              <span class="w-2 h-2 rounded-full bg-green-400 animate-pulse"></span>
              <span class="text-sm uppercase tracking-wider font-bold">Zen Mode</span>
            </button>
          </div>

          <div class="space-y-10">
            <div v-for="step in recipe.steps" :key="step.stepNo" class="group relative pl-8 border-l border-gray-800 hover:border-primary transition-colors duration-300">
              <span class="absolute -left-[9px] top-0 w-4 h-4 rounded-full bg-dark-bg border-2 border-gray-600 group-hover:border-primary transition-colors"></span>
              
              <h4 class="text-lg font-bold text-gray-400 mb-2 group-hover:text-white transition-colors">Step {{ step.stepNo }}</h4>
              <p class="text-gray-300 leading-relaxed mb-4">{{ step.desc }}</p>
              
              <!-- Key Step Highlight -->
              <div v-if="step.isKeyStep" class="inline-flex items-center gap-2 px-3 py-1 bg-yellow-500/10 text-yellow-500 text-xs rounded border border-yellow-500/20">
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
                Key Step
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- Right Column: Ingredients (Sticky) -->
      <aside class="hidden md:block">
        <div class="sticky top-10 p-8 bg-dark-surface rounded-2xl border border-gray-800">
          <h3 class="text-2xl font-serif text-primary mb-6">Ingredients</h3>
          <ul class="space-y-4">
            <li v-for="(ing, i) in recipe.ingredients" :key="i" class="flex justify-between items-center group cursor-pointer">
              <!-- Checkbox interaction -->
              <label class="flex items-center gap-3 cursor-pointer">
                <input type="checkbox" class="form-checkbox rounded bg-gray-700 border-gray-600 text-primary focus:ring-0" />
                <span class="group-hover:text-white transition-colors text-gray-300">{{ ing.name }}</span>
              </label>
              <span class="text-sm text-gray-500">{{ ing.amount }}</span>
            </li>
          </ul>
          
          <!-- Nutrition -->
          <div class="mt-10 pt-6 border-t border-gray-700">
            <h4 class="text-sm uppercase tracking-widest text-gray-500 mb-4">Nutrition per serving</h4>
            <div class="grid grid-cols-3 gap-4 text-center">
              <div>
                <div class="text-xl font-bold text-white">{{ recipe.protein }}g</div>
                <div class="text-xs text-gray-500">Protein</div>
              </div>
              <div>
                <div class="text-xl font-bold text-white">{{ recipe.fat }}g</div>
                <div class="text-xs text-gray-500">Fat</div>
              </div>
              <div>
                <div class="text-xl font-bold text-white">{{ recipe.carbs }}g</div>
                <div class="text-xs text-gray-500">Carbs</div>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </main>

    <!-- 3. Zen Mode Overlay -->
    <transition name="zen-fade">
      <div v-if="isZenMode" class="fixed inset-0 z-50 bg-black flex flex-col items-center justify-center p-8">
        <button @click="toggleZenMode" class="absolute top-8 right-8 text-gray-500 hover:text-white transition-colors">
          <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
        </button>

        <div class="text-center max-w-3xl space-y-8">
          <h2 class="text-primary text-sm tracking-[0.5em] uppercase">Step 2 of {{ recipe.steps.length }}</h2>
          <p class="text-4xl md:text-6xl font-serif leading-tight text-white">
            "{{ recipe.steps[1].desc }}"
          </p>
          
          <div class="flex justify-center gap-8 mt-12">
             <button class="w-16 h-16 rounded-full border border-gray-700 flex items-center justify-center hover:bg-white hover:text-black transition-all">
               <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path></svg>
             </button>
             <button class="w-16 h-16 rounded-full border border-gray-700 flex items-center justify-center hover:bg-white hover:text-black transition-all">
               <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg>
             </button>
          </div>
        </div>

        <!-- AI Assistant Orb (Fixed at bottom) -->
        <div class="absolute bottom-10 left-1/2 -translate-x-1/2 flex flex-col items-center gap-4">
          <AIAssistantOrb status="listening" />
          <span class="text-xs text-gray-500 tracking-widest">LISTENING...</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.zen-fade-enter-active,
.zen-fade-leave-active {
  transition: opacity 0.5s ease;
}

.zen-fade-enter-from,
.zen-fade-leave-to {
  opacity: 0;
}
</style>
