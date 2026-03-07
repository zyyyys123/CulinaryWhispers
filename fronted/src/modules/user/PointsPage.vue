<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { PointsAPI } from '@/api/points'
import type { PointsRecordVO } from '@/api/points'
import CwErrorState from '@/components/feedback/CwErrorState.vue'
import CwEmptyState from '@/components/feedback/CwEmptyState.vue'
import CwListFooter from '@/components/feedback/CwListFooter.vue'

const router = useRouter()

const historyLoading = ref(false)
const signInLoading = ref(false)
const historyError = ref('')
const signInError = ref('')
const signedPoints = ref<number | null>(null)

const page = ref(1)
const size = ref(10)
const total = ref(0)
const list = ref<PointsRecordVO[]>([])

const hasMore = computed(() => (total.value > 0 ? list.value.length < total.value : list.value.length > 0))

const loadHistory = async (reset = true) => {
  if (historyLoading.value) return
  historyLoading.value = true
  historyError.value = ''
  try {
    if (reset) {
      page.value = 1
      total.value = 0
      list.value = []
    }
    const res = await PointsAPI.history({ page: page.value, size: size.value })
    if (res.code !== 200) {
      historyError.value = res.message || '加载失败'
      return
    }
    total.value = Number(res.data.total ?? 0)
    const records = res.data.records ?? []
    const seen = new Set(list.value.map(r => r.id))
    for (const r of records) {
      if (r?.id && !seen.has(r.id)) {
        list.value.push(r)
        seen.add(r.id)
      }
    }
    page.value++
  } catch {
    historyError.value = '加载失败，请检查网络或稍后重试'
  } finally {
    historyLoading.value = false
  }
}

const signIn = async () => {
  if (signInLoading.value) return
  signInLoading.value = true
  signInError.value = ''
  signedPoints.value = null
  try {
    const res = await PointsAPI.signIn()
    if (res.code !== 200) {
      signInError.value = res.message || '签到失败'
      return
    }
    signedPoints.value = res.data
    const latest = await PointsAPI.history({ page: 1, size: 1 })
    if (latest.code === 200) {
      total.value = Number(latest.data.total ?? total.value)
      const r = latest.data.records?.[0]
      if (r?.id && !list.value.some(x => x.id === r.id)) {
        list.value.unshift(r)
      }
    }
  } catch {
    signInError.value = '签到失败，请稍后重试'
  } finally {
    signInLoading.value = false
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
          <div class="text-xs tracking-widest uppercase text-gray-500 mb-2">成长</div>
          <h1 class="text-4xl md:text-5xl font-serif text-primary">积分</h1>
        </div>
        <button
          @click="router.push({ name: 'home' })"
          class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-sm tracking-widest uppercase"
        >
          返回首页
        </button>
      </div>

      <div class="rounded-2xl border border-white/10 bg-black/20 p-5 md:p-6 mb-8 flex flex-col md:flex-row gap-4 md:items-center md:justify-between">
        <div class="text-gray-300 text-sm tracking-wider">
          每日签到可获得积分，积分可用于后续兑换（演示功能）。
        </div>
        <button
          @click="signIn"
          :disabled="signInLoading"
          class="px-6 py-3 rounded-xl bg-primary text-black font-bold disabled:opacity-60"
        >
          {{ signInLoading ? '处理中…' : '签到' }}
        </button>
      </div>

      <div v-if="signedPoints !== null" class="mb-6 rounded-2xl border border-primary/30 bg-primary/10 px-6 py-4 text-primary">
        签到成功，本次获得 {{ signedPoints }} 积分
      </div>

      <CwErrorState v-if="signInError" class="mb-6" :message="signInError" action-label="重试" @action="signIn" />

      <div class="rounded-2xl border border-white/10 overflow-hidden">
        <div class="bg-black/30 px-5 py-3 text-xs tracking-widest uppercase text-gray-500">
          积分流水（{{ total }}）
        </div>
        <CwErrorState v-if="historyError" class="m-5" :message="historyError" action-label="重试" @action="loadHistory(true)" />
        <CwEmptyState
          v-else-if="list.length === 0 && !historyLoading"
          title="暂无流水"
          description="完成签到、发布或互动后会产生积分记录。"
          action-label="刷新"
          @action="loadHistory(true)"
        />
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

      <CwListFooter
        v-if="!historyError && list.length > 0"
        :loading="historyLoading"
        :hasMore="hasMore"
        load-more-label="加载更多"
        loading-label="加载中…"
        end-label="已到底"
        @loadMore="loadHistory(false)"
      />
    </div>
  </div>
</template>
