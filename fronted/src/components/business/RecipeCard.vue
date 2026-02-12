<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { RecipePageVO } from '@/types/recipe'

// Props
const props = defineProps<{
  data: RecipePageVO
  index?: number
}>()

const router = useRouter()

// Methods
const goToDetail = () => {
  router.push({ name: 'recipe-detail', params: { id: props.data.id } })
}

// Computed
const formattedTime = computed(() => {
  const h = Math.floor(props.data.timeCost / 60)
  const m = props.data.timeCost % 60
  return h > 0 ? `${h}h ${m}m` : `${m}m`
})

const difficultyLabel = computed(() => {
  return ['Easy', 'Medium', 'Hard', 'Expert', 'Master'][props.data.difficulty - 1] || 'Medium'
})
</script>

<template>
  <div @click="goToDetail" class="recipe-card group relative w-full break-inside-avoid mb-6 cursor-pointer">
    <!-- Image Container -->
    <div class="relative w-full overflow-hidden rounded-2xl bg-gray-800 shadow-lg">
      <!-- Aspect Ratio Hack or just natural height -->
      <img 
        :src="data.coverUrl" 
        :alt="data.title"
        loading="lazy"
        class="w-full h-auto object-cover transition-transform duration-700 ease-out group-hover:scale-105"
      />
      
      <!-- Gradient Overlay (Always present but subtly changes) -->
      <div class="absolute inset-0 bg-gradient-to-t from-black/90 via-black/20 to-transparent opacity-60 group-hover:opacity-80 transition-opacity duration-300"></div>

      <!-- Top Right: Difficulty Badge -->
      <div class="absolute top-3 right-3 px-2 py-1 bg-white/10 backdrop-blur-md rounded-full border border-white/20">
        <span class="text-xs font-medium text-white tracking-wide uppercase">{{ difficultyLabel }}</span>
      </div>

      <!-- Content Overlay -->
      <div class="absolute bottom-0 left-0 w-full p-5 transform translate-y-2 group-hover:translate-y-0 transition-transform duration-300 ease-out">
        <!-- Tags -->
        <div class="flex flex-wrap gap-2 mb-2 opacity-0 group-hover:opacity-100 transition-opacity duration-300 delay-100">
          <span 
            v-for="tag in data.tags.slice(0, 2)" 
            :key="tag"
            class="text-[10px] uppercase tracking-wider text-primary bg-primary/10 px-2 py-0.5 rounded-sm backdrop-blur-sm"
          >
            {{ tag }}
          </span>
        </div>

        <!-- Title -->
        <h3 class="text-xl font-serif text-white leading-tight mb-2 group-hover:text-primary transition-colors duration-300">
          {{ data.title }}
        </h3>

        <!-- Meta Info (Author & Stats) -->
        <div class="flex items-center justify-between text-gray-300 text-xs font-medium">
          <div class="flex items-center gap-2">
            <img :src="data.authorAvatar" class="w-5 h-5 rounded-full border border-white/30" />
            <span>{{ data.authorName }}</span>
          </div>
          
          <div class="flex items-center gap-3 opacity-80">
            <span class="flex items-center gap-1">
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
              {{ formattedTime }}
            </span>
            <span class="flex items-center gap-1">
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path></svg>
              {{ data.likeCount }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* High-end glassmorphism hints */
.recipe-card {
  perspective: 1000px;
}
</style>
