<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '@/api/http'

const router = useRouter()

const sending = ref(false)
const resultMessage = ref('')

const payloadObj = ref<any>({
  eventType: 'ui_error',
  level: 'ERROR',
  message: 'Demo log from DataLab',
  timestamp: Date.now(),
  traceId: 'demo-trace',
  tags: { page: 'data-lab' },
  context: { route: '/data-lab', action: 'send_demo_log' },
  stack: 'Error: demo\n    at DataLabPage.vue'
})

const payloadText = ref(JSON.stringify(payloadObj.value, null, 2))

const send = async () => {
  if (sending.value) return
  sending.value = true
  resultMessage.value = ''
  try {
    let parsed: any
    try {
      parsed = JSON.parse(payloadText.value)
    } catch {
      resultMessage.value = 'JSON 格式不合法'
      return
    }
    parsed.timestamp = Date.now()
    payloadObj.value = parsed
    payloadText.value = JSON.stringify(payloadObj.value, null, 2)

    const res = await http.post('/log/capture', payloadObj.value)
    if (res.data?.code === 200) {
      resultMessage.value = '已上报（可在后端 logs/json-capture.log 查看）'
    } else {
      resultMessage.value = res.data?.message || '上报失败'
    }
  } catch {
    resultMessage.value = '上报失败，请检查网络或稍后重试'
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-dark-bg text-white px-4 md:px-8 py-10">
    <div class="max-w-5xl mx-auto">
      <div class="flex items-start justify-between gap-4 mb-8">
        <div>
          <div class="text-xs tracking-widest uppercase text-gray-500 mb-2">Lab</div>
          <h1 class="text-4xl md:text-5xl font-serif text-primary">Data Lab</h1>
        </div>
        <button
          @click="router.push({ name: 'home' })"
          class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-sm tracking-widest uppercase"
        >
          Back Home
        </button>
      </div>

      <div class="rounded-2xl border border-white/10 bg-black/20 p-5 md:p-6">
        <div class="text-gray-300 text-sm tracking-wider mb-4">
          用于演示前端 JSON 日志上报接口：POST /api/log/capture
        </div>

        <textarea
          v-model="payloadText"
          class="w-full min-h-[260px] px-4 py-3 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary font-mono text-xs"
        />

        <div class="flex items-center justify-between gap-4 mt-4">
          <button
            @click="send"
            :disabled="sending"
            class="px-6 py-3 rounded-xl bg-primary text-black font-bold disabled:opacity-60"
          >
            {{ sending ? 'Sending...' : 'Send Demo Log' }}
          </button>
          <div class="text-sm text-gray-400">{{ resultMessage }}</div>
        </div>
      </div>
    </div>
  </div>
</template>
