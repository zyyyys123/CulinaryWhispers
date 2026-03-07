<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    message: string
    actionLabel?: string
  }>(),
  {
    actionLabel: '重试'
  }
)

const emit = defineEmits<{
  (e: 'action'): void
}>()
</script>

<template>
  <div class="rounded-2xl border border-white/10 bg-black/20 px-6 py-4 flex items-center justify-between gap-4">
    <div class="text-gray-300 text-sm tracking-wider">{{ props.message }}</div>
    <button @click="emit('action')" class="px-5 py-2 rounded-full bg-primary text-black font-bold text-sm">
      {{ props.actionLabel }}
    </button>
  </div>
</template>

