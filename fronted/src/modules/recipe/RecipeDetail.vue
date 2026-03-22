<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { RecipeAPI } from '@/api/recipe'
import { SocialAPI } from '@/api/social'
import { AiAPI, type AiChatMessage } from '@/api/ai'
import type { RecipeDetailVO } from '@/types/recipe'
import AIAssistantOrb from '@/components/visual/AIAssistantOrb.vue'
import { NRate } from 'naive-ui'
import CommentSection from './components/CommentSection.vue'
import { useAuthStore } from '@/stores/auth'
import { normalizeAssetUrl } from '@/utils/assetUrl'

gsap.registerPlugin(ScrollTrigger)

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const recipe = ref<RecipeDetailVO | null>(null)
const loading = ref(true)
const isZenMode = ref(false)
const isLiked = ref(false)
const isCollected = ref(false)
const zenStepIndex = ref(0)
const zenInput = ref('')
const zenInputRef = ref<HTMLInputElement | null>(null)
const zenMessages = ref<Array<{ role: 'user' | 'assistant'; content: string }>>([])
const zenSending = ref(false)
const showLoginPrompt = ref(false)

const openLoginPrompt = () => {
  showLoginPrompt.value = true
}

const goLogin = () => {
  showLoginPrompt.value = false
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

const closeLoginPrompt = () => {
  showLoginPrompt.value = false
}

const highlightCommentId = computed(() => (typeof route.query.highlightCommentId === 'string' ? route.query.highlightCommentId : ''))

const authorAvatarSrc = computed(() => {
  const v = normalizeAssetUrl(recipe.value?.authorAvatar)
  if (v) return v
  const seed = recipe.value?.authorName || recipe.value?.authorId || 'user'
  return `https://api.dicebear.com/7.x/thumbs/svg?seed=${encodeURIComponent(String(seed))}`
})

const hasNutrition = computed(() => {
  if (!recipe.value) return false
  const protein = Number(recipe.value.protein ?? 0)
  const fat = Number(recipe.value.fat ?? 0)
  const carbs = Number(recipe.value.carbs ?? 0)
  const calories = Number(recipe.value.calories ?? 0)
  return protein > 0 || fat > 0 || carbs > 0 || calories > 0
})

const proteinText = computed(() => (hasNutrition.value ? `${recipe.value?.protein ?? 0}g` : '--'))
const fatText = computed(() => (hasNutrition.value ? `${recipe.value?.fat ?? 0}g` : '--'))
const carbsText = computed(() => (hasNutrition.value ? `${recipe.value?.carbs ?? 0}g` : '--'))

const videoUrl = computed(() => (recipe.value?.videoUrl ?? '').trim())
const isDirectVideo = computed(() => /\.(mp4|webm|ogg)(\?.*)?$/i.test(videoUrl.value))
const youtubeEmbedSrc = computed(() => {
  const url = videoUrl.value
  if (!url) return ''
  const toEmbed = (id: string) => `https://www.youtube-nocookie.com/embed/${encodeURIComponent(id)}`
  try {
    const u = new URL(url)
    const host = u.hostname.replace(/^www\./, '')
    if (host.includes('youtube.com')) {
      if (u.pathname.startsWith('/watch')) {
        const v = u.searchParams.get('v')
        if (v) return toEmbed(v)
      }
      if (u.pathname.startsWith('/shorts/')) {
        const id = u.pathname.split('/')[2]
        if (id) return toEmbed(id)
      }
      if (u.pathname.startsWith('/embed/')) {
        const id = u.pathname.split('/')[2]
        if (id) return toEmbed(id)
      }
      if (u.pathname.startsWith('/live/')) {
        const id = u.pathname.split('/')[2]
        if (id) return toEmbed(id)
      }
    }
    if (host === 'youtu.be') {
      const id = u.pathname.replace('/', '').trim()
      if (id) return toEmbed(id)
    }
  } catch {
  }
  return ''
})
const iframeLoaded = ref(false)
const iframeFailed = ref(false)
let iframeTimer: number | undefined

const resetIframeState = () => {
  iframeLoaded.value = false
  iframeFailed.value = false
  if (iframeTimer) {
    window.clearTimeout(iframeTimer)
    iframeTimer = undefined
  }
  if (youtubeEmbedSrc.value) {
    iframeTimer = window.setTimeout(() => {
      if (!iframeLoaded.value) iframeFailed.value = true
    }, 6000)
  }
}

const onIframeLoad = () => {
  iframeLoaded.value = true
  if (iframeTimer) {
    window.clearTimeout(iframeTimer)
    iframeTimer = undefined
  }
}

const onIframeError = () => {
  iframeFailed.value = true
}

watch([videoUrl, youtubeEmbedSrc], resetIframeState, { immediate: true })

const containsAscii = (text: string) => /[A-Za-z]/.test(text || '')

const displaySteps = computed(() => {
  const src = recipe.value?.steps ?? []
  const filtered = src
    .map(s => ({ ...s, desc: (s?.desc ?? '').trim() }))
    .filter(s => s.desc.length > 0)

  const merged: typeof filtered = []
  for (let i = 0; i < filtered.length; i++) {
    const cur = filtered[i]
    const next = filtered[i + 1]
    if (next && containsAscii(cur.desc) && cur.desc.length <= 12) {
      filtered[i + 1] = { ...next, desc: `${cur.desc} ${next.desc}`.trim() }
      continue
    }
    merged.push(cur)
  }

  return merged.map((s, i) => ({ ...s, stepNo: i + 1 }))
})

const activeStep = computed(() => {
  if (!displaySteps.value.length) return null
  const i = Math.min(Math.max(zenStepIndex.value, 0), displaySteps.value.length - 1)
  return displaySteps.value[i]
})

const toZhStep = (text: string) => {
  if (!text) return ''
  if (!containsAscii(text)) return text
  const t = text
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/\s+/g, ' ')
    .trim()
  // Expanded dictionary
  const dict: Array<[RegExp, string]> = [
    [/preheat/gi, '预热'],
    [/heat/gi, '加热'],
    [/mix/gi, '混合'],
    [/stir/gi, '搅拌'],
    [/cook/gi, '烹饪'],
    [/bake/gi, '烘烤'],
    [/roast/gi, '烤制'],
    [/boil/gi, '煮'],
    [/simmer/gi, '小火煮'],
    [/fry/gi, '煎/炸'],
    [/grill/gi, '炙烤'],
    [/serve/gi, '装盘'],
    [/minutes?/gi, '分钟'],
    [/seconds?/gi, '秒'],
    [/oven/gi, '烤箱'],
    [/pan/gi, '平底锅'],
    [/salt/gi, '盐'],
    [/pepper/gi, '黑胡椒'],
    [/oil/gi, '油'],
    [/water/gi, '水'],
    [/sauce/gi, '酱汁'],
    [/add/gi, '加入'],
    [/cut/gi, '切'],
    [/slice/gi, '切片'],
    [/dice/gi, '切丁'],
    [/chop/gi, '剁碎'],
    [/peel/gi, '去皮'],
    [/bowl/gi, '碗'],
    [/whisk/gi, '搅打'],
    [/pour/gi, '倒入'],
    [/place/gi, '放置'],
    [/remove/gi, '取出'],
    [/cool/gi, '冷却'],
    [/combine/gi, '混合'],
    [/ingredients/gi, '食材']
  ]
  let out = t
  for (const [re, rep] of dict) out = out.replace(re, rep)
  out = out
    .replace(/\b(tbsp|tablespoon|tablespoons)\b/gi, '汤匙')
    .replace(/\b(tsp|teaspoon|teaspoons)\b/gi, '茶匙')
    .replace(/\b(cups?)\b/gi, '杯')
    .replace(/\b(kg)\b/gi, '千克')
    .replace(/\b(g)\b/gi, '克')
    .replace(/\b(l)\b/gi, '升')
    .replace(/\b(ml)\b/gi, '毫升')
    .replace(/\s*([,.;:!?])\s*/g, ' ')
    .replace(/\s{2,}/g, ' ')
    .trim()
  
  if (!out || out.length < 6) {
    return t
  }
  return out
}

