<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

// Props: status = 'idle' | 'listening' | 'processing' | 'speaking'
const props = defineProps<{ status: string }>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
let animationFrameId: number
let time = 0

// 绘制声波动画
const draw = (ctx: CanvasRenderingContext2D, width: number, height: number) => {
  ctx.clearRect(0, 0, width, height)
  
  const centerX = width / 2
  const centerY = height / 2
  
  // Base Circle
  ctx.beginPath()
  ctx.arc(centerX, centerY, 10, 0, Math.PI * 2)
  ctx.fillStyle = '#D4AF37' // Primary Gold
  ctx.fill()
  
  // Dynamic Waves
  if (props.status === 'listening' || props.status === 'processing') {
    ctx.strokeStyle = '#D4AF37'
    ctx.lineWidth = 1.5
    
    // Create 3 waves
    for (let i = 0; i < 3; i++) {
      ctx.beginPath()
      // Use sine wave to modulate radius
      // Offset each wave by phase (i * 2)
      const amplitude = props.status === 'listening' ? 10 : 5
      const speed = props.status === 'listening' ? 0.05 : 0.1
      
      const r = 15 + i * 8 + Math.sin(time * speed + i) * amplitude
      
      ctx.arc(centerX, centerY, r, 0, Math.PI * 2)
      // Fade out outer waves
      ctx.globalAlpha = 1 - (i / 3) - 0.2
      ctx.stroke()
    }
    ctx.globalAlpha = 1.0 // Reset alpha
  }
  
  time += 1
  animationFrameId = requestAnimationFrame(() => draw(ctx, width, height))
}

onMounted(() => {
  if (canvasRef.value) {
    const ctx = canvasRef.value.getContext('2d')
    if (ctx) {
      draw(ctx, canvasRef.value.width, canvasRef.value.height)
    }
  }
})

onBeforeUnmount(() => {
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
})
</script>

<template>
  <div class="orb-container relative w-[60px] h-[60px] cursor-pointer group">
    <!-- Glow Effect -->
    <div class="absolute inset-0 bg-primary/20 rounded-full blur-md group-hover:bg-primary/40 transition-colors duration-500"></div>
    
    <!-- Canvas for dynamic waves -->
    <canvas ref="canvasRef" width="60" height="60" class="relative z-10"></canvas>
    
    <!-- Tooltip -->
    <div class="absolute -top-8 left-1/2 -translate-x-1/2 whitespace-nowrap px-2 py-1 bg-black/80 text-white text-[10px] rounded opacity-0 group-hover:opacity-100 transition-opacity">
      AI Assistant
    </div>
  </div>
</template>
