<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { gsap } from 'gsap'
import HeroScene from '@/components/visual/HeroScene.vue'
import StorybookSplash from '@/components/visual/StorybookSplash.vue'
import RecipeFeed from '@/components/business/RecipeFeed.vue'
import { useAuthStore } from '@/stores/auth'
import { AiAPI, type AiChatMessage } from '@/api/ai'

const router = useRouter()
const titleRef = ref(null)
const subtitleRef = ref(null)
const auth = useAuthStore()
const showAi = ref(false)
const aiInput = ref('')
const aiSending = ref(false)
const aiMessages = ref<Array<{ role: 'user' | 'assistant'; content: string; sources?: string[]; usedRemoteModel?: boolean }>>([])

const hasToken = computed(() => Boolean(auth.token))

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

const sendAi = async () => {
  const q = aiInput.value.trim()
  if (!q) return
  if (aiSending.value) return
  aiMessages.value.push({ role: 'user', content: q })
  aiInput.value = ''

  const history: AiChatMessage[] = aiMessages.value
    .filter(m => m.role === 'user' || m.role === 'assistant')
    .slice(-8)
    .map(m => ({ role: m.role, content: m.content }))

  aiSending.value = true
  try {
    const res = await AiAPI.chat({ message: q, history })
    if (res.code !== 200) {
      aiMessages.value.push({ role: 'assistant', content: res.message || 'AI 服务暂不可用，请稍后再试。' })
      return
    }
    aiMessages.value.push({
      role: 'assistant',
      content: (res.data?.reply ?? '').trim() || '未获取到回答。',
      sources: res.data?.sources ?? [],
      usedRemoteModel: Boolean(res.data?.usedRemoteModel)
    })
  } catch {
    aiMessages.value.push({ role: 'assistant', content: 'AI 请求失败，请检查网络或稍后重试。' })
  } finally {
    aiSending.value = false
  }
}
</script>

