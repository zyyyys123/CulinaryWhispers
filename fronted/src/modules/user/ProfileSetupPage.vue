<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { UserAPI } from '@/api/user'
import { FileAPI } from '@/api/file'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const errorMessage = ref('')
const uploadingAvatar = ref(false)
const uploadingBg = ref(false)

const form = ref({
  nickname: '',
  avatarUrl: '',
  bgImageUrl: '',
  gender: 0,
  signature: '',
  city: '',
  job: '',
  cookAge: 0,
  favoriteCuisine: '',
  tastePreference: '',
  dietaryRestrictions: ''
})

const coverUrl = computed(() => {
  const bg = form.value.bgImageUrl
  return bg && bg.trim().length > 0
    ? bg
    : 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1800&q=80'
})

const avatarUrl = computed(() => {
  const a = form.value.avatarUrl
  return a && a.trim().length > 0 ? a : 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'
})

const load = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await UserAPI.getProfile()
    if (res.code !== 200 || !res.data) {
      errorMessage.value = res.message || '加载失败'
      return
    }
    form.value = {
      nickname: res.data.nickname ?? '',
      avatarUrl: res.data.avatarUrl ?? '',
      bgImageUrl: res.data.bgImageUrl ?? '',
      gender: res.data.gender ?? 0,
      signature: res.data.signature ?? '',
      city: res.data.city ?? '',
      job: res.data.job ?? '',
      cookAge: res.data.cookAge ?? 0,
      favoriteCuisine: res.data.favoriteCuisine ?? '',
      tastePreference: res.data.tastePreference ?? '',
      dietaryRestrictions: res.data.dietaryRestrictions ?? ''
    }
  } catch {
    errorMessage.value = '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const uploadAvatar = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  uploadingAvatar.value = true
  errorMessage.value = ''
  try {
    const res = await FileAPI.uploadImage(file)
    if (res.code !== 200 || !res.data?.url) {
      errorMessage.value = res.message || '头像上传失败'
      return
    }
    form.value.avatarUrl = res.data.url
  } catch {
    errorMessage.value = '头像上传失败，请稍后重试'
  } finally {
    uploadingAvatar.value = false
  }
}

const uploadBg = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  uploadingBg.value = true
  errorMessage.value = ''
  try {
    const res = await FileAPI.uploadImage(file)
    if (res.code !== 200 || !res.data?.url) {
      errorMessage.value = res.message || '背景上传失败'
      return
    }
    form.value.bgImageUrl = res.data.url
  } catch {
    errorMessage.value = '背景上传失败，请稍后重试'
  } finally {
    uploadingBg.value = false
  }
}

