<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { gsap } from 'gsap'
import type { BadgeVO } from '@/types/user'

const { badges } = defineProps<{
  badges: BadgeVO[]
}>()

const emit = defineEmits(['close'])

const overlayRef = ref<HTMLElement | null>(null)
const contentRef = ref<HTMLElement | null>(null)
const active = ref<'all' | 'unlocked' | 'locked'>('all')

const list = computed(() => {
  if (active.value === 'unlocked') return badges.filter(b => b.isUnlocked)
  if (active.value === 'locked') return badges.filter(b => !b.isUnlocked)
  return badges
})

const tone = (b: BadgeVO) => {
  const name = (b.name || '').toLowerCase()
  if (name.includes('vip') || name.includes('黄金')) return 'gold'
  if (name.includes('白银')) return 'silver'
  if (name.includes('青铜')) return 'bronze'
  if (name.includes('等级')) return 'level'
  if (name.includes('活动')) return 'event'
  return 'default'
}

const monogram = (b: BadgeVO) => {
  const n = (b.name || '').trim()
  if (!n) return 'CW'
  const parts = n.split(/\s+/).filter(Boolean)
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
  return n.slice(0, 2).toUpperCase()
}

onMounted(() => {
  gsap.from(overlayRef.value, { opacity: 0, duration: 0.35 })
  gsap.from(contentRef.value, { y: 18, opacity: 0, duration: 0.45, ease: 'power2.out' })
})

const close = () => {
  gsap.to(overlayRef.value, { opacity: 0, duration: 0.25 })
  gsap.to(contentRef.value, { y: 12, opacity: 0, duration: 0.25, onComplete: () => emit('close') })
}
</script>

<template>
  <div ref="overlayRef" class="fixed inset-0 z-50 bg-black/70 backdrop-blur-xl" @click="close">
    <div ref="contentRef" class="max-w-6xl mx-auto px-4 md:px-8 py-10" @click.stop>
      <div class="flex items-start justify-between gap-4 mb-8">
        <div>
          <div class="text-xs tracking-widest text-gray-500 mb-2">BADGES</div>
          <h2 class="text-4xl md:text-5xl font-serif text-primary">勋章墙</h2>
          <div class="text-sm text-gray-400 mt-3 tracking-wider">记录你的成长与高光时刻</div>
        </div>
        <button
          @click="close"
          class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-sm tracking-widest"
        >
          关闭
        </button>
      </div>

      <div class="flex flex-wrap gap-2 mb-6">
        <button
          @click="active = 'all'"
          class="px-4 py-2 rounded-full text-xs tracking-widest uppercase border transition-colors"
          :class="active === 'all' ? 'border-primary text-primary' : 'border-white/10 text-gray-300 hover:border-white/30'"
        >
          全部
        </button>
        <button
          @click="active = 'unlocked'"
          class="px-4 py-2 rounded-full text-xs tracking-widest uppercase border transition-colors"
          :class="active === 'unlocked' ? 'border-primary text-primary' : 'border-white/10 text-gray-300 hover:border-white/30'"
        >
          已解锁
        </button>
        <button
          @click="active = 'locked'"
          class="px-4 py-2 rounded-full text-xs tracking-widest uppercase border transition-colors"
          :class="active === 'locked' ? 'border-primary text-primary' : 'border-white/10 text-gray-300 hover:border-white/30'"
        >
          未解锁
        </button>
      </div>

      <div v-if="list.length === 0" class="text-center py-24 text-gray-500 tracking-widest">
        暂无勋章
      </div>

      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        <div
          v-for="b in list"
          :key="b.id"
          class="relative rounded-2xl border border-white/10 bg-black/20 overflow-hidden"
          :class="b.isUnlocked ? 'hover:border-primary/40 transition-colors' : 'opacity-70'"
        >
          <div
            class="absolute inset-0 opacity-60"
            :class="
              tone(b) === 'gold'
                ? 'bg-gradient-to-br from-yellow-500/30 via-white/5 to-black/30'
                : tone(b) === 'silver'
                  ? 'bg-gradient-to-br from-gray-300/25 via-white/5 to-black/30'
                  : tone(b) === 'bronze'
                    ? 'bg-gradient-to-br from-amber-700/25 via-white/5 to-black/30'
                    : tone(b) === 'level'
                      ? 'bg-gradient-to-br from-blue-500/25 via-white/5 to-black/30'
                      : tone(b) === 'event'
                        ? 'bg-gradient-to-br from-fuchsia-500/25 via-white/5 to-black/30'
                        : 'bg-gradient-to-br from-white/10 via-white/5 to-black/30'
            "
          ></div>

          <div class="relative p-5 flex items-start gap-4">
            <div class="w-14 h-14 rounded-2xl border border-white/10 bg-black/30 flex items-center justify-center">
              <div class="text-sm tracking-widest text-gray-200 font-bold">
                {{ monogram(b) }}
              </div>
            </div>

            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2 min-w-0">
                <div class="font-serif text-xl text-white truncate">{{ b.name }}</div>
                <div
                  class="px-2 py-0.5 rounded-full border text-[10px] tracking-widest shrink-0"
                  :class="b.isUnlocked ? 'border-primary/40 text-primary' : 'border-white/10 text-gray-400'"
                >
                  {{ b.isUnlocked ? '已解锁' : '未解锁' }}
                </div>
              </div>
              <div class="text-sm text-gray-300 mt-2 leading-relaxed">
                {{ b.description || '—' }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
