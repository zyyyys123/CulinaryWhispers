<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { gsap } from 'gsap'
import HeroScene from '@/components/visual/HeroScene.vue'
import StorybookSplash from '@/components/visual/StorybookSplash.vue'

const titleRef = ref(null)
const subtitleRef = ref(null)

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
      <div class="absolute bottom-10 left-1/2 -translate-x-1/2 animate-bounce z-20">
        <svg class="w-6 h-6 text-white opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3"></path>
        </svg>
      </div>
    </div>

    <!-- Featured Section (Placeholder for Masonry) -->
    <div class="content min-h-screen bg-dark-bg p-8 md:p-20 relative z-20">
      <div class="max-w-7xl mx-auto">
        <div class="flex items-end justify-between mb-12">
          <h2 class="text-5xl font-serif text-primary">Featured Recipes</h2>
          <button class="text-white hover:text-primary transition-colors underline decoration-1 underline-offset-8">View All</button>
        </div>
        
        <!-- Grid Placeholder -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <div v-for="i in 6" :key="i" class="group relative aspect-[3/4] bg-dark-surface rounded-2xl overflow-hidden cursor-pointer">
            <!-- Image Placeholder -->
            <div class="absolute inset-0 bg-gray-800 transition-transform duration-700 group-hover:scale-110"></div>
            <!-- Content Overlay -->
            <div class="absolute inset-0 bg-gradient-to-t from-black/80 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex flex-col justify-end p-6">
              <h3 class="text-2xl font-serif text-white mb-2 translate-y-4 group-hover:translate-y-0 transition-transform duration-300">Recipe Title {{ i }}</h3>
              <p class="text-sm text-gray-300 translate-y-4 group-hover:translate-y-0 transition-transform duration-300 delay-75">By Master Chef</p>
            </div>
          </div>
        </div>
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
