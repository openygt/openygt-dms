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
    port: 5176,
    host: '0.0.0.0',
    allowedHosts: true,
    historyApiFallback: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        rewrite: (path) => path
      }
    }
  }
})
