<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { gsap } from 'gsap'

const emit = defineEmits(['complete'])

// Refs for animation elements
const containerRef = ref(null)
const leftCurtainRef = ref(null)
const rightCurtainRef = ref(null)
const logoRef = ref(null)
const circleRef = ref(null)
const textRef = ref(null)

onMounted(() => {
  const tl = gsap.timeline({
    onComplete: () => {
      emit('complete')
      // Optional: remove from DOM to save performance
      gsap.set(containerRef.value, { display: 'none' })
    }
  })

  // 1. Initial State: Logo Pulse
  // The CSS animation handles the continuous pulse, but we'll scale it up
  
  // 2. The "Magic Opening" Sequence
  tl.to(circleRef.value, {
    scale: 50, // Expand circle to fill screen (portal effect)
    duration: 1.5,
    ease: 'power2.inOut',
    delay: 0.5
  })
  
  // 3. Text Fade Out
  .to([logoRef.value, textRef.value], {
    opacity: 0,
    duration: 0.5,
    ease: 'power2.out'
  }, '<') // Start at same time as circle expansion
  
  // 4. Curtains Parting (The "Storybook" opening)
  .to(leftCurtainRef.value, {
    xPercent: -100,
    duration: 2,
    ease: 'power3.inOut'
  }, '-=1.0') // Overlap with circle expansion
  
  .to(rightCurtainRef.value, {
    xPercent: 100,
    duration: 2,
    ease: 'power3.inOut'
  }, '<') // Sync with left curtain
  
  // 5. Fade out the container background AND content to reveal 3D scene underneath perfectly
  .to([leftCurtainRef.value, rightCurtainRef.value], {
    opacity: 0,
    duration: 1,
    ease: 'power2.inOut'
  }, '-=1.0') // Start fading while moving
  
  .to(containerRef.value, {
    backgroundColor: 'transparent',
    opacity: 0, // Fade out the entire container
    duration: 1,
    ease: 'power2.inOut'
  }, '<')
})
</script>

<template>
  <div ref="containerRef" class="splash-screen fixed inset-0 z-50 flex items-center justify-center overflow-hidden bg-black pointer-events-none">
    
    <!-- The "Curtains" - Deep textured layers -->
    <div ref="leftCurtainRef" class="curtain absolute inset-y-0 left-0 w-1/2 bg-black z-10 border-r border-gray-900/30">
        <!-- Add subtle texture or gradient here if needed -->
        <div class="absolute inset-0 bg-gradient-to-r from-transparent to-gray-900/50"></div>
    </div>
    <div ref="rightCurtainRef" class="curtain absolute inset-y-0 right-0 w-1/2 bg-black z-10 border-l border-gray-900/30">
        <div class="absolute inset-0 bg-gradient-to-l from-transparent to-gray-900/50"></div>
    </div>

    <!-- The "Portal" Circle -->
    <div ref="circleRef" class="portal absolute w-4 h-4 rounded-full bg-primary z-20 mix-blend-screen pointer-events-none"></div>

    <!-- Logo & Text Content (Centered) -->
    <div class="content relative z-30 flex flex-col items-center text-center">
      <div ref="logoRef" class="logo text-6xl md:text-8xl font-serif text-primary mb-4 animate-pulse-slow">
        CW
      </div>
      <div ref="textRef" class="text text-sm md:text-base text-gray-400 tracking-[0.5em] uppercase opacity-0 animate-fade-in-up">
        Enter the Realm of Flavor
      </div>
    </div>
  </div>
</template>

<style scoped>
.animate-pulse-slow {
  animation: pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(0.95); }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.animate-fade-in-up {
  animation: fadeInUp 1s ease-out forwards 0.5s;
}

/* Optional: Add a subtle grain texture to curtains for more "paper/book" feel */
.curtain::before {
  content: "";
  position: absolute;
  inset: 0;
  opacity: 0.05;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
  pointer-events: none;
}
</style>
