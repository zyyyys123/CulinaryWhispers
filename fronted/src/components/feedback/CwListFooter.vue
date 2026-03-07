<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    loading: boolean
    hasMore: boolean
    loadMoreLabel?: string
    loadingLabel?: string
    endLabel?: string
  }>(),
  {
    loadMoreLabel: '加载更多',
    loadingLabel: '加载中…',
    endLabel: '已到底'
  }
)

const emit = defineEmits<{
  (e: 'loadMore'): void
}>()
</script>

<template>
  <div class="mt-12">
    <div v-if="props.hasMore" class="flex justify-center">
      <button
        @click="emit('loadMore')"
        class="group relative px-8 py-3 bg-transparent border border-gray-700 hover:border-primary text-gray-300 hover:text-primary transition-colors duration-300 rounded-full overflow-hidden disabled:opacity-60 disabled:cursor-not-allowed"
        :disabled="props.loading"
      >
        <span class="relative z-10 text-sm tracking-widest uppercase">
          {{ props.loading ? props.loadingLabel : props.loadMoreLabel }}
        </span>
        <div class="absolute inset-0 bg-primary/10 transform scale-x-0 group-hover:scale-x-100 transition-transform origin-left duration-500"></div>
      </button>
    </div>
    <div v-else class="text-center text-gray-600 text-sm tracking-widest uppercase">
      {{ props.endLabel }}
    </div>
  </div>
</template>

