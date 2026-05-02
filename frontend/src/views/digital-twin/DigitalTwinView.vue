<template>
  <div class="digital-twin">
    <!-- 页面标题栏 -->
    <div class="dt-header">
      <div class="dt-title">
        <el-icon :size="28"><Monitor /></el-icon>
        <h1>设备数字孪生</h1>
        <el-tag v-if="deviceStore.connected" type="success" effect="dark">WebSocket 已连接</el-tag>
        <el-tag v-else type="info" effect="dark">WebSocket 断开</el-tag>
      </div>
      <div class="dt-stats">
        <el-statistic title="在线设备" :value="deviceStore.onlineCount" />
        <el-statistic title="离线设备" :value="deviceStore.offlineCount">
          <template #suffix><span style="color:#F56C6C">台</span></template>
        </el-statistic>
        <el-statistic title="故障设备" :value="deviceStore.faultCount">
          <template #suffix><span style="color:#E6A23C">台</span></template>
        </el-statistic>
      </div>
    </div>

    <!-- 设备筛选 -->
    <div class="dt-filter">
      <el-radio-group v-model="activeType" size="large">
        <el-radio-button :label="0">全部设备</el-radio-button>
        <el-radio-button :label="1">煎药机</el-radio-button>
        <el-radio-button :label="2">包装机</el-radio-button>
        <el-radio-button :label="3">标签打印机</el-radio-button>
        <el-radio-button :label="4">激光打印机</el-radio-button>
      </el-radio-group>
      <el-button :icon="Refresh" circle :class="{ rotating: refreshing }" @click="refreshData" />
    </div>

    <!-- 数字孪生设备网格 -->
    <div class="dt-grid" v-loading="loading">
      <div
        v-for="device in filteredDevices"
        :key="device.deviceCode"
        class="dt-device"
        :class="[`dt-type-${device.deviceType}`, `dt-status-${device.status?.toLowerCase()}`]"
      >
        <!-- 设备头部信息 -->
        <div class="dt-device-header">
          <div class="dt-device-info">
            <h3 class="dt-device-name">{{ device.name }}</h3>
            <div class="dt-device-meta">
              <el-tag size="small" :type="deviceTypeTag(device.deviceType)">{{ deviceTypeName(device.deviceType) }}</el-tag>
              <el-tag v-if="device.manufacturer" size="small" effect="plain">{{ device.manufacturer }}</el-tag>
              <el-tag v-if="device.modelNum" size="small" effect="plain">{{ device.modelNum }}</el-tag>
            </div>
          </div>
          <div class="dt-status-indicator" :class="{ online: device.status !== 'OFFLINE', offline: device.status === 'OFFLINE', fault: device.detailStatus === 'FAULT' }">
            <div class="dt-status-light"></div>
            <span class="dt-status-text">{{ formatStatusName(device) }}</span>
          </div>
        </div>

        <!-- SVG 数字孪生可视化区域 -->
        <div class="dt-visual">
          <!-- 煎药机 SVG -->
          <template v-if="device.deviceType === 1">
            <svg viewBox="0 0 200 160" class="dt-svg">
              <!-- 底座 -->
              <rect x="40" y="130" width="120" height="20" rx="3" fill="#8B7355" />
              <!-- 锅体外壳 -->
              <rect x="50" y="50" width="100" height="85" rx="8" :fill="device.status === 'OFFLINE' ? '#999' : '#C0C0C0'" stroke="#666" stroke-width="2" />
              <!-- 锅内液体 -->
              <rect v-if="device.status !== 'OFFLINE'" x="55" y="90" width="90" height="40" rx="4" :fill="getLiquidColor(device.currentTemp)" opacity="0.7">
                <animate v-if="isRunning(device)" attributeName="y" values="88;92;88" dur="2s" repeatCount="indefinite" />
              </rect>
              <!-- 温度数字 -->
              <text x="100" y="80" text-anchor="middle" font-size="22" font-weight="bold" :fill="device.status === 'OFFLINE' ? '#666' : '#333'">
                {{ device.status === 'OFFLINE' ? '--' : (device.currentTemp?.toFixed(1) + '°C') }}
              </text>
              <!-- 锅盖 -->
              <path d="M45 50 Q100 20 155 50" :fill="device.status === 'OFFLINE' ? '#888' : '#A0A0A0'" stroke="#666" stroke-width="2" />
              <!-- 蒸汽动画 -->
              <g v-if="isRunning(device) && device.currentTemp > 80">
                <circle cx="80" cy="25" r="4" fill="rgba(255,255,255,0.6)">
                  <animate attributeName="cy" values="25;5" dur="1.5s" repeatCount="indefinite" />
                  <animate attributeName="opacity" values="0.6;0" dur="1.5s" repeatCount="indefinite" />
                </circle>
                <circle cx="100" cy="28" r="5" fill="rgba(255,255,255,0.5)">
                  <animate attributeName="cy" values="28;3" dur="1.8s" repeatCount="indefinite" />
                  <animate attributeName="opacity" values="0.5;0" dur="1.8s" repeatCount="indefinite" />
                </circle>
                <circle cx="120" cy="22" r="3" fill="rgba(255,255,255,0.7)">
                  <animate attributeName="cy" values="22;2" dur="1.3s" repeatCount="indefinite" />
                  <animate attributeName="opacity" values="0.7;0" dur="1.3s" repeatCount="indefinite" />
                </circle>
              </g>
              <!-- 进度条背景 -->
              <rect x="40" y="145" width="120" height="8" rx="4" fill="#e0e0e0" />
              <!-- 进度条 -->
              <rect x="40" y="145" :width="(device.progressPercent || 0) * 1.2" height="8" rx="4" fill="#67C23A" />
            </svg>
            <div class="dt-metric-row">
              <span class="dt-metric">目标温度: {{ device.targetTemp || '--' }}°C</span>
              <span class="dt-metric">剩余: {{ formatTime(device.remainingTime) }}</span>
              <span class="dt-metric">水位: {{ device.waterLevel || 0 }}%</span>
            </div>
          </template>

          <!-- 包装机 SVG -->
          <template v-else-if="device.deviceType === 2">
            <svg viewBox="0 0 200 160" class="dt-svg">
              <!-- 机身 -->
              <rect x="30" y="40" width="140" height="100" rx="6" :fill="device.status === 'OFFLINE' ? '#999' : '#409EFF'" opacity="0.8" stroke="#2C5F8D" stroke-width="2" />
              <!-- 传送带 -->
              <rect x="20" y="120" width="160" height="20" rx="2" fill="#666" />
              <!-- 传送带动画 -->
              <g v-if="isRunning(device)">
                <line x1="30" y1="130" x2="170" y2="130" stroke="#999" stroke-width="2" stroke-dasharray="10,10">
                  <animate attributeName="stroke-dashoffset" from="0" to="20" dur="0.5s" repeatCount="indefinite" />
                </line>
              </g>
              <!-- 包装袋 -->
              <rect x="80" y="60" width="40" height="50" rx="3" fill="rgba(255,255,255,0.3)" stroke="#fff" stroke-width="1" />
              <!-- 计数 -->
              <text x="100" y="85" text-anchor="middle" font-size="28" font-weight="bold" fill="#fff">{{ device.packageNum || 0 }}</text>
              <text x="100" y="105" text-anchor="middle" font-size="12" fill="rgba(255,255,255,0.8)">已包装(袋)</text>
              <!-- 进度条 -->
              <rect x="30" y="148" width="140" height="6" rx="3" fill="#e0e0e0" />
              <rect x="30" y="148" :width="(device.progressPercent || 0) * 1.4" height="6" rx="3" fill="#67C23A" />
            </svg>
            <div class="dt-metric-row">
              <span class="dt-metric">容量: {{ device.packageCapacity || '--' }} ml/袋</span>
              <span class="dt-metric">进度: {{ device.progressPercent || 0 }}%</span>
            </div>
          </template>

          <!-- 标签打印机 SVG -->
          <template v-else-if="device.deviceType === 3">
            <svg viewBox="0 0 200 160" class="dt-svg">
              <!-- 打印机主体 -->
              <rect x="40" y="50" width="120" height="80" rx="5" :fill="device.status === 'OFFLINE' ? '#999' : '#606266'" stroke="#333" stroke-width="2" />
              <!-- 出纸口 -->
              <rect x="55" y="40" width="90" height="15" rx="2" fill="#333" />
              <!-- 纸张 -->
              <rect x="65" y="20" width="70" height="25" rx="1" fill="#fff" stroke="#ccc" stroke-width="1">
                <animate v-if="isRunning(device)" attributeName="y" values="20;25;20" dur="1s" repeatCount="indefinite" />
              </rect>
              <!-- 标签内容 -->
              <text x="100" y="36" text-anchor="middle" font-size="8" fill="#333">{{ device.currentPrescriptionCode || 'LABEL' }}</text>
              <!-- 状态灯 -->
              <circle cx="145" cy="65" r="5" :fill="printStatusColor(device.printStatus)" />
              <!-- 打印计数 -->
              <text x="100" y="105" text-anchor="middle" font-size="32" font-weight="bold" fill="#fff">{{ device.packageNum || 0 }}</text>
              <text x="100" y="125" text-anchor="middle" font-size="12" fill="rgba(255,255,255,0.7)">待打印(张)</text>
            </svg>
            <div class="dt-metric-row">
              <span class="dt-metric">模式: {{ device.labelMode || '--' }}</span>
              <span class="dt-metric">状态: {{ printStatusText(device.detailStatus) }}</span>
            </div>
          </template>

          <!-- 激光打印机 SVG -->
          <template v-else-if="device.deviceType === 4">
            <svg viewBox="0 0 200 160" class="dt-svg">
              <!-- 打印机主体 -->
              <rect x="45" y="55" width="110" height="70" rx="5" :fill="device.status === 'OFFLINE' ? '#999' : '#909399'" stroke="#555" stroke-width="2" />
              <!-- 纸盘 -->
              <rect x="55" y="125" width="90" height="15" rx="2" fill="#666" />
              <!-- 出纸 -->
              <rect x="60" y="35" width="80" height="25" rx="1" fill="#fff" stroke="#ccc">
                <animate v-if="isRunning(device)" attributeName="y" values="35;40;35" dur="1.2s" repeatCount="indefinite" />
              </rect>
              <!-- 激光头指示灯 -->
              <circle cx="135" cy="70" r="4" :fill="isRunning(device) ? '#67C23A' : '#909399'">
                <animate v-if="isRunning(device)" attributeName="opacity" values="1;0.3;1" dur="0.8s" repeatCount="indefinite" />
              </circle>
              <!-- 计数 -->
              <text x="100" y="100" text-anchor="middle" font-size="24" font-weight="bold" fill="#fff">{{ device.packageNum || 0 }}</text>
              <text x="100" y="118" text-anchor="middle" font-size="11" fill="rgba(255,255,255,0.7)">已打印(份)</text>
            </svg>
            <div class="dt-metric-row">
              <span class="dt-metric">状态: {{ printStatusText(device.detailStatus) }}</span>
            </div>
          </template>

          <!-- 通用/未知设备 -->
          <template v-else>
            <svg viewBox="0 0 200 160" class="dt-svg">
              <rect x="50" y="50" width="100" height="80" rx="8" :fill="device.status === 'OFFLINE' ? '#999' : '#E6A23C'" opacity="0.6" stroke="#666" />
              <text x="100" y="95" text-anchor="middle" font-size="16" fill="#333">未知设备类型</text>
            </svg>
          </template>
        </div>

        <!-- 设备操作区 -->
        <div class="dt-operations">
          <template v-if="device.status !== 'OFFLINE'">
            <!-- 煎药机操作 -->
            <template v-if="device.deviceType === 1">
              <el-button v-if="isIdle(device)" type="primary" @click="sendCommand(device, 'START_SOAK')">开始浸泡</el-button>
              <el-button v-if="isRunning(device)" type="warning" @click="sendCommand(device, 'PAUSE')">暂停</el-button>
              <el-button v-if="isPaused(device)" type="success" @click="sendCommand(device, 'RESUME')">继续</el-button>
              <el-button v-if="!isFault(device)" type="danger" @click="handleEmergencyStop(device)">急停</el-button>
            </template>

            <!-- 包装机操作 -->
            <template v-if="device.deviceType === 2">
              <el-button v-if="isIdle(device)" type="primary" @click="sendCommand(device, 'START_PACKAGE')">开始包装</el-button>
              <el-button v-if="isRunning(device)" type="warning" @click="sendCommand(device, 'PAUSE')">暂停</el-button>
              <el-button v-if="isPaused(device)" type="success" @click="sendCommand(device, 'RESUME')">继续</el-button>
              <el-button v-if="!isFault(device)" type="danger" @click="handleEmergencyStop(device)">急停</el-button>
            </template>

            <!-- 标签打印机操作 -->
            <template v-if="device.deviceType === 3">
              <el-button v-if="isIdle(device) || device.printStatus === 'PENDING'" type="primary" @click="sendCommand(device, 'REPRINT_LABEL')">补打标签</el-button>
              <el-button v-if="isRunning(device)" type="warning" @click="sendCommand(device, 'PAUSE_PRINT')">暂停</el-button>
              <el-button v-if="isPaused(device)" type="success" @click="sendCommand(device, 'RESUME')">继续</el-button>
            </template>

            <!-- 激光打印机操作 -->
            <template v-if="device.deviceType === 4">
              <el-button v-if="isIdle(device) || device.printStatus === 'PENDING'" type="primary" @click="sendCommand(device, 'START_PRINT')">开始打印</el-button>
              <el-button v-if="isRunning(device)" type="warning" @click="sendCommand(device, 'PAUSE_PRINT')">暂停</el-button>
              <el-button v-if="isPaused(device)" type="success" @click="sendCommand(device, 'RESUME')">继续</el-button>
            </template>
          </template>

          <el-button v-else type="info">设备离线</el-button>
        </div>

        <!-- 处方/任务信息 -->
        <div v-if="device.currentPrescriptionCode" class="dt-task-info">
          <el-icon><Document /></el-icon>
          <span>当前: {{ device.currentPrescriptionCode }}</span>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && filteredDevices.length === 0" description="暂无设备数据" />

    <!-- 急停确认弹窗 -->
    <el-dialog v-model="emergencyVisible" title="⚠️ 紧急停止确认" width="400px">
      <p><strong>设备:</strong> {{ currentDevice?.name }}</p>
      <p><strong>当前状态:</strong> {{ currentDevice ? formatStatusName(currentDevice) : '' }}</p>
      <el-alert type="error" :closable="false">急停将中断当前设备运行，请确认！</el-alert>
      <template #footer>
        <el-button @click="emergencyVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmEmergencyStop">确认急停</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Monitor, Refresh, Document } from '@element-plus/icons-vue'
