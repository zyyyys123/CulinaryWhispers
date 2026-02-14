<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { gsap } from 'gsap'
import { RecipeAPI } from '@/api/recipe'
import { SearchAPI } from '@/api/search'
import type { RecipePageVO } from '@/types/recipe'

const router = useRouter()

const rootRef = ref<HTMLElement | null>(null)
const heroRef = ref<HTMLElement | null>(null)
const kpiRefs = ref<Array<HTMLElement | null>>([])
const panelRefs = ref<Array<HTMLElement | null>>([])

const loading = ref(true)
const recipeTotal = ref<number | null>(null)
const searchOk = ref<boolean | null>(null)
const latest = ref<RecipePageVO[]>([])
const hint = ref('')

const formatNum = (n: number | null) => {
  if (n === null || Number.isNaN(n)) return '—'
  return n.toLocaleString('zh-CN')
}

const quickJump = (name: string, params?: any) => router.push({ name, params })

const checkSearch = async () => {
  try {
    const res = await SearchAPI.searchRecipe({ keyword: '家常', page: 1, size: 1 })
    searchOk.value = res.code === 200
  } catch {
    searchOk.value = false
  }
}

const loadOverview = async () => {
  loading.value = true
  hint.value = ''
  try {
    const [recipeRes] = await Promise.all([
      RecipeAPI.getList({ page: 1, size: 1 })
    ])
    recipeTotal.value = recipeRes.code === 200 ? Number(recipeRes.data?.total ?? 0) : null
  } catch {
    recipeTotal.value = null
  }

  try {
    const r = await RecipeAPI.recommend({ page: 1, size: 8 })
    latest.value = r.code === 200 ? (r.data?.records ?? []) : []
  } catch {
    latest.value = []
  }

  await checkSearch()

  if (recipeTotal.value === null && searchOk.value === false) {
    hint.value = '检测到服务不可达或接口异常：先确认后端、ES、数据库是否可从前端访问。'
  }

  loading.value = false
}

const animateIn = async () => {
  await nextTick()
  const hero = heroRef.value
  const kpis = kpiRefs.value.filter(Boolean) as HTMLElement[]
  const panels = panelRefs.value.filter(Boolean) as HTMLElement[]
  const tl = gsap.timeline()
  if (hero) {
    tl.from(hero, { y: 18, opacity: 0, duration: 0.9, ease: 'power3.out' })
  }
  if (kpis.length) {
    tl.from(kpis, { y: 12, opacity: 0, duration: 0.7, ease: 'power3.out', stagger: 0.08 }, hero ? '-=0.4' : 0)
  }
  if (panels.length) {
    tl.from(panels, { y: 12, opacity: 0, duration: 0.7, ease: 'power3.out', stagger: 0.08 }, '-=0.35')
  }
}

onMounted(async () => {
  await loadOverview()
  await animateIn()
})
</script>

