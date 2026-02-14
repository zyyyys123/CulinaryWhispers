import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { UserAPI } from '@/api/user'
import type { UserProfileVO } from '@/types/user'

const TOKEN_KEY = 'cw_token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const profile = ref<UserProfileVO | null>(null)
  const loadingProfile = ref(false)

  const isAuthed = computed(() => Boolean(token.value))

  const setToken = (t: string) => {
    token.value = t
    localStorage.setItem(TOKEN_KEY, t)
  }

  const clear = () => {
    token.value = null
    profile.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  const loadProfile = async () => {
    if (!token.value || loadingProfile.value) return false
    loadingProfile.value = true
    try {
      const res = await UserAPI.getProfile()
      if (res.code === 200) {
        profile.value = res.data
        return true
      }
      return false
    } catch {
      return false
    } finally {
      loadingProfile.value = false
    }
  }

  return { token, profile, loadingProfile, isAuthed, setToken, clear, loadProfile }
})
