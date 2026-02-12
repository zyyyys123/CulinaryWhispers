<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { gsap } from 'gsap'
import HeroScene from '@/components/visual/HeroScene.vue'
import StorybookSplash from '@/components/visual/StorybookSplash.vue'
import RecipeFeed from '@/components/business/RecipeFeed.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const titleRef = ref(null)
const subtitleRef = ref(null)
const auth = useAuthStore()

const hasToken = computed(() => Boolean(auth.token))

onMounted(() => {
  if (auth.token && !auth.profile) auth.loadProfile()
})

// Called when StorybookSplash finishes its animation
const onSplashComplete = () => {
  const tl = gsap.timeline()
  
  // Text Entrance (Staggered) - Triggered AFTER splash opens
  tl.from(titleRef.value, {
    y: 100,
    opacity: 0,
    duration: 1.5,
    ease: 'power4.out',
  })
  .from(subtitleRef.value, {
    y: 20,
    opacity: 0,
    duration: 1,
    ease: 'power3.out'
  }, '-=1.2')
}
</script>

<template>
  <div class="home-page bg-dark-bg text-white">
    <!-- New Storybook Splash Screen -->
    <StorybookSplash @complete="onSplashComplete" />

    <!-- Top Navigation / Profile Link -->
    <nav class="absolute top-0 w-full z-30 p-8 flex justify-end gap-6">
        <div @click="router.push({ name: 'market' })" class="cursor-pointer group flex items-center gap-2 text-white hover:text-primary transition-colors">
             <span class="text-xs font-bold uppercase tracking-widest">Market</span>
        </div>

        <div
          v-if="hasToken"
          @click="router.push({ name: 'recipe-publish' })"
          class="cursor-pointer group flex items-center gap-2 text-white hover:text-primary transition-colors"
        >
          <span class="text-xs font-bold uppercase tracking-widest">Publish</span>
        </div>
        
        <div
          v-if="hasToken"
          @click="router.push({ name: 'user-profile' })"
          class="cursor-pointer group flex items-center gap-3"
        >
             <span class="text-xs font-bold uppercase tracking-widest text-white group-hover:text-primary transition-colors">My Profile</span>
             <div class="w-10 h-10 rounded-full border border-white/20 flex items-center justify-center group-hover:border-primary transition-colors">
                <!-- User Icon -->
                <svg class="w-4 h-4 text-white group-hover:text-primary transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
             </div>
        </div>

        <div
          v-else
          @click="router.push({ name: 'login', query: { redirect: '/' } })"
          class="cursor-pointer group flex items-center gap-3"
        >
          <span class="text-xs font-bold uppercase tracking-widest text-white group-hover:text-primary transition-colors">Sign In</span>
          <div class="w-10 h-10 rounded-full border border-white/20 flex items-center justify-center group-hover:border-primary transition-colors">
            <svg class="w-4 h-4 text-white group-hover:text-primary transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6A2.25 2.25 0 005.25 5.25v13.5A2.25 2.25 0 007.5 21h6A2.25 2.25 0 0015.75 18.75V15M18 15l3-3m0 0l-3-3m3 3H9"></path></svg>
          </div>
        </div>
    </nav>

    <!-- Hero Section -->
    <div class="hero h-screen w-full relative overflow-hidden">
      
      <!-- 3D Scene Layer (Background) -->
      <div class="absolute inset-0 z-0">
        <HeroScene />
      </div>
      
      <!-- Gradient Overlay to make text readable -->
      <div class="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-dark-bg z-1 pointer-events-none"></div>
      
      <!-- Text Content Layer -->
      <div class="absolute inset-0 z-10 flex flex-col justify-center items-center pointer-events-none">
        <h1 ref="titleRef" class="text-7xl md:text-9xl font-serif text-white mix-blend-difference mb-4 text-center leading-tight">
          Culinary <br /> Whispers
        </h1>
        <p ref="subtitleRef" class="text-lg md:text-xl font-sans text-primary tracking-[0.5em] uppercase">
          Taste the Sound
        </p>
      </div>

      <!-- Scroll Indicator -->
      <div class="absolute bottom-10 left-1/2 -translate-x-1/2 animate-bounce z-20 flex flex-col items-center gap-2 pointer-events-none">
        <span class="text-[10px] text-gray-400 uppercase tracking-widest">Scroll</span>
        <svg class="w-6 h-6 text-white opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3"></path>
        </svg>
      </div>
    </div>

    <!-- Featured Section (Masonry) -->
    <div class="content min-h-screen bg-dark-bg p-4 md:p-8 lg:p-20 relative z-20">
      <div class="max-w-7xl mx-auto">
        <div class="flex items-end justify-between mb-12 px-2">
          <h2 class="text-4xl md:text-5xl font-serif text-primary">Featured Recipes</h2>
          <button class="hidden md:block text-white hover:text-primary transition-colors underline decoration-1 underline-offset-8 text-sm tracking-widest uppercase">View All Collection</button>
        </div>
        
        <!-- Recipe Feed Component -->
        <RecipeFeed />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Ensure smooth blending */
.mix-blend-difference {
  mix-blend-mode: difference;
}
</style>