import { useDeviceStore } from '@/stores/device'
import { useDeviceWebSocket } from '@/composables/useDeviceWebSocket'
import { getDeviceList, createCommand } from '@/api/equipment'
import EmptyState from '@/components/states/EmptyState.vue'

const deviceStore = useDeviceStore()
const { connect, disconnect } = useDeviceWebSocket('default')

const loading = ref(false)
const refreshing = ref(false)
const activeType = ref(0)
const emergencyVisible = ref(false)
const currentDevice = ref<any>(null)

const filteredDevices = computed(() => {
  let list = deviceStore.deviceList
  if (activeType.value > 0) {
    list = list.filter((d: any) => d.deviceType === activeType.value)
  }
  return [...list].sort((a: any, b: any) => {
    // 排序：故障 > 离线 > 运行中 > 空闲
    const pa = statusPriority(a)
    const pb = statusPriority(b)
    if (pa !== pb) return pa - pb
    return (a.deviceCode || '').localeCompare(b.deviceCode || '')
  })
})

function statusPriority(device: any) {
  if (device.detailStatus === 'FAULT' || device.status === 'FAULT') return 0
  if (device.status === 'OFFLINE') return 1
  if (isRunning(device)) return 2
  return 3
}

function deviceTypeName(type?: number) {
  const map: Record<number, string> = {
    1: '煎药机',
    2: '包装机',
    3: '标签打印机',
    4: '激光打印机',
    5: 'PDA'
  }
  return map[type || 0] || '未知'
}

