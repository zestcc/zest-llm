<template>
  <el-select
    :model-value="modelValue"
    filterable
    :allow-create="allowCreate"
    :default-first-option="allowCreate"
    :clearable="clearable"
    :placeholder="placeholder"
    :style="selectStyle"
    :class="selectClass"
    :loading="loading"
    @update:model-value="onUpdate"
    @change="emit('change', $event)"
    @clear="emit('clear')"
  >
    <el-option
      v-for="a in activeApps"
      :key="a.appKey"
      :label="formatLabel(a)"
      :value="a.appKey"
    />
  </el-select>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { adminApi, normalizePage, type AppVO } from '../api/admin'
import { getLastAppKey, setLastAppKey } from '../utils/lastAppKey'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    placeholder?: string
    clearable?: boolean
    allowCreate?: boolean
    remember?: boolean
    width?: string
    selectClass?: string
  }>(),
  {
    modelValue: '',
    placeholder: '选择应用',
    clearable: false,
    allowCreate: false,
    remember: true,
    width: '100%'
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  change: [value: string | undefined]
  clear: []
}>()

const loading = ref(false)
const apps = ref<AppVO[]>([])

const activeApps = computed(() => apps.value.filter((a) => a.status === 'ACTIVE'))

const selectStyle = computed(() => ({ width: props.width }))

function formatLabel(a: AppVO) {
  return a.appName ? `${a.appKey} · ${a.appName}` : a.appKey
}

function onUpdate(value: string | undefined) {
  const next = value ?? ''
  emit('update:modelValue', next)
  if (props.remember && next) {
    setLastAppKey(next)
  }
}

async function loadApps() {
  loading.value = true
  try {
    const data = await adminApi.listApps(1, 500)
    apps.value = normalizePage(data, 1, 500).records
    applyRememberDefault()
  } finally {
    loading.value = false
  }
}

function applyRememberDefault() {
  if (!props.remember || props.modelValue) return
  const last = getLastAppKey()
  if (last && activeApps.value.some((a) => a.appKey === last)) {
    onUpdate(last)
    emit('change', last)
  } else if (!props.allowCreate && activeApps.value.length) {
    const first = activeApps.value[0].appKey
    onUpdate(first)
    emit('change', first)
  }
}

watch(
  () => props.modelValue,
  (value) => {
    if (props.remember && value) {
      setLastAppKey(value)
    }
  }
)

async function loadAppsOnMount() {
  await loadApps()
}

onMounted(loadAppsOnMount)
</script>
