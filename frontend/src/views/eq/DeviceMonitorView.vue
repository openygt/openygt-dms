<template>
  <div class="device-monitor" :class="{ 'is-fullscreen': isFullscreen }">
    <!-- 顶部工具栏 -->
    <div class="monitor-toolbar">
      <div class="toolbar-left">
        <h1 class="page-title">设备监控</h1>
        <el-tag
          v-if="offlineCount > 0"
          type="danger"
          effect="dark"
          class="alarm-badge"
          data-testid="alarm-indicator"
        >
          {{ offlineCount }}台离线
        </el-tag>
        <el-tag
          v-if="faultCount > 0"
          type="danger"
          effect="dark"
          class="alarm-badge"
        >
          {{ faultCount }}台故障
        </el-tag>
      </div>
      <div class="toolbar-right">
        <el-radio-group v-model="activeType" size="large">
          <el-radio-button :label="1">煎药机</el-radio-button>
          <el-radio-button :label="2">包装机</el-radio-button>
          <el-radio-button :label="0">全部</el-radio-button>
        </el-radio-group>
        <el-button
          :icon="Refresh"
          circle
          size="large"
          :class="{ rotating: refreshing }"
          @click="refreshData"
          data-testid="refresh-btn"
        />
        <el-button
          :icon="FullScreen"
          circle
          size="large"
          @click="toggleFullscreen"
          data-testid="fullscreen-btn"
        />
      </div>
    </div>

    <!-- 设备卡片网格 -->
    <div class="device-grid" v-loading="loading">
      <div
        v-for="device in filteredDevices"
        :key="device.deviceCode"
        class="device-card"
        :class="[`status-${device.detailStatus || device.status}`, { offline: device.status === 'OFFLINE' }]"
        :data-testid="`device-card-${device.deviceCode}`"
        :data-device-code="device.deviceCode"
        @click="goToDetail(device)"
      >
        <!-- 卡片头部 -->
        <div class="card-header">
          <h2 class="device-name" data-testid="device-name">{{ device.name }}</h2>
          <div
            class="status-block"
            :class="getStatusAnimation(device)"
            data-testid="status-block"
          >
            <span class="status-name" data-testid="status-text">
              {{ formatStatusName(device.detailStatus || device.status) }}
              <template v-if="device._networkOffline">(?)</template>
            </span>
            <span v-if="device.remainingTime > 0 && isRunning(device)" class="remaining-time">
              剩{{ formatTime(device.remainingTime) }}
            </span>
          </div>
        </div>

        <!-- 温度仪表盘区域 -->
        <div class="temp-section">
          <div class="temp-value" data-testid="temperature-value">
            <template v-if="device.status === 'OFFLINE' || device.status === 'FAULT' && !device.currentTemp">--</template>
            <template v-else>{{ device.currentTemp?.toFixed(1) }}°C</template>
          </div>
          <div
            class="temp-gauge-mini"
            :style="getGaugeStyle(device)"
          />
        </div>

        <!-- 信息区域 -->
        <div class="info-section">
          <div class="info-row" v-if="device.currentPrescriptionCode">
            <span class="info-label">处方:</span>
            <span class="info-value">{{ device.currentPrescriptionCode }}</span>
          </div>
          <div class="info-row" v-if="device.currentOperatorName">
            <span class="info-label">负责人:</span>
            <span class="info-value">👤 {{ device.currentOperatorName }}</span>
          </div>
          <div class="info-row" v-if="device.estimatedFinishTime">
            <span class="info-label">预计完成:</span>
            <span class="info-value">⏰ {{ formatDateTime(device.estimatedFinishTime) }}</span>
          </div>
          <div class="info-row" v-if="isRunning(device) && device.progressPercent > 0">
            <el-progress
              :percentage="device.progressPercent"
              :stroke-width="12"
              :status="device.progressPercent >= 100 ? 'success' : ''"
            />
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-section">
          <el-button
            type="warning"
            size="large"
            @click.stop="handleShiftHandover(device)"
            data-testid="shift-handover-btn"
          >
            换班
          </el-button>
          <el-button
            type="danger"
            size="large"
            class="emergency-btn"
            @click.stop="handleEmergencyStop(device)"
            data-testid="emergency-stop-btn"
          >
            急停
          </el-button>
          <el-button
            size="large"
            @click.stop="goToDetail(device)"
          >
            查看详情
          </el-button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <EmptyState v-if="!loading && filteredDevices.length === 0" description="暂无设备" />

    <!-- 换班弹窗 -->
    <el-dialog
      v-model="shiftDialogVisible"
      title="换班交接"
      width="420px"
      data-testid="shift-handover-dialog"
    >
      <div v-if="currentDevice">
        <p>设备: {{ currentDevice.name }}</p>
        <p>当前: {{ currentDevice.currentOperatorName || '无' }}</p>
        <el-form :model="shiftForm" label-width="80px">
          <el-form-item label="接班人">
            <el-select
              v-model="shiftForm.operatorId"
              placeholder="选择接班人"
              data-testid="operator-select"
            >
              <el-option
                v-for="user in operatorOptions"
                :key="user.id"
                :label="user.realName || user.username"
                :value="user.id"
                :data-testid="`operator-option-${user.realName || user.username}`"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="shiftDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmShiftHandover" data-testid="confirm-shift">
          确认换班
        </el-button>
      </template>
    </el-dialog>

    <!-- 急停确认弹窗 -->
    <el-dialog
      v-model="emergencyDialogVisible"
      title="⚠️ 紧急停止确认"
      width="420px"
      data-testid="confirm-dialog"
    >
      <div v-if="currentDevice">
        <p><strong>设备:</strong> {{ currentDevice.name }}</p>
        <p><strong>当前状态:</strong> {{ formatStatusName(currentDevice.detailStatus || currentDevice.status) }}</p>
        <p><strong>处方:</strong> {{ currentDevice.currentPrescriptionCode || '无' }}</p>
        <el-alert type="error" :closable="false">
          急停将中断当前煎药进程
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="emergencyDialogVisible = false" data-testid="cancel-emergency-stop">
          取消
        </el-button>
        <el-button type="danger" @click="confirmEmergencyStop" data-testid="confirm-emergency-stop">
          确认急停
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, FullScreen } from '@element-plus/icons-vue'
import { getDeviceList, shiftHandover, createCommand } from '@/api/equipment'
import { useDeviceStore } from '@/stores/device'
import { useDeviceWebSocket } from '@/composables/useDeviceWebSocket'
import EmptyState from '@/components/states/EmptyState.vue'

