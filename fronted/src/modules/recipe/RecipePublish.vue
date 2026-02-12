<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { RecipeAPI } from '@/api/recipe'
import type { RecipePublishDTO } from '@/types/recipe'

const router = useRouter()

const loading = ref(false)
const errorText = ref('')
const successText = ref('')

const form = ref<RecipePublishDTO>({
  title: '',
  coverUrl: '',
  description: '',
  difficulty: 1,
  timeCost: 30,
  calories: 0,
  protein: 0,
  fat: 0,
  carbs: 0,
  tips: '',
  tags: [],
  steps: [{ stepNo: 1, desc: '', isKeyStep: true }]
})

const tagInput = ref('')

const addTag = () => {
  const t = tagInput.value.trim()
  if (!t) return
  if (!form.value.tags.includes(t)) form.value.tags.push(t)
  tagInput.value = ''
}

const removeTag = (t: string) => {
  form.value.tags = form.value.tags.filter(x => x !== t)
}

const addStep = () => {
  const nextNo = form.value.steps.length + 1
  form.value.steps.push({ stepNo: nextNo, desc: '' })
}

const removeStep = (idx: number) => {
  if (form.value.steps.length <= 1) return
  form.value.steps.splice(idx, 1)
  form.value.steps = form.value.steps.map((s, i) => ({ ...s, stepNo: i + 1 }))
}

const submit = async () => {
  if (loading.value) return
  errorText.value = ''
  successText.value = ''

  const payload = {
    ...form.value,
    title: form.value.title.trim(),
    coverUrl: form.value.coverUrl.trim(),
    description: form.value.description.trim(),
    steps: form.value.steps
      .map(s => ({ ...s, desc: (s.desc ?? '').trim() }))
      .filter(s => s.desc.length > 0)
  }

  if (!payload.title || !payload.coverUrl || !payload.description || payload.steps.length === 0) {
    errorText.value = '请补齐标题/封面/简介/至少1步步骤'
    return
  }

  loading.value = true
  try {
    const res = await RecipeAPI.publish(payload)
    if (res.code !== 200) {
      errorText.value = res.message || '发布失败'
      return
    }
    successText.value = '发布成功'
    const id = res.data
    if (id) router.replace({ name: 'recipe-detail', params: { id } })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-dark-bg text-white px-6 py-10">
    <div class="max-w-3xl mx-auto">
      <div class="flex items-center justify-between mb-8">
        <h1 class="text-3xl font-serif text-primary">Publish Recipe</h1>
        <button @click="router.back()" class="px-4 py-2 rounded-full border border-white/10 text-gray-300 hover:text-white hover:border-white/30 transition-colors">
          Back
        </button>
      </div>

      <div v-if="errorText" class="mb-5 text-sm text-red-300 bg-red-500/10 border border-red-500/20 rounded-xl px-4 py-3">
        {{ errorText }}
      </div>
      <div v-if="successText" class="mb-5 text-sm text-green-300 bg-green-500/10 border border-green-500/20 rounded-xl px-4 py-3">
        {{ successText }}
      </div>

      <div class="bg-black/20 border border-white/10 rounded-2xl p-6 md:p-8 space-y-8">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="md:col-span-2">
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Title</div>
            <input v-model="form.title" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
          <div class="md:col-span-2">
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Cover URL</div>
            <input v-model="form.coverUrl" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
          <div class="md:col-span-2">
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Description</div>
            <textarea v-model="form.description" rows="3" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary"></textarea>
          </div>
          <div>
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Difficulty</div>
            <select v-model.number="form.difficulty" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary">
              <option :value="1">1</option>
              <option :value="2">2</option>
              <option :value="3">3</option>
              <option :value="4">4</option>
              <option :value="5">5</option>
            </select>
          </div>
          <div>
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Time Cost (min)</div>
            <input v-model.number="form.timeCost" type="number" min="1" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
          <div>
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Calories</div>
            <input v-model.number="form.calories" type="number" min="0" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
          <div>
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Protein (g)</div>
            <input v-model.number="form.protein" type="number" min="0" step="0.1" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
          <div>
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Fat (g)</div>
            <input v-model.number="form.fat" type="number" min="0" step="0.1" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
          <div>
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Carbs (g)</div>
            <input v-model.number="form.carbs" type="number" min="0" step="0.1" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
          <div class="md:col-span-2">
            <div class="text-xs tracking-widest uppercase text-gray-500 mb-1">Tips</div>
            <input v-model="form.tips" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" />
          </div>
        </div>

        <div>
          <div class="text-xs tracking-widest uppercase text-gray-500 mb-2">Tags</div>
          <div class="flex gap-2">
            <input v-model="tagInput" class="flex-1 px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary" @keyup.enter="addTag" />
            <button @click="addTag" class="px-5 py-3 rounded-xl bg-primary text-black font-bold">Add</button>
          </div>
          <div class="flex flex-wrap gap-2 mt-3">
            <button
              v-for="t in form.tags"
              :key="t"
              class="px-3 py-1 rounded-full text-xs bg-white/10 border border-white/10 hover:border-primary hover:text-primary transition-colors"
              @click="removeTag(t)"
              title="Remove"
            >
              {{ t }}
            </button>
          </div>
        </div>

        <div>
          <div class="flex items-center justify-between mb-3">
            <div class="text-xs tracking-widest uppercase text-gray-500">Steps</div>
            <button @click="addStep" class="px-4 py-2 rounded-full bg-white/10 border border-white/10 hover:border-primary hover:text-primary transition-colors">
              Add Step
            </button>
          </div>
          <div class="space-y-4">
            <div v-for="(s, idx) in form.steps" :key="s.stepNo" class="bg-black/10 border border-white/10 rounded-2xl p-4">
              <div class="flex items-center justify-between mb-3">
                <div class="text-sm font-bold text-gray-200">Step {{ s.stepNo }}</div>
                <button @click="removeStep(idx)" class="text-xs text-gray-400 hover:text-white">Remove</button>
              </div>
              <textarea v-model="s.desc" rows="2" class="w-full px-4 py-3 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary"></textarea>
              <div class="flex items-center justify-between mt-3">
                <label class="flex items-center gap-2 text-xs text-gray-300">
                  <input v-model="s.isKeyStep" type="checkbox" class="form-checkbox rounded bg-gray-700 border-gray-600 text-primary focus:ring-0" />
                  Key Step
                </label>
                <input v-model.number="s.timeCost" type="number" min="0" placeholder="timeCost(min)" class="w-40 px-3 py-2 rounded-xl bg-black/20 border border-white/10 text-white focus:outline-none focus:border-primary text-xs" />
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-end">
          <button @click="submit" :disabled="loading" class="px-8 py-3 rounded-full bg-primary text-black font-bold disabled:opacity-60">
            {{ loading ? 'Publishing…' : 'Publish' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

