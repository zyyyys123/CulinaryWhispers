<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { VipAPI, type VipPlanVO } from '@/api/vip'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const plans = ref<VipPlanVO[]>([])
const vipLevel = ref<number>(0)
const vipExpireTime = ref<string>('')

const vipText = computed(() => {
  if (!vipLevel.value) return '非会员'
  const t = vipLevel.value === 1 ? 'VIP 青铜' : vipLevel.value === 2 ? 'VIP 白银' : 'VIP 黄金'
  return vipExpireTime.value ? `${t}（有效期至 ${vipExpireTime.value}）` : t
})

const load = async () => {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const res = await VipAPI.plans()
    if (res.code !== 200) {
      errorMessage.value = res.message || '加载失败'
      return
    }
    plans.value = res.data ?? []
    if (auth.token) {
      const me = await VipAPI.me()
      if (me.code === 200 && me.data) {
        vipLevel.value = Number(me.data.vipLevel ?? 0)
        vipExpireTime.value = me.data.vipExpireTime ?? ''
      }
    }
  } catch {
    errorMessage.value = '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const exchange = async (level: number) => {
  if (!auth.token) {
    await router.push({ name: 'login', query: { redirect: '/vip' } })
    return
  }
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const res = await VipAPI.exchange(level)
    if (res.code !== 200) {
      errorMessage.value = res.message || '兑换失败'
      return
    }
    const me = await VipAPI.me()
    if (me.code === 200 && me.data) {
      vipLevel.value = Number(me.data.vipLevel ?? 0)
      vipExpireTime.value = me.data.vipExpireTime ?? ''
    }
    await auth.loadProfile()
    successMessage.value = '兑换成功'
  } catch (e: any) {
    const message = e?.response?.data?.message
    errorMessage.value = message || '兑换失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
})
</script>

<template>
  <div class="min-h-screen bg-dark-bg text-white px-4 md:px-8 py-10">
    <div class="max-w-5xl mx-auto">
      <div class="flex items-start justify-between gap-4 mb-8">
        <div>
          <div class="text-xs tracking-widest text-gray-500 mb-2">VIP</div>
          <h1 class="text-4xl md:text-5xl font-serif text-primary">会员中心</h1>
          <div class="text-sm text-gray-300 mt-3 tracking-wider">当前状态：{{ vipText }}</div>
        </div>
        <div class="flex gap-2">
          <button
            @click="router.push({ name: 'points' })"
            class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-sm tracking-widest"
          >
            去积分中心
          </button>
          <button
            @click="router.push({ name: 'home' })"
            class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-sm tracking-widest"
          >
            返回首页
          </button>
        </div>
      </div>

      <div v-if="errorMessage" class="mb-6 rounded-2xl border border-white/10 bg-black/20 px-6 py-4 flex items-center justify-between gap-4">
        <div class="text-gray-300 text-sm tracking-wider">{{ errorMessage }}</div>
        <button @click="load" class="px-5 py-2 rounded-full bg-primary text-black font-bold text-sm">重试</button>
      </div>
      <div v-if="successMessage" class="mb-6 rounded-2xl border border-emerald-500/20 bg-emerald-500/10 px-6 py-4 flex items-center justify-between gap-4">
        <div class="text-emerald-200 text-sm tracking-wider">{{ successMessage }}</div>
        <button @click="successMessage = ''" class="px-5 py-2 rounded-full border border-emerald-500/30 text-emerald-100 hover:border-emerald-500/60 transition-colors text-sm">
          知道了
        </button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div v-for="p in plans" :key="p.level" class="rounded-2xl border border-white/10 bg-black/15 p-6">
          <div class="text-xs tracking-widest text-gray-500 mb-2">VIP {{ p.level }}</div>
          <div class="text-2xl font-serif text-white mb-3">{{ p.name }}</div>
          <div class="text-sm text-gray-300 mb-4">积分：<span class="text-primary font-bold">{{ p.costPoints }}</span> · 时长：{{ p.durationDays }} 天</div>
          <ul class="space-y-2 text-sm text-gray-300">
            <li v-for="b in p.benefits" :key="b" class="flex items-start gap-2">
              <span class="text-primary mt-0.5">•</span>
              <span>{{ b }}</span>
            </li>
          </ul>
          <button
            @click="exchange(p.level)"
            :disabled="loading"
            class="mt-6 w-full px-5 py-2.5 rounded-xl bg-primary text-black font-bold disabled:opacity-60"
          >
            兑换
          </button>
        </div>
      </div>

      <div class="mt-10 rounded-2xl border border-white/10 bg-black/10 p-6">
        <div class="text-sm tracking-widest text-gray-500 mb-3">说明</div>
        <div class="text-sm text-gray-300 leading-relaxed">
          VIP 采用“积分兑换限时会员”的方式：到期自动失效；同等级可续期；不支持用低等级覆盖高等级。部分权益为已上线能力，部分为后续规划。
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
          <div class="rounded-2xl border border-white/10 bg-black/20 p-5">
            <div class="text-xs tracking-widest text-gray-500 mb-3">已上线</div>
            <ul class="space-y-2 text-sm text-gray-300">
              <li class="flex items-start gap-2"><span class="text-primary mt-0.5">•</span><span>会员中心与兑换规则（含到期时间）</span></li>
              <li class="flex items-start gap-2"><span class="text-primary mt-0.5">•</span><span>个人主页 VIP 标识/勋章墙扩展入口</span></li>
              <li class="flex items-start gap-2"><span class="text-primary mt-0.5">•</span><span>社交页通知中心：点赞/评论/收藏聚合展示</span></li>
            </ul>
          </div>
          <div class="rounded-2xl border border-white/10 bg-black/20 p-5">
            <div class="text-xs tracking-widest text-gray-500 mb-3">规划中</div>
            <ul class="space-y-2 text-sm text-gray-300">
              <li class="flex items-start gap-2"><span class="text-primary mt-0.5">•</span><span>推荐权重与个性化推荐强化（按会员等级加成）</span></li>
              <li class="flex items-start gap-2"><span class="text-primary mt-0.5">•</span><span>AI 助手高级能力/额度（按会员等级解锁）</span></li>
              <li class="flex items-start gap-2"><span class="text-primary mt-0.5">•</span><span>市集折扣/专属商品/活动优先参与</span></li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
