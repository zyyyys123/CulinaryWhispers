<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { gsap } from 'gsap'
import { UserAPI } from '@/api/user'
import type { BadgeVO, UserProfileVO, UserStatsVO } from '@/types/user'
import RecipeFeed from '@/components/business/RecipeFeed.vue'
import BadgeWall2D from './components/BadgeWall2D.vue'
import { SocialAPI } from '@/api/social'
import type { RecipePageVO } from '@/types/recipe'
import RecipeCard from '@/components/business/RecipeCard.vue'
import { useAuthStore } from '@/stores/auth'
import { FileAPI } from '@/api/file'
import { normalizeAssetUrl } from '@/utils/assetUrl'

// State
const profile = ref<UserProfileVO | null>(null)
const stats = ref<UserStatsVO | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const activeTab = ref('recipes') // 'recipes' | 'likes' | 'about'
const showBadgeWall = ref(false)
const showEdit = ref(false)
const saving = ref(false)
const uploadingAvatar = ref(false)
const uploadingBg = ref(false)
const collectLoading = ref(false)
const collectError = ref('')
const collectPage = ref(1)
const collectHasMore = ref(true)
const collectedRecipes = ref<RecipePageVO[]>([])

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const viewingUserId = computed(() => (route.params.id ? String(route.params.id) : ''))
const isPublicView = computed(() => Boolean(viewingUserId.value))
const isSelf = computed(() => !isPublicView.value || viewingUserId.value === auth.profile?.userId)

const editForm = ref({
  nickname: '',
  avatarUrl: '',
  bgImageUrl: '',
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
  const bg = normalizeAssetUrl(profile.value?.bgImageUrl)
  return bg && bg.trim().length > 0
    ? bg
    : 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1800&q=80'
})

const avatarLoadError = ref(false)
const fallbackAvatarUrl = computed(
  () =>
    `https://api.dicebear.com/7.x/thumbs/svg?seed=${encodeURIComponent(profile.value?.nickname || profile.value?.username || 'user')}`
)
const avatarSrc = computed(() => {
  const url = normalizeAssetUrl(profile.value?.avatarUrl) ?? ''
  if (!url || avatarLoadError.value) return fallbackAvatarUrl.value
  return url
})
const onAvatarError = () => {
  avatarLoadError.value = true
}

const masterLabel = computed(() => {
  if (!profile.value?.isMasterChef) return ''
  const t = profile.value.masterTitle
  return t && t.trim().length > 0 ? t : '认证大厨'
})

const formatCount = (n: number) => {
  if (n >= 100_000_000) return `${(n / 100_000_000).toFixed(1)}亿`
  if (n >= 10_000) return `${(n / 10_000).toFixed(1)}万`
  return String(n ?? 0)
}

const genderText = computed(() => {
  const g = profile.value?.gender
  if (g === 1) return '男'
  if (g === 2) return '女'
  return '未知'
})

const moneyText = (n?: number) => {
  const v = Number(n ?? 0)
  if (!Number.isFinite(v)) return '-'
  return `¥${v.toFixed(2)}`
}

const badgeWallBadges = computed<BadgeVO[]>(() => {
  const level = stats.value?.level ?? 1
  const vip = Number(profile.value?.vipLevel ?? 0)
  const base = stats.value?.badges ?? []
  const baseNames = new Set(base.filter(b => b.isUnlocked).map(b => b.name))

  const extra: BadgeVO[] = [
    { id: 'lvl_5', name: '等级 Lv.5', iconUrl: '/badges/chef-hat.png', description: '等级达到 5', isUnlocked: level >= 5 },
    { id: 'lvl_10', name: '等级 Lv.10', iconUrl: '/badges/chef-hat.png', description: '等级达到 10', isUnlocked: level >= 10 },
    { id: 'vip_1', name: 'VIP 青铜', iconUrl: '/badges/chef-hat.png', description: '积分兑换获得（限时）', isUnlocked: vip >= 1 },
    { id: 'vip_2', name: 'VIP 白银', iconUrl: '/badges/chef-hat.png', description: '积分兑换获得（限时）', isUnlocked: vip >= 2 },
    { id: 'vip_3', name: 'VIP 黄金', iconUrl: '/badges/chef-hat.png', description: '积分兑换获得（限时）', isUnlocked: vip >= 3 },
    { id: 'event_only', name: '活动限定勋章', iconUrl: '/badges/chef-hat.png', description: '参与活动渠道获取', isUnlocked: baseNames.has('活动限定勋章') }
  ]

  return [...extra, ...base]
})

