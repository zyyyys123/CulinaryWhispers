<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { gsap } from 'gsap'
import { UserAPI } from '@/api/user'
import type { UserProfileVO, UserStatsVO } from '@/types/user'
import RecipeFeed from '@/components/business/RecipeFeed.vue'
import BadgeWall3D from './components/BadgeWall3D.vue'

// State
const profile = ref<UserProfileVO | null>(null)
const stats = ref<UserStatsVO | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const activeTab = ref('recipes') // 'recipes' | 'likes' | 'about'
const showBadgeWall = ref(false)
const showEdit = ref(false)
const saving = ref(false)

const router = useRouter()
const route = useRoute()

const editForm = ref({
  nickname: '',
  avatarUrl: '',
  gender: 0,
  signature: '',
  city: '',
  job: '',
  cookAge: 0,
  favoriteCuisine: '',
  tastePreference: '',
  dietaryRestrictions: ''
})

const coverUrl = computed(() => {
  const bg = profile.value?.bgImageUrl
  return bg && bg.trim().length > 0
    ? bg
    : 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1800&q=80'
})

const masterLabel = computed(() => {
  if (!profile.value?.isMasterChef) return ''
  const t = profile.value.masterTitle
  return t && t.trim().length > 0 ? t : 'MASTER CHEF'
})

