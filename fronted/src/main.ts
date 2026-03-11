import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { VueQueryPlugin } from '@tanstack/vue-query'
import App from './App.vue'
import router from './router'
import './styles/main.scss'
import { useAuthStore } from '@/stores/auth'

const app = createApp(App)

const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(VueQueryPlugin)

window.addEventListener('cw:auth:clear', () => {
  const auth = useAuthStore(pinia)
  auth.clear()
})

app.mount('#app')
