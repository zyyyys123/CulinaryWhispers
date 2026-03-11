<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { SocialAPI } from '@/api/social'
import type { FollowVO } from '@/types/social'
import { NotifyAPI } from '@/api/notify'
import type { NotificationVO } from '@/types/notify'
import { normalizeAssetUrl } from '@/utils/assetUrl'
import CwErrorState from '@/components/feedback/CwErrorState.vue'
import CwEmptyState from '@/components/feedback/CwEmptyState.vue'
import CwListFooter from '@/components/feedback/CwListFooter.vue'

const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')

const activeTab = ref<'following' | 'followers' | 'notifications'>('notifications')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const list = ref<FollowVO[]>([])
const lastFetchCount = ref(0)

const nPage = ref(1)
const nSize = ref(10)
const nTotal = ref(0)
const notifications = ref<NotificationVO[]>([])
const lastNotifyFetchCount = ref(0)
const notifyFilter = ref<'all' | 'like' | 'comment' | 'collect'>('all')

const targetUserId = ref('')
const editingRemarkUserId = ref<string | null>(null)
const remarkDraft = ref('')

const hasMore = computed(() => (total.value > 0 ? list.value.length < total.value : lastFetchCount.value === size.value))
const hasMoreNotify = computed(() => (nTotal.value > 0 ? notifications.value.length < nTotal.value : lastNotifyFetchCount.value === nSize.value))

const notifyTypeLabel = (t: number) => {
  if (t === 1) return '回复'
  if (t === 2) return '评论'
  if (t === 3) return '点赞'
  if (t === 4) return '收藏'
  if (t === 5) return '点赞'
  return '通知'
}

const avatarFallback = (seed: string) =>
  `https://api.dicebear.com/7.x/thumbs/svg?seed=${encodeURIComponent(seed || 'user')}`

const avatarSrc = (url?: string, seed?: string) => normalizeAssetUrl(url) ?? avatarFallback(seed || 'user')
const notifyAvatarSrc = (url?: string, seed?: string) => normalizeAssetUrl(url) ?? `https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(seed || 'u')}`

const onAvatarError = (e: Event) => {
  const img = e.target as HTMLImageElement | null
  if (!img) return
  const seed = img.dataset.seed || 'user'
  const next = avatarFallback(seed)
  if (img.src === next) return
  img.src = next
}

const filteredNotifications = computed(() => {
  if (notifyFilter.value === 'all') return notifications.value
  if (notifyFilter.value === 'like') return notifications.value.filter(n => n.type === 3 || n.type === 5)
  if (notifyFilter.value === 'comment') return notifications.value.filter(n => n.type === 1 || n.type === 2)
  if (notifyFilter.value === 'collect') return notifications.value.filter(n => n.type === 4)
  return notifications.value
})

const load = async (reset = true) => {
  if (loading.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    if (activeTab.value === 'notifications') {
      if (reset) {
        nPage.value = 1
        nTotal.value = 0
        notifications.value = []
        notifyFilter.value = 'all'
      }
      const res = await NotifyAPI.list({ page: nPage.value, size: nSize.value })
      if (res.code !== 200) {
        errorMessage.value = res.message || '加载失败'
        return
      }
      nTotal.value = Number(res.data.total ?? 0)
      const batch = res.data.records ?? []
      notifications.value.push(...batch)
      lastNotifyFetchCount.value = batch.length
      nPage.value++
    } else {
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
      const batch = res.data.records ?? []
      list.value.push(...batch)
      lastFetchCount.value = batch.length
      page.value++
    }
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

const startEditRemark = (item: FollowVO) => {
  editingRemarkUserId.value = item.userId
  remarkDraft.value = item.remarkName ?? ''
}

const cancelEditRemark = () => {
  editingRemarkUserId.value = null
  remarkDraft.value = ''
}

const saveRemark = async (item: FollowVO) => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await SocialAPI.updateFollowRemark(item.userId, remarkDraft.value.trim() ? remarkDraft.value.trim() : undefined)
    if (res.code !== 200) {
      errorMessage.value = res.message || '备注失败'
      return
    }
    item.remarkName = remarkDraft.value.trim() ? remarkDraft.value.trim() : undefined
    cancelEditRemark()
  } catch {
    errorMessage.value = '备注失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load(true)
})