<template>
  <div class="home-page bg-dark-bg text-white">
    <!-- New Storybook Splash Screen -->
    <StorybookSplash @complete="onSplashComplete" />

    <!-- Top Navigation / Profile Link -->
    <nav class="absolute top-0 w-full z-30 p-8 flex justify-end gap-6">
        <div @click="router.push({ name: 'search' })" class="cursor-pointer group flex items-center gap-2 text-white hover:text-primary transition-colors">
             <span class="text-xs font-bold tracking-widest">搜索</span>
        </div>

        <div @click="router.push({ name: 'data-lab' })" class="cursor-pointer group flex items-center gap-2 text-white hover:text-primary transition-colors">
             <span class="text-xs font-bold tracking-widest">数据实验室</span>
        </div>

        <div @click="router.push({ name: 'market' })" class="cursor-pointer group flex items-center gap-2 text-white hover:text-primary transition-colors">
             <span class="text-xs font-bold tracking-widest">市集</span>
        </div>

        <div @click="router.push({ name: 'vip' })" class="cursor-pointer group flex items-center gap-2 text-white hover:text-primary transition-colors">
             <span class="text-xs font-bold tracking-widest">VIP</span>
        </div>

        <div
          v-if="hasToken"
          @click="router.push({ name: 'recipe-publish' })"
          class="cursor-pointer group flex items-center gap-2 text-white hover:text-primary transition-colors"
        >
          <span class="text-xs font-bold tracking-widest">发布</span>
        </div>

        <div
          v-if="hasToken"
          @click="router.push({ name: 'social' })"
          class="cursor-pointer group flex items-center gap-2 text-white hover:text-primary transition-colors"
        >
          <span class="text-xs font-bold tracking-widest">社交</span>
        </div>

        <div
          v-if="hasToken"
          @click="router.push({ name: 'points' })"
          class="cursor-pointer group flex items-center gap-2 text-white hover:text-primary transition-colors"
        >
          <span class="text-xs font-bold tracking-widest">积分</span>
        </div>
        
        <div
          v-if="hasToken"
          @click="router.push({ name: 'user-profile' })"
          class="cursor-pointer group flex items-center gap-3"
        >
             <span class="text-xs font-bold tracking-widest text-white group-hover:text-primary transition-colors">我的主页</span>
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
          <span class="text-xs font-bold tracking-widest text-white group-hover:text-primary transition-colors">登录</span>
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
      <div class="absolute bottom-10 inset-x-0 animate-bounce z-20 flex flex-col items-center gap-2 pointer-events-none">
        <span class="text-[10px] text-gray-400 tracking-widest">下滑探索</span>
        <svg class="w-6 h-6 text-white opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3"></path>
        </svg>
      </div>
    </div>

    <!-- Featured Section (Masonry) -->
    <div class="content min-h-screen bg-dark-bg p-4 md:p-8 lg:p-20 relative z-20">
      <div class="max-w-7xl mx-auto">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-14 px-2">
          <div class="rounded-2xl border border-white/10 bg-black/20 p-6">
            <div class="text-xs tracking-widest text-gray-500 mb-2">活动</div>
            <div class="text-2xl font-serif text-white mb-2">发布食谱赢积分</div>
            <div class="text-sm text-gray-300 leading-relaxed">发布优质菜谱将获得积分奖励，互动越多奖励越多。</div>
            <button v-if="hasToken" @click="router.push({ name: 'recipe-publish' })" class="mt-5 px-5 py-2.5 rounded-xl bg-primary text-black font-bold">
              去发布
            </button>
            <button v-else @click="router.push({ name: 'login', query: { redirect: '/recipe/publish' } })" class="mt-5 px-5 py-2.5 rounded-xl bg-primary text-black font-bold">
              登录后参与
            </button>
          </div>
          <div class="rounded-2xl border border-white/10 bg-black/20 p-6">
            <div class="text-xs tracking-widest text-gray-500 mb-2">活动</div>
            <div class="text-2xl font-serif text-white mb-2">积分兑换 VIP</div>
            <div class="text-sm text-gray-300 leading-relaxed">使用积分兑换限时会员，解锁更多权益与专属勋章。</div>
            <button @click="router.push({ name: 'vip' })" class="mt-5 px-5 py-2.5 rounded-xl border border-white/10 text-gray-200 hover:border-primary hover:text-primary transition-colors">
              查看权益
            </button>
          </div>
          <div class="rounded-2xl border border-white/10 bg-black/20 p-6">
            <div class="text-xs tracking-widest text-gray-500 mb-2">活动</div>
            <div class="text-2xl font-serif text-white mb-2">每日签到拿加成</div>
            <div class="text-sm text-gray-300 leading-relaxed">连续签到可获得额外积分加成，保持活跃更容易升级。</div>
            <button v-if="hasToken" @click="router.push({ name: 'points' })" class="mt-5 px-5 py-2.5 rounded-xl border border-white/10 text-gray-200 hover:border-primary hover:text-primary transition-colors">
              去签到
            </button>
            <button v-else @click="router.push({ name: 'login', query: { redirect: '/points' } })" class="mt-5 px-5 py-2.5 rounded-xl border border-white/10 text-gray-200 hover:border-primary hover:text-primary transition-colors">
              登录后签到
            </button>
          </div>
        </div>

        <div class="flex items-end justify-between mb-12 px-2">
          <h2 class="text-4xl md:text-5xl font-serif text-primary">精选食谱</h2>
          <button class="hidden md:block text-white hover:text-primary transition-colors underline decoration-1 underline-offset-8 text-sm tracking-widest">查看全部</button>
        </div>
        
        <!-- Recipe Feed Component -->
        <RecipeFeed />
      </div>
    </div>

    <div class="fixed right-6 bottom-6 z-40">
      <button
        @click="showAi = !showAi"
        class="w-14 h-14 rounded-full bg-primary text-black font-bold shadow-lg hover:bg-white transition-colors"
        title="AI 助手"
      >
        AI
      </button>
    </div>

    <transition name="fade">
      <div v-if="showAi" class="fixed inset-0 z-40 bg-black/50 backdrop-blur" @click="showAi = false"></div>
    </transition>
    <transition name="slide">
      <div v-if="showAi" class="fixed right-0 top-0 bottom-0 z-50 w-[92vw] max-w-md bg-dark-surface border-l border-white/10 p-6 flex flex-col" @click.stop>
        <div class="flex items-center justify-between mb-4">
          <div class="text-sm tracking-widest text-gray-400">AI 助手</div>
          <button @click="showAi = false" class="text-gray-400 hover:text-white transition-colors">✕</button>
        </div>
        <div class="flex-1 overflow-auto space-y-3 pr-1">
          <div v-if="aiMessages.length === 0" class="text-sm text-gray-500 leading-relaxed">
            你可以问我：菜谱推荐、食材替换、火候时间、口味调整……（支持本地知识库检索与远程模型占位）
          </div>
          <div v-for="(m, i) in aiMessages" :key="i" class="text-sm leading-relaxed">
            <span class="text-primary font-bold mr-2">{{ m.role === 'user' ? '我' : '助手' }}</span>
            <span class="text-gray-200">{{ m.content }}</span>
            <div v-if="m.role === 'assistant' && m.sources && m.sources.length" class="mt-2 text-[11px] text-gray-500 leading-relaxed">
              <div class="tracking-widest">参考来源</div>
              <div class="break-words">{{ m.sources.join(' · ') }}</div>
            </div>
            <div v-if="m.role === 'assistant' && m.usedRemoteModel === false" class="mt-1 text-[11px] text-gray-600 tracking-widest">
              本地知识库占位（未启用远程模型）
            </div>
          </div>
          <div v-if="aiSending" class="text-sm text-gray-500 tracking-widest">助手思考中…</div>
        </div>
        <div class="mt-4 flex items-center gap-2">
          <input
            v-model="aiInput"
            @keyup.enter="sendAi"
            placeholder="输入问题…"
            :disabled="aiSending"
            class="flex-1 px-4 py-2 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary text-sm disabled:opacity-60"
          />
          <button @click="sendAi" :disabled="aiSending || !aiInput.trim()" class="px-4 py-2 rounded-xl bg-primary text-black font-bold text-sm disabled:opacity-60">
            发送
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
/* Ensure smooth blending */
.mix-blend-difference {
  mix-blend-mode: difference;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-enter-active,
.slide-leave-active {
  transition: transform 0.25s ease;
}
.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}
</style>
