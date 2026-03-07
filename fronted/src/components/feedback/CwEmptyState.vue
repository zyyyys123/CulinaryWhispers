<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    title?: string
    description?: string
    actionLabel?: string
  }>(),
  {
    title: '暂无数据',
    description: ''
  }
)

const emit = defineEmits<{
  (e: 'action'): void
}>()
</script>

<template>
  <div class="text-center mt-12">
    <div class="text-gray-200 font-bold tracking-wider">{{ props.title }}</div>
    <div v-if="props.description" class="mt-2 text-gray-500 text-sm tracking-wider">
      {{ props.description }}
    </div>
    <div v-if="props.actionLabel" class="mt-6 flex justify-center">
      <button @click="emit('action')" class="px-6 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors text-sm">
        {{ props.actionLabel }}
      </button>
    </div>
  </div>
</template>

