<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { gsap } from 'gsap'
import { CommerceAPI } from '@/api/commerce'
import type { CartItem, ProductVO } from '@/types/commerce'
import type { OrderVO } from '@/types/commerce'
import { useAuthStore } from '@/stores/auth'
import CwErrorState from '@/components/feedback/CwErrorState.vue'
import CwEmptyState from '@/components/feedback/CwEmptyState.vue'
import CwListFooter from '@/components/feedback/CwListFooter.vue'

const router = useRouter()
const auth = useAuthStore()

const products = ref<ProductVO[]>([])
const listLoading = ref(false)
const checkoutLoading = ref(false)
const listErrorMessage = ref('')
const checkoutErrorMessage = ref('')
const keyword = ref('')
const selectedCategoryId = ref<number | null>(null)
const page = ref(1)
const size = ref(12)
const total = ref(0)

// Cart Animation State
const isCartOpen = ref(false)
const cartBtnRef = ref<HTMLElement | null>(null)
const cartItems = ref<CartItem[]>([])
const cartCount = computed(() => cartItems.value.reduce((acc, it) => acc + it.quantity, 0))
const cartTotal = computed(() => cartItems.value.reduce((acc, it) => acc + it.product.price * it.quantity, 0))

const isOrdersOpen = ref(false)
const ordersLoading = ref(false)
const ordersErrorMessage = ref('')
const oPage = ref(1)
const oSize = ref(10)
const oTotal = ref(0)
const orders = ref<OrderVO[]>([])
const lastOrdersFetchCount = ref(0)
const selectedOrder = ref<OrderVO | null>(null)

const categories: Array<{ id: number | null; name: string }> = [
  { id: null, name: '全部' },
  { id: 1, name: '厨具' },
  { id: 2, name: '食材' },
  { id: 3, name: '调味' },
  { id: 4, name: '课程' },
  { id: 5, name: '周边' }
]

const categoryName = (id: string) => {
  const n = Number(id)
  const hit = categories.find(c => c.id === n)
  return hit?.name ?? `分类 ${id}`
}

const hasMore = computed(() => (total.value > 0 ? products.value.length < total.value : products.value.length > 0))
const hasMoreOrders = computed(() => (oTotal.value > 0 ? orders.value.length < oTotal.value : lastOrdersFetchCount.value === oSize.value))

const orderStatusLabel = (s: number) => {
  if (s === 0) return '待支付'
  if (s === 1) return '已支付'
  if (s === 2) return '已发货'
  if (s === 3) return '已完成'
  if (s === 4) return '已取消'
  return `状态 ${s}`
}

// Data Fetching
const fetchProducts = async (reset = true) => {
  if (listLoading.value) return
  listLoading.value = true
  listErrorMessage.value = ''
  try {
    if (reset) {
      page.value = 1
      total.value = 0
      products.value = []
    }
    const res = await CommerceAPI.getList({
      page: page.value,
      size: size.value,
      keyword: keyword.value.trim() || undefined,
      categoryId: selectedCategoryId.value ?? undefined
    })
    if (res.code !== 200) {
      listErrorMessage.value = res.message || '加载失败'
      return
    }
    total.value = Number(res.data.total ?? 0)
    const batch = res.data.records ?? []
    products.value.push(...batch)
    page.value++

    if (reset) {
      setTimeout(() => {
        gsap.from('.product-card', {
          y: 40,
          opacity: 0,
          duration: 0.6,
          stagger: 0.06,
          ease: 'power2.out'
        })
      }, 80)
    }
  } catch {
    listErrorMessage.value = '加载失败，请检查网络或稍后重试'
  } finally {
    listLoading.value = false
  }
}

const fetchOrders = async (reset = true) => {
  if (ordersLoading.value) return
  if (!auth.token) {
    ordersErrorMessage.value = '请先登录后查看订单'
    return
  }
  ordersLoading.value = true
  ordersErrorMessage.value = ''
  try {
    if (reset) {
      oPage.value = 1
      oTotal.value = 0
      orders.value = []
      selectedOrder.value = null
    }
    const res = await CommerceAPI.listMyOrders({ page: oPage.value, size: oSize.value })
    if (res.code !== 200) {
      ordersErrorMessage.value = res.message || '加载失败'
      return
    }
    oTotal.value = Number(res.data.total ?? 0)
    const batch = res.data.records ?? []
    orders.value.push(...batch)
    lastOrdersFetchCount.value = batch.length
    oPage.value++
  } catch {
    ordersErrorMessage.value = '加载失败，请检查网络或稍后重试'
  } finally {
    ordersLoading.value = false
  }
}

