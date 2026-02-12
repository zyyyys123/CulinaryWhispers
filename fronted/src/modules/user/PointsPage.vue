<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { PointsAPI } from '@/api/points'
import type { PointsRecordVO } from '@/api/points'

const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const signedPoints = ref<number | null>(null)

const page = ref(1)
const size = ref(10)
const total = ref(0)
const list = ref<PointsRecordVO[]>([])

const loadHistory = async (reset = true) => {
  if (loading.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    if (reset) {
      page.value = 1
      total.value = 0
      list.value = []
    }
    const res = await PointsAPI.history({ page: page.value, size: size.value })
    if (res.code !== 200) {
      errorMessage.value = res.message || '加载失败'
      return
    }
    total.value = Number(res.data.total ?? 0)
    list.value.push(...(res.data.records ?? []))
    page.value++
  } catch {
    errorMessage.value = '加载失败，请检查网络或稍后重试'
  } finally {
    loading.value = false
  }
}

const signIn = async () => {
  if (loading.value) return
  loading.value = true
  errorMessage.value = ''
  signedPoints.value = null
  try {
    const res = await PointsAPI.signIn()
    if (res.code !== 200) {
      errorMessage.value = res.message || '签到失败'
      return
    }
    signedPoints.value = res.data
    await loadHistory(true)
  } catch {
    errorMessage.value = '签到失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadHistory(true)
})
</script>

<template>
  <div class="min-h-screen bg-dark-bg text-white px-4 md:px-8 py-10">
    <div class="max-w-5xl mx-auto">
      <div class="flex items-start justify-between gap-4 mb-8">
        <div>
          <div class="text-xs tracking-widest uppercase text-gray-500 mb-2">Growth</div>
          <h1 class="text-4xl md:text-5xl font-serif text-primary">Points</h1>
        </div>
        <button
          @click="router.push({ name: 'home' })"
          class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-sm tracking-widest uppercase"
        >
          Back Home
        </button>
      </div>

      <div class="rounded-2xl border border-white/10 bg-black/20 p-5 md:p-6 mb-8 flex flex-col md:flex-row gap-4 md:items-center md:justify-between">
        <div class="text-gray-300 text-sm tracking-wider">
          每日签到可获得积分，积分可用于后续兑换（演示功能）。
        </div>
        <button
          @click="signIn"
          :disabled="loading"
          class="px-6 py-3 rounded-xl bg-primary text-black font-bold disabled:opacity-60"
        >
          {{ loading ? 'Working...' : 'Sign In' }}
        </button>
      </div>

      <div v-if="signedPoints !== null" class="mb-6 rounded-2xl border border-primary/30 bg-primary/10 px-6 py-4 text-primary">
        签到成功，本次获得 {{ signedPoints }} 积分
      </div>

      <div v-if="errorMessage" class="mb-6 rounded-2xl border border-red-400/20 bg-red-400/10 px-6 py-4 text-red-300">
        {{ errorMessage }}
      </div>

      <div class="rounded-2xl border border-white/10 overflow-hidden">
        <div class="bg-black/30 px-5 py-3 text-xs tracking-widest uppercase text-gray-500">
          History ({{ total }})
        </div>
        <div v-if="list.length === 0 && !loading" class="px-6 py-10 text-center text-gray-600 text-sm tracking-widest uppercase">
          No Records
        </div>
        <div v-else class="divide-y divide-white/5">
          <div v-for="r in list" :key="r.id" class="px-5 py-4 flex items-start justify-between gap-4">
            <div class="min-w-0">
              <div class="font-bold">
                {{ r.description || `Type ${r.type}` }}
              </div>
              <div class="text-xs text-gray-600 mt-1">{{ r.createTime }}</div>
            </div>
            <div class="shrink-0 text-right">
              <div class="font-bold" :class="r.amount >= 0 ? 'text-primary' : 'text-red-300'">
                {{ r.amount >= 0 ? `+${r.amount}` : r.amount }}
              </div>
              <div class="text-xs text-gray-600 mt-1">Type {{ r.type }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="flex justify-center mt-10">
        <button
          v-if="list.length < total && !loading"
          @click="loadHistory(false)"
          class="px-8 py-3 rounded-full border border-gray-700 hover:border-primary text-gray-300 hover:text-primary transition-colors text-sm tracking-widest uppercase"
        >
          Load More
        </button>
        <div v-else-if="!loading && list.length > 0" class="text-gray-600 text-sm tracking-widest uppercase">
          End of List
        </div>
      </div>
    </div>
  </div>
</template>

