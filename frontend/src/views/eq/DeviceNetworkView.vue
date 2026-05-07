<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" class="network-tabs">
      <!-- Tab 1: 协议配置 -->
      <el-tab-pane label="协议配置" name="config">
        <el-row :gutter="16">
          <!-- 左侧设备列表 -->
          <el-col :xs="24" :sm="8" :md="7" :lg="6">
            <el-card shadow="never" class="device-list-card">
              <template #header>
                <el-input v-model="deviceKeyword" placeholder="搜索设备编码/名称" clearable size="small" />
              </template>
              <div
                v-for="d in filteredDevices"
                :key="d.id"
                class="device-item"
                :class="{ active: selectedDevice?.id === d.id }"
                @click="selectDevice(d)"
              >
                <div class="device-name">{{ d.deviceName || d.name }}</div>
                <div class="device-code">{{ d.deviceCode }}</div>
                <div class="device-meta">
                  <el-tag size="small">{{ deviceTypeText(d.deviceType) }}</el-tag>
                  <el-tag size="small" :type="d.status === 'ONLINE' ? 'success' : 'info'">
                    {{ d.status === 'ONLINE' ? '在线' : '离线' }}
                  </el-tag>
                </div>
              </div>
              <el-empty v-if="filteredDevices.length === 0" description="暂无设备" />
            </el-card>
          </el-col>

          <!-- 右侧MQTT配置表单 -->
          <el-col :xs="24" :sm="16" :md="17" :lg="18">
            <el-card shadow="never" v-if="selectedDevice">
              <template #header>
                <span>MQTT 协议配置 — {{ selectedDevice.deviceName || selectedDevice.name }} ({{ selectedDevice.deviceCode }})</span>
              </template>
              <el-form :model="mqttForm" label-width="120px" :disabled="saving">
                <el-form-item label="Broker地址">
                  <el-input v-model="mqttForm.brokerUrl" placeholder="mqtt://127.0.0.1" />
                </el-form-item>
                <el-form-item label="端口">
                  <el-input-number v-model="mqttForm.port" :min="1" :max="65535" />
                </el-form-item>
                <el-form-item label="Client ID">
                  <el-input v-model="mqttForm.clientId" :placeholder="selectedDevice.deviceCode + '_client'" />
                </el-form-item>
                <el-form-item label="用户名">
                  <el-input v-model="mqttForm.username" />
                </el-form-item>
                <el-form-item label="密码">
                  <el-input v-model="mqttForm.password" type="password" show-password />
                </el-form-item>
                <el-form-item label="订阅Topic">
                  <el-input v-model="mqttForm.subscribeTopic" :placeholder="'device/' + selectedDevice.deviceCode + '/cmd'" />
                </el-form-item>
                <el-form-item label="发布Topic">
                  <el-input v-model="mqttForm.publishTopic" :placeholder="'device/' + selectedDevice.deviceCode + '/data'" />
                </el-form-item>
                <el-form-item label="QoS等级">
                  <el-radio-group v-model="mqttForm.qos">
                    <el-radio :label="0">0</el-radio>
                    <el-radio :label="1">1</el-radio>
                    <el-radio :label="2">2</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="保持会话">
                  <el-radio-group v-model="mqttForm.cleanSession">
                    <el-radio :label="0">开启</el-radio>
                    <el-radio :label="1">关闭</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-form>
              <div class="form-actions">
                <el-button @click="resetMqttForm">重置</el-button>
                <el-button type="primary" :loading="saving" @click="saveMqttConfig">保存配置</el-button>
              </div>
            </el-card>
            <el-empty v-else description="请选择左侧设备" />
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- Tab 2: 在线心跳 -->
      <el-tab-pane label="在线心跳" name="heartbeat">
        <el-card shadow="never">
          <template #header>
            <span>在线设备监测</span>
          </template>
          <el-table :data="onlineDevices" v-loading="loadingOnline" border>
            <el-table-column prop="deviceCode" label="设备编码" min-width="140" />
            <el-table-column prop="deviceName" label="设备名称" min-width="160" />
            <el-table-column prop="deviceType" label="类型" width="120">
              <template #default="{ row }">
                {{ deviceTypeText(row.deviceType) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag type="success">{{ row.status === 'ONLINE' ? '在线' : row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastHeartbeat" label="最后心跳" min-width="160" />
          </el-table>
          <el-empty v-if="!loadingOnline && onlineDevices.length === 0" description="当前无在线设备" />
        </el-card>
      </el-tab-pane>

      <!-- Tab 3: 指令通道 -->
      <el-tab-pane label="指令通道" name="channel">
        <el-card shadow="never">
          <template #header>
            <span>指令下发通道</span>
          </template>
          <el-alert type="info" :closable="false" show-icon>
            指令下发功能请前往「设备管理 → 远程操控」页面操作。
          </el-alert>
          <div style="margin-top: 16px;">
            <el-button type="primary" @click="$router.push('/device-command')">跳转到远程操控</el-button>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const activeTab = ref('config')
const deviceKeyword = ref('')
const devices = ref<any[]>([])
const selectedDevice = ref<any>(null)
const saving = ref(false)
const loadingOnline = ref(false)
const onlineDevices = ref<any[]>([])

const mqttForm = ref({
  brokerUrl: '',
  port: 1883,
  clientId: '',
  username: '',
  password: '',
  subscribeTopic: '',
  publishTopic: '',
  qos: 1,
  cleanSession: 0,
})

const filteredDevices = computed(() => {
  const kw = deviceKeyword.value.trim().toLowerCase()
  if (!kw) return devices.value
  return devices.value.filter(d =>
    (d.deviceCode || '').toLowerCase().includes(kw) ||
    (d.deviceName || d.name || '').toLowerCase().includes(kw)
  )
})

function deviceTypeText(type: number) {
  const map: Record<number, string> = { 1: '煎药机', 2: '包装机', 3: '标签打印机', 4: '激光打印机', 5: 'PDA' }
  return map[type] || '未知'
}

async function loadDevices() {
  try {
    const res: any = await request.get('/v1/eq/devices', { params: { page: 1, size: 200 } })
    devices.value = res.data?.records || []
    if (devices.value.length > 0 && !selectedDevice.value) {
      selectDevice(devices.value[0])
    }
  } catch (e) {
    ElMessage.error('加载设备列表失败')
  }
}

async function selectDevice(device: any) {
  selectedDevice.value = device
  resetMqttForm()
  try {
    const res: any = await request.get(`/v1/eq/devices/${device.deviceCode}/mqtt-config`)
    if (res.data) {
      mqttForm.value = { ...mqttForm.value, ...res.data }
    }
  } catch {
    // 无配置时保持默认值
  }
}

function resetMqttForm() {
  mqttForm.value = {
    brokerUrl: '',
    port: 1883,
    clientId: selectedDevice.value ? `${selectedDevice.value.deviceCode}_client` : '',
    username: '',
    password: '',
    subscribeTopic: selectedDevice.value ? `device/${selectedDevice.value.deviceCode}/cmd` : '',
    publishTopic: selectedDevice.value ? `device/${selectedDevice.value.deviceCode}/data` : '',
    qos: 1,
    cleanSession: 0,
  }
}

async function saveMqttConfig() {
  if (!selectedDevice.value) return
  saving.value = true
  try {
    await request.post(`/v1/eq/devices/${selectedDevice.value.deviceCode}/mqtt-config`, mqttForm.value)
    ElMessage.success('配置保存成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function loadOnlineDevices() {
  loadingOnline.value = true
  try {
    const res: any = await request.get('/v1/eq/gateway/online-managed')
    onlineDevices.value = res.data || []
  } catch (e) {
    ElMessage.error('加载在线设备失败')
  } finally {
    loadingOnline.value = false
  }
}

onMounted(() => {
  loadDevices()
  loadOnlineDevices()
})
</script>

<style scoped lang="scss">
.page-container {
  padding: var(--ygt-space-4);
}
.network-tabs {
  margin-top: 16px;
}
.device-list-card {
  .device-item {
    padding: 12px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 4px;
    margin-bottom: 8px;
    cursor: pointer;
    transition: all 0.2s;
    border-left: 3px solid transparent;

    &:hover, &.active {
      background: #f5f7fa;
      border-left-color: #409EFF;
    }
  }
  .device-name {
    font-weight: 500;
    font-size: 14px;
  }
  .device-code {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
  .device-meta {
    margin-top: 6px;
    display: flex;
    gap: 6px;
  }
}
.form-actions {
  margin-top: 16px;
  text-align: right;
}
</style>