const openOrders = async () => {
  isOrdersOpen.value = true
  await fetchOrders(true)
}

const viewOrder = async (id: string) => {
  if (!auth.token) return
  ordersLoading.value = true
  ordersErrorMessage.value = ''
  try {
    const res = await CommerceAPI.getMyOrder(id)
    if (res.code !== 200) {
      ordersErrorMessage.value = res.message || '加载失败'
      return
    }
    selectedOrder.value = res.data
  } catch {
    ordersErrorMessage.value = '加载失败，请稍后重试'
  } finally {
    ordersLoading.value = false
  }
}

const cancelOrder = async (id: string) => {
  ordersLoading.value = true
  ordersErrorMessage.value = ''
  try {
    const res = await CommerceAPI.cancelOrder(id)
    if (res.code !== 200) {
      ordersErrorMessage.value = res.message || '取消失败'
      return
    }
    await fetchOrders(true)
  } catch {
    ordersErrorMessage.value = '取消失败，请稍后重试'
  } finally {
    ordersLoading.value = false
  }
}

const deliverOrder = async (id: string) => {
  ordersLoading.value = true
  ordersErrorMessage.value = ''
  try {
    const res = await CommerceAPI.deliverOrder(id)
    if (res.code !== 200) {
      ordersErrorMessage.value = res.message || '发货失败'
      return
    }
    await viewOrder(id)
    await fetchOrders(true)
  } catch {
    ordersErrorMessage.value = '发货失败，请稍后重试'
  } finally {
    ordersLoading.value = false
  }
}

const finishOrder = async (id: string) => {
  ordersLoading.value = true
  ordersErrorMessage.value = ''
  try {
    const res = await CommerceAPI.finishOrder(id)
    if (res.code !== 200) {
      ordersErrorMessage.value = res.message || '完成失败'
      return
    }
    await viewOrder(id)
    await fetchOrders(true)
  } catch {
    ordersErrorMessage.value = '完成失败，请稍后重试'
  } finally {
    ordersLoading.value = false
  }
}

// Add to Cart Animation
const addToCart = (e: MouseEvent, product: ProductVO) => {
  if ((product.stock ?? 0) <= 0) return
  const existed = cartItems.value.find(it => it.productId === product.id)
  if (existed) {
    if (existed.quantity >= product.stock) return
    existed.quantity++
  } else {
    cartItems.value.push({ productId: product.id, product, quantity: 1 })
  }
  
  // 1. Create a flying clone of the product image
  const card = (e.target as HTMLElement).closest('.product-card')
  const img = card?.querySelector('img')
  
  if (img && cartBtnRef.value) {
    const clone = img.cloneNode() as HTMLImageElement
    const rect = img.getBoundingClientRect()
    const targetRect = cartBtnRef.value.getBoundingClientRect()
    
    // Initial styles
    clone.style.position = 'fixed'
    clone.style.left = `${rect.left}px`
    clone.style.top = `${rect.top}px`
    clone.style.width = `${rect.width}px`
    clone.style.height = `${rect.height}px`
    clone.style.borderRadius = '12px'
    clone.style.zIndex = '9999'
    clone.style.pointerEvents = 'none'
    document.body.appendChild(clone)
    
    // Parabolic Flight Animation
    const tl = gsap.timeline({
      onComplete: () => {
        clone.remove()
        // Shake the cart icon
        gsap.to(cartBtnRef.value, {
          scale: 1.2,
          duration: 0.1,
          yoyo: true,
          repeat: 1
        })
      }
    })
    
    // X axis (Linear)
    tl.to(clone, {
      x: targetRect.left - rect.left + 10,
      duration: 0.8,
      ease: 'power1.in'
    })
    
    // Y axis (Bezier / Ease Out then In) - Simulating gravity
    // Using simple ease for now, but keyframes would be better for parabola
    tl.to(clone, {
      y: targetRect.top - rect.top + 10,
      duration: 0.8,
      ease: 'circ.in'
    }, '<')
    
    // Shrink
    tl.to(clone, {
      width: 20,
      height: 20,
      opacity: 0.5,
      duration: 0.8
    }, '<')
  }
}

const inc = (idx: number) => {
  const it = cartItems.value[idx]
  if (!it) return
  const max = it.product.stock ?? 0
  if (max > 0 && it.quantity >= max) return
  it.quantity++
}

const dec = (idx: number) => {
  const it = cartItems.value[idx]
  if (!it) return
  if (it.quantity <= 1) {
    cartItems.value.splice(idx, 1)
    return
  }
  it.quantity--
}

