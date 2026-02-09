<script setup lang="ts">
import { NSkeleton } from 'naive-ui'
</script>

<template>
  <div class="recipe-skeleton p-4 rounded-xl bg-dark-surface shadow-sm border border-gray-800">
    <!-- Cover Image Skeleton -->
    <div class="w-full aspect-[4/3] mb-4 rounded-lg overflow-hidden bg-gray-800/50">
      <n-skeleton height="100%" width="100%" :sharp="false" class="opacity-20" />
    </div>
    
    <!-- Title Skeleton -->
    <n-skeleton text style="width: 70%" class="mb-2 h-6 bg-gray-700/50" />
    <n-skeleton text style="width: 40%" class="mb-4 h-4 bg-gray-700/50" />
    
    <!-- Author Info Skeleton -->
    <div class="flex items-center gap-3 mt-4 pt-4 border-t border-gray-800">
      <n-skeleton circle size="small" class="bg-gray-700/50" />
      <n-skeleton text style="width: 30%" class="bg-gray-700/50" />
    </div>
  </div>
</template>