function deviceTypeTag(type?: number) {
  const map: Record<number, any> = { 1: 'danger', 2: 'primary', 3: 'success', 4: 'warning', 5: 'info' }
  return map[type || 0] || 'info'
}

function formatStatusName(device: any) {
  if (device.status === 'OFFLINE') return '离线'
  const map: Record<string, string> = {
    IDLE: '空闲', READY: '就绪', SOAKING: '浸泡中', PRE_DECOCTING: '预热中',
    FIRST_DECOCTING: '一煎中', SECOND_DECOCTING: '二煎中', ADD_LATE: '后下提醒',
    DRAINING: '出液中', PACKAGING: '包装中', PAUSED: '暂停',
    FAULT: '故障', BUSY: '运行中', PRINTING: '打印中', PENDING: '待打印',
    PACKER_IDLE: '空闲', PACKER_READY: '就绪'
  }
  return map[device.detailStatus || device.status] || (device.detailStatus || device.status || '空闲')
}

function isRunning(device: any) {
  return ['FIRST_DECOCTING', 'SECOND_DECOCTING', 'SOAKING', 'PRE_DECOCTING', 'PACKAGING', 'DRAINING', 'BUSY', 'PRINTING'].includes(device.detailStatus || device.status)
}

function isIdle(device: any) {
  return ['IDLE', 'READY', 'PACKER_IDLE', 'PENDING'].includes(device.detailStatus || device.status)
}