const router = useRouter()
const deviceStore = useDeviceStore()
const { connect, disconnect } = useDeviceWebSocket('default')

const loading = ref(false)
const refreshing = ref(false)
const activeType = ref(0)
const isFullscreen = ref(false)

// 弹窗相关
const shiftDialogVisible = ref(false)
const emergencyDialogVisible = ref(false)
const currentDevice = ref<any>(null)
const shiftForm = ref({ operatorId: null as number | null, operatorName: '' })
const operatorOptions = ref<any[]>([])

// 排序优先级（故障在最前）
const statusPriority: Record<string, number> = {
  FAULT: 0,
  OFFLINE: 1,
  ADD_LATE: 2,
  FIRST_DECOCTING: 3,
  SECOND_DECOCTING: 4,
  PACKAGING: 5,
  SOAKING: 6,
  PRE_DECOCTING: 7,
  IDLE: 8,
  PAUSED: 9,
}

const filteredDevices = computed(() => {
  let list = deviceStore.deviceList
  if (activeType.value > 0) {
    list = list.filter((d: any) => d.deviceType === activeType.value)
  }
  // 排序：故障 > 离线 > 运行中 > 空闲
  return [...list].sort((a: any, b: any) => {
    const pa = statusPriority[a.detailStatus || a.status] ?? 99
    const pb = statusPriority[b.detailStatus || b.status] ?? 99
    return pa - pb
  })
})

