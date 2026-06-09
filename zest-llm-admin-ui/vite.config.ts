import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  base: '/',
  build: {
    outDir: resolve(__dirname, '../zest-llm-admin/src/main/resources/static'),
    emptyOutDir: true
  },
  server: {
    port: 5174,
    proxy: {
      '/api': 'http://127.0.0.1:8088',
      '/v1': 'http://127.0.0.1:8088'
    }
  }
})
