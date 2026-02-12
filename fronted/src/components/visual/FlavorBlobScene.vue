<script setup lang="ts">
import { shallowRef, watch, onBeforeUnmount } from 'vue'
import { useRenderLoop } from '@tresjs/core'
import { OrbitControls, Stars } from '@tresjs/cientos'
import { gsap } from 'gsap'
import type { Group, Mesh } from 'three'

// Refs
const blobRef = shallowRef<Mesh | null>(null)
const groupRef = shallowRef<Group | null>(null)

// Shader Material for the "Flavor Blob"
const vertexShader = `
varying vec2 vUv;
varying float vDisplacement;
uniform float uTime;

// Simplex 3D Noise function (simplified for brevity)
vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 permute(vec4 x) { return mod289(((x*34.0)+1.0)*x); }
vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }
float snoise(vec3 v) {
  const vec2  C = vec2(1.0/6.0, 1.0/3.0) ;
  const vec4  D = vec4(0.0, 0.5, 1.0, 2.0);
  vec3 i  = floor(v + dot(v, C.yyy) );
  vec3 x0 = v - i + dot(i, C.xxx) ;
  vec3 g = step(x0.yzx, x0.xyz);
  vec3 l = 1.0 - g;
  vec3 i1 = min( g.xyz, l.zxy );
  vec3 i2 = max( g.xyz, l.zxy );
  vec3 x1 = x0 - i1 + C.xxx;
  vec3 x2 = x0 - i2 + C.yyy; // 2.0*C.x = 1/3 = C.y
  vec3 x3 = x0 - D.yyy;      // -1.0+3.0*C.x = -0.5 = -D.y
  i = mod289(i);
  vec4 p = permute( permute( permute(
             i.z + vec4(0.0, i1.z, i2.z, 1.0 ))
           + i.y + vec4(0.0, i1.y, i2.y, 1.0 ))
           + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));
  float n_ = 0.142857142857; // 1.0/7.0
  vec3  ns = n_ * D.wyz - D.xzx;
  vec4 j = p - 49.0 * floor(p * ns.z * ns.z);  //  mod(p,N)
  vec4 x_ = floor(j * ns.z);
  vec4 y_ = floor(j - 7.0 * x_ );    // mod(j,N)
  vec4 x = x_ *ns.x + ns.yyyy;
  vec4 y = y_ *ns.x + ns.yyyy;
  vec4 h = 1.0 - abs(x) - abs(y);
  vec4 b0 = vec4( x.xy, y.xy );
  vec4 b1 = vec4( x.zw, y.zw );
  vec4 s0 = floor(b0)*2.0 + 1.0;
  vec4 s1 = floor(b1)*2.0 + 1.0;
  vec4 sh = -step(h, vec4(0.0));
  vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy ;
  vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww ;
  vec3 p0 = vec3(a0.xy,h.x);
  vec3 p1 = vec3(a0.zw,h.y);
  vec3 p2 = vec3(a1.xy,h.z);
  vec3 p3 = vec3(a1.zw,h.w);
  vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
  p0 *= norm.x;
  p1 *= norm.y;
  p2 *= norm.z;
  p3 *= norm.w;
  vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
  m = m * m;
  return 42.0 * dot( m*m, vec4( dot(p0,x0), dot(p1,x1),
                                dot(p2,x2), dot(p3,x3) ) );
}

void main() {
  vUv = uv;
  // Use noise to displace vertices
  vDisplacement = snoise(position + vec3(2.0 * uTime));
  vec3 newPosition = position + normal * (vDisplacement * 0.3);
  gl_Position = projectionMatrix * modelViewMatrix * vec4(newPosition, 1.0);
}
`

const fragmentShader = `
varying vec2 vUv;
varying float vDisplacement;
uniform float uTime;

void main() {
  // Color based on displacement to give depth
  // Gold: #D4AF37 -> RGB (0.83, 0.68, 0.21)
  // Deep Orange: #FF5722 -> RGB (1.0, 0.34, 0.13)
  
  vec3 color1 = vec3(0.83, 0.68, 0.21); // Gold
  vec3 color2 = vec3(1.0, 0.34, 0.13); // Spicy Red
  
  float mixStrength = smoothstep(-1.0, 1.0, vDisplacement);
  vec3 finalColor = mix(color1, color2, mixStrength);
  
  // Add some "shine"
  float shine = pow(max(0.0, vDisplacement + 0.5), 3.0);
  
  gl_FragColor = vec4(finalColor + shine * 0.5, 1.0);
}
`

const uniforms = {
  uTime: { value: 0 },
}

const { onLoop } = useRenderLoop()

onLoop(({ elapsed }) => {
  uniforms.uTime.value = elapsed * 0.5 // Slow down the shader animation
  if (groupRef.value) {
    groupRef.value.rotation.y = elapsed * 0.05 // Slow down global rotation
    groupRef.value.rotation.z = elapsed * 0.02
  }
})

// Mouse Interaction
const onMouseMove = (ev: MouseEvent) => {
  // Simple parallax
  const x = (ev.clientX / window.innerWidth) * 2 - 1
  const y = -(ev.clientY / window.innerHeight) * 2 + 1
  
  if (groupRef.value) {
    gsap.to(groupRef.value.rotation, {
      x: y * 0.5,
      y: x * 0.5 + uniforms.uTime.value * 0.1, // Keep rotating but influence by mouse
      duration: 1,
      ease: 'power2.out'
    })
  }
}

// Global Event Listener for mouse move (since canvas covers full screen)
watch(groupRef, (val) => {
  if(val) {
    window.addEventListener('mousemove', onMouseMove)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onMouseMove)
})
</script>

<template>
  <TresPerspectiveCamera :position="[0, 0, 4]" :fov="45" />
  <OrbitControls :enable-zoom="false" :enable-pan="false" :enable-rotate="false" />

  <!-- Ambient Light -->
  <TresAmbientLight :intensity="0.5" />
  
  <!-- Directional Light for drama -->
  <TresDirectionalLight :position="[5, 5, 5]" :intensity="2" cast-shadow />
  <TresPointLight :position="[-5, -5, 5]" :intensity="1" color="#D4AF37" />

  <!-- The Abstract Flavor Blob -->
  <TresGroup ref="groupRef">
    <TresMesh ref="blobRef">
      <TresIcosahedronGeometry :args="[1, 64]" />
      <TresShaderMaterial
        :vertex-shader="vertexShader"
        :fragment-shader="fragmentShader"
        :uniforms="uniforms"
        :transparent="true"
      />
    </TresMesh>
    
    <!-- Floating Particles around it -->
    <Stars 
      :radius="1.5" 
      :depth="50" 
      :count="1000" 
      :size="0.02" 
      :size-attenuation="true" 
      color="#D4AF37"
    />
  </TresGroup>
</template>
