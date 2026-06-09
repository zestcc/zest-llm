<template>
  <div class="captcha-wrapper">
    <canvas
      ref="canvasRef"
      :width="width"
      :height="height"
      class="captcha-canvas"
      @click="refresh"
    />
    <el-button text class="refresh-btn" @click="refresh">
      <el-icon><Refresh /></el-icon>
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    width?: number
    height?: number
  }>(),
  { width: 120, height: 40 }
)

const emit = defineEmits<{ 'update:code': [code: string] }>()

const canvasRef = ref<HTMLCanvasElement>()
let currentCode = ''

const CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'

function randomChar() {
  return CHARS[Math.floor(Math.random() * CHARS.length)]
}

function randomColor() {
  const r = Math.floor(Math.random() * 180 + 40)
  const g = Math.floor(Math.random() * 180 + 40)
  const b = Math.floor(Math.random() * 180 + 40)
  return `rgb(${r}, ${g}, ${b})`
}

function draw() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const { width, height } = props
  ctx.fillStyle = '#f0f2f5'
  ctx.fillRect(0, 0, width, height)

  currentCode = ''
  for (let i = 0; i < 4; i++) currentCode += randomChar()
  emit('update:code', currentCode)

  const chars = currentCode.split('')
  const charWidth = width / chars.length
  ctx.textBaseline = 'middle'
  ctx.font = 'bold 22px cursive'

  chars.forEach((char, i) => {
    const x = charWidth * i + charWidth / 2
    const y = height / 2 + Math.random() * 6 - 3
    ctx.fillStyle = randomColor()
    ctx.save()
    ctx.translate(x, y)
    ctx.rotate((Math.random() - 0.5) * 0.4)
    ctx.fillText(char, -8, 0)
    ctx.restore()
  })

  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = randomColor()
    ctx.lineWidth = 1
    ctx.globalAlpha = 0.4
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }
  ctx.globalAlpha = 1

  for (let i = 0; i < 30; i++) {
    ctx.fillStyle = randomColor()
    ctx.globalAlpha = 0.3
    ctx.beginPath()
    ctx.arc(Math.random() * width, Math.random() * height, Math.random() * 2 + 1, 0, Math.PI * 2)
    ctx.fill()
  }
  ctx.globalAlpha = 1
}

function refresh() {
  draw()
}

defineExpose({ refresh })

onMounted(draw)
</script>

<style scoped>
.captcha-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.captcha-canvas {
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid #d9dde3;
}

.refresh-btn {
  padding: 4px;
  font-size: 16px;
}
</style>
