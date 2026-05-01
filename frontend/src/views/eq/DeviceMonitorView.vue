<template>
  <div class="device-monitor" :class="{ 'is-fullscreen': isFullscreen }">
    <div class="page-header-title">设备监控：<span class="page-header-sub">设备实时状态、温度曲线、故障告警</span></div>
    <!-- 顶部工具栏 -->
    <div class="monitor-toolbar">
      <div class="toolbar-left">

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
        <el-button-group class="quick-links">
          <el-button size="large" @click="$router.push('/traces')">煎药追溯</el-button>
          <el-button size="large" @click="$router.push('/eq-dashboard')">数据看板</el-button>
          <el-button size="large" @click="$router.push('/workload')">工作量</el-button>
          <el-button size="large" @click="$router.push('/device-utilization')">利用率</el-button>
        </el-button-group>
        <el-radio-group v-model="activeType" size="large">
          <el-radio-button :label="1">煎药机</el-radio-button>
          <el-radio-button :label="2">包装机</el-radio-button>
          <el-radio-button :label="3">标签打印机</el-radio-button>
          <el-radio-button :label="4">激光打印机</el-radio-button>
          <el-radio-button :label="5">PDA</el-radio-button>
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
            :class="[getStatusAnimation(device), device.status === 'OFFLINE' ? 'offline-status' : '']"
            data-testid="status-block"
          >
            <span class="status-name" data-testid="status-text">
              {{ formatStatusName(device) }}
              <template v-if="device._networkOffline">(?)</template>
            </span>
            <span v-if="device.remainingTime > 0 && isRunning(device)" class="remaining-time">
              剩{{ formatTime(device.remainingTime) }}
            </span>
          </div>
        </div>

        <!-- ====== 煎药机卡片内容 ====== -->
        <template v-if="device.deviceType === 1">
          <div class="temp-section">
            <div class="temp-value" data-testid="temperature-value">
              <template v-if="device.status === 'OFFLINE' || device.status === 'FAULT' && !device.currentTemp">--</template>
              <template v-else>{{ device.currentTemp?.toFixed(1) }}°C</template>
            </div>
            <div class="temp-gauge-mini" :style="getGaugeStyle(device)" />
          </div>
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
              <el-progress :percentage="device.progressPercent" :stroke-width="12" :status="device.progressPercent >= 100 ? 'success' : ''" />
            </div>
          </div>
          <!-- 煎药机操作按钮 -->
          <div class="action-section">
            <template v-if="!isOffline(device)">
              <el-button v-if="isIdle(device)" type="warning" size="large" @click.stop="handleShiftHandover(device)" data-testid="shift-handover-btn">换班</el-button>
              <el-button v-if="isIdle(device)" type="primary" size="large" @click.stop="sendDeviceCommand(device, 'START_SOAK')">开始浸泡</el-button>
              <el-button v-if="isRunning(device)" type="warning" size="large" @click.stop="sendDeviceCommand(device, 'PAUSE')">暂停</el-button>
              <el-button v-if="isPaused(device)" type="primary" size="large" @click.stop="sendDeviceCommand(device, 'RESUME')">继续</el-button>
              <el-button v-if="!isFault(device)" type="danger" size="large" class="emergency-btn" @click.stop="handleEmergencyStop(device)" data-testid="emergency-stop-btn">急停</el-button>
            </template>
            <el-button size="large" @click.stop="goToDetail(device)">查看详情</el-button>
          </div>
        </template>

        <!-- ====== 包装机卡片内容 ====== -->
        <template v-else-if="device.deviceType === 2">
          <div class="temp-section">
            <div class="metric-value" :class="{ 'metric-offline': isOffline(device) }">
              <template v-if="isOffline(device)">--</template>
              <template v-else>{{ device.packageNum || 0 }}<span class="metric-unit">袋</span></template>
            </div>
            <div class="metric-label">已包装</div>
          </div>
          <div class="info-section">
            <div class="info-row" v-if="device.currentPrescriptionCode">
              <span class="info-label">当前任务:</span>
              <span class="info-value">{{ device.currentPrescriptionCode }}</span>
            </div>
            <div class="info-row" v-if="device.currentOperatorName">
              <span class="info-label">负责人:</span>
              <span class="info-value">👤 {{ device.currentOperatorName }}</span>
            </div>
            <div class="info-row" v-if="device.packageCapacity">
              <span class="info-label">容量:</span>
              <span class="info-value">{{ device.packageCapacity }} ml/袋</span>
            </div>
            <div class="info-row" v-if="isRunning(device) && device.progressPercent > 0">
              <el-progress :percentage="device.progressPercent" :stroke-width="12" :status="device.progressPercent >= 100 ? 'success' : ''" />
            </div>
          </div>
          <!-- 包装机操作按钮 -->
          <div class="action-section">
            <template v-if="!isOffline(device)">
              <el-button v-if="isIdle(device)" type="primary" size="large" @click.stop="sendDeviceCommand(device, 'START_PACKAGE')">开始包装</el-button>
              <el-button v-if="isRunning(device)" type="warning" size="large" @click.stop="sendDeviceCommand(device, 'PAUSE')">暂停</el-button>
              <el-button v-if="isPaused(device)" type="primary" size="large" @click.stop="sendDeviceCommand(device, 'RESUME')">继续</el-button>
              <el-button v-if="!isFault(device)" type="danger" size="large" class="emergency-btn" @click.stop="handleEmergencyStop(device)">急停</el-button>
            </template>
            <el-button size="large" @click.stop="goToDetail(device)">查看详情</el-button>
          </div>
        </template>

        <!-- ====== 标签打印机卡片内容 ====== -->
        <template v-else-if="device.deviceType === 3">
          <div class="temp-section">
            <div class="metric-value" :class="{ 'metric-offline': isOffline(device) }">
              <template v-if="isOffline(device)">--</template>
              <template v-else>{{ device.printCopies || 0 }}<span class="metric-unit">张</span></template>
            </div>
            <div class="metric-label">待打印</div>
          </div>
          <div class="info-section">
            <div class="info-row">
              <span class="info-label">打印状态:</span>
              <span class="info-value">
                <el-tag :type="printStatusTag(device.printStatus)" size="small">{{ printStatusText(device.printStatus) }}</el-tag>
              </span>
            </div>
            <div class="info-row" v-if="device.currentPrescriptionCode">
              <span class="info-label">当前标签:</span>
              <span class="info-value">{{ device.currentPrescriptionCode }}</span>
            </div>
            <div class="info-row" v-if="device.labelMode">
              <span class="info-label">模式:</span>
              <span class="info-value">{{ device.labelMode }}</span>
            </div>
          </div>
          <!-- 标签打印机操作按钮 -->
          <div class="action-section">
            <template v-if="!isOffline(device)">
              <el-button v-if="isIdle(device) || device.printStatus === 'PENDING'" type="primary" size="large" @click.stop="sendDeviceCommand(device, 'REPRINT_LABEL')">补打</el-button>
              <el-button v-if="isRunning(device)" type="warning" size="large" @click.stop="sendDeviceCommand(device, 'PAUSE_PRINT')">暂停</el-button>
              <el-button v-if="isPaused(device)" type="primary" size="large" @click.stop="sendDeviceCommand(device, 'RESUME')">继续</el-button>
            </template>
            <el-button size="large" @click.stop="goToDetail(device)">查看详情</el-button>
          </div>
        </template>

        <!-- ====== 激光打印机卡片内容 ====== -->
        <template v-else-if="device.deviceType === 4">
          <div class="temp-section">
            <div class="metric-value" :class="{ 'metric-offline': isOffline(device) }">
              <template v-if="isOffline(device)">--</template>
              <template v-else><el-icon :size="32"><Printer /></el-icon></template>
            </div>
            <div class="metric-label">{{ printStatusText(device.printStatus) || '就绪' }}</div>
          </div>
          <div class="info-section">
            <div class="info-row">
              <span class="info-label">打印状态:</span>
              <span class="info-value">
                <el-tag :type="printStatusTag(device.printStatus)" size="small">{{ printStatusText(device.printStatus) }}</el-tag>
              </span>
            </div>
            <div class="info-row" v-if="device.printCopies !== undefined">
              <span class="info-label">已打印:</span>
              <span class="info-value">{{ device.printCopies || 0 }} 份</span>
            </div>
            <div class="info-row" v-if="device.currentPrescriptionCode">
              <span class="info-label">当前文档:</span>
              <span class="info-value">{{ device.currentPrescriptionCode }}</span>
            </div>
          </div>
          <!-- 激光打印机操作按钮 -->
          <div class="action-section">
            <template v-if="!isOffline(device)">
              <el-button v-if="isIdle(device) || device.printStatus === 'PENDING'" type="primary" size="large" @click.stop="sendDeviceCommand(device, 'START_PRINT')">开始打印</el-button>
              <el-button v-if="isRunning(device)" type="warning" size="large" @click.stop="sendDeviceCommand(device, 'PAUSE_PRINT')">暂停</el-button>
              <el-button v-if="isPaused(device)" type="primary" size="large" @click.stop="sendDeviceCommand(device, 'RESUME')">继续</el-button>
            </template>
            <el-button size="large" @click.stop="goToDetail(device)">查看详情</el-button>
          </div>
        </template>

        <!-- ====== PDA 卡片内容 ====== -->
        <template v-else>
          <div class="temp-section">
            <div class="metric-value" :class="{ 'metric-offline': isOffline(device) }">
              <template v-if="isOffline(device)">--</template>
              <template v-else>{{ device.currentTemp || 0 }}<span class="metric-unit">%</span></template>
            </div>
            <div class="metric-label">电量</div>
          </div>
          <div class="info-section">
            <div class="info-row">
              <span class="info-label">状态:</span>
              <span class="info-value">
                <el-tag :type="isOffline(device) ? 'info' : 'success'" size="small">{{ isOffline(device) ? '离线' : '在线' }}</el-tag>
              </span>
            </div>
            <div class="info-row" v-if="device.currentOperatorName">
              <span class="info-label">操作人:</span>
              <span class="info-value">👤 {{ device.currentOperatorName }}</span>
            </div>
            <div class="info-row" v-if="device.lastHeartbeat">
              <span class="info-label">最后心跳:</span>
              <span class="info-value">{{ formatDateTime(device.lastHeartbeat) }}</span>
            </div>
          </div>
          <div class="action-section">
            <el-button size="large" @click.stop="goToDetail(device)">查看详情</el-button>
          </div>
        </template>
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
        <p><strong>当前状态:</strong> {{ formatStatusName(currentDevice) }}</p>
        <p v-if="currentDevice.deviceType === 1"><strong>处方:</strong> {{ currentDevice.currentPrescriptionCode || '无' }}</p>
        <p v-else-if="currentDevice.deviceType === 2"><strong>当前任务:</strong> {{ currentDevice.currentPrescriptionCode || '无' }}</p>
        <p v-else-if="currentDevice.deviceType === 3 || currentDevice.deviceType === 4"><strong>当前文档:</strong> {{ currentDevice.currentPrescriptionCode || '无' }}</p>
        <el-alert type="error" :closable="false">
          <template v-if="currentDevice.deviceType === 1">急停将中断当前煎药进程</template>
          <template v-else-if="currentDevice.deviceType === 2">急停将中断当前包装任务</template>
          <template v-else-if="currentDevice.deviceType === 3 || currentDevice.deviceType === 4">急停将中断当前打印任务</template>
          <template v-else>确认执行急停操作</template>
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
import { Refresh, FullScreen, Printer } from '@element-plus/icons-vue'
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
  // 按设备编码稳定排序，避免状态变化时卡片跳动
  return [...list].sort((a: any, b: any) => {
    const ca = a.deviceCode || ''
    const cb = b.deviceCode || ''
    return ca.localeCompare(cb)
  })
})

