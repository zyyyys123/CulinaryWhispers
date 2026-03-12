<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CommerceAPI } from '@/api/commerce'
import type { ProductVO } from '@/types/commerce'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const product = ref<ProductVO | null>(null)
const quantity = ref(1)
const createdOrderId = ref('')

const productId = computed(() => String(route.params.id || '').trim())

const load = async () => {
  if (!productId.value) {
    errorMessage.value = '商品不存在'
    return
  }
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const res = await CommerceAPI.getProduct(productId.value)
    if (res.code !== 200) {
      errorMessage.value = res.message || '加载失败'
      return
    }
    product.value = res.data
    quantity.value = 1
  } catch {
    errorMessage.value = '加载失败，请检查网络或稍后重试'
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push({ name: 'market' })
}

const createOrder = async () => {
  if (!product.value) return
  if (!auth.token) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (submitting.value) return
  if (quantity.value < 1) quantity.value = 1
  if (quantity.value > product.value.stock) quantity.value = product.value.stock
  submitting.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const res = await CommerceAPI.createOrder({ [product.value.id]: quantity.value })
    if (res.code !== 200 || !res.data) {
      errorMessage.value = res.message || '下单失败'
      return
    }
    createdOrderId.value = res.data
    successMessage.value = '下单成功'
  } catch (e: any) {
    const message = e?.response?.data?.message
    errorMessage.value = message || '下单失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

const openOrders = () => {
  router.push({ name: 'market', query: { openOrders: '1', orderId: createdOrderId.value || undefined } })
}

onMounted(() => {
  load()
})
</script>

<template>
  <div class="min-h-screen bg-dark-bg text-white px-4 md:px-8 py-10">
    <div class="max-w-4xl mx-auto">
      <div class="flex items-center justify-between gap-4 mb-8">
        <div>
          <div class="text-xs tracking-widest text-gray-500 mb-2">市集</div>
          <h1 class="text-4xl md:text-5xl font-serif text-primary">商品详情</h1>
        </div>
        <div class="flex gap-2">
          <button
            @click="goBack"
            class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-sm tracking-widest"
          >
            返回市集
          </button>
        </div>
      </div>

      <div v-if="errorMessage" class="mb-6 rounded-2xl border border-white/10 bg-black/20 px-6 py-4 flex items-center justify-between gap-4">
        <div class="text-gray-300 text-sm tracking-wider">{{ errorMessage }}</div>
        <button @click="load" class="px-5 py-2 rounded-full bg-primary text-black font-bold text-sm">重试</button>
      </div>
      <div v-if="successMessage" class="mb-6 rounded-2xl border border-emerald-500/20 bg-emerald-500/10 px-6 py-4 flex items-center justify-between gap-4">
        <div class="text-emerald-200 text-sm tracking-wider">{{ successMessage }}<span v-if="createdOrderId">（订单号 {{ createdOrderId }}）</span></div>
        <button
          v-if="createdOrderId"
          @click="openOrders"
          class="px-5 py-2 rounded-full border border-emerald-500/30 text-emerald-100 hover:border-emerald-500/60 transition-colors text-sm"
        >
          查看订单
        </button>
        <button
          v-else
          @click="successMessage = ''"
          class="px-5 py-2 rounded-full border border-emerald-500/30 text-emerald-100 hover:border-emerald-500/60 transition-colors text-sm"
        >
          知道了
        </button>
      </div>

      <div v-if="loading" class="rounded-2xl border border-white/10 bg-black/10 p-10 text-gray-400 tracking-widest">
        加载中…
      </div>

      <div v-else-if="product" class="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div class="rounded-2xl border border-white/10 bg-black/10 overflow-hidden">
          <div class="aspect-square bg-white">
            <img :src="product.coverUrl" class="w-full h-full object-contain p-6" />
          </div>
        </div>

        <div class="rounded-2xl border border-white/10 bg-black/10 p-6">
          <div class="text-2xl font-serif text-white mb-2">{{ product.name }}</div>
          <div class="text-sm text-gray-400 leading-relaxed mb-6">{{ product.description || '—' }}</div>

          <div class="flex items-center justify-between mb-6">
            <div class="text-primary font-bold text-2xl">¥{{ product.price.toFixed(2) }}</div>
            <div class="text-xs text-gray-500 tracking-widest">库存 {{ product.stock }}</div>
          </div>

          <div class="flex items-center gap-3 mb-6">
            <div class="text-xs tracking-widest text-gray-500">数量</div>
            <button
              class="w-9 h-9 rounded-lg border border-white/10 text-gray-200 hover:border-white/30 transition-colors"
              :disabled="quantity <= 1 || submitting"
              @click="quantity = Math.max(1, quantity - 1)"
            >
              -
            </button>
            <input
              v-model.number="quantity"
              type="number"
              min="1"
              :max="product.stock"
              class="w-20 px-3 py-2 rounded-lg bg-black/20 border border-white/10 text-white text-sm focus:outline-none focus:border-primary text-center"
            />
            <button
              class="w-9 h-9 rounded-lg border border-white/10 text-gray-200 hover:border-white/30 transition-colors"
              :disabled="quantity >= product.stock || submitting"
              @click="quantity = Math.min(product.stock, quantity + 1)"
            >
              +
            </button>
          </div>

          <button
            @click="createOrder"
            :disabled="submitting || product.stock <= 0"
            class="w-full px-6 py-3 rounded-xl bg-primary text-black font-bold disabled:opacity-60"
          >
            {{ submitting ? '下单中…' : product.stock <= 0 ? '已售罄' : '立即下单' }}
          </button>

          <div class="mt-4 text-xs text-gray-500 leading-relaxed">
            提示：本项目为“模拟市集/模拟支付”，下单后可在市集页打开订单面板查看状态。
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
