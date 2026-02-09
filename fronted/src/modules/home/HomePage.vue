<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { gsap } from 'gsap'

const titleRef = ref(null)
const subtitleRef = ref(null)

onMounted(() => {
  // Simple entrance animation
  const tl = gsap.timeline()
  
  tl.from(titleRef.value, {
    y: 100,
    opacity: 0,
    duration: 1.5,
    ease: 'power4.out',
    delay: 0.5
  })
  .from(subtitleRef.value, {
    y: 50,
    opacity: 0,
    duration: 1,
    ease: 'power3.out'
  }, '-=1')
})
</script>

<template>
  <div class="home-page">
    <div class="hero min-h-screen flex flex-col justify-center items-center relative overflow-hidden">
      <!-- Background Placeholder (3D Scene would go here) -->
      <div class="absolute inset-0 bg-gradient-to-b from-dark-bg to-black z-0"></div>
      
      <!-- Content -->
      <div class="z-10 text-center px-4">
        <h1 ref="titleRef" class="text-6xl md:text-9xl font-serif text-primary mb-6 mix-blend-overlay">
          Culinary <br /> Whispers
        </h1>
        <p ref="subtitleRef" class="text-xl md:text-2xl font-sans text-gray-300 tracking-widest uppercase">
          Taste the Sound, Feel the Flavor
        </p>
      </div>

      <!-- Scroll Indicator -->
      <div class="absolute bottom-10 left-1/2 -translate-x-1/2 animate-bounce">
        <span class="text-white text-opacity-50 text-sm">SCROLL</span>
      </div>
    </div>

    <!-- Content Placeholder for Scrolling -->
    <div class="content min-h-screen bg-dark-surface p-20">
      <h2 class="text-4xl font-serif text-white mb-10">Featured Recipes</h2>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div v-for="i in 3" :key="i" class="aspect-[3/4] bg-gray-800 rounded-xl hover:scale-105 transition-transform duration-500"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hero {
  perspective: 1000px;
}
</style>