const offlineCount = computed(() => deviceStore.offlineCount)
const faultCount = computed(() => deviceStore.faultCount)

function formatStatusName(device: any) {
  // 离线优先判断
  if (device.status === 'OFFLINE') return '离线'
  const status = device.detailStatus || device.status
  const map: Record<string, string> = {
    IDLE: '空闲', STANDBY: '待机', READY: '就绪',
    SOAKING: '浸泡中', PRE_DECOCTING: '预热中',
    FIRST_DECOCTING: '一煎中', SECOND_DECOCTING: '二煎中',
    ADD_LATE: '后下提醒', DRAINING: '出液中',
    PACKAGING: '包装中', PAUSED: '暂停',
    FAULT: '故障', OFFLINE: '离线',
    BUSY: '运行中', MAINTENANCE: '维护中',
    PACKER_IDLE: '空闲', PACKER_READY: '就绪',
    PRINTING: '打印中', PENDING: '待打印',
  }
  return map[status || ''] || status || '空闲'
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
  const runningStatuses = ['FIRST_DECOCTING', 'SECOND_DECOCTING', 'SOAKING', 'PRE_DECOCTING', 'PACKAGING', 'DRAINING', 'BUSY', 'PRINTING']
  return runningStatuses.includes(device.detailStatus || device.status)
}

