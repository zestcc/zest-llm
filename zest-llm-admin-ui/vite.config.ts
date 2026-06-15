import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { existsSync, readFileSync } from 'fs'
import { resolve } from 'path'

function resolveAdminApiTarget(): string {
  const envPort = process.env.VITE_ADMIN_PORT
  if (envPort && /^\d+$/.test(envPort)) {
    return `http://127.0.0.1:${envPort}`
  }
  const portFile = resolve(__dirname, '../deploy/logs/pids/admin-local.port')
  if (existsSync(portFile)) {
    const port = parseInt(readFileSync(portFile, 'utf8').trim(), 10)
    if (port > 0) {
      return `http://127.0.0.1:${port}`
    }
  }
  return 'http://127.0.0.1:8088'
}

const devPort = parseInt(process.env.VITE_DEV_PORT || '5174', 10)
const adminTarget = resolveAdminApiTarget()

export default defineConfig({
  plugins: [vue()],
  base: '/',
  build: {
    outDir: resolve(__dirname, '../zest-llm-admin/src/main/resources/static'),
    emptyOutDir: true,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/echarts')) {
            return 'echarts'
          }
          if (id.includes('node_modules/element-plus') || id.includes('node_modules/@element-plus')) {
            return 'element-plus'
          }
          if (id.includes('node_modules/vue') || id.includes('node_modules/@vue') || id.includes('node_modules/vue-router')) {
            return 'vue-vendor'
          }
        }
      }
    }
  },
  server: {
    host: '127.0.0.1',
    port: devPort,
    strictPort: true,
    proxy: {
      '/api': adminTarget,
      '/v1': adminTarget
    }
  }
})
