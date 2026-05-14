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
        </div>
        <div class="actions-row">
          <el-button size="small" type="primary" @click="printWorkorder(d)" :disabled="d.status !== 'IDLE'">打印工单</el-button>
          <el-button size="small" type="warning" @click="cancelPrint(d)" :disabled="d.status !== 'BUSY'">取消任务</el-button>
          <el-button size="small" @click="retryPrint(d)">重试打印</el-button>
          <el-dropdown @command="(cmd: string) => injectFault(d, cmd)">
            <el-button size="small" type="danger">故障注入<el-icon><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="NO_PAPER">缺纸</el-dropdown-item>
                <el-dropdown-item command="PAPER_JAM">卡纸</el-dropdown-item>
                <el-dropdown-item command="LOW_TONER">墨粉不足</el-dropdown-item>
                <el-dropdown-item command="CLEAR">清除故障</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-card>
    <el-empty v-if="devices.length === 0" description="暂无激光打印机设备" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

interface Device {
  id: number; deviceCode: string; name: string; status: string
  manufacturer?: string; modelNum?: string
}

const devices = ref<Device[]>([])

async function fetchDevices() {
  try {
    const res: any = await request.get('/v1/eq/devices', { params: { deviceType: 3, size: 50 } })
    devices.value = res.data?.records || []
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
async function printWorkorder(d: Device) {
  await request.post('/v1/eq/commands', { deviceCode: d.deviceCode, commandType: 'PRINT_WORKORDER' })
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'BUSY' })
  ElMessage.success(`已向 ${d.name} 发送工单打印指令`)
  fetchDevices()
}
async function cancelPrint(d: Device) {
  await request.post('/v1/eq/commands', { deviceCode: d.deviceCode, commandType: 'CANCEL_PRINT' })
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'IDLE' })
  ElMessage.success('已取消打印任务')
  fetchDevices()
}
async function retryPrint(d: Device) {
  try {
    await request.post(`/v1/prt/tasks/${d.id}/retry`, null, { params: { deviceCode: d.deviceCode, operatorId: 'SIM' } })
    ElMessage.success('重试成功')
  } catch (e: any) {
    ElMessage.warning('重试失败（无失败任务）')
  }
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

function statusTag(s: string) {
  if (s === 'IDLE' || s === 'ONLINE') return 'success'
  if (s === 'OFFLINE') return 'info'
  if (s === 'BUSY' || s === 'RUNNING') return 'warning'
  return 'danger'
}
function statusText(s: string) {
  const map: Record<string, string> = { IDLE: '空闲', ONLINE: '在线', OFFLINE: '离线', BUSY: '运行中', RUNNING: '运行中', FAULT: '故障', ERROR: '故障' }
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
.info-row { display: flex; gap: 24px; color: var(--el-text-color-regular); font-size: 14px; }
.actions-row { display: flex; flex-wrap: wrap; gap: 8px; }
</style>