function isOffline(device: any) {
  return device.status === 'OFFLINE'
}

function isFault(device: any) {
  return (device.detailStatus || device.status) === 'FAULT'
}

function isIdle(device: any) {
  const s = device.detailStatus || device.status
  return ['IDLE', 'READY', 'PACKER_IDLE', 'PENDING'].includes(s)
}

function isPaused(device: any) {
  return (device.detailStatus || device.status) === 'PAUSED'
}

function printStatusText(status?: string) {
  const map: Record<string, string> = {
    PENDING: '待打印', PRINTING: '打印中', PAUSED: '暂停', COMPLETED: '完成', ERROR: '故障', READY: '就绪'
  }
  return map[status || ''] || status || '就绪'
}

function printStatusTag(status?: string) {
  if (status === 'PRINTING') return 'primary'
  if (status === 'COMPLETED') return 'success'
  if (status === 'ERROR') return 'danger'
  if (status === 'PAUSED') return 'warning'
  return 'info'
}

async function sendDeviceCommand(device: any, commandType: string) {
  try {
    await createCommand({ deviceCode: device.deviceCode, commandType, payload: null })
    ElMessage.success('指令已发送')
    // 立即刷新设备列表，确保状态即时更新（WebSocket可能有延迟）
    await loadDevices()
  } catch (err) {
    ElMessage.error('指令发送失败')
  }
}

function getStatusAnimation(device: any) {
  // 离线设备无动画
  if (device.status === 'OFFLINE') return ''
  const status = device.detailStatus || device.status
  if (status === 'FIRST_DECOCTING' || status === 'SECOND_DECOCTING' || status === 'PACKAGING' || status === 'SOAKING' || status === 'BUSY' || status === 'PRINTING') {
    return 'pulse'
  }
  if (status === 'FAULT') {
    return 'blink'
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

  &.offline-status {
    background: #BFBFBF;
  }

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
  flex-direction: column;

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

  .metric-value {
    font-size: 36px;
    font-weight: bold;
    color: var(--el-color-primary);
    display: flex;
    align-items: baseline;
    gap: 4px;

    .metric-unit {
      font-size: 16px;
      font-weight: normal;
      color: var(--el-text-color-secondary);
    }

    &.metric-offline {
      color: var(--el-text-color-disabled);
    }
  }

  .metric-label {
    font-size: 14px;
    color: var(--el-text-color-secondary);
    margin-top: 4px;
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