const checkout = async () => {
  if (cartItems.value.length === 0) return
  if (!auth.token) {
    await router.push({ name: 'login', query: { redirect: '/market' } })
    return
  }
  checkoutLoading.value = true
  checkoutErrorMessage.value = ''
  try {
    const productCounts: Record<string, number> = {}
    cartItems.value.forEach(it => {
      productCounts[it.productId] = (productCounts[it.productId] ?? 0) + it.quantity
    })
    const res = await CommerceAPI.createOrder(productCounts)
    if (res.code !== 200) {
      checkoutErrorMessage.value = res.message || '下单失败'
      return
    }
    await CommerceAPI.paymentNotify(res.data)
    isCartOpen.value = false
    cartItems.value = []
    await openOrders()
    await viewOrder(res.data)
    await fetchProducts(true)
  } catch {
    checkoutErrorMessage.value = '下单失败，请稍后重试'
  } finally {
    checkoutLoading.value = false
  }
}

onMounted(() => {
  fetchProducts(true)
})
</script>

<template>
  <div class="commerce-page min-h-screen bg-dark-bg text-white pb-20">
    
    <!-- Header -->
    <header class="sticky top-0 z-40 bg-dark-bg/80 backdrop-blur border-b border-gray-800">
      <div class="max-w-7xl mx-auto px-6 py-4 flex justify-between items-center">
        <div class="flex items-center gap-4">
          <h1 class="text-2xl font-serif text-primary">厨友市集</h1>
          <div class="hidden md:flex items-center gap-2 text-xs tracking-widest text-gray-500">
            甄选厨具 · 食材 · 调味 · 课程 · 周边（订单支付为模拟流程）
          </div>
        </div>
        
        <div class="flex items-center gap-6">
          <button @click="router.push({ name: 'home' })" class="text-xs font-bold tracking-widest text-gray-400 hover:text-white transition-colors">
            返回首页
          </button>

          <button
            v-if="auth.token"
            @click="openOrders"
            class="text-xs font-bold tracking-widest text-gray-400 hover:text-white transition-colors"
          >
            我的订单
          </button>
          
          <!-- Cart Icon -->
          <div ref="cartBtnRef" @click="isCartOpen = !isCartOpen" class="relative cursor-pointer group">
            <svg class="w-8 h-8 text-white group-hover:text-primary transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"></path></svg>
            <span v-if="cartCount > 0" class="absolute -top-2 -right-2 w-5 h-5 bg-primary text-black text-xs font-bold flex items-center justify-center rounded-full">
              {{ cartCount }}
            </span>
          </div>
        </div>
      </div>
    </header>

    <div
      class="fixed inset-y-0 right-0 w-[92vw] max-w-3xl bg-dark-surface shadow-2xl transform transition-transform duration-300 z-50 border-l border-gray-800 p-6"
      :class="isOrdersOpen ? 'translate-x-0' : 'translate-x-full'"
    >
      <div class="flex justify-between items-center mb-8">
        <h2 class="text-xl font-serif text-primary">我的订单</h2>
        <button @click="isOrdersOpen = false" class="text-gray-500 hover:text-white">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
        </button>
      </div>

      <CwErrorState
        v-if="ordersErrorMessage"
        class="mb-4"
        :message="ordersErrorMessage"
        action-label="重试"
        @action="fetchOrders(true)"
      />

      <div v-if="selectedOrder" class="rounded-2xl border border-white/10 bg-black/10 p-5 mb-6">
        <div class="flex items-center justify-between gap-4">
          <div class="min-w-0">
            <div class="text-xs text-gray-500 tracking-widest uppercase">Order {{ selectedOrder.id }}</div>
            <div class="text-sm text-gray-300 mt-1">{{ selectedOrder.createTime }}</div>
          </div>
          <div class="text-right">
            <div class="text-primary font-bold">¥{{ selectedOrder.totalAmount.toFixed(2) }}</div>
            <div class="text-xs text-gray-400 mt-1">{{ orderStatusLabel(selectedOrder.status) }}</div>
          </div>
        </div>
        <div class="mt-4 space-y-2">
          <div v-for="it in selectedOrder.items" :key="it.productId" class="flex items-center justify-between text-sm">
            <div class="text-gray-200 truncate">{{ it.productTitle }}</div>
            <div class="text-gray-400 shrink-0">x{{ it.count }}</div>
          </div>
        </div>
        <div class="flex gap-2 mt-5 justify-end">
          <button
            v-if="selectedOrder.status === 0"
            @click="cancelOrder(selectedOrder.id)"
            class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-xs tracking-widest uppercase"
            :disabled="ordersLoading"
          >
            取消订单
          </button>
          <button
            v-if="selectedOrder.status === 1"
            @click="deliverOrder(selectedOrder.id)"
            class="px-5 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-xs tracking-widest uppercase"
            :disabled="ordersLoading"
          >
            模拟发货
          </button>
          <button
            v-if="selectedOrder.status === 1 || selectedOrder.status === 2"
            @click="finishOrder(selectedOrder.id)"
            class="px-5 py-2 rounded-full bg-primary text-black font-bold text-xs tracking-widest uppercase disabled:opacity-60"
            :disabled="ordersLoading"
          >
            确认收货
          </button>
        </div>
      </div>

      <div class="space-y-3">
        <CwEmptyState
          v-if="!ordersLoading && !ordersErrorMessage && orders.length === 0"
          title="暂无订单"
          description="下单后会在这里展示订单记录。"
          action-label="刷新"
          @action="fetchOrders(true)"
        />

        <div
          v-for="o in orders"
          :key="o.id"
          class="rounded-2xl border border-white/10 bg-black/10 p-5 cursor-pointer hover:border-primary/40 transition-colors"
          @click="viewOrder(o.id)"
        >
          <div class="flex items-center justify-between gap-4">
            <div class="min-w-0">
              <div class="font-bold truncate">订单 {{ o.id }}</div>
              <div class="text-xs text-gray-500 mt-1">{{ o.createTime }}</div>
            </div>
            <div class="text-right shrink-0">
              <div class="text-primary font-bold">¥{{ o.totalAmount.toFixed(2) }}</div>
              <div class="text-xs text-gray-400 mt-1">{{ orderStatusLabel(o.status) }}</div>
            </div>
          </div>
        </div>

        <CwListFooter
          v-if="!ordersErrorMessage && orders.length > 0"
          :loading="ordersLoading"
          :hasMore="hasMoreOrders"
          load-more-label="加载更多"
          loading-label="加载中…"
          end-label="已到底"
          @loadMore="fetchOrders(false)"
        />
      </div>
    </div>

    <!-- Cart Sidebar -->
    <div 
      class="fixed inset-y-0 right-0 w-80 bg-dark-surface shadow-2xl transform transition-transform duration-300 z-50 border-l border-gray-800 p-6"
      :class="isCartOpen ? 'translate-x-0' : 'translate-x-full'"
    >
      <div class="flex justify-between items-center mb-8">
        <h2 class="text-xl font-serif text-primary">购物车</h2>
        <button @click="isCartOpen = false" class="text-gray-500 hover:text-white">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
        </button>
      </div>

      <div v-if="cartItems.length === 0" class="text-center text-gray-500 mt-20">
        <p>购物车还是空的。</p>
        <p class="text-sm mt-2">从精选商品开始挑选吧！</p>
      </div>

      <div v-else class="space-y-4 h-[calc(100vh-200px)] overflow-y-auto">
        <div v-for="(item, idx) in cartItems" :key="item.productId" class="flex gap-4 items-center bg-dark-bg p-3 rounded-lg">
          <img :src="item.product.coverUrl" class="w-12 h-12 object-cover rounded" />
          <div class="flex-1 min-w-0">
            <h4 class="text-sm font-bold text-white truncate">{{ item.product.name }}</h4>
            <p class="text-xs text-primary">¥{{ item.product.price.toFixed(2) }}</p>
            <div class="mt-2 flex items-center gap-2">
              <button @click="dec(idx)" class="w-7 h-7 rounded-lg border border-white/10 text-gray-300 hover:border-primary hover:text-primary transition-colors">-</button>
              <div class="text-xs text-gray-300 w-8 text-center">{{ item.quantity }}</div>
              <button @click="inc(idx)" class="w-7 h-7 rounded-lg border border-white/10 text-gray-300 hover:border-primary hover:text-primary transition-colors">+</button>
            </div>
          </div>
          <button class="text-gray-500 hover:text-red-500" @click="cartItems.splice(idx, 1)">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
          </button>
        </div>
      </div>

      <div v-if="cartItems.length > 0" class="absolute bottom-6 left-6 right-6">
        <CwErrorState
          v-if="checkoutErrorMessage"
          class="mb-4"
          :message="checkoutErrorMessage"
          action-label="重试"
          @action="checkout"
        />
        <div class="flex justify-between mb-4 text-white font-bold">
          <span>合计</span>
          <span>¥{{ cartTotal.toFixed(2) }}</span>
        </div>
        <button @click="checkout" :disabled="checkoutLoading" class="w-full py-3 bg-primary text-black font-bold rounded-lg hover:bg-white transition-colors disabled:opacity-60">
          去结算（模拟支付）
        </button>
      </div>
    </div>

    <!-- Product Grid -->
    <main class="max-w-7xl mx-auto px-6 py-12">
      <div class="rounded-2xl border border-white/10 bg-black/20 p-5 md:p-6 mb-10">
        <div class="flex flex-col md:flex-row gap-4 md:items-center">
          <input
            v-model="keyword"
            placeholder="搜索商品（关键词）"
            class="flex-1 w-full px-4 py-3 rounded-xl bg-black/30 border border-white/10 text-white focus:outline-none focus:border-primary"
            @keyup.enter="fetchProducts(true)"
          />
          <button
            @click="fetchProducts(true)"
            :disabled="listLoading"
            class="px-6 py-3 rounded-xl bg-primary text-black font-bold disabled:opacity-60"
          >
            搜索
          </button>
        </div>
        <div class="flex flex-wrap gap-2 mt-4">
          <button
            v-for="c in categories"
            :key="String(c.id)"
            @click="selectedCategoryId = c.id; fetchProducts(true)"
            class="px-4 py-2 rounded-full text-xs tracking-widest uppercase border transition-colors"
            :class="selectedCategoryId === c.id ? 'border-primary text-primary' : 'border-white/10 text-gray-300 hover:border-white/30'"
          >
            {{ c.name }}
          </button>
        </div>
        <CwErrorState v-if="listErrorMessage" class="mt-4" :message="listErrorMessage" action-label="重试" @action="fetchProducts(true)" />
      </div>

      <div v-if="listLoading && products.length === 0" class="grid grid-cols-1 md:grid-cols-4 gap-8">
        <div v-for="i in 8" :key="i" class="animate-pulse bg-gray-800 h-80 rounded-xl"></div>
      </div>

      <CwEmptyState
        v-else-if="!listLoading && !listErrorMessage && products.length === 0"
        title="暂无商品"
        description="试试换个关键词或切换分类。"
        action-label="清空筛选"
        @action="keyword = ''; selectedCategoryId = null; fetchProducts(true)"
      />

      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
        <div 
          v-for="product in products" 
          :key="product.id"
          class="product-card group bg-dark-surface rounded-xl overflow-hidden border border-gray-800 hover:border-primary transition-colors duration-300"
        >
          <!-- Image -->
          <div class="relative aspect-square overflow-hidden bg-white">
            <img :src="product.coverUrl" class="w-full h-full object-contain p-4 group-hover:scale-110 transition-transform duration-500" />
            <div v-if="product.stock <= 0" class="absolute inset-0 bg-black/60 flex items-center justify-center">
              <div class="px-4 py-2 rounded-full border border-white/20 text-white text-xs tracking-widest">售罄</div>
            </div>
            
            <!-- Quick Add Button (Visible on Hover) -->
            <button 
              @click="(e) => addToCart(e, product)"
              :disabled="product.stock <= 0"
              class="absolute bottom-4 right-4 w-10 h-10 bg-black text-white rounded-full flex items-center justify-center opacity-0 translate-y-4 group-hover:opacity-100 group-hover:translate-y-0 transition-all duration-300 hover:bg-primary hover:text-black shadow-lg disabled:opacity-40 disabled:hover:bg-black disabled:hover:text-white"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path></svg>
            </button>
          </div>
          
          <!-- Info -->
          <div class="p-4">
            <h3 class="font-serif text-lg text-white mb-1 truncate">{{ product.name }}</h3>
            <div class="flex items-center justify-between text-xs text-gray-500 mb-2">
              <div class="tracking-widest">{{ categoryName(product.categoryId) }}</div>
              <div class="tracking-widest">库存 {{ product.stock }}</div>
            </div>
            <div class="text-xs text-gray-400 leading-relaxed mb-3 min-h-[32px]">
              {{ product.description || '—' }}
            </div>
            <div class="flex justify-between items-center">
              <div class="flex flex-col">
                <span class="text-primary font-bold">¥{{ product.price.toFixed(2) }}</span>
                <span v-if="product.originalPrice" class="text-xs text-gray-500 line-through">¥{{ product.originalPrice.toFixed(2) }}</span>
              </div>
              <div class="flex items-center gap-1 text-xs text-gray-400">
                <svg class="w-3 h-3 text-yellow-500 fill-current" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>
                {{ product.rating }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <CwListFooter
        v-if="!listErrorMessage && products.length > 0"
        :loading="listLoading"
        :hasMore="hasMore"
        load-more-label="加载更多"
        loading-label="加载中…"
        end-label="已到底"
        @loadMore="fetchProducts(false)"
      />
    </main>
  </div>
</template>