// Data Fetching
const fetchData = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const [profileRes, statsRes] = await Promise.all(
      isPublicView.value
        ? [UserAPI.getProfileById(viewingUserId.value), UserAPI.getStatsById(viewingUserId.value)]
        : [UserAPI.getProfile(), UserAPI.getStats()]
    )
    
    if (!isPublicView.value && (profileRes.code === 401 || statsRes.code === 401)) {
      localStorage.removeItem('cw_token')
      errorMessage.value = '请先登录'
      await router.replace({ name: 'login', query: { redirect: route.fullPath } })
      return
    }

    if (profileRes.code === 200) profile.value = profileRes.data
    if (statsRes.code === 200) stats.value = statsRes.data

    if (!profile.value || !stats.value) {
      if (profileRes.code !== 200) errorMessage.value = profileRes.message || '加载失败，请稍后重试'
      else if (statsRes.code !== 200) errorMessage.value = statsRes.message || '加载失败，请稍后重试'
      else errorMessage.value = '加载失败，请稍后重试'
    }
  } catch (e: any) {
    const status = e?.response?.status
    if (status === 401 && !isPublicView.value) {
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
      bgImageUrl: profile.value.bgImageUrl ?? '',
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
  if (auth.token && !auth.profile) auth.loadProfile()
  fetchData()
})

const fetchCollected = async (reset = false) => {
  if (collectLoading.value || (!collectHasMore.value && !reset)) return
  collectLoading.value = true
  collectError.value = ''
  try {
    if (reset) {
      collectPage.value = 1
      collectHasMore.value = true
      collectedRecipes.value = []
    }
    const res = await SocialAPI.getCollectedRecipes({ page: collectPage.value, size: 9 })
    if (res.code !== 200) {
      collectError.value = res.message || '加载失败，请稍后重试'
      return
    }
    const records = res.data.records ?? []
    if (records.length === 0) {
      collectHasMore.value = false
      return
    }
    collectedRecipes.value.push(...records)
    collectPage.value++
  } catch {
    collectError.value = '加载失败，请检查网络或稍后重试'
  } finally {
    collectLoading.value = false
  }
}

const setTab = async (tab: 'recipes' | 'likes' | 'about') => {
  activeTab.value = tab
  if (tab === 'likes' && collectedRecipes.value.length === 0) {
    await fetchCollected(true)
  }
}

const followTarget = async () => {
  if (!profile.value) return
  if (!auth.token) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  await SocialAPI.follow(profile.value.userId)
}

