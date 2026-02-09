<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { TresCanvas, useRenderLoop } from '@tresjs/core'
import { OrbitControls, Stars } from '@tresjs/cientos'
import { SRGBColorSpace, NoToneMapping, DoubleSide } from 'three'
import { gsap } from 'gsap'
import type { BadgeVO } from '@/types/user'

const props = defineProps<{
  badges: BadgeVO[]
}>()

const emit = defineEmits(['close'])

const groupRef = ref(null)
const { onLoop } = useRenderLoop()

// Rotate the carousel
onLoop(({ elapsed }) => {
  if (groupRef.value) {
    // Gentle floating rotation
    groupRef.value.rotation.y = Math.sin(elapsed * 0.1) * 0.05
  }
})

// Entrance Animation
const overlayRef = ref(null)
const contentRef = ref(null)

onMounted(() => {
  gsap.from(overlayRef.value, { opacity: 0, duration: 0.5 })
  gsap.from(contentRef.value, { scale: 0.8, opacity: 0, duration: 0.8, ease: 'back.out(1.7)' })
})

const close = () => {
  gsap.to(overlayRef.value, { opacity: 0, duration: 0.3 })
  gsap.to(contentRef.value, { 
    scale: 0.8, 
    opacity: 0, 
    duration: 0.3, 
    onComplete: () => emit('close') 
  })
}
</script>

<template>
  <div ref="overlayRef" class="fixed inset-0 z-50 flex items-center justify-center bg-black/95 backdrop-blur-xl">
    
    <!-- Close Button -->
    <button @click="close" class="absolute top-8 right-8 text-gray-500 hover:text-white z-50 transition-colors">
      <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
    </button>

    <div ref="contentRef" class="w-full h-full relative flex flex-col items-center justify-center">
      <div class="absolute top-10 text-center z-10 pointer-events-none">
        <h2 class="text-5xl font-serif text-transparent bg-clip-text bg-gradient-to-r from-primary via-white to-primary mb-2 animate-pulse">Hall of Fame</h2>
        <p class="text-gray-400 tracking-[0.3em] uppercase text-sm">Exclusive Collection</p>
      </div>

      <Suspense>
        <TresCanvas
          alpha
          :output-color-space="SRGBColorSpace"
          :tone-mapping="NoToneMapping"
        >
          <TresPerspectiveCamera :position="[0, 0, 6]" :fov="45" />
          <OrbitControls :enable-zoom="false" :enable-pan="false" :max-polar-angle="Math.PI / 1.5" :min-polar-angle="Math.PI / 3" />
          
          <!-- Dramatic Lighting -->
          <TresAmbientLight :intensity="0.2" />
          <TresSpotLight :position="[5, 5, 5]" :intensity="2" :penumbra="1" color="#4f46e5" />
          <TresSpotLight :position="[-5, -5, 5]" :intensity="2" :penumbra="1" color="#D4AF37" />
          <TresPointLight :position="[0, 0, 2]" :intensity="0.5" color="#ffffff" />

          <TresGroup ref="groupRef">
            <!-- Generate Badge Models (Using simple geometry placeholders for now) -->
            <TresGroup 
              v-for="(badge, index) in badges" 
              :key="badge.id"
              :position="[(index - 1) * 3, 0, 0]"
            >
              <!-- Pedestal -->
              <TresMesh :position="[0, -1.5, 0]">
                <TresCylinderGeometry :args="[1, 1.2, 0.5, 32]" />
                <TresMeshStandardMaterial color="#333" :roughness="0.5" :metalness="0.8" />
              </TresMesh>

              <!-- Badge (Sphere placeholder) -->
              <TresMesh :position="[0, 0, 0]">
                <TresIcosahedronGeometry :args="[1, 1]" />
                <TresMeshStandardMaterial 
                  :color="index === 0 ? '#D4AF37' : index === 1 ? '#C0C0C0' : '#CD7F32'" 
                  :roughness="0.2" 
                  :metalness="1" 
                  :emissive="index === 0 ? '#D4AF37' : '#000'"
                  :emissive-intensity="0.2"
                />
              </TresMesh>
            </TresGroup>
          </TresGroup>

          <Stars :radius="10" :depth="50" :count="3000" :size="0.03" />
        </TresCanvas>
      </Suspense>
      
      <!-- HTML Labels (Overlay for crisp text) -->
      <div class="absolute bottom-20 w-full flex justify-center gap-16 pointer-events-none">
        <div v-for="badge in badges" :key="badge.id" class="text-center w-48">
          <h3 class="text-xl font-serif text-primary mb-1 drop-shadow-lg">{{ badge.name }}</h3>
          <p class="text-xs text-gray-300 bg-black/50 px-2 py-1 rounded">{{ badge.description }}</p>
        </div>
      </div>
    </div>
  </div>
</template>
