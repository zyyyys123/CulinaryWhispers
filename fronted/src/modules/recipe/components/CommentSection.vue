<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { gsap } from 'gsap'
import { SocialAPI } from '@/api/social'
import type { CommentVO } from '@/types/social'

const props = defineProps<{
  recipeId: string
}>()

const comments = ref<CommentVO[]>([])
const loading = ref(true)
const newComment = ref('')

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

// Post
const submitComment = async () => {
  if (!newComment.value.trim()) return
  
  // Optimistic UI Update
  const tempComment: CommentVO = {
    id: `temp_${Date.now()}`,
    recipeId: props.recipeId,
    content: newComment.value,
    author: {
      userId: 'me',
      username: 'me',
      nickname: 'You',
      avatarUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
      totalSpend: 0,
      isMasterChef: false
    },
    createTime: 'Just now',
    likeCount: 0,
    isLiked: false
  }
  
  comments.value.unshift(tempComment)
  const content = newComment.value
  newComment.value = ''
  
  // Animate the new item
  setTimeout(() => {
    gsap.fromTo(
      document.querySelector('.comment-item:first-child'),
      { height: 0, opacity: 0, marginTop: 0 },
      { height: 'auto', opacity: 1, marginTop: '1.5rem', duration: 0.5, ease: 'back.out' }
    )
  }, 10)

  // Real API call
  await SocialAPI.postComment({ recipeId: props.recipeId, content })
}

const toggleLike = async (comment: CommentVO) => {
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
    <h3 class="text-2xl font-serif text-primary mb-8">Discussion ({{ comments.length }})</h3>
    
    <!-- Input Area -->
    <div class="flex gap-4 mb-10">
      <div class="w-10 h-10 rounded-full bg-gray-700 overflow-hidden shrink-0">
        <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" class="w-full h-full object-cover" />
      </div>
      <div class="flex-1 relative">
        <textarea 
          v-model="newComment"
          placeholder="Share your thoughts or ask a question..." 
          class="w-full bg-dark-surface border border-gray-700 rounded-xl p-4 text-white placeholder-gray-500 focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all resize-none h-24"
        ></textarea>
        <button 
          @click="submitComment"
          class="absolute bottom-3 right-3 px-4 py-1.5 bg-primary text-black text-xs font-bold rounded-lg hover:bg-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="!newComment.trim()"
        >
          POST
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
            <span v-if="comment.author.isMasterChef" class="w-3 h-3 bg-primary rounded-full" title="Master Chef"></span>
            <span class="text-xs text-gray-500">• {{ comment.createTime }}</span>
          </div>
          
          <p class="text-gray-300 text-sm leading-relaxed mb-2">{{ comment.content }}</p>
          
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
            <button class="hover:text-white transition-colors">Reply</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
