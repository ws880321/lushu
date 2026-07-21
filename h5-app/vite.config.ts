import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  base: '/app/',
  plugins: [vue()],
  server: {
    port: 3001,
    proxy: {
      '/api': {
        target: 'https://www.wychen.net',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'https://www.wychen.net',
        changeOrigin: true,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: { vant: ['vant'] },
      },
    },
  },
})
