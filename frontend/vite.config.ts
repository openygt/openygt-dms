import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  define: {
    global: 'globalThis'
  },
  build: {
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        manualChunks: {
          echarts: ['echarts'],
          vendor: ['vue', 'vue-router', 'pinia', 'element-plus', '@element-plus/icons-vue'],
          websocket: ['@stomp/stompjs', 'sockjs-client']
        }
      }
    }
  },
  server: {
    // openygt-dms-03：前端 5173，代理本实例后端 9093（协作规范 5170+NN / 9090+NN）
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:9093',
        changeOrigin: true
      }
    }
  }
})