const openNotification = async (n: NotificationVO) => {
  try {
    if (!n.isRead) {
      await NotifyAPI.markRead(n.id)
      n.isRead = true
    }
  } finally {
    if (n.targetType === 1) {
      router.push({ name: 'recipe-detail', params: { id: n.targetId } })
    }
  }
}
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
            @click="activeTab = 'notifications'; load(true)"
            class="px-4 py-2 rounded-full text-xs tracking-widest uppercase border transition-colors"
            :class="activeTab === 'notifications' ? 'border-primary text-primary' : 'border-white/10 text-gray-300 hover:border-white/30'"
          >
            通知
          </button>
          <button
            @click="activeTab = 'following'; load(true)"
            class="px-4 py-2 rounded-full text-xs tracking-widest uppercase border transition-colors"
            :class="activeTab === 'following' ? 'border-primary text-primary' : 'border-white/10 text-gray-300 hover:border-white/30'"
          >
            关注中
          </button>
          <button
            @click="activeTab = 'followers'; load(true)"
            class="px-4 py-2 rounded-full text-xs tracking-widest uppercase border transition-colors"
            :class="activeTab === 'followers' ? 'border-primary text-primary' : 'border-white/10 text-gray-300 hover:border-white/30'"
          >
            粉丝
          </button>
        </div>

        <CwErrorState v-if="errorMessage" class="mt-4" :message="errorMessage" action-label="重试" @action="load(true)" />
      </div>

      <div v-if="activeTab !== 'notifications'" class="space-y-3">
        <CwEmptyState
          v-if="!loading && !errorMessage && list.length === 0"
          :title="activeTab === 'following' ? '暂无关注' : '暂无粉丝'"
          description="先去逛逛内容，找到感兴趣的厨友吧。"
          action-label="刷新"
          @action="load(true)"
        />
        <div v-for="item in list" :key="item.userId" class="space-y-3">
          <div class="rounded-2xl border border-white/10 bg-black/10 p-5 flex items-center justify-between gap-4">
            <div class="flex items-center gap-4 min-w-0">
              <img
                :src="avatarSrc(item.user.avatarUrl, item.user.nickname)"
                class="w-12 h-12 rounded-full border border-white/10"
                :data-seed="item.user.nickname || 'user'"
                @error="onAvatarError"
              />
              <div class="min-w-0">
                <div class="flex items-center gap-2 min-w-0">
                  <div class="font-bold truncate">{{ item.remarkName || item.user.nickname }}</div>
                  <div
                    v-if="item.isMutual"
                    class="px-2 py-0.5 rounded-full border border-primary/40 text-[10px] tracking-widest text-primary shrink-0"
                  >
                    互关
                  </div>
                </div>
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
                取消关注
              </button>
              <button
                v-if="activeTab === 'following' && editingRemarkUserId !== item.userId"
                @click="startEditRemark(item)"
                class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-xs tracking-widest uppercase"
              >
                备注
              </button>
            </div>
          </div>

          <div
            v-if="activeTab === 'following' && editingRemarkUserId === item.userId"
            class="rounded-2xl border border-white/10 bg-black/10 p-5 flex items-center justify-between gap-4"
          >
            <div class="flex items-center gap-3 min-w-0 flex-1">
              <div class="text-sm text-gray-300 shrink-0">备注名</div>
              <input
                v-model="remarkDraft"
                placeholder="输入备注名（最多 64 字）"
                class="flex-1 w-full px-4 py-2 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary"
              />
            </div>
            <div class="flex gap-2 shrink-0">
              <button
                @click="saveRemark(item)"
                class="px-5 py-2 rounded-full bg-primary text-black font-bold text-xs tracking-widest uppercase"
                :disabled="loading"
              >
                保存
              </button>
              <button
                @click="cancelEditRemark"
                class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-xs tracking-widest uppercase"
                :disabled="loading"
              >
                取消
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="space-y-3">
        <div class="flex flex-wrap gap-2 mb-3">
          <button
            @click="notifyFilter = 'all'"
            class="px-4 py-2 rounded-full text-xs tracking-widest uppercase border transition-colors"
            :class="notifyFilter === 'all' ? 'border-primary text-primary' : 'border-white/10 text-gray-300 hover:border-white/30'"
          >
            全部
          </button>
          <button
            @click="notifyFilter = 'like'"
            class="px-4 py-2 rounded-full text-xs tracking-widest uppercase border transition-colors"
            :class="notifyFilter === 'like' ? 'border-primary text-primary' : 'border-white/10 text-gray-300 hover:border-white/30'"
          >
            点赞
          </button>
          <button
            @click="notifyFilter = 'comment'"
            class="px-4 py-2 rounded-full text-xs tracking-widest uppercase border transition-colors"
            :class="notifyFilter === 'comment' ? 'border-primary text-primary' : 'border-white/10 text-gray-300 hover:border-white/30'"
          >
            评论/回复
          </button>
          <button
            @click="notifyFilter = 'collect'"
            class="px-4 py-2 rounded-full text-xs tracking-widest uppercase border transition-colors"
            :class="notifyFilter === 'collect' ? 'border-primary text-primary' : 'border-white/10 text-gray-300 hover:border-white/30'"
          >
            收藏
          </button>
        </div>

        <CwEmptyState
          v-if="!loading && !errorMessage && filteredNotifications.length === 0"
          title="暂无通知"
          description="有新的互动会第一时间通知你。"
          action-label="刷新"
          @action="load(true)"
        />

        <div
          v-for="n in filteredNotifications"
          :key="n.id"
          class="rounded-2xl border border-white/10 bg-black/10 p-5 flex items-center justify-between gap-4 cursor-pointer hover:border-primary/40 transition-colors"
          @click="openNotification(n)"
        >
          <div class="flex items-center gap-4 min-w-0">
            <div class="relative">
              <img :src="notifyAvatarSrc(n.fromAvatarUrl, n.fromUserId)" class="w-12 h-12 rounded-full border border-white/10" />
              <span v-if="!n.isRead" class="absolute -top-1 -right-1 w-3 h-3 rounded-full bg-primary"></span>
            </div>
            <div class="min-w-0">
              <div class="flex items-center gap-2 min-w-0">
                <div class="font-bold truncate">{{ n.fromNickname || `用户 ${n.fromUserId}` }}</div>
                <div class="px-2 py-0.5 rounded-full border border-white/10 text-[10px] tracking-widest text-gray-400 shrink-0">
                  {{ notifyTypeLabel(n.type) }}
                </div>
              </div>
              <div class="text-sm text-gray-300 mt-1 truncate">{{ n.content }}</div>
              <div class="text-xs text-gray-600 mt-1">{{ n.createTime }}</div>
            </div>
          </div>
          <div class="text-xs tracking-widest text-gray-500 shrink-0">查看</div>
        </div>
      </div>

      <CwListFooter
        v-if="!errorMessage && activeTab !== 'notifications' && list.length > 0"
        :loading="loading"
        :hasMore="hasMore"
        load-more-label="加载更多"
        loading-label="加载中…"
        end-label="已到底"
        @loadMore="load(false)"
      />

      <CwListFooter
        v-else-if="!errorMessage && activeTab === 'notifications' && notifications.length > 0"
        :loading="loading"
        :hasMore="hasMoreNotify"
        load-more-label="加载更多"
        loading-label="加载中…"
        end-label="已到底"
        @loadMore="load(false)"
      />
    </div>
  </div>
</template>