const saveProfile = async () => {
  if (!profile.value || saving.value) return
  saving.value = true
  try {
    const res = await UserAPI.updateProfile({
      nickname: editForm.value.nickname,
      avatarUrl: editForm.value.avatarUrl,
      bgImageUrl: editForm.value.bgImageUrl,
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

const uploadAvatar = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  uploadingAvatar.value = true
  try {
    const res = await FileAPI.uploadImage(file)
    if (res.code === 200 && res.data?.url) {
      editForm.value.avatarUrl = res.data.url
    }
  } finally {
    uploadingAvatar.value = false
  }
}

const uploadBg = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  uploadingBg.value = true
  try {
    const res = await FileAPI.uploadImage(file)
    if (res.code === 200 && res.data?.url) {
      editForm.value.bgImageUrl = res.data.url
    }
  } finally {
    uploadingBg.value = false
  }
}

const logout = () => {
  localStorage.removeItem('cw_token')
  router.replace({ name: 'home' })
}

const goLogin = () => {
  localStorage.removeItem('cw_token')
  router.replace({ name: 'login', query: { redirect: route.fullPath } })
}
</script>

<template>
  <div v-if="loading" class="min-h-screen bg-dark-bg flex items-center justify-center">
    <div class="text-primary animate-pulse">正在加载个人资料...</div>
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
        
        <!-- Back Home Button (Moved to top-left of page with z-index) -->
        <button @click="$router.push({ name: 'home' })" class="absolute top-4 left-4 md:top-6 md:left-6 flex items-center gap-2 text-white/70 hover:text-white transition-colors z-30 group">
          <div class="w-10 h-10 rounded-full bg-black/20 backdrop-blur flex items-center justify-center border border-white/10 group-hover:bg-primary group-hover:text-black group-hover:border-primary transition-all">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
          </div>
          <span class="font-bold tracking-wider text-sm hidden md:block">返回首页</span>
        </button>

        <!-- Avatar with Badge Indicator -->
        <div class="relative group/avatar cursor-pointer" @click="showBadgeWall = true">
          <div class="w-32 h-32 rounded-full border-4 border-dark-bg overflow-hidden relative z-10">
            <img :src="avatarSrc" class="w-full h-full object-cover" @error="onAvatarError" />
          </div>
          <!-- 3D Badge Indicator -->
          <div class="absolute -bottom-2 -right-2 bg-primary text-black text-xs font-bold px-3 py-1 rounded-full z-20 animate-bounce">
            查看勋章
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
          <p class="text-gray-300 max-w-xl">{{ profile.signature || '这个人很懒，什么都没写' }}</p>
        </div>

        <!-- Action Buttons -->
        <div class="flex gap-4 mb-4">
          <template v-if="isSelf">
            <button @click="showEdit = true" class="px-7 py-2.5 bg-white text-black font-bold rounded-full hover:bg-gray-200 transition-colors">
              编辑资料
            </button>
            <button @click="logout" class="w-11 h-11 rounded-full border border-gray-600 flex items-center justify-center hover:bg-gray-800 transition-colors" title="退出登录">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6A2.25 2.25 0 005.25 5.25v13.5A2.25 2.25 0 007.5 21h6A2.25 2.25 0 0015.75 18.75V15M18 15l3-3m0 0l-3-3m3 3H9"></path></svg>
            </button>
          </template>
          <template v-else>
            <button
              @click="followTarget"
              class="px-7 py-2.5 bg-primary text-black font-bold rounded-full hover:bg-white transition-colors"
            >
              关注
            </button>
          </template>
        </div>
      </div>
    </header>

    <!-- 2. Stats Bar -->
    <div class="border-b border-gray-800 bg-dark-bg/80 backdrop-blur sticky top-0 z-40">
      <div class="max-w-7xl mx-auto px-8 py-4 flex justify-between items-center">
        <div class="flex gap-12">
          <div class="stat-item text-center cursor-pointer hover:text-primary transition-colors">
            <div class="text-xl font-bold font-serif">{{ stats.recipeCount }}</div>
            <div class="text-xs text-gray-500 tracking-widest">食谱</div>
          </div>
          <div class="stat-item text-center cursor-pointer hover:text-primary transition-colors">
            <div class="text-xl font-bold font-serif">{{ formatCount(stats.followerCount) }}</div>
            <div class="text-xs text-gray-500 tracking-widest">粉丝</div>
          </div>
          <div class="stat-item text-center cursor-pointer hover:text-primary transition-colors">
            <div class="text-xl font-bold font-serif">{{ formatCount(stats.likeCount) }}</div>
            <div class="text-xs text-gray-500 tracking-widest">获赞</div>
          </div>
        </div>
        
        <!-- Tabs -->
        <div class="flex gap-8 text-sm font-medium">
          <button 
            v-for="tab in [{ key: 'recipes', label: '食谱' }, { key: 'likes', label: '收藏' }, { key: 'about', label: '关于' }]" 
            :key="tab.key"
            @click="setTab(tab.key as any)"
            class="capitalize relative py-4 transition-colors"
            :class="activeTab === tab.key ? 'text-white' : 'text-gray-500 hover:text-gray-300'"
          >
            {{ tab.label }}
            <span v-if="activeTab === tab.key" class="absolute bottom-0 left-0 w-full h-0.5 bg-primary layout-id='active-tab'"></span>
          </button>
        </div>
      </div>
    </div>

    <!-- 3. Content Area -->
    <main class="max-w-7xl mx-auto px-4 md:px-8 py-12">
      <!-- Recipes Tab -->
      <transition name="fade" mode="out-in">
        <div v-if="activeTab === 'recipes'" key="recipes">
          <RecipeFeed :authorId="profile.userId" />
        </div>
        
        <div v-else-if="activeTab === 'likes'" key="likes">
          <div v-if="collectError" class="mb-6 rounded-2xl border border-white/10 bg-black/20 px-6 py-4 flex items-center justify-between gap-4">
            <div class="text-gray-300 text-sm tracking-wider">{{ collectError }}</div>
            <button @click="fetchCollected(true)" class="px-5 py-2 rounded-full bg-primary text-black font-bold text-sm">
              重试
            </button>
          </div>

          <div v-if="!collectLoading && !collectError && collectedRecipes.length === 0" class="text-center py-20 text-gray-600 text-sm tracking-widest">
            暂无收藏
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <RecipeCard v-for="r in collectedRecipes" :key="r.id" :data="r" />
          </div>

          <div class="flex justify-center mt-10">
            <button
              v-if="collectHasMore && !collectLoading && collectedRecipes.length > 0"
              @click="fetchCollected(false)"
              class="px-8 py-3 rounded-full border border-gray-700 hover:border-primary text-gray-300 hover:text-primary transition-colors text-sm tracking-widest"
            >
              加载更多
            </button>
            <div v-else-if="collectLoading" class="text-gray-500 text-sm tracking-widest">加载中...</div>
            <div v-else-if="!collectHasMore && collectedRecipes.length > 0" class="text-gray-600 text-sm tracking-widest">
              已到底
            </div>
          </div>
        </div>
        
        <div v-else key="about" class="max-w-2xl mx-auto">
          <div class="bg-black/20 border border-white/10 rounded-2xl p-6 md:p-8">
            <div class="text-sm tracking-widest text-gray-500 mb-6">关于我</div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <div class="text-xs tracking-widest text-gray-500 mb-1">账号</div>
                <div class="text-gray-200">{{ profile.username || '-' }}</div>
              </div>
              <div>
                <div class="text-xs tracking-widest text-gray-500 mb-1">昵称</div>
                <div class="text-gray-200">{{ profile.nickname || '-' }}</div>
              </div>
              <div>
                <div class="text-xs tracking-widest text-gray-500 mb-1">手机号</div>
                <div class="text-gray-200">{{ profile.mobile || '-' }}</div>
              </div>
              <div>
                <div class="text-xs tracking-widest text-gray-500 mb-1">邮箱</div>
                <div class="text-gray-200">{{ profile.email || '-' }}</div>
              </div>
              <div>
                <div class="text-xs tracking-widest text-gray-500 mb-1">性别</div>
                <div class="text-gray-200">{{ genderText }}</div>
              </div>
              <div>
                <div class="text-xs tracking-widest text-gray-500 mb-1">厨龄</div>
                <div class="text-gray-200">{{ profile.cookAge ?? '-' }} 年</div>
              </div>
              <div>
                <div class="text-xs tracking-widest text-gray-500 mb-1">所在地</div>
                <div class="text-gray-200">{{ [profile.country, profile.province, profile.city].filter(Boolean).join(' / ') || '-' }}</div>
              </div>
              <div>
                <div class="text-xs tracking-widest text-gray-500 mb-1">职业</div>
                <div class="text-gray-200">{{ profile.job || '-' }}</div>
              </div>
              <div>
                <div class="text-xs tracking-widest text-gray-500 mb-1">VIP 等级</div>
                <div class="text-gray-200">Lv.{{ profile.vipLevel ?? 0 }}</div>
              </div>
              <div>
                <div class="text-xs tracking-widest text-gray-500 mb-1">总消费</div>
                <div class="text-gray-200">{{ moneyText(profile.totalSpend) }}</div>
              </div>
              <div class="md:col-span-2">
                <div class="text-xs tracking-widest text-gray-500 mb-1">兴趣</div>
                <div class="text-gray-200 whitespace-pre-wrap">{{ profile.interests || '-' }}</div>
              </div>
              <div class="md:col-span-2">
                <div class="text-xs tracking-widest text-gray-500 mb-1">喜欢菜系</div>
                <div class="text-gray-200 whitespace-pre-wrap">{{ profile.favoriteCuisine || '-' }}</div>
              </div>
              <div class="md:col-span-2">
                <div class="text-xs tracking-widest text-gray-500 mb-1">口味偏好</div>
                <div class="text-gray-200 whitespace-pre-wrap">{{ profile.tastePreference || '-' }}</div>
              </div>
              <div class="md:col-span-2">
                <div class="text-xs tracking-widest text-gray-500 mb-1">忌口/限制</div>
                <div class="text-gray-200 whitespace-pre-wrap">{{ profile.dietaryRestrictions || '-' }}</div>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </main>

    <!-- 3D Badge Wall Modal -->
    <BadgeWall2D 
      v-if="showBadgeWall" 
      :badges="badgeWallBadges" 
      @close="showBadgeWall = false" 
    />

    <!-- Edit Profile Modal -->
    <div v-if="showEdit" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur p-4">
      <div class="w-full max-w-md bg-dark-bg border border-white/10 rounded-2xl p-5 md:p-6 shadow-2xl">
        <div class="flex items-center justify-between mb-5">
          <div class="text-sm tracking-widest text-gray-400 font-bold">编辑资料</div>
          <button @click="showEdit = false" class="text-gray-400 hover:text-white transition-colors">✕</button>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 max-h-[70vh] overflow-y-auto pr-2 custom-scrollbar">
          <div class="md:col-span-2">
            <div class="text-xs tracking-widest text-gray-500 mb-1">昵称</div>
            <input v-model="editForm.nickname" class="w-full px-3 py-2 rounded-lg bg-black/20 border border-white/10 text-white text-sm focus:outline-none focus:border-primary" />
          </div>
          <div class="md:col-span-2">
            <div class="text-xs tracking-widest text-gray-500 mb-1">头像地址</div>
            <input v-model="editForm.avatarUrl" class="w-full px-3 py-2 rounded-lg bg-black/20 border border-white/10 text-white text-sm focus:outline-none focus:border-primary" />
            <label class="mt-2 inline-flex items-center gap-2 px-4 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-xs tracking-widest cursor-pointer">
              {{ uploadingAvatar ? '头像上传中...' : '上传头像' }}
              <input type="file" accept="image/*" class="hidden" :disabled="uploadingAvatar" @change="uploadAvatar" />
            </label>
          </div>
          <div class="md:col-span-2">
            <div class="text-xs tracking-widest text-gray-500 mb-1">背景图地址</div>
            <input v-model="editForm.bgImageUrl" class="w-full px-3 py-2 rounded-lg bg-black/20 border border-white/10 text-white text-sm focus:outline-none focus:border-primary" />
            <label class="mt-2 inline-flex items-center gap-2 px-4 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-xs tracking-widest cursor-pointer">
              {{ uploadingBg ? '背景上传中...' : '上传背景图' }}
              <input type="file" accept="image/*" class="hidden" :disabled="uploadingBg" @change="uploadBg" />
            </label>
          </div>
          <div>
            <div class="text-xs tracking-widest text-gray-500 mb-1">性别</div>
            <select v-model.number="editForm.gender" class="w-full px-3 py-2 rounded-lg bg-black/20 border border-white/10 text-white text-sm focus:outline-none focus:border-primary">
              <option :value="0">未知</option>
              <option :value="1">男</option>
              <option :value="2">女</option>
            </select>
          </div>
          <div>
            <div class="text-xs tracking-widest text-gray-500 mb-1">城市</div>
            <input v-model="editForm.city" class="w-full px-3 py-2 rounded-lg bg-black/20 border border-white/10 text-white text-sm focus:outline-none focus:border-primary" />
          </div>
          <div>
            <div class="text-xs tracking-widest text-gray-500 mb-1">职业</div>
            <input v-model="editForm.job" class="w-full px-3 py-2 rounded-lg bg-black/20 border border-white/10 text-white text-sm focus:outline-none focus:border-primary" />
          </div>
          <div>
            <div class="text-xs tracking-widest text-gray-500 mb-1">厨龄 (年)</div>
            <input v-model.number="editForm.cookAge" type="number" min="0" max="80" class="w-full px-3 py-2 rounded-lg bg-black/20 border border-white/10 text-white text-sm focus:outline-none focus:border-primary" />
          </div>
          <div class="md:col-span-2">
            <div class="text-xs tracking-widest text-gray-500 mb-1">个性签名</div>
            <textarea v-model="editForm.signature" rows="2" class="w-full px-3 py-2 rounded-lg bg-black/20 border border-white/10 text-white text-sm focus:outline-none focus:border-primary"></textarea>
          </div>
          <div class="md:col-span-2">
            <div class="text-xs tracking-widest text-gray-500 mb-1">喜欢菜系</div>
            <input v-model="editForm.favoriteCuisine" class="w-full px-3 py-2 rounded-lg bg-black/20 border border-white/10 text-white text-sm focus:outline-none focus:border-primary" placeholder="如：川菜、日料" />
          </div>
          <div class="md:col-span-2">
            <div class="text-xs tracking-widest text-gray-500 mb-1">口味偏好</div>
            <input v-model="editForm.tastePreference" class="w-full px-3 py-2 rounded-lg bg-black/20 border border-white/10 text-white text-sm focus:outline-none focus:border-primary" placeholder="如：辣、甜" />
          </div>
          <div class="md:col-span-2">
            <div class="text-xs tracking-widest text-gray-500 mb-1">忌口/限制</div>
            <input v-model="editForm.dietaryRestrictions" class="w-full px-3 py-2 rounded-lg bg-black/20 border border-white/10 text-white text-sm focus:outline-none focus:border-primary" placeholder="如：花生过敏" />
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6 pt-4 border-t border-white/5">
          <button @click="showEdit = false" class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-sm">
            取消
          </button>
          <button @click="saveProfile" :disabled="saving" class="px-5 py-2 rounded-full bg-primary text-black font-bold disabled:opacity-60 text-sm">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

  </div>
  
  <div v-else class="min-h-screen bg-dark-bg flex flex-col items-center justify-center text-white px-6">
    <div class="text-lg font-bold mb-2">个人主页</div>
    <div class="text-gray-400 mb-6">{{ errorMessage || '加载失败' }}</div>
    <div class="flex gap-3">
      <button @click="fetchData" class="px-6 py-3 rounded-full bg-primary text-black font-bold">
        重试
      </button>
      <button @click="goLogin" class="px-6 py-3 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors">
        去登录
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
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: rgba(255,255,255,0.05);
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255,255,255,0.2);
  border-radius: 2px;
}
</style>
