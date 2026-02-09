<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { gsap } from 'gsap'
import { UserAPI } from '@/api/user'
import type { UserProfileVO, UserStatsVO } from '@/types/user'
import RecipeFeed from '@/components/business/RecipeFeed.vue'
import BadgeWall3D from './components/BadgeWall3D.vue'

// State
const profile = ref<UserProfileVO | null>(null)
const stats = ref<UserStatsVO | null>(null)
const loading = ref(true)
const activeTab = ref('recipes') // 'recipes' | 'likes' | 'about'
const showBadgeWall = ref(false)

// Data Fetching
const fetchData = async () => {
  const [profileRes, statsRes] = await Promise.all([
    UserAPI.getProfile(),
    UserAPI.getStats()
  ])
  
  if (profileRes.code === 200) profile.value = profileRes.data
  if (statsRes.code === 200) stats.value = statsRes.data
  loading.value = false
  
  // Animation after load
  setTimeout(() => {
    gsap.from('.stat-item', {
      y: 20,
      opacity: 0,
      duration: 0.6,
      stagger: 0.1,
      ease: 'power2.out'
    })
  }, 100)
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div v-if="loading" class="min-h-screen bg-dark-bg flex items-center justify-center">
    <div class="text-primary animate-pulse">Loading Profile...</div>
  </div>

  <div v-else-if="profile && stats" class="profile-page min-h-screen bg-dark-bg text-white pb-20">
    
    <!-- 1. Header with Parallax Cover -->
    <header class="relative h-[400px] overflow-hidden group">
      <div class="absolute inset-0 bg-gray-900">
        <img 
          :src="profile.bgImageUrl" 
          class="w-full h-full object-cover opacity-60 group-hover:scale-105 transition-transform duration-[20s] ease-linear" 
        />
        <div class="absolute inset-0 bg-gradient-to-t from-dark-bg via-dark-bg/50 to-transparent"></div>
      </div>

      <!-- User Info Overlay -->
      <div class="absolute bottom-0 left-0 w-full p-8 md:p-12 flex flex-col md:flex-row items-end md:items-center gap-8">
        
        <!-- Back Home Button (Absolute Top Left of Header) -->
        <button @click="$router.push('/')" class="absolute top-8 left-8 flex items-center gap-2 text-white/70 hover:text-white transition-colors z-30 group">
          <div class="w-10 h-10 rounded-full bg-black/20 backdrop-blur flex items-center justify-center border border-white/10 group-hover:bg-primary group-hover:text-black group-hover:border-primary transition-all">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
          </div>
          <span class="font-bold tracking-wider text-sm">BACK HOME</span>
        </button>

        <!-- Avatar with Badge Indicator -->
        <div class="relative group/avatar cursor-pointer" @click="showBadgeWall = true">
          <div class="w-32 h-32 rounded-full border-4 border-dark-bg overflow-hidden relative z-10">
            <img :src="profile.avatarUrl" class="w-full h-full object-cover" />
          </div>
          <!-- 3D Badge Indicator -->
          <div class="absolute -bottom-2 -right-2 bg-primary text-black text-xs font-bold px-3 py-1 rounded-full z-20 animate-bounce">
            View Badges
          </div>
          <!-- Glow -->
          <div class="absolute inset-0 bg-primary/30 blur-2xl rounded-full opacity-0 group-hover/avatar:opacity-100 transition-opacity duration-500"></div>
        </div>

        <!-- Text Info -->
        <div class="flex-1 mb-2">
          <div class="flex items-center gap-4 mb-2">
            <h1 class="text-4xl font-serif font-bold">{{ profile.nickname }}</h1>
            <span v-if="profile.isMasterChef" class="bg-primary text-black text-xs font-bold px-2 py-0.5 rounded uppercase tracking-wider">
              {{ profile.masterTitle }}
            </span>
          </div>
          <p class="text-gray-300 max-w-xl">{{ profile.signature }}</p>
        </div>

        <!-- Action Buttons -->
        <div class="flex gap-4 mb-4">
          <button class="px-8 py-3 bg-white text-black font-bold rounded-full hover:bg-gray-200 transition-colors">
            Follow
          </button>
          <button class="w-12 h-12 rounded-full border border-gray-600 flex items-center justify-center hover:bg-gray-800 transition-colors">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"></path></svg>
          </button>
        </div>
      </div>
    </header>

    <!-- 2. Stats Bar (Odometer style placeholder) -->
    <div class="border-b border-gray-800 bg-dark-bg/80 backdrop-blur sticky top-0 z-40">
      <div class="max-w-7xl mx-auto px-8 py-4 flex justify-between items-center">
        <div class="flex gap-12">
          <div class="stat-item text-center cursor-pointer hover:text-primary transition-colors">
            <div class="text-xl font-bold font-serif">{{ stats.recipeCount }}</div>
            <div class="text-xs text-gray-500 uppercase tracking-widest">Recipes</div>
          </div>
          <div class="stat-item text-center cursor-pointer hover:text-primary transition-colors">
            <div class="text-xl font-bold font-serif">{{ (stats.followerCount / 1000000).toFixed(1) }}M</div>
            <div class="text-xs text-gray-500 uppercase tracking-widest">Followers</div>
          </div>
          <div class="stat-item text-center cursor-pointer hover:text-primary transition-colors">
            <div class="text-xl font-bold font-serif">{{ (stats.likeCount / 1000000).toFixed(1) }}M</div>
            <div class="text-xs text-gray-500 uppercase tracking-widest">Likes</div>
          </div>
        </div>
        
        <!-- Tabs -->
        <div class="flex gap-8 text-sm font-medium">
          <button 
            v-for="tab in ['recipes', 'likes', 'about']" 
            :key="tab"
            @click="activeTab = tab"
            class="capitalize relative py-4 transition-colors"
            :class="activeTab === tab ? 'text-white' : 'text-gray-500 hover:text-gray-300'"
          >
            {{ tab }}
            <span v-if="activeTab === tab" class="absolute bottom-0 left-0 w-full h-0.5 bg-primary layout-id='active-tab'"></span>
          </button>
        </div>
      </div>
    </div>

    <!-- 3. Content Area -->
    <main class="max-w-7xl mx-auto px-4 md:px-8 py-12">
      <!-- Recipes Tab -->
      <transition name="fade" mode="out-in">
        <div v-if="activeTab === 'recipes'" key="recipes">
          <RecipeFeed />
        </div>
        
        <div v-else-if="activeTab === 'likes'" key="likes" class="text-center py-20 text-gray-600">
          Likes list placeholder...
        </div>
        
        <div v-else key="about" class="text-center py-20 text-gray-600">
          About section placeholder...
        </div>
      </transition>
    </main>

    <!-- 3D Badge Wall Modal -->
    <BadgeWall3D 
      v-if="showBadgeWall" 
      :badges="stats.badges" 
      @close="showBadgeWall = false" 
    />

  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
