<template>
  <div class="app-container" v-loading="loading">
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
import { ref, onMounted, reactive, computed } from 'vue'
import request from '@/api/request'
import { useDeviceStore } from '@/stores/device'

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

const deviceStore = useDeviceStore()

const loading = ref(false)
const items = reactive<HubItem[]>([
  { type: 1, label: '煎药机', icon: 'Monitor', color: 'var(--el-color-primary)', path: '/simulator/decoction', online: 0, total: 0, busy: 0, fault: 0 },
  { type: 2, label: '包装机', icon: 'Box', color: 'var(--el-color-success)', path: '/simulator/packaging', online: 0, total: 0, busy: 0, fault: 0 },
  { type: 5, label: '标签打印机', icon: 'Tickets', color: 'var(--el-color-warning)', path: '/simulator/label-printer', online: 0, total: 0, busy: 0, fault: 0 },
  { type: 3, label: '打印机', icon: 'Printer', color: 'var(--el-color-danger)', path: '/simulator/printer', online: 0, total: 0, busy: 0, fault: 0 },
  { type: 4, label: 'PDA', icon: 'Iphone', color: 'var(--el-color-info)', path: '/simulator/pda', online: 0, total: 0, busy: 0, fault: 0 },
])

function updateFromStore() {
  for (const item of items) {
    const list = deviceStore.deviceList.filter(d => d.deviceType === item.type)
    item.total = list.length
    item.online = list.filter(d => d.status === 'IDLE' || d.status === 'ONLINE').length
    item.busy = list.filter(d => d.status === 'BUSY' || d.status === 'RUNNING' || d.status === 'WORKING').length
    item.fault = list.filter(d => d.status === 'FAULT' || d.status === 'ERROR').length
  }
}

// Watch store changes reactively
const storeWatcher = computed(() => deviceStore.deviceList.length)
computed(() => { storeWatcher.value; updateFromStore(); return null })

async function loadStats() {
  loading.value = true
  // Fetch all device types to populate store
  try {
    const results = await Promise.allSettled(
      items.map(item =>
        request.get('/v1/eq/devices', { params: { deviceType: item.type, size: 200 } })
      )
    )
    results.forEach((r, i) => {
      if (r.status === 'fulfilled') {
        const records = (r.value as any).data?.records || []
        records.forEach((d: any) => {
          deviceStore.updateDevice(d.deviceCode, {
            deviceCode: d.deviceCode, name: d.name, status: d.status || 'IDLE',
            detailStatus: d.detailStatus || d.status || 'IDLE', deviceType: items[i].type,
            manufacturer: d.manufacturer, modelNum: d.modelNum,
            currentTemp: d.currentTemp, targetTemp: d.targetTemp,
            packageCapacity: d.packageCapacity, packageNum: d.packageNum,
            labelMode: d.labelMode, communicationId: d.communicationId,
            lastHeartbeat: d.lastHeartbeat,
          })
        })
      }
    })
  } finally {
    updateFromStore()
    loading.value = false
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