const offlineCount = computed(() => deviceStore.offlineCount)
const faultCount = computed(() => deviceStore.faultCount)

function formatStatusName(status: string) {
  const map: Record<string, string> = {
    IDLE: '空闲',
    STANDBY: '待机',
    READY: '就绪',
    SOAKING: '浸泡中',
    PRE_DECOCTING: '预热中',
    FIRST_DECOCTING: '一煎中',
    SECOND_DECOCTING: '二煎中',
    ADD_LATE: '后下提醒',
    DRAINING: '出液中',
    PACKAGING: '包装中',
    PAUSED: '暂停',
    FAULT: '故障',
    OFFLINE: '离线',
    BUSY: '运行中',
    MAINTENANCE: '维护中',
  }
  return map[status] || status
}

function formatTime(seconds: number) {
  if (seconds <= 0) return '即将完成'
  const mins = Math.floor(seconds / 60)
  const hrs = Math.floor(mins / 60)
  if (hrs > 0) return `${hrs}小时${mins % 60}分`
  return `${mins}分`
}

function formatDateTime(dt: string) {
  if (!dt) return '-'
  const d = new Date(dt)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

function isRunning(device: any) {
  const runningStatuses = ['FIRST_DECOCTING', 'SECOND_DECOCTING', 'SOAKING', 'PRE_DECOCTING', 'PACKAGING', 'DRAINING']
  return runningStatuses.includes(device.detailStatus || device.status)
}

function getStatusAnimation(device: any) {
  const status = device.detailStatus || device.status
  if (status === 'FIRST_DECOCTING' || status === 'SECOND_DECOCTING' || status === 'PACKAGING' || status === 'SOAKING') {
    return 'pulse'
  }
  if (status === 'FAULT') {
    return 'blink'
  }
  if (status === 'OFFLINE') {
    return ''
  }
  return 'breath'
}

function getGaugeStyle(device: any) {
  const temp = device.currentTemp || 0
  let color = '#52C41A'
  if (temp > 120) color = '#CF1322'
  else if (temp > 100) color = '#F5222D'
  else if (temp > 80) color = '#FAAD14'
  else if (temp > 50) color = '#52C41A'
  return {
    background: `conic-gradient(${color} 0% ${Math.min(temp / 150 * 100, 100)}%, #e8e8e8 ${Math.min(temp / 150 * 100, 100)}% 100%)`,
  }
}

async function loadDevices() {
  loading.value = true
  try {
    const res: any = await getDeviceList({ page: 1, size: 100 })
    const records = res.data?.records || []
    // 同步到 deviceStore
    records.forEach((d: any) => {
      deviceStore.updateDevice(d.deviceCode, d)
    })
  } catch (err) {
    ElMessage.error('加载设备失败')
  } finally {
    loading.value = false
  }
}

async function refreshData() {
  refreshing.value = true
  await loadDevices()
  setTimeout(() => { refreshing.value = false }, 800)
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

function goToDetail(device: any) {
  router.push(`/device/${device.deviceCode}/detail`)
}

function handleShiftHandover(device: any) {
  currentDevice.value = device
  shiftForm.value.operatorId = null
  shiftDialogVisible.value = true
}

async function confirmShiftHandover() {
  if (!shiftForm.value.operatorId || !currentDevice.value) return
  const user = operatorOptions.value.find((u: any) => u.id === shiftForm.value.operatorId)
  try {
    await shiftHandover(
      currentDevice.value.deviceCode,
      shiftForm.value.operatorId,
      user?.realName || user?.username || ''
    )
    ElMessage.success('换班成功')
    shiftDialogVisible.value = false
    await loadDevices()
  } catch (err) {
    ElMessage.error('换班失败')
  }
}

function handleEmergencyStop(device: any) {
  currentDevice.value = device
  emergencyDialogVisible.value = true
}

async function confirmEmergencyStop() {
  if (!currentDevice.value) return
  try {
    await createCommand({
      deviceCode: currentDevice.value.deviceCode,
      commandType: 'EMERGENCY_STOP',
      payload: null
    })
    ElMessage.success('急停指令已发送')
    emergencyDialogVisible.value = false
    await loadDevices()
  } catch (err) {
    ElMessage.error('急停指令发送失败')
  }
}

onMounted(() => {
  loadDevices()
  connect()
  // 模拟加载操作人列表（实际应从用户API获取）
  operatorOptions.value = [
    { id: 1, username: 'zhangsan', realName: '张三' },
    { id: 2, username: 'lisi', realName: '李四' },
    { id: 3, username: 'wangwu', realName: '王五' },
  ]
})

onUnmounted(() => {
  disconnect()
})
</script>

<style scoped lang="scss">
.device-monitor {
  padding: 16px;
  min-height: 100vh;
  background: var(--el-bg-color-page);

  &.is-fullscreen {
    padding: 24px;
  }
}

.monitor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .page-title {
      font-size: 24px;
      font-weight: bold;
      margin: 0;
      color: var(--el-text-color-primary);
    }

    .alarm-badge {
      font-size: 16px;
      padding: 6px 12px;
    }
  }

  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.device-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.device-card {
  background: var(--el-bg-color);
  border-radius: 12px;
  padding: 20px;
  border: 2px solid var(--el-border-color-light);
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  gap: 16px;

  &:hover {
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  &.offline {
    opacity: 0.6;
    border-color: var(--el-border-color);
  }

  &.status-FAULT {
    border-color: #CF1322;
    border-width: 3px;
    background: #FFF1F0;

    &.blink {
      animation: border-blink 1s infinite;
    }
  }

  &.status-OFFLINE {
    border-color: #BFBFBF;
  }
}

@keyframes border-blink {
  0%, 100% { border-color: #CF1322; }
  50% { border-color: #FF7875; }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;

  .device-name {
    font-size: 20px;
    font-weight: bold;
    margin: 0;
    color: var(--el-text-color-primary);
    line-height: 1.3;
  }
}

.status-block {
  min-width: 88px;
  min-height: 48px;
  padding: 8px 16px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #52C41A;
  color: #fff;

  &.pulse {
    animation: status-pulse 1.5s infinite;
  }

  &.blink {
    animation: status-blink 1s infinite;
    background: #CF1322;
  }

  &.breath {
    animation: status-breath 2s infinite;
  }
}

@keyframes status-pulse {
  0%, 100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(245, 34, 45, 0.4); }
  50% { transform: scale(1.03); box-shadow: 0 0 0 8px rgba(245, 34, 45, 0); }
}

@keyframes status-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

@keyframes status-breath {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.status-name {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  line-height: 1.2;
}

.remaining-time {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  margin-top: 2px;
}

.temp-section {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;

  .temp-value {
    font-size: 28px;
    font-weight: bold;
    color: var(--el-text-color-primary);
  }

  .temp-gauge-mini {
    width: 60px;
    height: 60px;
    border-radius: 50%;
    mask: radial-gradient(transparent 55%, black 56%);
    -webkit-mask: radial-gradient(transparent 55%, black 56%);
  }
}

.info-section {
  .info-row {
    display: flex;
    gap: 8px;
    margin-bottom: 6px;
    font-size: 14px;

    .info-label {
      color: var(--el-text-color-secondary);
      min-width: 70px;
    }

    .info-value {
      color: var(--el-text-color-primary);
      font-weight: 500;
    }
  }
}

.action-section {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-light);

  .el-button {
    min-width: 80px;
    min-height: 48px;
    font-size: 16px;
  }

  .emergency-btn {
    border-radius: 50%;
    width: 56px;
    height: 56px;
    min-width: 56px;
    padding: 0;
  }
}

.rotating {
  animation: rotate 0.8s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .device-grid {
    grid-template-columns: 1fr;
  }

  .monitor-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
