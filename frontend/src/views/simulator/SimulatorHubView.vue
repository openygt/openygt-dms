<template>
  <div class="app-container">
    <div class="hub-grid">
      <el-card v-for="item in items" :key="item.type" class="hub-card" shadow="hover">
        <div class="hub-card-inner">
          <div class="hub-card-header">
            <el-icon :size="28" :color="item.color"><component :is="item.icon" /></el-icon>
            <span class="hub-card-title">{{ item.label }}</span>
          </div>
          <el-divider />
          <div class="hub-stats">
            <div class="hub-stat">
              <span class="hub-stat-num" :style="{ color: item.color }">{{ item.online }}</span>
              <span class="hub-stat-label">在线</span>
            </div>
            <el-divider direction="vertical" />
            <div class="hub-stat">
              <span class="hub-stat-num">{{ item.total }}</span>
              <span class="hub-stat-label">总数</span>
            </div>
            <el-divider direction="vertical" />
            <div class="hub-stat">
              <span class="hub-stat-num" style="color: var(--el-color-warning)">{{ item.busy }}</span>
              <span class="hub-stat-label">运行中</span>
            </div>
            <el-divider direction="vertical" />
            <div class="hub-stat">
              <span class="hub-stat-num" style="color: var(--el-color-danger)">{{ item.fault }}</span>
              <span class="hub-stat-label">故障</span>
            </div>
          </div>
          <el-button type="primary" style="margin-top: 12px; width: 100%" @click="$router.push(item.path)">
            进入模拟
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request from '@/api/request'

interface HubItem {
  type: number
  label: string
  icon: string
  color: string
  path: string
  online: number
  total: number
  busy: number
  fault: number
}

const items = reactive<HubItem[]>([
  { type: 1, label: '煎药机', icon: 'Monitor', color: 'var(--el-color-primary)', path: '/simulator/decoction', online: 0, total: 0, busy: 0, fault: 0 },
  { type: 2, label: '包装机', icon: 'Box', color: 'var(--el-color-success)', path: '/simulator/packaging', online: 0, total: 0, busy: 0, fault: 0 },
  { type: 5, label: '标签打印机', icon: 'Tickets', color: 'var(--el-color-warning)', path: '/simulator/label-printer', online: 0, total: 0, busy: 0, fault: 0 },
  { type: 3, label: '打印机', icon: 'Printer', color: 'var(--el-color-danger)', path: '/simulator/printer', online: 0, total: 0, busy: 0, fault: 0 },
  { type: 4, label: 'PDA', icon: 'Iphone', color: 'var(--el-color-info)', path: '/simulator/pda', online: 0, total: 0, busy: 0, fault: 0 },
])

async function loadStats() {
  for (const item of items) {
    try {
      const res: any = await request.get('/v1/eq/devices', { params: { deviceType: item.type, size: 200 } })
      const list = res.data?.records || []
      item.total = list.length
      item.online = list.filter((d: any) => d.status === 'IDLE' || d.status === 'ONLINE').length
      item.busy = list.filter((d: any) => d.status === 'BUSY' || d.status === 'RUNNING' || d.status === 'WORKING').length
      item.fault = list.filter((d: any) => d.status === 'FAULT' || d.status === 'ERROR').length
    } catch (e) { /* ignore */ }
  }
}

onMounted(loadStats)
</script>

<style scoped>
.hub-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
.hub-card {
  cursor: default;
}
.hub-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.hub-card-title {
  font-size: 18px;
  font-weight: 600;
}
.hub-stats {
  display: flex;
  justify-content: space-around;
  align-items: center;
}
.hub-stat {
  text-align: center;
}
.hub-stat-num {
  font-size: 24px;
  font-weight: 700;
  display: block;
  line-height: 1.2;
}
.hub-stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