function isPaused(device: any) {
  return (device.detailStatus || device.status) === 'PAUSED'
}

function isFault(device: any) {
  return (device.detailStatus || device.status) === 'FAULT'
}

function formatTime(seconds?: number) {
  if (!seconds || seconds <= 0) return '--'
  const mins = Math.floor(seconds / 60)
  const hrs = Math.floor(mins / 60)
  if (hrs > 0) return `${hrs}h${mins % 60}m`
  return `${mins}m`
}

function getLiquidColor(temp?: number) {
  if (!temp) return '#87CEEB'
  if (temp > 100) return '#FF6B6B'
  if (temp > 80) return '#FFD93D'
  if (temp > 50) return '#6BCB77'
  return '#87CEEB'
}

function printStatusText(status?: string) {
  const map: Record<string, string> = { PENDING: '待打印', PRINTING: '打印中', PAUSED: '暂停', COMPLETED: '完成', ERROR: '故障', READY: '就绪' }
  return map[status || ''] || status || '就绪'
}

function printStatusColor(status?: string) {
  const map: Record<string, string> = { PENDING: '#E6A23C', PRINTING: '#67C23A', PAUSED: '#F56C6C', COMPLETED: '#409EFF', ERROR: '#F56C6C', READY: '#909399' }
  return map[status || ''] || '#909399'
}

