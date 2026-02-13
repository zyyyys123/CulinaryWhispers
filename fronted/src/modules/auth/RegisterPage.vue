<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UserAPI } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('')
const nickname = ref('')
const password = ref('')
const loading = ref(false)
const errorText = ref('')

const submit = async () => {
  if (loading.value) return
  errorText.value = ''
  loading.value = true
  try {
    const res = await UserAPI.register({ username: username.value, password: password.value, nickname: nickname.value })
    if (res.code !== 200) {
      errorText.value = res.message || '注册失败'
      return
    }
    const loginRes = await UserAPI.login({ username: username.value, password: password.value })
    if (loginRes.code !== 200 || !loginRes.data) {
      router.replace({ name: 'login', query: route.query })
      return
    }
    auth.setToken(loginRes.data)
    await auth.loadProfile()
    router.replace({ name: 'user-profile-setup' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-dark-bg text-white flex items-center justify-center px-6">
    <div class="w-full max-w-md bg-black/20 border border-white/10 rounded-2xl p-8">
      <div class="text-center mb-8">
        <div class="text-sm tracking-[0.4em] uppercase text-gray-500 mb-2">Culinary Whispers</div>
        <h1 class="text-3xl font-serif text-primary">Create Account</h1>
      </div>

      <div v-if="errorText" class="mb-5 text-sm text-red-300 bg-red-500/10 border border-red-500/20 rounded-xl px-4 py-3">
        {{ errorText }}
      </div>

      <div class="space-y-4">
        <div>
          <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Username</div>
          <input v-model="username" autocomplete="username" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
        </div>
        <div>
          <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Nickname</div>
          <input v-model="nickname" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
        </div>
        <div>
          <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Password</div>
          <input v-model="password" type="password" autocomplete="new-password" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" @keyup.enter="submit" />
        </div>

        <button class="w-full mt-2 px-6 py-3 rounded-full bg-primary text-black font-bold disabled:opacity-60" :disabled="!username || !nickname || !password || loading" @click="submit">
          {{ loading ? 'Creating…' : 'Create' }}
        </button>

        <div class="text-center text-sm text-gray-400 mt-6">
          <span>Already have an account?</span>
          <button class="text-primary hover:underline ml-2" @click="router.replace({ name: 'login', query: route.query })">
            Sign in
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
