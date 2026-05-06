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
    // openygt-dms-06：前端 5176，代理本实例后端 9096（协作规范 5170+NN / 9090+NN）
    port: 5176,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:9091',
        changeOrigin: true
      }
    }
  }
})
