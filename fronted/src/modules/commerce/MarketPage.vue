<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { gsap } from 'gsap'
import { CommerceAPI } from '@/api/commerce'
import type { ProductVO } from '@/types/commerce'

const products = ref<ProductVO[]>([])
const loading = ref(true)

// Cart Animation State
const cartCount = ref(0)
const isCartOpen = ref(false)
const cartBtnRef = ref<HTMLElement | null>(null)
const cartItems = ref<ProductVO[]>([])

// Data Fetching
const fetchProducts = async () => {
  const res = await CommerceAPI.getList({ page: 1, size: 8 })
  if (res.code === 200) {
    products.value = res.data.records
    loading.value = false
    // Stagger entrance
    setTimeout(() => {
      gsap.from('.product-card', {
        y: 50,
        opacity: 0,
        duration: 0.8,
        stagger: 0.1,
        ease: 'power2.out'
      })
    }, 100)
  }
}

// Add to Cart Animation
const addToCart = (e: MouseEvent, product: ProductVO) => {
  cartCount.value++
  cartItems.value.push(product)
  
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

const checkout = async () => {
  if (cartItems.value.length === 0) return
  
  // Mock Payment Process
  const orderId = `ord_${Date.now()}`
  
  // 1. Close Cart
  isCartOpen.value = false
  
  // 2. Show Success Message (Mock)
  // In real app, redirect to payment gateway or show payment modal
  // Here we just simulate a successful transaction
  
  // Reset Cart
  cartItems.value = []
  cartCount.value = 0
  
  alert(`Order ${orderId} placed successfully! Thank you for your purchase.`)
}

onMounted(() => {
  fetchProducts()
})
</script>

<template>
  <div class="commerce-page min-h-screen bg-dark-bg text-white pb-20">
    
    <!-- Header -->
    <header class="sticky top-0 z-40 bg-dark-bg/80 backdrop-blur border-b border-gray-800">
      <div class="max-w-7xl mx-auto px-6 py-4 flex justify-between items-center">
        <h1 class="text-2xl font-serif text-primary">Chef's Market</h1>
        
        <div class="flex items-center gap-6">
          <button @click="$router.push('/')" class="text-xs font-bold uppercase tracking-widest text-gray-400 hover:text-white transition-colors">
            Back to Home
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

    <!-- Cart Sidebar -->
    <div 
      class="fixed inset-y-0 right-0 w-80 bg-dark-surface shadow-2xl transform transition-transform duration-300 z-50 border-l border-gray-800 p-6"
      :class="isCartOpen ? 'translate-x-0' : 'translate-x-full'"
    >
      <div class="flex justify-between items-center mb-8">
        <h2 class="text-xl font-serif text-primary">Your Cart</h2>
        <button @click="isCartOpen = false" class="text-gray-500 hover:text-white">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
        </button>
      </div>

      <div v-if="cartItems.length === 0" class="text-center text-gray-500 mt-20">
        <p>Your cart is empty.</p>
        <p class="text-sm mt-2">Start adding some premium tools!</p>
      </div>

      <div v-else class="space-y-4 h-[calc(100vh-200px)] overflow-y-auto">
        <div v-for="(item, idx) in cartItems" :key="idx" class="flex gap-4 items-center bg-dark-bg p-3 rounded-lg">
          <img :src="item.coverUrl" class="w-12 h-12 object-cover rounded" />
          <div class="flex-1 min-w-0">
            <h4 class="text-sm font-bold text-white truncate">{{ item.name }}</h4>
            <p class="text-xs text-primary">${{ item.price }}</p>
          </div>
          <button class="text-gray-500 hover:text-red-500" @click="cartItems.splice(idx, 1); cartCount--">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
          </button>
        </div>
      </div>

      <div v-if="cartItems.length > 0" class="absolute bottom-6 left-6 right-6">
        <div class="flex justify-between mb-4 text-white font-bold">
          <span>Total</span>
          <span>${{ cartItems.reduce((acc, item) => acc + item.price, 0).toFixed(2) }}</span>
        </div>
        <button @click="checkout" class="w-full py-3 bg-primary text-black font-bold rounded-lg hover:bg-white transition-colors">
          Checkout
        </button>
      </div>
    </div>

    <!-- Product Grid -->
    <main class="max-w-7xl mx-auto px-6 py-12">
      <div v-if="loading" class="grid grid-cols-1 md:grid-cols-4 gap-8">
        <div v-for="i in 4" :key="i" class="animate-pulse bg-gray-800 h-80 rounded-xl"></div>
      </div>
      
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
        <div 
          v-for="product in products" 
          :key="product.id"
          class="product-card group bg-dark-surface rounded-xl overflow-hidden border border-gray-800 hover:border-primary transition-colors duration-300"
        >
          <!-- Image -->
          <div class="relative aspect-square overflow-hidden bg-white">
            <img :src="product.coverUrl" class="w-full h-full object-contain p-4 group-hover:scale-110 transition-transform duration-500" />
            
            <!-- Quick Add Button (Visible on Hover) -->
            <button 
              @click="(e) => addToCart(e, product)"
              class="absolute bottom-4 right-4 w-10 h-10 bg-black text-white rounded-full flex items-center justify-center opacity-0 translate-y-4 group-hover:opacity-100 group-hover:translate-y-0 transition-all duration-300 hover:bg-primary hover:text-black shadow-lg"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path></svg>
            </button>
          </div>
          
          <!-- Info -->
          <div class="p-4">
            <h3 class="font-serif text-lg text-white mb-1 truncate">{{ product.name }}</h3>
            <div class="flex justify-between items-center">
              <div class="flex flex-col">
                <span class="text-primary font-bold">${{ product.price }}</span>
                <span v-if="product.originalPrice" class="text-xs text-gray-500 line-through">${{ product.originalPrice }}</span>
              </div>
              <div class="flex items-center gap-1 text-xs text-gray-400">
                <svg class="w-3 h-3 text-yellow-500 fill-current" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>
                {{ product.rating }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>
