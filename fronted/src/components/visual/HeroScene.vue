<script setup lang="ts">
import { TresCanvas } from '@tresjs/core'
import { SRGBColorSpace, NoToneMapping } from 'three'
import FlavorBlobScene from './FlavorBlobScene.vue'
</script>

<template>
  <TresCanvas
    clear-color="#121212"
    shadows
    alpha
    power-preference="high-performance"
    :output-color-space="SRGBColorSpace"
    :tone-mapping="NoToneMapping"
  >
    <FlavorBlobScene />
  </TresCanvas>
</template>
