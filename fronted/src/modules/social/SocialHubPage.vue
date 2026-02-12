<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { SocialAPI } from '@/api/social'
import type { FollowVO } from '@/types/social'

const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')

const activeTab = ref<'following' | 'followers'>('following')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const list = ref<FollowVO[]>([])

const targetUserId = ref('')

const hasMore = computed(() => list.value.length < total.value)

const load = async (reset = true) => {
  if (loading.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    if (reset) {
      page.value = 1
      total.value = 0
      list.value = []
    }
    const res =
      activeTab.value === 'following'
        ? await SocialAPI.getFollowing({ page: page.value, size: size.value })
        : await SocialAPI.getFollowers({ page: page.value, size: size.value })

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

const follow = async () => {
  const uid = targetUserId.value.trim()
  if (!uid) return
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await SocialAPI.follow(uid)
    if (res.code !== 200) {
      errorMessage.value = res.message || '关注失败'
      return
    }
    await load(true)
  } catch {
    errorMessage.value = '关注失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const unfollow = async (uid: string) => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await SocialAPI.unfollow(uid)
    if (res.code !== 200) {
      errorMessage.value = res.message || '取消关注失败'
      return
    }
    await load(true)
  } catch {
    errorMessage.value = '取消关注失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load(true)
})
</script>

<template>
  <div class="min-h-screen bg-dark-bg text-white px-4 md:px-8 py-10">
    <div class="max-w-5xl mx-auto">
      <div class="flex items-start justify-between gap-4 mb-8">
        <div>
          <div class="text-xs tracking-widest uppercase text-gray-500 mb-2">Social</div>
          <h1 class="text-4xl md:text-5xl font-serif text-primary">Follow Center</h1>
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
            v-model="targetUserId"
            placeholder="输入用户ID关注（示例：1000）"
            class="flex-1 w-full px-4 py-3 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary"
          />
          <button
            @click="follow"
            :disabled="loading"
            class="px-6 py-3 rounded-xl bg-primary text-black font-bold disabled:opacity-60"
          >
            Follow
          </button>
        </div>

        <div class="flex gap-2 mt-4">
          <button
            @click="activeTab = 'following'; load(true)"
            class="px-4 py-2 rounded-full text-xs tracking-widest uppercase border transition-colors"
            :class="activeTab === 'following' ? 'border-primary text-primary' : 'border-white/10 text-gray-300 hover:border-white/30'"
          >
            Following
          </button>
          <button
            @click="activeTab = 'followers'; load(true)"
            class="px-4 py-2 rounded-full text-xs tracking-widest uppercase border transition-colors"
            :class="activeTab === 'followers' ? 'border-primary text-primary' : 'border-white/10 text-gray-300 hover:border-white/30'"
          >
            Followers
          </button>
        </div>

        <div v-if="errorMessage" class="mt-4 text-sm text-red-300">{{ errorMessage }}</div>
      </div>

      <div class="space-y-3">
        <div
          v-for="item in list"
          :key="item.userId"
          class="rounded-2xl border border-white/10 bg-black/10 p-5 flex items-center justify-between gap-4"
        >
          <div class="flex items-center gap-4 min-w-0">
            <img :src="item.user.avatarUrl" class="w-12 h-12 rounded-full border border-white/10" />
            <div class="min-w-0">
              <div class="font-bold truncate">{{ item.user.nickname }}</div>
              <div class="text-xs text-gray-500 tracking-widest uppercase">User {{ item.userId }}</div>
              <div class="text-xs text-gray-600 mt-1">{{ item.createTime }}</div>
            </div>
          </div>
          <div class="flex gap-2 shrink-0">
            <button
              v-if="activeTab === 'following'"
              @click="unfollow(item.userId)"
              class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-xs tracking-widest uppercase"
            >
              Unfollow
            </button>
          </div>
        </div>
      </div>

      <div class="flex justify-center mt-10">
        <button
          v-if="hasMore && !loading"
          @click="load(false)"
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

