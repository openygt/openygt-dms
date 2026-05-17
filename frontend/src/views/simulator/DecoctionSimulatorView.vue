<template>
  <div class="app-container">
    <el-tabs v-model="activeBrand" type="card">
      <el-tab-pane v-for="brand in brands" :key="brand" :label="brand" :name="brand">
        <div class="device-list" v-loading="loading">
          <el-card
            v-for="d in brandDevices(brand)"
            :key="d.id"
            class="device-card"
            :class="{ 'is-fault': d.status === 'FAULT', 'is-busy': d.status === 'BUSY' }"
          >
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
                <span>厂商：{{ d.manufacturer || brand }}</span>
                <span>型号：{{ d.modelNum || '-' }}</span>
                <span>
                  当前温度：
                  <span class="temp-val" :class="tempClass(d.currentTemp)">{{ formatTemp(d.currentTemp) }}</span>
                </span>
                <span v-if="d.targetTemp">目标温度：{{ d.targetTemp }} °C</span>
                <span v-if="d.currentPrescriptionCode">处方：{{ d.currentPrescriptionCode }}</span>
              </div>
              <div class="info-row" v-if="d.decoctMode != null || d.pressureMode != null || d.slowFireTime != null">
                <span>煎煮模式：{{ decoctModeText(d.decoctMode) }}</span>
                <span>压力模式：{{ pressureModeText(d.pressureMode) }}</span>
                <span>文火时间：{{ d.slowFireTime || 0 }} 分钟</span>
              </div>
              <div class="temp-control">
                <span class="temp-label">目标温度</span>
                <el-slider
                  v-model="tempSliders[d.deviceCode]"
                  :min="25" :max="150" :step="1"
                  show-input
                  style="flex: 1; max-width: 400px"
                />
                <el-button size="small" @click="setTemp(d)">设置</el-button>
              </div>
              <div class="actions-row">
                <el-button size="small" type="primary" @click="startDecoct(d)" :disabled="d.status !== 'IDLE'">启动煎药</el-button>
                <el-button size="small" type="warning" @click="stopDecoct(d)" :disabled="d.status !== 'BUSY'">停止煎药</el-button>
                <el-button size="small" type="success" @click="addWater(d)">加水</el-button>
                <el-radio-group v-model="fireMode[d.deviceCode]" size="small">
                  <el-radio-button value="FAST">武火</el-radio-button>
                  <el-radio-button value="SLOW">文火</el-radio-button>
                </el-radio-group>
                <el-button size="small" @click="switchFire(d)">切换</el-button>
                <el-dropdown @command="(cmd: string) => injectFault(d, cmd)">
                  <el-button size="small" type="danger">故障注入<el-icon><ArrowDown /></el-icon></el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="DRY_BURN">干烧</el-dropdown-item>
                      <el-dropdown-item command="PROBE_OPEN">探头断开</el-dropdown-item>
                      <el-dropdown-item command="NO_WATER">缺水</el-dropdown-item>
                      <el-dropdown-item command="CLEAR" divided>清除故障</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </el-card>
          <el-empty v-if="brandDevices(brand).length === 0" :description="brand + ' 暂无煎药机'" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useDeviceStore, type DeviceState } from '@/stores/device'

const deviceStore = useDeviceStore()

const brands = ['全部', '东华原', '卫康', '朋霖', '和利康源']
const activeBrand = ref('全部')
const loading = ref(false)
const tempSliders = reactive<Record<string, number>>({})
const fireMode = reactive<Record<string, string>>({})

const allDevices = computed(() =>
  deviceStore.deviceList.filter(d => d.deviceType === 1 || d.deviceType == null)
)

function brandDevices(brand: string) {
  const list = allDevices.value as DeviceState[]
  if (brand === '全部') return list
  return list.filter(d => d.manufacturer === brand)
}

async function fetchDevices() {
  loading.value = true
  try {
    const res: any = await request.get('/v1/eq/devices', { params: { deviceType: 1, size: 200 } })
    const records = res.data?.records || []
    records.forEach((d: any) => {
      deviceStore.updateDevice(d.deviceCode, {
        deviceCode: d.deviceCode, name: d.name, status: d.status || 'IDLE',
        detailStatus: d.detailStatus || d.status || 'IDLE', deviceType: 1,
        currentTemp: d.currentTemp, targetTemp: d.targetTemp,
        currentPrescriptionCode: d.currentPrescriptionCode,
        manufacturer: d.manufacturer, modelNum: d.modelNum,
        decoctMode: d.decoctMode, pressureMode: d.pressureMode, slowFireTime: d.slowFireTime,
        lastHeartbeat: d.lastHeartbeat,
      })
      if (!tempSliders[d.deviceCode]) tempSliders[d.deviceCode] = d.targetTemp || 100
      if (!fireMode[d.deviceCode]) fireMode[d.deviceCode] = 'SLOW'
    })
  } catch (e) { ElMessage.error('获取设备列表失败') }
  finally { loading.value = false }
}

