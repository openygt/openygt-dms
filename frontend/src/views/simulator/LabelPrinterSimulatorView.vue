<template>
  <div class="app-container" v-loading="loading">
    <el-card v-for="d in devices" :key="d.id" class="device-card" :class="{ 'is-fault': d.status === 'FAULT' }">
      <template #header>
        <div class="device-header">
          <div class="device-info">
            <span class="device-name">{{ d.name }}</span>
            <el-tag size="small" :type="statusTag(d.status)">{{ statusText(d.status) }}</el-tag>
            <span class="device-code">{{ d.deviceCode }}</span>
            <el-tag v-if="d.labelMode" size="small" type="info">{{ labelModeText(d.labelMode) }}</el-tag>
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
        </div>
        <div class="actions-row">
          <el-input v-model="labelForm[d.deviceCode].prescriptionNo" placeholder="处方号" size="small" style="width: 140px" />
          <el-input v-model="labelForm[d.deviceCode].patientName" placeholder="患者名" size="small" style="width: 120px" />
          <el-button size="small" type="primary" @click="printLabel(d)" :disabled="d.status !== 'IDLE'">打印标签</el-button>
          <el-input-number v-model="labelForm[d.deviceCode].batchCount" :min="1" :max="100" size="small" style="width: 100px" />
          <el-button size="small" type="primary" @click="batchPrint(d)" :disabled="d.status !== 'IDLE'">批量打印</el-button>
          <el-select v-model="labelForm[d.deviceCode].mode" size="small" style="width: 130px">
            <el-option label="处方标签" value="PRESCRIPTION" />
            <el-option label="药品标签" value="MEDICINE" />
            <el-option label="物流标签" value="LOGISTICS" />
          </el-select>
          <el-button size="small" @click="setMode(d)">切换模式</el-button>
          <el-dropdown @command="(cmd: string) => injectFault(d, cmd)">
            <el-button size="small" type="danger">故障注入<el-icon><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="NO_PAPER">缺纸</el-dropdown-item>
                <el-dropdown-item command="PAPER_JAM">卡纸</el-dropdown-item>
                <el-dropdown-item command="RIBBON_FAULT">碳带故障</el-dropdown-item>
                <el-dropdown-item command="HEAD_OVERHEAT">打印头过热</el-dropdown-item>
                <el-dropdown-item command="CLEAR">清除故障</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-card>
    <el-empty v-if="devices.length === 0" description="暂无标签打印机设备" />

    <!-- 打印队列 -->
    <el-card v-if="devices.length > 0" style="margin-top: 16px">
      <template #header>打印队列</template>
      <el-table :data="printQueue" size="small">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="taskId" label="任务号" width="100" />
        <el-table-column prop="printType" label="类型" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'COMPLETED' ? 'success' : row.status === 'FAILED' ? 'danger' : 'warning'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deviceCode" label="打印机" />
        <el-table-column prop="retryCount" label="重试次数" width="80" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useDeviceStore, type DeviceState } from '@/stores/device'

const deviceStore = useDeviceStore()

const loading = ref(false)
const printQueue = ref<any[]>([])
const labelForm = reactive<Record<string, any>>({})

const devices = computed(() =>
  deviceStore.deviceList.filter(d => d.deviceType === 5)
)

function getForm(d: DeviceState) {
  if (!labelForm[d.deviceCode]) {
    labelForm[d.deviceCode] = { prescriptionNo: '', patientName: '', batchCount: 10, mode: d.labelMode || 'PRESCRIPTION' }
  }
  return labelForm[d.deviceCode]
}

async function fetchDevices() {
  loading.value = true
  try {
    const res: any = await request.get('/v1/eq/devices', { params: { deviceType: 5, size: 50 } })
    const records = res.data?.records || []
    records.forEach((d: any) => {
      deviceStore.updateDevice(d.deviceCode, {
        deviceCode: d.deviceCode, name: d.name, status: d.status || 'IDLE',
        detailStatus: d.detailStatus || d.status || 'IDLE', deviceType: 5,
        manufacturer: d.manufacturer, modelNum: d.modelNum,
        labelMode: d.labelMode, lastHeartbeat: d.lastHeartbeat,
      })
    })
  } catch (e) { ElMessage.error('获取设备列表失败') }
  finally { loading.value = false }
}

async function fetchPrintQueue() {
  try {
    const res: any = await request.get('/v1/prt/queue')
    printQueue.value = res.data || []
  } catch (e) { /* ignore */ }
}

async function powerOn(d: DeviceState) {
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'IDLE' })
  deviceStore.updateDevice(d.deviceCode, { status: 'IDLE', detailStatus: 'IDLE' })
  ElMessage.success(`${d.name} 已开机`)
}
async function powerOff(d: DeviceState) {
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'OFFLINE' })
  deviceStore.updateDevice(d.deviceCode, { status: 'OFFLINE', detailStatus: 'OFFLINE' })
  ElMessage.success(`${d.name} 已关机`)
}
async function printLabel(d: DeviceState) {
  const f = getForm(d)
  await request.post('/v1/eq/commands', {
    deviceCode: d.deviceCode,
    commandType: 'PRINT_LABEL',
    payload: JSON.stringify({ prescriptionNo: f.prescriptionNo, patientName: f.patientName })
  })
  ElMessage.success(`已向 ${d.name} 发送标签打印指令`)
  fetchPrintQueue()
}
async function batchPrint(d: DeviceState) {
  const f = getForm(d)
  await request.post('/v1/eq/commands', {
    deviceCode: d.deviceCode,
    commandType: 'BATCH_PRINT',
    payload: JSON.stringify({ count: f.batchCount })
  })
  ElMessage.success(`${d.name} 批量打印 ${f.batchCount} 张`)
  fetchPrintQueue()
}
async function setMode(d: DeviceState) {
  const f = getForm(d)
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, labelMode: f.mode })
  deviceStore.updateDevice(d.deviceCode, { labelMode: f.mode })
  ElMessage.success(`${d.name} 模式已切换为 ${f.mode}`)
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
function labelModeText(m: string) {
  const map: Record<string, string> = { PRESCRIPTION: '处方标签', MEDICINE: '药品标签', LOGISTICS: '物流标签' }
  return map[m] || m
}

onMounted(() => { fetchDevices(); fetchPrintQueue() })
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
.info-row { display: flex; gap: 24px; color: var(--el-text-color-regular); font-size: 14px; }
.actions-row { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
</style>
