<template>
  <div class="page-container">
    <div class="page-header-title">设备操控：<span class="page-header-sub">远程启停单台设备、调参数、急停，不用跑车间按按钮</span></div>
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
              <div class="device-name">{{ d.name }}</div>
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
            <span>指令下发 — {{ selectedDevice?.name || '请选择设备' }}</span>
          </template>
          <div v-if="selectedDevice">
            <el-form label-width="100px">
              <el-form-item label="指令类型">
                <el-select v-model="commandForm.commandType" placeholder="选择指令" style="width: 200px">
                  <el-option label="启动" value="START" />
                  <el-option label="暂停" value="PAUSE" />
                  <el-option label="急停" value="EMERGENCY_STOP" />
                  <el-option label="调温" value="SET_TEMPERATURE" />
                  <el-option label="调压" value="SET_PRESSURE" />
                </el-select>
              </el-form-item>
              <el-form-item label="参数" v-if="commandForm.commandType === 'SET_TEMPERATURE'">
                <el-input-number v-model="commandForm.payload" :min="0" :max="150" />
                <span class="ml-2">°C</span>
              </el-form-item>
              <el-form-item label="参数" v-if="commandForm.commandType === 'SET_PRESSURE'">
                <el-input-number v-model="commandForm.payload" :min="0" :max="10" />
                <span class="ml-2">kPa</span>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="sendCommand">下发指令</el-button>
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
              :type="cmd.status === 'SUCCESS' ? 'success' : cmd.status === 'FAILED' ? 'danger' : 'primary'"
            >
              <div>{{ cmd.commandType }}</div>
              <div class="text-xs text-gray">{{ cmd.createdAt }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
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

// 模拟设备列表，实际应调接口
const deviceList = ref<Device[]>([
  { deviceCode: 'JY-001', name: '煎药机01', status: 'ONLINE' },
  { deviceCode: 'JY-002', name: '煎药机02', status: 'ONLINE' },
  { deviceCode: 'JY-003', name: '煎药机03', status: 'OFFLINE' },
  { deviceCode: 'BZ-001', name: '包装机01', status: 'ONLINE' },
])

const filteredDevices = computed(() => {
  if (!deviceSearch.value) return deviceList.value
  return deviceList.value.filter(d =>
    d.deviceCode.includes(deviceSearch.value) || d.name.includes(deviceSearch.value)
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

async function sendCommand() {
  if (!selectedDevice.value || !commandForm.commandType) {
    ElMessage.warning('请选择设备和指令类型')
    return
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
</script>

<style scoped>
.page-container { padding: 16px; }
.mt-4 { margin-top: 16px; }
.ml-2 { margin-left: 8px; }
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
</style>