async function powerOn(d: DeviceState) {
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'IDLE' })
  deviceStore.updateDevice(d.deviceCode, { status: 'IDLE', detailStatus: 'IDLE' })
  ElMessage.success(`${d.name} 已开机`)
  fetchDevices()
}
async function powerOff(d: DeviceState) {
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'OFFLINE' })
  deviceStore.updateDevice(d.deviceCode, { status: 'OFFLINE', detailStatus: 'OFFLINE' })
  ElMessage.success(`${d.name} 已关机`)
  fetchDevices()
}
async function startDecoct(d: DeviceState) {
  const target = tempSliders[d.deviceCode]
  const mode = fireMode[d.deviceCode]
  await request.post('/v1/eq/commands', {
    deviceCode: d.deviceCode,
    commandType: 'START_DECOCT',
    payload: JSON.stringify({ targetTemp: target, fireMode: mode })
  })
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'BUSY', targetTemp: target })
  deviceStore.updateDevice(d.deviceCode, { status: 'BUSY', detailStatus: 'BUSY', targetTemp: target })
  ElMessage.success(`${d.name} 开始煎药，目标温度 ${target}°C`)
  fetchDevices()
}
async function stopDecoct(d: DeviceState) {
  await request.post('/v1/eq/commands', { deviceCode: d.deviceCode, commandType: 'STOP_DECOCT' })
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'IDLE' })
  deviceStore.updateDevice(d.deviceCode, { status: 'IDLE', detailStatus: 'IDLE' })
  ElMessage.success(`${d.name} 停止煎药`)
  fetchDevices()
}
async function addWater(d: DeviceState) {
  await request.post('/v1/eq/commands', { deviceCode: d.deviceCode, commandType: 'ADD_WATER' })
  ElMessage.success(`${d.name} 加水指令已发送`)
}
async function setTemp(d: DeviceState) {
  const target = tempSliders[d.deviceCode]
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, targetTemp: target })
  deviceStore.updateDevice(d.deviceCode, { targetTemp: target })
  ElMessage.success(`${d.name} 目标温度已设为 ${target}°C`)
}
async function switchFire(d: DeviceState) {
  const mode = fireMode[d.deviceCode]
  await request.post('/v1/eq/commands', {
    deviceCode: d.deviceCode,
    commandType: 'SWITCH_FIRE_MODE',
    payload: mode
  })
  ElMessage.success(`${d.name} 已切换为${mode === 'FAST' ? '武火' : '文火'}`)
}
async function injectFault(d: DeviceState, code: string) {
  if (code === 'CLEAR') {
    await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'IDLE', detailStatus: 'IDLE', faultCode: null })
    deviceStore.updateDevice(d.deviceCode, { status: 'IDLE', detailStatus: 'IDLE', faultCode: undefined })
    ElMessage.success(`${d.name} 故障已清除`)
  } else {
    await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'FAULT', detailStatus: 'FAULT', faultCode: code })
    deviceStore.updateDevice(d.deviceCode, { status: 'FAULT', detailStatus: 'FAULT', faultCode: code })
    ElMessage.error(`${d.name} 故障注入：${code}`)
  }
  fetchDevices()
}

function formatTemp(t?: number) { return t != null ? t + ' °C' : '-' }
function tempClass(t?: number) {
  if (t == null) return ''
  if (t >= 100) return 'temp-critical'
  if (t >= 70) return 'temp-hot'
  if (t >= 50) return 'temp-warm'
  return ''
}
function statusTag(s: string) {
  if (s === 'IDLE' || s === 'ONLINE') return 'success'
  if (s === 'OFFLINE') return 'info'
  if (s === 'BUSY' || s === 'RUNNING') return 'warning'
  return 'danger'
}
function statusText(s: string) {
  const map: Record<string, string> = { IDLE: '空闲', ONLINE: '在线', OFFLINE: '离线', BUSY: '运行中', FAULT: '故障' }
  return map[s] || s
}
function decoctModeText(m?: number) {
  const map: Record<number, string> = { 1: '常压煎药', 2: '高压煎药', 3: '减压煎药' }
  return m != null ? map[m] : '-'
}
function pressureModeText(m?: number) {
  const map: Record<number, string> = { 0: '无压力', 1: '低压', 2: '中压', 3: '高压' }
  return m != null ? map[m] : '-'
}

onMounted(fetchDevices)
</script>

<style scoped>
.device-card { margin-bottom: 16px; }
.device-card.is-fault { border-color: var(--el-color-danger); }
.device-card.is-busy { border-color: var(--el-color-warning); }
.device-header { display: flex; justify-content: space-between; align-items: center; }
.device-info { display: flex; align-items: center; gap: 10px; }
.device-name { font-weight: 600; font-size: 16px; }
.device-code { font-size: 13px; color: var(--el-text-color-secondary); }
.header-actions { display: flex; gap: 8px; }
.device-body { display: flex; flex-direction: column; gap: 12px; }
.info-row { display: flex; flex-wrap: wrap; gap: 24px; color: var(--el-text-color-regular); font-size: 14px; }
.temp-control { display: flex; align-items: center; gap: 12px; }
.temp-label { font-size: 13px; color: var(--el-text-color-secondary); white-space: nowrap; }
.actions-row { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.temp-val { font-weight: 600; }
.temp-critical { color: var(--el-color-danger); animation: blink 1s infinite; }
.temp-hot { color: var(--el-color-warning); }
.temp-warm { color: var(--el-color-primary); }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
</style>
