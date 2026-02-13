<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { RecipeAPI } from '@/api/recipe'
import { SocialAPI } from '@/api/social'
import type { RecipeDetailVO } from '@/types/recipe'
import AIAssistantOrb from '@/components/visual/AIAssistantOrb.vue'
import { NRate } from 'naive-ui'
import CommentSection from './components/CommentSection.vue'
import { useAuthStore } from '@/stores/auth'

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

const activeStep = computed(() => {
  if (!recipe.value?.steps?.length) return null
  const i = Math.min(Math.max(zenStepIndex.value, 0), recipe.value.steps.length - 1)
  return recipe.value.steps[i]
})

const containsAscii = (text: string) => /[A-Za-z]/.test(text || '')

const toZhStep = (text: string, stepNo: number) => {
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
  // Remove remaining long english words if mixed
  // out = out.replace(/[A-Za-z]{3,}/g, '') 
  out = out.replace(/\s{2,}/g, ' ').trim()
  
  if (!out || out.length < 6) {
    return `第${stepNo}步：按提示完成该步骤（建议使用中文数据源重新生成步骤）。`
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
    router.push({ name: 'login', query: { redirect: route.fullPath } })
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
    router.push({ name: 'login', query: { redirect: route.fullPath } })
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

const openZenModeAt = async (index: number) => {
  if (!recipe.value?.steps?.length) return
  zenStepIndex.value = Math.min(Math.max(index, 0), recipe.value.steps.length - 1)
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
  if (!recipe.value?.steps?.length) return
  zenStepIndex.value = Math.max(0, zenStepIndex.value - 1)
}

const zenNext = () => {
  if (!recipe.value?.steps?.length) return
  zenStepIndex.value = Math.min(recipe.value.steps.length - 1, zenStepIndex.value + 1)
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

const sendZen = () => {
  const q = zenInput.value.trim()
  if (!q) return
  zenMessages.value.push({ role: 'user', content: q })
  const stepNo = (activeStep.value?.stepNo ?? zenStepIndex.value + 1)
  const stepText = activeStep.value?.desc ? toZhStep(activeStep.value.desc, stepNo) : ''
  
  setTimeout(() => {
    zenMessages.value.push({ role: 'assistant', content: assistantReply(q, stepText, stepNo) })
  }, 500)
  
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
            <div class="flex items-center gap-2">
              <img :src="recipe.authorAvatar" class="w-8 h-8 rounded-full border border-primary/50" />
              <span>作者 {{ recipe.authorName }}</span>
            </div>
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
              v-for="(step, idx) in recipe.steps"
              :key="step.stepNo"
              class="group relative pl-8 border-l border-gray-800 hover:border-primary transition-colors duration-300 cursor-pointer"
              @click="openZenModeAt(idx)"
            >
              <span class="absolute -left-[9px] top-0 w-4 h-4 rounded-full bg-dark-bg border-2 border-gray-600 group-hover:border-primary transition-colors"></span>
              
              <h4 class="text-lg font-bold text-gray-400 mb-2 group-hover:text-white transition-colors">第 {{ step.stepNo }} 步</h4>
              <p class="text-gray-300 leading-relaxed mb-4">{{ toZhStep(step.desc, step.stepNo) }}</p>
              
              <!-- Key Step Highlight -->
              <div v-if="step.isKeyStep" class="inline-flex items-center gap-2 px-3 py-1 bg-yellow-500/10 text-yellow-500 text-xs rounded border border-yellow-500/20">
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
                关键步骤
              </div>
            </div>
          </div>
        </section>

        <!-- Comments Section -->
        <CommentSection :recipeId="recipe.id" />
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
                <div class="text-xl font-bold text-white">{{ recipe.protein }}g</div>
                <div class="text-xs text-gray-500">蛋白质</div>
              </div>
              <div>
                <div class="text-xl font-bold text-white">{{ recipe.fat }}g</div>
                <div class="text-xs text-gray-500">脂肪</div>
              </div>
              <div>
                <div class="text-xl font-bold text-white">{{ recipe.carbs }}g</div>
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

        <div class="text-center max-w-3xl space-y-8">
          <h2 class="text-primary text-sm tracking-[0.35em]">第 {{ (activeStep?.stepNo ?? 1) }} 步 / 共 {{ recipe.steps.length }} 步</h2>
          <p class="text-4xl md:text-6xl font-serif leading-tight text-white">
            "{{ toZhStep(activeStep?.desc ?? '', activeStep?.stepNo ?? 1) }}"
          </p>
          
          <div class="flex justify-center gap-8 mt-12">
             <button
               class="w-16 h-16 rounded-full border border-gray-700 flex items-center justify-center hover:bg-white hover:text-black transition-all disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-white"
               :disabled="zenStepIndex <= 0"
               @click="zenPrev"
             >
               <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path></svg>
             </button>
             <button
               class="w-16 h-16 rounded-full border border-gray-700 flex items-center justify-center hover:bg-white hover:text-black transition-all disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-white"
               :disabled="zenStepIndex >= recipe.steps.length - 1"
               @click="zenNext"
             >
               <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg>
             </button>
          </div>
        </div>

        <!-- AI Assistant Orb (Fixed at bottom) -->
        <div class="absolute bottom-8 left-1/2 -translate-x-1/2 w-[92vw] max-w-2xl">
          <div class="flex flex-col items-center gap-3">
            <AIAssistantOrb status="thinking" />
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
                  :disabled="!zenInput.trim()"
                >
                  发送
                </button>
              </div>
            </div>
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