const save = async () => {
  if (loading.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await UserAPI.updateProfile({
      nickname: form.value.nickname,
      avatarUrl: form.value.avatarUrl,
      bgImageUrl: form.value.bgImageUrl,
      gender: form.value.gender,
      signature: form.value.signature,
      city: form.value.city,
      job: form.value.job,
      cookAge: form.value.cookAge,
      favoriteCuisine: form.value.favoriteCuisine,
      tastePreference: form.value.tastePreference,
      dietaryRestrictions: form.value.dietaryRestrictions
    })
    if (res.code !== 200) {
      errorMessage.value = res.message || '保存失败'
      return
    }
    await auth.loadProfile()
    router.replace({ name: 'user-profile' })
  } catch {
    errorMessage.value = '保存失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const skip = async () => {
  await auth.loadProfile()
  router.replace({ name: 'user-profile' })
}

onMounted(async () => {
  if (auth.token && !auth.profile) await auth.loadProfile()
  await load()
})
</script>

<template>
  <div class="min-h-screen bg-dark-bg text-white">
    <div class="relative h-[46vh] min-h-[360px] overflow-hidden">
      <img :src="coverUrl" class="absolute inset-0 w-full h-full object-cover" />
      <div class="absolute inset-0 bg-gradient-to-b from-black/70 via-black/20 to-dark-bg"></div>

      <div class="relative max-w-6xl mx-auto px-6 pt-10">
        <div class="flex items-start justify-between gap-4">
          <div>
            <div class="text-xs tracking-widest text-gray-400 mb-2">WELCOME</div>
            <h1 class="text-4xl md:text-5xl font-serif text-primary">完善个人资料</h1>
            <div class="text-sm text-gray-300 mt-3 tracking-wider">上传头像/背景，让你的主页更高级</div>
          </div>
          <div class="flex gap-2">
            <button
              @click="skip"
              class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-sm tracking-widest"
            >
              以后再说
            </button>
          </div>
        </div>

        <div class="mt-10 flex items-end justify-between">
          <div class="flex items-end gap-6">
            <div class="relative w-28 h-28 rounded-full overflow-hidden border-2 border-white/20 bg-black/30">
              <img :src="avatarUrl" class="w-full h-full object-cover" />
              <label
                class="absolute inset-0 flex items-center justify-center text-xs tracking-widest bg-black/40 opacity-0 hover:opacity-100 transition-opacity cursor-pointer"
              >
                {{ uploadingAvatar ? '上传中...' : '更换头像' }}
                <input type="file" accept="image/*" class="hidden" :disabled="uploadingAvatar" @change="uploadAvatar" />
              </label>
            </div>
            <div class="pb-2">
              <div class="text-xs tracking-widest text-gray-400 mb-2">背景图</div>
              <label class="inline-flex items-center gap-2 px-5 py-2.5 rounded-full border border-white/10 text-gray-200 hover:border-primary hover:text-primary transition-colors cursor-pointer">
                <span class="text-sm tracking-widest">{{ uploadingBg ? '上传中...' : '更换背景' }}</span>
                <input type="file" accept="image/*" class="hidden" :disabled="uploadingBg" @change="uploadBg" />
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="max-w-6xl mx-auto px-6 pb-16">
      <div v-if="errorMessage" class="mb-6 rounded-2xl border border-white/10 bg-black/20 px-6 py-4 flex items-center justify-between gap-4">
        <div class="text-gray-300 text-sm tracking-wider">{{ errorMessage }}</div>
        <button @click="load" class="px-5 py-2 rounded-full bg-primary text-black font-bold text-sm">重试</button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="rounded-2xl border border-white/10 bg-black/10 p-6">
          <div class="text-sm tracking-widest text-gray-500 mb-5">基础信息</div>
          <div class="space-y-4">
            <div>
              <div class="text-xs tracking-widest text-gray-500 mb-1">昵称</div>
              <input v-model="form.nickname" class="w-full px-4 py-3 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary" />
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <div class="text-xs tracking-widest text-gray-500 mb-1">性别</div>
                <select v-model.number="form.gender" class="w-full px-4 py-3 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary">
                  <option :value="0">未知</option>
                  <option :value="1">男</option>
                  <option :value="2">女</option>
                </select>
              </div>
              <div>
                <div class="text-xs tracking-widest text-gray-500 mb-1">城市</div>
                <input v-model="form.city" class="w-full px-4 py-3 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <div class="text-xs tracking-widest text-gray-500 mb-1">职业</div>
                <input v-model="form.job" class="w-full px-4 py-3 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary" />
              </div>
              <div>
                <div class="text-xs tracking-widest text-gray-500 mb-1">厨龄</div>
                <input v-model.number="form.cookAge" type="number" min="0" max="80" class="w-full px-4 py-3 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary" />
              </div>
            </div>
            <div>
              <div class="text-xs tracking-widest text-gray-500 mb-1">个性签名</div>
              <textarea v-model="form.signature" rows="3" class="w-full px-4 py-3 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary"></textarea>
            </div>
          </div>
        </div>

        <div class="rounded-2xl border border-white/10 bg-black/10 p-6">
          <div class="text-sm tracking-widest text-gray-500 mb-5">口味偏好</div>
          <div class="space-y-4">
            <div>
              <div class="text-xs tracking-widest text-gray-500 mb-1">喜欢菜系</div>
              <input v-model="form.favoriteCuisine" class="w-full px-4 py-3 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary" placeholder="如：川菜、粤菜、日料" />
            </div>
            <div>
              <div class="text-xs tracking-widest text-gray-500 mb-1">口味偏好</div>
              <input v-model="form.tastePreference" class="w-full px-4 py-3 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary" placeholder="如：辣、清淡、酸甜" />
            </div>
            <div>
              <div class="text-xs tracking-widest text-gray-500 mb-1">忌口/限制</div>
              <input v-model="form.dietaryRestrictions" class="w-full px-4 py-3 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary" placeholder="如：花生过敏" />
            </div>
          </div>
        </div>
      </div>

      <div class="flex justify-end gap-3 mt-8">
        <button @click="skip" class="px-6 py-3 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors tracking-widest">
          跳过
        </button>
        <button @click="save" :disabled="loading" class="px-6 py-3 rounded-full bg-primary text-black font-bold disabled:opacity-60 tracking-widest">
          {{ loading ? '保存中...' : '保存并进入主页' }}
        </button>
      </div>
    </div>
  </div>
</template>

