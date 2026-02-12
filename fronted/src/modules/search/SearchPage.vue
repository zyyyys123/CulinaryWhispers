<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { SearchAPI } from '@/api/search'

const router = useRouter()

const keyword = ref('')
const loading = ref(false)
const errorMessage = ref('')
const activeMode = ref<'personalized' | 'global'>('personalized')

const page = ref(1)
const size = ref(10)
const total = ref(0)
const items = ref<Array<{ id: string; title: string; description: string; tags: string[]; authorName: string; difficulty: number; timeCost: number; score: number }>>([])

const hasMore = computed(() => items.value.length < total.value)

const search = async (reset = true) => {
  if (loading.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    if (reset) {
      page.value = 1
      items.value = []
      total.value = 0
    }

    const res =
      activeMode.value === 'personalized'
        ? await SearchAPI.searchPersonalized({ keyword: keyword.value || undefined, page: page.value, size: size.value })
        : await SearchAPI.searchRecipe({ keyword: keyword.value, page: page.value, size: size.value })

    if (res.code !== 200) {
      errorMessage.value = res.message || '搜索失败'
      return
    }

    const p = res.data
    total.value = Number(p.total ?? 0)
    items.value.push(...(p.records ?? []))
    page.value++
  } catch {
    errorMessage.value = '搜索失败，请检查网络或稍后重试'
  } finally {
    loading.value = false
  }
}

const openDetail = (id: string) => {
  router.push({ name: 'recipe-detail', params: { id } })
}
</script>

<template>
  <div class="min-h-screen bg-dark-bg text-white px-4 md:px-8 py-10">
    <div class="max-w-5xl mx-auto">
      <div class="flex items-start justify-between gap-4 mb-8">
        <div>
          <div class="text-xs tracking-widest uppercase text-gray-500 mb-2">Search</div>
          <h1 class="text-4xl md:text-5xl font-serif text-primary">Find Recipes</h1>
        </div>
        <button
          @click="router.push({ name: 'home' })"
          class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-sm tracking-widest uppercase"
        >
          Back Home
        </button>
      </div>

      <div class="rounded-2xl border border-white/10 bg-black/20 p-5 md:p-6 mb-8">
        <div class="flex flex-col md:flex-row gap-4 md:items-center">
          <input
            v-model="keyword"
            @keyup.enter="search(true)"
            placeholder="输入关键词：例如 鸡胸肉 / pasta / dessert"
            class="flex-1 w-full px-4 py-3 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary"
          />
          <button
            @click="search(true)"
            :disabled="loading"
            class="px-6 py-3 rounded-xl bg-primary text-black font-bold disabled:opacity-60"
          >
            {{ loading ? 'Searching...' : 'Search' }}
          </button>
        </div>

        <div class="flex gap-2 mt-4">
          <button
            @click="activeMode = 'personalized'; search(true)"
            class="px-4 py-2 rounded-full text-xs tracking-widest uppercase border transition-colors"
            :class="activeMode === 'personalized' ? 'border-primary text-primary' : 'border-white/10 text-gray-300 hover:border-white/30'"
          >
            Personalized
          </button>
          <button
            @click="activeMode = 'global'; search(true)"
            class="px-4 py-2 rounded-full text-xs tracking-widest uppercase border transition-colors"
            :class="activeMode === 'global' ? 'border-primary text-primary' : 'border-white/10 text-gray-300 hover:border-white/30'"
          >
            Global
          </button>
        </div>

        <div v-if="errorMessage" class="mt-4 text-sm text-red-300">{{ errorMessage }}</div>
      </div>

      <div class="space-y-4">
        <button
          v-for="r in items"
          :key="r.id"
          @click="openDetail(r.id)"
          class="w-full text-left rounded-2xl border border-white/10 bg-black/10 hover:bg-black/20 transition-colors p-5"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="min-w-0">
              <div class="text-lg font-bold truncate">{{ r.title }}</div>
              <div class="text-gray-400 text-sm mt-1 line-clamp-2">{{ r.description }}</div>
              <div class="flex flex-wrap gap-2 mt-3">
                <span v-for="t in r.tags" :key="t" class="text-[11px] px-2 py-1 rounded-full bg-white/5 border border-white/10 text-gray-300">
                  {{ t }}
                </span>
              </div>
            </div>
            <div class="shrink-0 text-right text-xs text-gray-400 tracking-widest uppercase">
              <div>Score {{ r.score.toFixed(1) }}</div>
              <div class="mt-1">Lv {{ r.difficulty }}</div>
              <div class="mt-1">{{ r.timeCost }} min</div>
            </div>
          </div>
        </button>
      </div>

      <div class="flex justify-center mt-10">
        <button
          v-if="hasMore && !loading"
          @click="search(false)"
          class="px-8 py-3 rounded-full border border-gray-700 hover:border-primary text-gray-300 hover:text-primary transition-colors text-sm tracking-widest uppercase"
        >
          Load More
        </button>
        <div v-else-if="!loading && items.length > 0" class="text-gray-600 text-sm tracking-widest uppercase">
          End of Results
        </div>
      </div>
    </div>
  </div>
</template>

