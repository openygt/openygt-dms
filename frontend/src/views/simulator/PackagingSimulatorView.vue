<template>
  <div class="app-container">
    <el-card v-for="d in devices" :key="d.id" class="device-card" :class="{ 'is-fault': d.status === 'FAULT' }">
      <template #header>
        <div class="device-header">
          <div class="device-info">
            <span class="device-name">{{ d.name }}</span>
            <el-tag size="small" :type="statusTag(d.status)">{{ statusText(d.status) }}</el-tag>
            <span class="device-code">{{ d.deviceCode }}</span>
          </div>
          <div class="header-actions">
            <el-button size="small" type="success" @click="powerOn(d)" :disabled="d.status === 'IDLE' || d.status === 'BUSY'">开机</el-button>
            <el-button size="small" type="info" @click="powerOff(d)" :disabled="d.status === 'OFFLINE'">关机</el-button>
          </div>
        </div>
      </template>
      <div class="device-body">
        <div class="info-row">
          <span>厂商：{{ d.manufacturer || '-' }}</span>
          <span>型号：{{ d.modelNum || '-' }}</span>
          <span>当前温度：<span class="temp-val" :class="tempClass(d.currentTemp)">{{ formatTemp(d.currentTemp) }}</span></span>
          <span>包装容量：{{ d.packageCapacity || '-' }} ml/袋</span>
          <span>已包装：<strong>{{ d.packageNum || 0 }}</strong> 袋</span>
        </div>
        <div class="actions-row">
          <el-button size="small" type="primary" @click="startPack(d)" :disabled="d.status !== 'IDLE'">启动包装</el-button>
          <el-button size="small" type="warning" @click="stopPack(d)" :disabled="d.status !== 'BUSY'">停止包装</el-button>
          <el-input-number v-model="packageCapacityInput[d.deviceCode]" :min="50" :max="500" :step="10" size="small" style="width: 130px" />
          <el-button size="small" @click="setCapacity(d)">设置容量</el-button>
          <el-dropdown @command="(cmd: string) => injectFault(d, cmd)">
            <el-button size="small" type="danger">故障注入<el-icon><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="PAPER_JAM">卡纸</el-dropdown-item>
                <el-dropdown-item command="SEAL_FAULT">封口故障</el-dropdown-item>
                <el-dropdown-item command="NO_BAG">缺袋</el-dropdown-item>
                <el-dropdown-item command="CLEAR">清除故障</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-card>
    <el-empty v-if="devices.length === 0" description="暂无包装机设备" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

interface Device {
  id: number; deviceCode: string; name: string; status: string
  manufacturer?: string; modelNum?: string; currentTemp?: number
  packageCapacity?: number; packageNum?: number
}

const devices = ref<Device[]>([])
const packageCapacityInput = reactive<Record<string, number>>({})

async function fetchDevices() {
  try {
    const res: any = await request.get('/v1/eq/devices', { params: { deviceType: 2, size: 50 } })
    devices.value = res.data?.records || []
    devices.value.forEach(d => { if (!packageCapacityInput[d.deviceCode]) packageCapacityInput[d.deviceCode] = d.packageCapacity || 200 })
  } catch (e) { ElMessage.error('获取设备列表失败') }
}

async function powerOn(d: Device) {
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'IDLE' })
  ElMessage.success(`${d.name} 已开机`)
  fetchDevices()
}
async function powerOff(d: Device) {
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'OFFLINE' })
  ElMessage.success(`${d.name} 已关机`)
  fetchDevices()
}
async function startPack(d: Device) {
  await request.post('/v1/eq/commands', { deviceCode: d.deviceCode, commandType: 'START_PACKAGING' })
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'BUSY' })
  ElMessage.success(`${d.name} 开始包装`)
  fetchDevices()
}
async function stopPack(d: Device) {
  await request.post('/v1/eq/commands', { deviceCode: d.deviceCode, commandType: 'STOP_PACKAGING' })
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'IDLE' })
  ElMessage.success(`${d.name} 停止包装`)
  fetchDevices()
}
async function setCapacity(d: Device) {
  const val = packageCapacityInput[d.deviceCode]
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, packageCapacity: val })
  ElMessage.success(`${d.name} 包装容量已设为 ${val} ml/袋`)
  fetchDevices()
}
async function injectFault(d: Device, code: string) {
  if (code === 'CLEAR') {
    await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'IDLE', detailStatus: 'IDLE', faultCode: null })
    ElMessage.success(`${d.name} 故障已清除`)
  } else {
    await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'FAULT', detailStatus: 'FAULT', faultCode: code })
    ElMessage.error(`${d.name} 故障注入：${code}`)
  }
  fetchDevices()
}

function formatTemp(t?: number) { return t != null ? t + ' °C' : '-' }
function tempClass(t?: number) {
  if (t == null) return ''
  if (t >= 90) return 'temp-hot'
  if (t >= 60) return 'temp-warm'
  return ''
}
function statusTag(s: string) {
  if (s === 'IDLE' || s === 'ONLINE') return 'success'
  if (s === 'OFFLINE') return 'info'
  if (s === 'BUSY' || s === 'RUNNING') return 'warning'
  return 'danger'
}
function statusText(s: string) {
  const map: Record<string, string> = { IDLE: '空闲', ONLINE: '在线', OFFLINE: '离线', BUSY: '运行中', RUNNING: '运行中', FAULT: '故障' }
  return map[s] || s
}

onMounted(fetchDevices)
</script>

<style scoped>
.device-card { margin-bottom: 16px; }
.device-card.is-fault { border-color: var(--el-color-danger); }
.device-header { display: flex; justify-content: space-between; align-items: center; }
.device-info { display: flex; align-items: center; gap: 10px; }
.device-name { font-weight: 600; font-size: 16px; }
.device-code { font-size: 13px; color: var(--el-text-color-secondary); }
.header-actions { display: flex; gap: 8px; }
.device-body { display: flex; flex-direction: column; gap: 12px; }
.info-row { display: flex; flex-wrap: wrap; gap: 24px; color: var(--el-text-color-regular); font-size: 14px; }
.actions-row { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.temp-val { font-weight: 600; }
.temp-hot { color: var(--el-color-danger); }
.temp-warm { color: var(--el-color-warning); }
</style>