const formatCount = (n: number) => {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`
  return String(n)
}

// Data Fetching
const fetchData = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const [profileRes, statsRes] = await Promise.all([
      UserAPI.getProfile(),
      UserAPI.getStats()
    ])
    
    if (profileRes.code === 200) profile.value = profileRes.data
    if (statsRes.code === 200) stats.value = statsRes.data
    if (!profile.value || !stats.value) {
      errorMessage.value = '加载失败，请稍后重试'
    }
  } catch (e: any) {
    const status = e?.response?.status
    if (status === 401) {
      localStorage.removeItem('cw_token')
      errorMessage.value = '登录已失效，请重新登录'
      await router.replace({ name: 'login', query: { redirect: route.fullPath } })
      return
    }
    errorMessage.value = '加载失败，请检查网络或稍后重试'
  } finally {
    loading.value = false
  }

  if (profile.value) {
    editForm.value = {
      nickname: profile.value.nickname ?? '',
      avatarUrl: profile.value.avatarUrl ?? '',
      gender: profile.value.gender ?? 0,
      signature: profile.value.signature ?? '',
      city: profile.value.city ?? '',
      job: profile.value.job ?? '',
      cookAge: profile.value.cookAge ?? 0,
      favoriteCuisine: profile.value.favoriteCuisine ?? '',
      tastePreference: profile.value.tastePreference ?? '',
      dietaryRestrictions: profile.value.dietaryRestrictions ?? ''
    }
  }
  
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

const saveProfile = async () => {
  if (!profile.value || saving.value) return
  saving.value = true
  try {
    const res = await UserAPI.updateProfile({
      nickname: editForm.value.nickname,
      avatarUrl: editForm.value.avatarUrl,
      gender: editForm.value.gender,
      signature: editForm.value.signature,
      city: editForm.value.city,
      job: editForm.value.job,
      cookAge: editForm.value.cookAge,
      favoriteCuisine: editForm.value.favoriteCuisine,
      tastePreference: editForm.value.tastePreference,
      dietaryRestrictions: editForm.value.dietaryRestrictions
    })
    if (res.code === 200) {
      showEdit.value = false
      await fetchData()
    }
  } finally {
    saving.value = false
  }
}

const logout = () => {
  localStorage.removeItem('cw_token')
  router.replace({ name: 'home' })
}
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
          :src="coverUrl" 
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
              {{ masterLabel }}
            </span>
          </div>
          <p class="text-gray-300 max-w-xl">{{ profile.signature }}</p>
        </div>

        <!-- Action Buttons -->
        <div class="flex gap-4 mb-4">
          <button @click="showEdit = true" class="px-8 py-3 bg-white text-black font-bold rounded-full hover:bg-gray-200 transition-colors">
            Edit Profile
          </button>
          <button @click="logout" class="w-12 h-12 rounded-full border border-gray-600 flex items-center justify-center hover:bg-gray-800 transition-colors" title="Logout">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6A2.25 2.25 0 005.25 5.25v13.5A2.25 2.25 0 007.5 21h6A2.25 2.25 0 0015.75 18.75V15M18 15l3-3m0 0l-3-3m3 3H9"></path></svg>
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
            <div class="text-xl font-bold font-serif">{{ formatCount(stats.followerCount) }}</div>
            <div class="text-xs text-gray-500 uppercase tracking-widest">Followers</div>
          </div>
          <div class="stat-item text-center cursor-pointer hover:text-primary transition-colors">
            <div class="text-xl font-bold font-serif">{{ formatCount(stats.likeCount) }}</div>
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
        
        <div v-else key="about" class="max-w-2xl mx-auto">
          <div class="bg-black/20 border border-white/10 rounded-2xl p-6 md:p-8">
            <div class="text-sm tracking-widest uppercase text-gray-500 mb-6">About</div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">City</div>
                <div class="text-gray-200">{{ profile.city || '-' }}</div>
              </div>
              <div>
                <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Job</div>
                <div class="text-gray-200">{{ profile.job || '-' }}</div>
              </div>
              <div>
                <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Cook Age</div>
                <div class="text-gray-200">{{ profile.cookAge ?? '-' }}</div>
              </div>
              <div>
                <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Gender</div>
                <div class="text-gray-200">{{ profile.gender === 1 ? 'Male' : profile.gender === 2 ? 'Female' : 'Unknown' }}</div>
              </div>
              <div class="md:col-span-2">
                <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Favorite Cuisine</div>
                <div class="text-gray-200 whitespace-pre-wrap">{{ profile.favoriteCuisine || '-' }}</div>
              </div>
              <div class="md:col-span-2">
                <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Taste Preference</div>
                <div class="text-gray-200 whitespace-pre-wrap">{{ profile.tastePreference || '-' }}</div>
              </div>
              <div class="md:col-span-2">
                <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Dietary Restrictions</div>
                <div class="text-gray-200 whitespace-pre-wrap">{{ profile.dietaryRestrictions || '-' }}</div>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </main>

    <!-- 3D Badge Wall Modal -->
    <BadgeWall3D 
      v-if="showBadgeWall" 
      :badges="stats.badges" 
      @close="showBadgeWall = false" 
    />

    <div v-if="showEdit" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur">
      <div class="w-[92vw] max-w-2xl bg-dark-bg border border-white/10 rounded-2xl p-6 md:p-8">
        <div class="flex items-center justify-between mb-6">
          <div class="text-sm tracking-widest uppercase text-gray-400">Edit Profile</div>
          <button @click="showEdit = false" class="text-gray-400 hover:text-white transition-colors">✕</button>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Nickname</div>
            <input v-model="editForm.nickname" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
          <div>
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Avatar URL</div>
            <input v-model="editForm.avatarUrl" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
          <div>
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Gender</div>
            <select v-model.number="editForm.gender" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary">
              <option :value="0">Unknown</option>
              <option :value="1">Male</option>
              <option :value="2">Female</option>
            </select>
          </div>
          <div>
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">City</div>
            <input v-model="editForm.city" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
          <div>
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Job</div>
            <input v-model="editForm.job" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
          <div>
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Cook Age</div>
            <input v-model.number="editForm.cookAge" type="number" min="0" max="80" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
          <div class="md:col-span-2">
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Signature</div>
            <textarea v-model="editForm.signature" rows="2" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary"></textarea>
          </div>
          <div class="md:col-span-2">
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Favorite Cuisine</div>
            <input v-model="editForm.favoriteCuisine" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
          <div class="md:col-span-2">
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Taste Preference</div>
            <input v-model="editForm.tastePreference" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
          <div class="md:col-span-2">
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Dietary Restrictions</div>
            <input v-model="editForm.dietaryRestrictions" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-8">
          <button @click="showEdit = false" class="px-6 py-3 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors">
            Cancel
          </button>
          <button @click="saveProfile" :disabled="saving" class="px-6 py-3 rounded-full bg-primary text-black font-bold disabled:opacity-60">
            {{ saving ? 'Saving...' : 'Save' }}
          </button>
        </div>
      </div>
    </div>

  </div>
  
  <div v-else class="min-h-screen bg-dark-bg flex flex-col items-center justify-center text-white px-6">
    <div class="text-lg font-bold mb-2">User Profile</div>
    <div class="text-gray-400 mb-6">{{ errorMessage || '加载失败' }}</div>
    <div class="flex gap-3">
      <button @click="fetchData" class="px-6 py-3 rounded-full bg-primary text-black font-bold">
        Retry
      </button>
      <button @click="router.replace({ name: 'login', query: { redirect: route.fullPath } })" class="px-6 py-3 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors">
        Go Login
      </button>
    </div>
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
