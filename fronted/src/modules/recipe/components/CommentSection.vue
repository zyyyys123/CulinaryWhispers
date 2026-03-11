<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { gsap } from 'gsap'
import { SocialAPI } from '@/api/social'
import type { CommentVO } from '@/types/social'
import { useAuthStore } from '@/stores/auth'
import { useRoute, useRouter } from 'vue-router'
import { normalizeAssetUrl } from '@/utils/assetUrl'

const props = defineProps<{
  recipeId: string
}>()

const comments = ref<CommentVO[]>([])
const loading = ref(true)
const newComment = ref('')
const replyTo = ref<CommentVO | null>(null)
const inputEl = ref<HTMLTextAreaElement | null>(null)

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
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

const myAvatarSrc = () =>
  normalizeAssetUrl(auth.profile?.avatarUrl) ??
  `https://api.dicebear.com/7.x/thumbs/svg?seed=${encodeURIComponent(auth.profile?.nickname || auth.profile?.username || 'me')}`

// Fetch
const fetchComments = async () => {
  const res = await SocialAPI.getComments({ recipeId: props.recipeId, page: 1, size: 5 })
  if (res.code === 200) {
    comments.value = res.data.records
    loading.value = false
    
    // Animate entrance
    setTimeout(() => {
      gsap.from('.comment-item', {
        x: -20,
        opacity: 0,
        duration: 0.5,
        stagger: 0.1,
        ease: 'power2.out'
      })
    }, 100)
  }
}

const startReply = async (comment: CommentVO) => {
  replyTo.value = comment
  const mention = `@${comment.author.userId} `
  if (!newComment.value.trim()) {
    newComment.value = mention
  } else if (!newComment.value.startsWith(mention)) {
    newComment.value = mention + newComment.value.trimStart()
  }
  await nextTick()
  if (inputEl.value) {
    inputEl.value.scrollIntoView({ behavior: 'smooth', block: 'center' })
    inputEl.value.focus()
  }
}

const cancelReply = () => {
  replyTo.value = null
}

// Post
const submitComment = async () => {
  if (!newComment.value.trim()) return
  if (!auth.token) {
    openLoginPrompt()
    return
  }
  
  // Optimistic UI Update
  if (!auth.profile) {
    auth.loadProfile()
  }
  const tempComment: CommentVO = {
    id: `temp_${Date.now()}`,
    recipeId: props.recipeId,
    content: newComment.value,
    parentId: replyTo.value?.id,
    author: {
      userId: auth.profile?.userId ?? 'me',
      username: auth.profile?.username ?? 'me',
      nickname: auth.profile?.nickname ?? '我',
      avatarUrl: auth.profile?.avatarUrl ?? 'https://api.dicebear.com/7.x/avataaars/svg?seed=me',
      totalSpend: auth.profile?.totalSpend ?? 0,
      isMasterChef: Boolean(auth.profile?.isMasterChef)
    },
    createTime: '刚刚',
    likeCount: 0,
    isLiked: false
  }
  
  comments.value.unshift(tempComment)
  const content = newComment.value
  const parentId = replyTo.value?.id
  newComment.value = ''
  replyTo.value = null
  
  // Animate the new item
  setTimeout(() => {
    gsap.fromTo(
      document.querySelector('.comment-item:first-child'),
      { height: 0, opacity: 0, marginTop: 0 },
      { height: 'auto', opacity: 1, marginTop: '1.5rem', duration: 0.5, ease: 'back.out' }
    )
  }, 10)

  // Real API call
  const res = await SocialAPI.postComment({ recipeId: props.recipeId, content, parentId })
  if (res.code === 200) {
    fetchComments()
  }
}

const toggleLike = async (comment: CommentVO) => {
  if (!auth.token) {
    openLoginPrompt()
    return
  }
  // Optimistic UI
  comment.isLiked = !comment.isLiked
  comment.likeCount += comment.isLiked ? 1 : -1
  
  // API Call (Target Type 2 = Comment, Action 1 = Like)
  await SocialAPI.interact({
    targetType: 2,
    targetId: comment.id,
    actionType: 1
  })
}

onMounted(() => {
  fetchComments()
})
</script>

<template>
  <div class="comments-section mt-12 pt-12 border-t border-gray-800">
    <h3 class="text-2xl font-serif text-primary mb-8">评论区（{{ comments.length }}）</h3>
    
    <!-- Input Area -->
    <div class="flex gap-4 mb-10">
      <div class="w-10 h-10 rounded-full bg-gray-700 overflow-hidden shrink-0">
        <img :src="myAvatarSrc()" class="w-full h-full object-cover" />
      </div>
      <div class="flex-1 relative">
        <div v-if="replyTo" class="mb-2 inline-flex items-center gap-2 px-3 py-1 rounded-full border border-white/10 bg-black/20 text-xs text-gray-300">
          <span>回复 {{ replyTo.author.nickname }}</span>
          <button @click="cancelReply" class="text-gray-400 hover:text-white transition-colors">✕</button>
        </div>
        <textarea 
          v-model="newComment"
          ref="inputEl"
          placeholder="说点什么…（支持回复）" 
          class="w-full bg-dark-surface border border-gray-700 rounded-xl p-4 text-white placeholder-gray-500 focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all resize-none h-24"
        ></textarea>
        <button 
          @click="submitComment"
          class="absolute bottom-3 right-3 px-4 py-1.5 bg-primary text-black text-xs font-bold rounded-lg hover:bg-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="!newComment.trim()"
        >
          发布
        </button>
      </div>
    </div>

    <!-- List -->
    <div class="space-y-6">
      <div v-for="comment in comments" :key="comment.id" class="comment-item flex gap-4">
        <!-- Avatar -->
        <div class="w-10 h-10 rounded-full border border-gray-700 overflow-hidden shrink-0">
          <img :src="comment.author.avatarUrl" class="w-full h-full object-cover" />
        </div>
        
        <!-- Content -->
        <div class="flex-1">
          <div class="flex items-center gap-2 mb-1">
            <span class="font-bold text-white">{{ comment.author.nickname }}</span>
            <span v-if="comment.author.isMasterChef" class="w-3 h-3 bg-primary rounded-full" title="认证大厨"></span>
            <span class="text-xs text-gray-500">• {{ comment.createTime }}</span>
          </div>
          
          <p class="text-gray-300 text-sm leading-relaxed mb-2">
            <span v-if="comment.parentId" class="text-primary mr-1">@回复</span>
            {{ comment.content }}
          </p>
          
          <!-- Actions -->
          <div class="flex items-center gap-4 text-xs text-gray-500">
            <button 
              @click="toggleLike(comment)"
              class="flex items-center gap-1 hover:text-primary transition-colors group"
              :class="{ 'text-primary': comment.isLiked }"
            >
              <svg class="w-4 h-4 group-hover:fill-current" :class="{ 'fill-current': comment.isLiked }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path></svg>
              {{ comment.likeCount }}
            </button>
            <button @click="startReply(comment)" class="hover:text-white transition-colors">回复</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showLoginPrompt" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm px-6">
      <div class="w-full max-w-sm rounded-2xl border border-white/10 bg-black/70 p-6">
        <div class="text-lg font-bold mb-2">需要登录</div>
        <div class="text-gray-300 text-sm mb-6">请先登录后再进行评论或点赞操作。</div>
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
  </div>
</template>