async function sendCommand(device: any, commandType: string) {
  try {
    await createCommand({ deviceCode: device.deviceCode, commandType, payload: null })
    ElMessage.success(`指令 [${commandType}] 已发送至 ${device.name}`)
  } catch (err) {
    ElMessage.error('指令发送失败')
  }
}

function handleEmergencyStop(device: any) {
  currentDevice.value = device
  emergencyVisible.value = true
}

async function confirmEmergencyStop() {
  if (!currentDevice.value) return
  await sendCommand(currentDevice.value, 'EMERGENCY_STOP')
  emergencyVisible.value = false
}

async function loadDevices() {
  loading.value = true
  try {
    const res: any = await getDeviceList({ page: 1, size: 100 })
    const records = res.data?.records || []
    records.forEach((d: any) => deviceStore.updateDevice(d.deviceCode, d))
  } catch (err) {
    console.error('加载设备失败', err)
  } finally {
    loading.value = false
  }
}

async function refreshData() {
  refreshing.value = true
  await loadDevices()
  setTimeout(() => refreshing.value = false, 600)
}

onMounted(() => {
  loadDevices()
  connect()
})

onUnmounted(() => {
  disconnect()
})
</script>

<style scoped lang="scss">
.digital-twin {
  padding: 16px;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
}

.dt-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 16px;

  .dt-title {
    display: flex;
    align-items: center;
    gap: 12px;

    h1 {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }
  }

  .dt-stats {
    display: flex;
    gap: 24px;
  }
}

.dt-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.dt-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 20px;
}

.dt-device {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 2px solid transparent;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  gap: 14px;

  &:hover {
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
    transform: translateY(-4px);
  }

  &.dt-status-offline {
    opacity: 0.65;
    border-color: #c0c4cc;
    background: #f5f7fa;
  }

  &.dt-status-fault,
  &.dt-status-fault .dt-status-indicator {
    border-color: #f56c6c;
    animation: fault-pulse 2s infinite;
  }
}

@keyframes fault-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(245, 108, 111, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(245, 108, 111, 0); }
}

.dt-device-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;

  .dt-device-info {
    flex: 1;
    min-width: 0;

    .dt-device-name {
      margin: 0 0 6px 0;
      font-size: 18px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }

    .dt-device-meta {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }
  }
}

.dt-status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  background: #f0f9ff;

  .dt-status-light {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #c0c4cc;
  }

  .dt-status-text {
    font-size: 13px;
    font-weight: 500;
    color: #909399;
  }

  &.online {
    background: #f0f9eb;
    .dt-status-light { background: #67c23a; box-shadow: 0 0 6px #67c23a; }
    .dt-status-text { color: #67c23a; }
  }

  &.offline {
    background: #f4f4f5;
    .dt-status-light { background: #c0c4cc; }
    .dt-status-text { color: #909399; }
  }

  &.fault {
    background: #fef0f0;
    .dt-status-light { background: #f56c6c; animation: blink 1s infinite; }
    .dt-status-text { color: #f56c6c; }
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.dt-visual {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: linear-gradient(180deg, #fafafa 0%, #f0f0f0 100%);
  border-radius: 12px;
  min-height: 200px;
  justify-content: center;
}

.dt-svg {
  width: 100%;
  max-width: 220px;
  height: auto;
}

.dt-metric-row {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.dt-operations {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
  padding-top: 8px;
}

.dt-task-info {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: #ecf5ff;
  border-radius: 8px;
  font-size: 13px;
  color: #409eff;

  .el-icon {
    font-size: 14px;
  }
}

.rotating {
  animation: rotate 0.6s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .dt-grid {
    grid-template-columns: 1fr;
  }
  .dt-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
