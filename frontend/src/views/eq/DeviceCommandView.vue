<template>
  <div class="page-container">
    <div class="page-header-title">
      远程急停
      <span class="page-header-sub">远程启停单台设备、调参数、急停，不用跑车间按按钮</span>
    </div>
    <el-row :gutter="16" class="mt-4">
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>选择设备</template>
          <el-input v-model="deviceSearch" placeholder="搜索设备编码" clearable />
          <el-scrollbar height="400px" class="mt-2">
            <div
              v-for="d in filteredDevices"
              :key="d.deviceCode"
              class="device-item"
              :class="{ active: selectedDevice?.deviceCode === d.deviceCode }"
              @click="selectDevice(d)"
            >
              <div class="device-name">{{ d.name || d.deviceCode }}</div>
              <div class="device-code">{{ d.deviceCode }}</div>
              <el-tag :type="d.status === 'ONLINE' ? 'success' : 'danger'" size="small">
                {{ d.status === 'ONLINE' ? '在线' : '离线' }}
              </el-tag>
            </div>
          </el-scrollbar>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span>指令下发 — {{ selectedDevice?.name || selectedDevice?.deviceCode || '请选择设备' }}</span>
          </template>
          <div v-if="selectedDevice">
            <el-form label-width="100px">
              <el-form-item label="指令类型">
                <el-select v-model="commandForm.commandType" placeholder="选择指令" style="width: 220px">
                  <el-option label="启动泡药" value="START_SOAK" />
                  <el-option label="启动煎药" value="START_DECOCT" />
                  <el-option label="暂停" value="PAUSE" />
                  <el-option label="恢复" value="RESUME" />
                  <el-option label="急停" value="EMERGENCY_STOP" />
                  <el-option label="开始包装" value="START_PACKAGE" />
                  <el-option label="设置温度" value="SET_TEMP" />
                </el-select>
              </el-form-item>
              <el-form-item label="参数" v-if="commandForm.commandType === 'SET_TEMP'">
                <el-input-number v-model="commandForm.payload" :min="0" :max="150" />
                <span class="ml-2">°C</span>
              </el-form-item>
              <el-form-item>
                <el-button :type="resolveButtonType(commandForm.commandType)" @click="sendCommand">
                  下发指令
                </el-button>
              </el-form-item>
            </el-form>
          </div>
          <el-empty v-else description="请先选择左侧设备" />
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="never">
          <template #header>最近指令</template>
          <el-timeline>
            <el-timeline-item
              v-for="cmd in recentCommands"
              :key="cmd.id"
              :type="cmd.status === 'ACKED' ? 'success' : cmd.status === 'FAILED' ? 'danger' : 'primary'"
            >
              <div class="cmd-row">
                <span class="cmd-type">{{ formatCommandType(cmd.commandType) }}</span>
                <el-tag :type="resolveLevelTag(cmd.commandLevel)" size="small" class="ml-1">
                  {{ formatLevel(cmd.commandLevel) }}
                </el-tag>
              </div>
              <div class="cmd-meta">
                <el-tag :type="resolveRiskTag(cmd.riskLevel)" size="small">
                  {{ formatRisk(cmd.riskLevel) }}
                </el-tag>
                <el-tag v-if="cmd.requireConfirm === 1" type="danger" size="small" class="ml-1">需确认</el-tag>
                <el-tag v-else type="info" size="small" class="ml-1">无需确认</el-tag>
              </div>
              <div class="cmd-status">
                <el-tag :type="resolveStatusTag(cmd.status)" size="small">{{ formatStatus(cmd.status) }}</el-tag>
              </div>
              <div class="text-xs text-gray">{{ cmd.createdAt }}</div>
              <div v-if="cmd.failReason" class="text-xs text-danger">{{ cmd.failReason }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

interface Device {
  deviceCode: string
  name: string
  status: string
}

const deviceSearch = ref('')
const selectedDevice = ref<Device | null>(null)
const commandForm = reactive({ commandType: '', payload: 0 })
const recentCommands = ref<any[]>([])
const deviceList = ref<Device[]>([])

// 加载真实设备列表
async function loadDevices() {
  try {
    const res: any = await request.get('/v1/eq/devices', { params: { page: 1, size: 1000 } })
    const records = res.data?.records || []
    deviceList.value = records.map((d: any) => ({
      deviceCode: d.deviceCode,
      name: d.name || d.deviceCode,
      status: d.status || 'OFFLINE'
    }))
  } catch (e) {
    deviceList.value = []
  }
}
loadDevices()

const filteredDevices = computed(() => {
  if (!deviceSearch.value) return deviceList.value
  return deviceList.value.filter(d =>
    d.deviceCode.includes(deviceSearch.value) || (d.name && d.name.includes(deviceSearch.value))
  )
})

function selectDevice(d: Device) {
  selectedDevice.value = d
  loadRecentCommands(d.deviceCode)
}

async function loadRecentCommands(deviceCode: string) {
  try {
    const res: any = await request.get(`/v1/eq/commands/device/${deviceCode}/recent`, { params: { limit: 10 } })
    recentCommands.value = res.data || []
  } catch (e) {
    recentCommands.value = []
  }
}

function resolveButtonType(commandType: string) {
  if (commandType === 'EMERGENCY_STOP') return 'danger'
  return 'primary'
}

async function sendCommand() {
  if (!selectedDevice.value || !commandForm.commandType) {
    ElMessage.warning('请选择设备和指令类型')
    return
  }

  // 高风险指令二次确认
  if (commandForm.commandType === 'EMERGENCY_STOP') {
    try {
      await ElMessageBox.confirm(
        '【高风险操作】急停指令将立即停止设备运行，可能导致当前批次作废。是否确认执行？',
        '急停确认',
        { confirmButtonText: '确认急停', cancelButtonText: '取消', type: 'warning', confirmButtonClass: 'el-button--danger' }
      )
    } catch {
      return
    }
  }

  try {
    const res: any = await request.post('/v1/eq/commands', {
      deviceCode: selectedDevice.value.deviceCode,
      commandType: commandForm.commandType,
      payload: commandForm.payload ? String(commandForm.payload) : null
    })
    const commandId = res.data?.id
    if (commandId) {
      await request.post(`/v1/eq/commands/${commandId}/send`)
    }
    ElMessage.success('指令已下发')
    loadRecentCommands(selectedDevice.value.deviceCode)
  } catch (e: any) {
    ElMessage.error(e.message || '下发失败')
  }
}

// 格式化函数
function formatCommandType(type: string) {
  const map: Record<string, string> = {
    START: '启动',
    START_SOAK: '启动泡药',
    START_DECOCT: '启动煎药',
    PAUSE: '暂停',
    RESUME: '恢复',
    EMERGENCY_STOP: '急停',
    START_PACKAGE: '开始包装',
    SET_TEMP: '设置温度',
    ADD_LATE_REMIND: '加药提醒',
    CONFIRM_ADD_LATE: '确认加药'
  }
  return map[type] || type
}

function formatLevel(level: string) {
  const map: Record<string, string> = { NORMAL: '普通', IMPORTANT: '重要', CRITICAL: '严重' }
  return map[level] || level
}

function formatRisk(risk: string) {
  const map: Record<string, string> = { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' }
  return map[risk] || risk
}

function formatStatus(status: string) {
  const map: Record<string, string> = {
    PENDING: '待发送',
    SENT: '已发送',
    ACKED: '已确认',
    FAILED: '失败',
    TIMEOUT: '超时'
  }
  return map[status] || status
}

function resolveLevelTag(level: string) {
  if (level === 'CRITICAL') return 'danger'
  if (level === 'IMPORTANT') return 'warning'
  return 'info'
}

function resolveRiskTag(risk: string) {
  if (risk === 'HIGH') return 'danger'
  if (risk === 'MEDIUM') return 'warning'
  return 'info'
}

function resolveStatusTag(status: string) {
  if (status === 'ACKED') return 'success'
  if (status === 'FAILED' || status === 'TIMEOUT') return 'danger'
  if (status === 'SENT') return 'primary'
  return 'info'
}
</script>

<style scoped>
.page-container { padding: 16px; }
.mt-4 { margin-top: 16px; }
.ml-2 { margin-left: 8px; }
.ml-1 { margin-left: 4px; }
.device-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 6px;
  border: 1px solid var(--el-border-color-lighter);
}
.device-item:hover, .device-item.active {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
}
.device-name { font-weight: 500; }
.device-code { font-size: 12px; color: var(--el-text-color-secondary); margin: 4px 0; }
.text-xs { font-size: 12px; }
.text-gray { color: var(--el-text-color-secondary); }
.text-danger { color: var(--el-color-danger); }
.cmd-row { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.cmd-type { font-weight: 500; }
.cmd-meta { margin-top: 4px; }
.cmd-status { margin-top: 4px; }
</style>