<template>
  <div ref="rootRef" class="min-h-screen bg-dark-bg text-white">
    <div class="relative">
      <div class="pointer-events-none absolute inset-0 overflow-hidden">
        <div class="absolute -top-24 -left-24 h-72 w-72 rounded-full bg-primary/20 blur-3xl"></div>
        <div class="absolute top-40 -right-24 h-80 w-80 rounded-full bg-white/10 blur-3xl"></div>
        <div class="absolute bottom-0 left-1/3 h-64 w-64 rounded-full bg-primary/10 blur-3xl"></div>
      </div>

      <div class="mx-auto max-w-7xl px-6 py-8">
        <div class="flex items-center justify-between">
          <div ref="heroRef" class="space-y-2">
            <div class="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs tracking-widest text-white/70 backdrop-blur">
              <span>ADMIN CONSOLE</span>
              <span class="h-1 w-1 rounded-full bg-primary"></span>
              <span>运营可视化</span>
            </div>
            <h1 class="text-2xl font-bold tracking-tight md:text-3xl">后台管理系统</h1>
            <p class="max-w-2xl text-sm leading-6 text-white/70">
              用更少的点击完成更多决策：数据概览、内容巡检、风险提示与快捷操作统一在一个工作台。
            </p>
          </div>

          <div class="hidden items-center gap-2 md:flex">
            <button
              class="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm text-white/80 transition hover:bg-white/10"
              @click="quickJump('home')"
            >
              返回前台
            </button>
            <button
              class="rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-dark-bg transition hover:brightness-110"
              @click="loadOverview()"
            >
              刷新概览
            </button>
          </div>
        </div>

        <div v-if="hint" class="mt-6 rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/80 backdrop-blur">
          {{ hint }}
        </div>

        <div class="mt-8 grid grid-cols-1 gap-4 md:grid-cols-3">
          <div
            :ref="el => (kpiRefs[0] = el as HTMLElement)"
            class="group rounded-2xl border border-white/10 bg-white/5 p-5 backdrop-blur transition hover:-translate-y-0.5 hover:bg-white/10"
          >
            <div class="flex items-start justify-between">
              <div class="space-y-1">
                <div class="text-xs tracking-widest text-white/60">食谱总量</div>
                <div class="text-2xl font-semibold">{{ loading ? '…' : formatNum(recipeTotal) }}</div>
              </div>
              <div class="rounded-xl bg-primary/15 px-3 py-1 text-xs text-primary/90">CONTENT</div>
            </div>
            <div class="mt-3 text-xs text-white/60">
              用于衡量内容池规模与推荐长尾覆盖面。
            </div>
          </div>

          <div
            :ref="el => (kpiRefs[1] = el as HTMLElement)"
            class="group rounded-2xl border border-white/10 bg-white/5 p-5 backdrop-blur transition hover:-translate-y-0.5 hover:bg-white/10"
          >
            <div class="flex items-start justify-between">
              <div class="space-y-1">
                <div class="text-xs tracking-widest text-white/60">搜索能力</div>
                <div class="text-2xl font-semibold">
                  <span v-if="loading">…</span>
                  <span v-else-if="searchOk === true" class="text-emerald-300">正常</span>
                  <span v-else-if="searchOk === false" class="text-rose-300">异常</span>
                  <span v-else>—</span>
                </div>
              </div>
              <div class="rounded-xl bg-white/10 px-3 py-1 text-xs text-white/70">SEARCH</div>
            </div>
            <div class="mt-3 text-xs text-white/60">
              基于搜索接口的可用性探针，用于快速定位 ES/索引问题。
            </div>
          </div>

          <div
            :ref="el => (kpiRefs[2] = el as HTMLElement)"
            class="group rounded-2xl border border-white/10 bg-white/5 p-5 backdrop-blur transition hover:-translate-y-0.5 hover:bg-white/10"
          >
            <div class="flex items-start justify-between">
              <div class="space-y-1">
                <div class="text-xs tracking-widest text-white/60">工作流</div>
                <div class="text-2xl font-semibold">巡检</div>
              </div>
              <div class="rounded-xl bg-white/10 px-3 py-1 text-xs text-white/70">OPS</div>
            </div>
            <div class="mt-3 flex flex-wrap gap-2 text-xs">
              <button class="rounded-xl border border-white/10 bg-white/5 px-3 py-1 text-white/75 transition hover:bg-white/10" @click="quickJump('data-lab')">
                数据实验室
              </button>
              <button class="rounded-xl border border-white/10 bg-white/5 px-3 py-1 text-white/75 transition hover:bg-white/10" @click="quickJump('search')">
                搜索页
              </button>
              <button class="rounded-xl border border-white/10 bg-white/5 px-3 py-1 text-white/75 transition hover:bg-white/10" @click="quickJump('market')">
                市集
              </button>
            </div>
          </div>
        </div>

        <div class="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-12">
          <div :ref="el => (panelRefs[0] = el as HTMLElement)" class="lg:col-span-7">
            <div class="rounded-2xl border border-white/10 bg-white/5 p-5 backdrop-blur">
              <div class="flex items-center justify-between">
                <div class="space-y-1">
                  <div class="text-xs tracking-widest text-white/60">最新内容</div>
                  <div class="text-lg font-semibold">推荐流采样</div>
                </div>
                <button
                  class="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm text-white/75 transition hover:bg-white/10"
                  @click="quickJump('home')"
                >
                  查看前台
                </button>
              </div>

              <div class="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
                <div
                  v-for="r in latest"
                  :key="r.id"
                  class="group cursor-pointer rounded-2xl border border-white/10 bg-dark-surface/40 p-4 transition hover:border-primary/30 hover:bg-dark-surface/60"
                  @click="router.push({ name: 'recipe-detail', params: { id: r.id } })"
                >
                  <div class="flex items-start justify-between gap-3">
                    <div class="min-w-0">
                      <div class="truncate text-sm font-semibold text-white/90">{{ r.title }}</div>
                      <div class="mt-1 line-clamp-2 text-xs leading-5 text-white/60">{{ r.description }}</div>
                    </div>
                    <div class="shrink-0 rounded-xl bg-white/5 px-2 py-1 text-[11px] text-white/70">
                      {{ r.timeCost }}m
                    </div>
                  </div>
                  <div class="mt-3 flex items-center justify-between text-[11px] text-white/55">
                    <div class="flex items-center gap-3">
                      <span>浏览 {{ formatNum(r.viewCount) }}</span>
                      <span>赞 {{ formatNum(r.likeCount) }}</span>
                    </div>
                    <span class="text-primary/80 transition group-hover:text-primary">进入</span>
                  </div>
                </div>
                <div v-if="!loading && latest.length === 0" class="rounded-2xl border border-white/10 bg-dark-surface/40 p-4 text-sm text-white/70">
                  暂无可展示内容，先确认后端推荐接口是否正常。
                </div>
              </div>
            </div>
          </div>

          <div :ref="el => (panelRefs[1] = el as HTMLElement)" class="lg:col-span-5">
            <div class="rounded-2xl border border-white/10 bg-white/5 p-5 backdrop-blur">
              <div class="space-y-1">
                <div class="text-xs tracking-widest text-white/60">体验准则</div>
                <div class="text-lg font-semibold">对齐前端设计思考</div>
              </div>

              <div class="mt-4 space-y-3 text-sm text-white/75">
                <div class="rounded-2xl border border-white/10 bg-dark-surface/40 p-4">
                  <div class="font-semibold text-white/90">设计驱动</div>
                  <div class="mt-1 text-xs leading-5 text-white/60">信息层级清晰、动作有意义、微交互传递状态与反馈。</div>
                </div>
                <div class="rounded-2xl border border-white/10 bg-dark-surface/40 p-4">
                  <div class="font-semibold text-white/90">性能优先</div>
                  <div class="mt-1 text-xs leading-5 text-white/60">动效只做 transform/opacity，页面结构尽量稳定避免抖动。</div>
                </div>
                <div class="rounded-2xl border border-white/10 bg-dark-surface/40 p-4">
                  <div class="font-semibold text-white/90">可扩展</div>
                  <div class="mt-1 text-xs leading-5 text-white/60">后续可以在此页面继续扩展：用户管理、内容审核、运营活动、告警中心。</div>
                </div>
              </div>

              <div class="mt-4 grid grid-cols-2 gap-3">
                <button class="rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-xs text-white/75 transition hover:bg-white/10" @click="quickJump('points')">
                  积分中心
                </button>
                <button class="rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-xs text-white/75 transition hover:bg-white/10" @click="quickJump('social')">
                  社交中心
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-8 flex flex-col gap-2 pb-10 text-xs text-white/50 md:flex-row md:items-center md:justify-between">
          <div>© CulinaryWhispers Admin Console</div>
          <div class="flex items-center gap-3">
            <span class="h-1 w-1 rounded-full bg-white/30"></span>
            <span>按需引入组件 + 懒加载路由</span>
            <span class="h-1 w-1 rounded-full bg-white/30"></span>
            <span>动效聚焦 60fps</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>