// Mock Data Fetch
const fetchDetail = async () => {
  const id = route.params.id as string
  const res = await RecipeAPI.getDetail(id)
  if (res.code === 200) {
    recipe.value = res.data
    if (auth.token) {
      const s = await SocialAPI.getInteractionStatus({ targetType: 1, targetId: id })
      if (s.code === 200) {
        isLiked.value = s.data.liked
        isCollected.value = s.data.collected
      }
    } else {
      isLiked.value = false
      isCollected.value = false
    }
    // After data loaded, trigger animations
    setTimeout(() => initAnimations(), 100)
  }
  loading.value = false
}

const toggleLike = async () => {
  if (!recipe.value) return
  if (!auth.token) {
    openLoginPrompt()
    return
  }
  const before = isLiked.value
  isLiked.value = !before
  recipe.value.likeCount += isLiked.value ? 1 : -1
  try {
    const res = await SocialAPI.interact({ targetType: 1, targetId: recipe.value.id, actionType: 1 })
    if (res.code !== 200) {
      isLiked.value = before
      recipe.value.likeCount += isLiked.value ? 1 : -1
    }
  } catch {
    isLiked.value = before
    recipe.value.likeCount += isLiked.value ? 1 : -1
  }
}

const toggleCollect = async () => {
  if (!recipe.value) return
  if (!auth.token) {
    openLoginPrompt()
    return
  }
  const before = isCollected.value
  isCollected.value = !before
  recipe.value.collectCount += isCollected.value ? 1 : -1
  try {
    const res = await SocialAPI.interact({ targetType: 1, targetId: recipe.value.id, actionType: 2 })
    if (res.code !== 200) {
      isCollected.value = before
      recipe.value.collectCount += isCollected.value ? 1 : -1
    }
  } catch {
    isCollected.value = before
    recipe.value.collectCount += isCollected.value ? 1 : -1
  }
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
const toggleZenMode = async () => {
  isZenMode.value = !isZenMode.value
  if (isZenMode.value) {
    document.body.style.overflow = 'hidden' // Prevent scrolling
    if (zenMessages.value.length === 0) {
      zenInput.value = ''
    }
    await nextTick()
    zenInputRef.value?.focus()
  } else {
    document.body.style.overflow = ''
  }
}

const goAuthorProfile = () => {
  if (!recipe.value?.authorId) return
  router.push({ name: 'user-public', params: { id: recipe.value.authorId } })
}

const openZenModeAt = async (index: number) => {
  if (!displaySteps.value.length) return
  zenStepIndex.value = Math.min(Math.max(index, 0), displaySteps.value.length - 1)
  if (!isZenMode.value) {
    isZenMode.value = true
    document.body.style.overflow = 'hidden'
    if (zenMessages.value.length === 0) {
      zenInput.value = ''
    }
    await nextTick()
    zenInputRef.value?.focus()
  }
}

const zenPrev = () => {
  if (!displaySteps.value.length) return
  zenStepIndex.value = Math.max(0, zenStepIndex.value - 1)
}

const zenNext = () => {
  if (!displaySteps.value.length) return
  zenStepIndex.value = Math.min(displaySteps.value.length - 1, zenStepIndex.value + 1)
}

const assistantReply = (question: string, stepText: string, stepNo: number) => {
  const q = (question || '').trim()
  const base = `当前是第${stepNo}步。`
  if (!q) return `${base}你可以问我：火候/时间/替代食材/口味调整。`
  if (/替代|代替|没有|缺少/.test(q)) return `${base}可以用相近口感/相近香气的食材替代，保持“主料+香料+酸甜咸”平衡；先少量加入再调整。`
  if (/火候|温度|烤箱|预热|煎|炸|炒/.test(q)) return `${base}建议中小火起步，闻到香味再加大火；如果是烤箱类操作，先预热再计时，避免外焦里生。`
  if (/时间|多久|几分钟/.test(q)) return `${base}优先观察状态：颜色变化、出汁量、香味强度。一般收汁阶段 1–3 分钟即可，避免过干。`
  if (/英文|看不懂/.test(q)) return `${base}我已把关键动词做了中文化处理；如果仍有外语，建议换一份中文步骤数据。`
  const s = stepText ? `这一步的要点是：${stepText.slice(0, 80)}。` : ''
  return `${base}${s}你也可以说“帮我概括这一步”或“给我更细的操作”。`
}

const sendZen = async () => {
  const q = zenInput.value.trim()
  if (!q || zenSending.value) return
  zenMessages.value.push({ role: 'user', content: q })
  zenInput.value = ''

  const stepNo = zenStepIndex.value + 1
  const stepText = activeStep.value?.desc ? toZhStep(activeStep.value.desc) : ''
  const recipeTitle = recipe.value?.title ?? ''
  const message = `${recipeTitle ? `菜谱：${recipeTitle}\n` : ''}当前步骤：第${stepNo}步\n步骤内容：${stepText}\n问题：${q}`.trim()

  const history: AiChatMessage[] = zenMessages.value
    .filter(m => m.role === 'user' || m.role === 'assistant')
    .slice(-8)
    .map(m => ({ role: m.role, content: m.content }))

  zenSending.value = true
  try {
    const res = await AiAPI.chat({ message, history })
    if (res.code === 200 && res.data?.reply) {
      zenMessages.value.push({ role: 'assistant', content: res.data.reply.trim() })
      return
    }
    zenMessages.value.push({ role: 'assistant', content: assistantReply(q, stepText, stepNo) })
  } catch {
    zenMessages.value.push({ role: 'assistant', content: assistantReply(q, stepText, stepNo) })
  } finally {
    zenSending.value = false
  }
  zenInput.value = ''
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
    <div class="text-primary animate-pulse">正在加载食谱...</div>
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
            <button @click="goAuthorProfile" class="flex items-center gap-2 hover:text-primary transition-colors">
              <img :src="authorAvatarSrc" class="w-8 h-8 rounded-full border border-primary/50" />
              <span>作者 {{ recipe.authorName }}</span>
            </button>
            <div class="flex items-center gap-2">
              <NRate readonly :default-value="recipe.score" size="small" allow-half />
              <span>{{ recipe.score }}</span>
            </div>
            <div>{{ recipe.timeCost }} 分钟</div>
            <div>{{ recipe.calories }} 千卡</div>
            <button
              class="flex items-center gap-2 px-3 py-1 rounded-full border border-white/10 bg-black/20 hover:border-primary transition-colors"
              :class="isLiked ? 'text-primary' : 'text-gray-200'"
              @click.stop="toggleLike"
              title="点赞"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path></svg>
              <span class="text-xs">{{ recipe.likeCount }}</span>
            </button>
            <button
              class="flex items-center gap-2 px-3 py-1 rounded-full border border-white/10 bg-black/20 hover:border-primary transition-colors"
              :class="isCollected ? 'text-primary' : 'text-gray-200'"
              @click.stop="toggleCollect"
              title="收藏"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3-7 3V5z"></path></svg>
              <span class="text-xs">{{ recipe.collectCount }}</span>
            </button>
          </div>
        </div>
      </div>
      
      <!-- Back Button -->
      <button @click="router.back()" class="absolute top-8 left-8 z-20 w-10 h-10 flex items-center justify-center rounded-full bg-black/20 backdrop-blur hover:bg-primary hover:text-black transition-all">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
      </button>

      <div class="absolute top-8 right-8 z-20 flex items-center gap-3">
        <button @click="router.push({ name: 'home' })" class="w-10 h-10 rounded-full bg-black/20 backdrop-blur border border-white/10 hover:border-primary hover:text-primary transition-colors" title="首页">
          <svg class="w-5 h-5 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10.5L12 3l9 7.5V21a1 1 0 01-1 1h-5v-7H9v7H4a1 1 0 01-1-1v-10.5z"/></svg>
        </button>
        <button @click="router.push({ name: 'search' })" class="w-10 h-10 rounded-full bg-black/20 backdrop-blur border border-white/10 hover:border-primary hover:text-primary transition-colors" title="搜索">
          <svg class="w-5 h-5 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-4.35-4.35m1.85-5.15a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
        </button>
        <button @click="router.push({ name: 'vip' })" class="w-10 h-10 rounded-full bg-black/20 backdrop-blur border border-white/10 hover:border-primary hover:text-primary transition-colors" title="VIP">
          <svg class="w-5 h-5 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 2l2.4 6.9H22l-5.8 4.2L18.6 20 12 15.7 5.4 20l2.4-6.7L2 8.9h7.6L12 2z"/></svg>
        </button>
        <button v-if="auth.token" @click="router.push({ name: 'points' })" class="w-10 h-10 rounded-full bg-black/20 backdrop-blur border border-white/10 hover:border-primary hover:text-primary transition-colors" title="积分">
          <svg class="w-5 h-5 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-4.4 0-8 1.8-8 4s3.6 4 8 4 8-1.8 8-4-3.6-4-8-4zm0 0V4m-4 12v4m8-4v4"/></svg>
        </button>
        <button v-if="auth.token" @click="router.push({ name: 'social' })" class="w-10 h-10 rounded-full bg-black/20 backdrop-blur border border-white/10 hover:border-primary hover:text-primary transition-colors" title="社交">
          <svg class="w-5 h-5 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a4 4 0 00-4-4h-1m-4 6H2v-2a4 4 0 014-4h3m4-4a4 4 0 10-8 0 4 4 0 008 0zm6 2a3 3 0 10-6 0 3 3 0 006 0z"/></svg>
        </button>
        <button v-if="auth.token" @click="router.push({ name: 'user-profile' })" class="w-10 h-10 rounded-full bg-black/20 backdrop-blur border border-white/10 hover:border-primary hover:text-primary transition-colors" title="我的主页">
          <svg class="w-5 h-5 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>
        </button>
        <button v-else @click="router.push({ name: 'login', query: { redirect: route.fullPath } })" class="px-4 h-10 rounded-full bg-black/20 backdrop-blur border border-white/10 hover:border-primary hover:text-primary transition-colors text-xs tracking-widest font-bold" title="登录">
          登录
        </button>
      </div>
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

        <section v-if="videoUrl" class="content-block">
          <h3 class="text-2xl font-serif text-primary mb-6">视频教程</h3>
          <div class="rounded-2xl border border-white/10 bg-black/20 overflow-hidden">
            <div v-if="isDirectVideo" class="aspect-video bg-black">
              <video :src="videoUrl" controls playsinline class="w-full h-full"></video>
            </div>
            <div v-else-if="youtubeEmbedSrc" class="aspect-video bg-black relative">
              <iframe
                :src="youtubeEmbedSrc"
                class="w-full h-full"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowfullscreen
                @load="onIframeLoad"
                @error="onIframeError"
              ></iframe>
              <div v-if="iframeFailed" class="absolute inset-0 flex items-center justify-center bg-black/80 p-4">
                <div class="text-center space-y-3">
                  <div class="text-sm text-gray-300">视频加载失败，可点击新窗口打开</div>
                  <a :href="videoUrl" target="_blank" rel="noopener noreferrer" class="text-primary break-all hover:underline">
                    {{ videoUrl }}
                  </a>
                </div>
              </div>
            </div>
            <div v-else class="p-5">
              <div class="text-sm text-gray-300 mb-3">暂不支持直接播放该链接，可点击在新窗口打开：</div>
              <a :href="videoUrl" target="_blank" rel="noopener noreferrer" class="text-primary break-all hover:underline">
                {{ videoUrl }}
              </a>
            </div>
          </div>
        </section>

        <!-- Ingredients (Mobile Only view, hidden on Desktop if needed, but here we show inline) -->
        <section class="content-block md:hidden">
          <h3 class="text-2xl font-serif text-primary mb-6">食材</h3>
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
            <h3 class="text-3xl font-serif text-primary">步骤</h3>
            <button @click="toggleZenMode" class="flex items-center gap-2 px-4 py-2 bg-gray-800 rounded-full hover:bg-primary hover:text-black transition-colors">
              <span class="w-2 h-2 rounded-full bg-green-400 animate-pulse"></span>
              <span class="text-sm tracking-wider font-bold">专注模式 / AI 助手</span>
            </button>
          </div>

          <div class="space-y-10">
            <div
              v-for="(step, idx) in displaySteps"
              :key="idx"
              class="group relative pl-8 border-l border-gray-800 hover:border-primary transition-colors duration-300 cursor-pointer"
              @click="openZenModeAt(idx)"
            >
              <span class="absolute -left-[9px] top-0 w-4 h-4 rounded-full bg-dark-bg border-2 border-gray-600 group-hover:border-primary transition-colors"></span>
              
              <h4 class="text-lg font-bold text-gray-400 mb-2 group-hover:text-white transition-colors">第 {{ idx + 1 }} 步</h4>
              <p class="text-gray-300 leading-relaxed mb-4">{{ toZhStep(step.desc) }}</p>
              
              <!-- Key Step Highlight -->
              <div v-if="step.isKeyStep" class="inline-flex items-center gap-2 px-3 py-1 bg-yellow-500/10 text-yellow-500 text-xs rounded border border-yellow-500/20">
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
                关键步骤
              </div>
            </div>
          </div>
        </section>

        <!-- Comments Section -->
        <CommentSection :recipeId="recipe.id" :highlightCommentId="highlightCommentId" />
      </div>

      <!-- Right Column: Ingredients (Sticky) -->
      <aside class="hidden md:block">
        <div class="sticky top-10 p-8 bg-dark-surface rounded-2xl border border-gray-800">
          <h3 class="text-2xl font-serif text-primary mb-6">食材</h3>
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
            <h4 class="text-sm tracking-widest text-gray-500 mb-4">每份营养</h4>
            <div class="grid grid-cols-3 gap-4 text-center">
              <div>
                <div class="text-xl font-bold text-white">{{ proteinText }}</div>
                <div class="text-xs text-gray-500">蛋白质</div>
              </div>
              <div>
                <div class="text-xl font-bold text-white">{{ fatText }}</div>
                <div class="text-xs text-gray-500">脂肪</div>
              </div>
              <div>
                <div class="text-xl font-bold text-white">{{ carbsText }}</div>
                <div class="text-xs text-gray-500">碳水</div>
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

        <div class="text-center max-w-3xl space-y-6 -mt-6">
          <h2 class="text-primary text-sm tracking-[0.35em]">第 {{ zenStepIndex + 1 }} 步 / 共 {{ displaySteps.length }} 步</h2>
          <p class="text-3xl md:text-6xl font-serif leading-tight text-white max-h-[40vh] overflow-auto px-2 custom-scrollbar">
            "{{ toZhStep(activeStep?.desc ?? '') }}"
          </p>
          
          <div class="flex justify-center gap-8 mt-8">
             <button
               class="w-16 h-16 rounded-full border border-gray-700 flex items-center justify-center hover:bg-white hover:text-black transition-all disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-white"
               :disabled="zenStepIndex <= 0"
               @click="zenPrev"
             >
               <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path></svg>
             </button>
             <button
               class="w-16 h-16 rounded-full border border-gray-700 flex items-center justify-center hover:bg-white hover:text-black transition-all disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-white"
               :disabled="zenStepIndex >= displaySteps.length - 1"
               @click="zenNext"
             >
               <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg>
             </button>
          </div>
        </div>

        <!-- AI Assistant Orb (Fixed at bottom) -->
        <div class="absolute bottom-8 left-1/2 -translate-x-1/2 w-[92vw] max-w-2xl">
          <div class="flex flex-col items-center gap-3">
            <AIAssistantOrb :status="zenSending ? 'processing' : 'idle'" />
            <div class="w-full rounded-2xl border border-white/10 bg-black/40 backdrop-blur px-4 py-3">
              <div v-if="zenMessages.length" class="max-h-40 overflow-auto space-y-2 pr-1 custom-scrollbar">
                <div v-for="(m, i) in zenMessages" :key="i" class="text-sm leading-relaxed">
                  <span class="text-primary font-bold mr-2">{{ m.role === 'user' ? '我' : 'AI' }}</span>
                  <span class="text-gray-200">{{ m.content }}</span>
                </div>
              </div>
              <div v-else class="text-sm text-gray-500 text-center">输入问题：火候、时间、替代食材、口味调整…</div>
              <div class="flex items-center gap-2 mt-3">
                <input
                  ref="zenInputRef"
                  v-model="zenInput"
                  @keyup.enter="sendZen"
                  class="flex-1 px-4 py-2 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary text-sm"
                  placeholder="向 AI 助手提问…"
                />
                <button
                  @click="sendZen"
                  class="px-4 py-2 rounded-xl bg-primary text-black font-bold text-sm disabled:opacity-60"
                  :disabled="!zenInput.trim() || zenSending"
                >
                  发送
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <transition name="zen-fade">
      <div v-if="showLoginPrompt" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm px-6">
        <div class="w-full max-w-sm rounded-2xl border border-white/10 bg-black/70 p-6">
          <div class="text-lg font-bold mb-2">需要登录</div>
          <div class="text-gray-300 text-sm mb-6">请先登录后再进行点赞或收藏操作。</div>
          <div class="flex justify-end gap-3">
            <button
              @click="closeLoginPrompt"
              class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-sm"
            >
              取消
            </button>
            <button @click="goLogin" class="px-5 py-2 rounded-full bg-primary text-black font-bold text-sm">去登录</button>
          </div>
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
