<template>
  <div class="device-detail">
    <el-page-header title="设备详情" content="设备运行参数、历史记录、维护信息" class="mb-4" />

    <!-- 返回按钮 -->
    <div class="detail-header">
      <el-button :icon="ArrowLeft" size="large" @click="router.back()">返回</el-button>
      <h1 class="detail-title">{{ device?.name }} - 详情</h1>
      <div class="header-actions">
        <el-button
          :type="headerStatusType"
          size="large"
          :class="{ 'alarm-blink': isAlarm }"
        >
          {{ headerStatusText }}
        </el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- ========== 左侧：按设备类型显示主信息 + 基本信息 ========== -->
      <el-col :xs="24" :md="8">
        <!-- 煎药机：温度仪表盘 -->
        <template v-if="device?.deviceType === 1">
          <el-card class="detail-card">
            <template #header><span class="card-title">实时温度</span></template>
            <div ref="gaugeChartRef" class="gauge-chart" data-testid="temp-gauge" />
            <div class="gauge-value" data-testid="gauge-value">{{ device?.currentTemp?.toFixed(1) || '--' }}°C</div>
          </el-card>
        </template>

        <!-- 包装机：包装统计 -->
        <template v-else-if="device?.deviceType === 2">
          <el-card class="detail-card">
            <template #header><span class="card-title">包装统计</span></template>
            <div class="stat-grid">
              <div class="stat-item">
                <div class="stat-value">{{ device?.packageNum || 0 }}</div>
                <div class="stat-label">已包装(袋)</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ device?.packageCapacity || '--' }}</div>
                <div class="stat-label">容量(ml/袋)</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ device?.progressPercent || 0 }}%</div>
                <div class="stat-label">当前进度</div>
              </div>
            </div>
          </el-card>
        </template>

        <!-- 标签打印机：打印统计 -->
        <template v-else-if="device?.deviceType === 3">
          <el-card class="detail-card">
            <template #header><span class="card-title">打印统计</span></template>
            <div class="stat-grid">
              <div class="stat-item">
                <div class="stat-value">{{ device?.printCopies || 0 }}</div>
                <div class="stat-label">待打印(张)</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ device?.labelMode || '--' }}</div>
                <div class="stat-label">标签模式</div>
              </div>
              <div class="stat-item">
                <div class="stat-value" :class="printStatusTag(device?.printStatus)">{{ printStatusText(device?.printStatus) }}</div>
                <div class="stat-label">打印状态</div>
              </div>
            </div>
          </el-card>
        </template>

        <!-- 激光打印机：打印状态 + 耗材 -->
        <template v-else-if="device?.deviceType === 4">
          <el-card class="detail-card">
            <template #header><span class="card-title">打印状态</span></template>
            <div class="stat-grid">
              <div class="stat-item">
                <div class="stat-value">{{ device?.printCopies || 0 }}</div>
                <div class="stat-label">已打印(份)</div>
              </div>
              <div class="stat-item">
                <div class="stat-value" :class="printStatusTag(device?.printStatus)">{{ printStatusText(device?.printStatus) }}</div>
                <div class="stat-label">状态</div>
              </div>
            </div>
          </el-card>
        </template>

        <!-- PDA：电量 + 信号 -->
        <template v-else>
          <el-card class="detail-card">
            <template #header><span class="card-title">设备状态</span></template>
            <div class="stat-grid">
              <div class="stat-item">
                <div class="stat-value">{{ device?.currentTemp || 0 }}%</div>
                <div class="stat-label">电量</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ device?.status === 'ONLINE' ? '在线' : '离线' }}</div>
                <div class="stat-label">网络</div>
              </div>
            </div>
          </el-card>
        </template>

        <!-- 基本信息：所有设备通用 -->
        <el-card class="detail-card">
          <template #header><span class="card-title">基本信息</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="设备编码">{{ device?.deviceCode }}</el-descriptions-item>
            <el-descriptions-item label="设备类型">{{ formatDeviceType(device?.deviceType) }}</el-descriptions-item>
            <el-descriptions-item label="厂商">{{ device?.manufacturer || '-' }}</el-descriptions-item>
            <el-descriptions-item label="型号">{{ device?.modelNum || '-' }}</el-descriptions-item>
            <el-descriptions-item label="序列号">{{ device?.serialNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="安装日期">{{ device?.installDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="保修到期">{{ device?.warrantyExpire || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- ========== 中间：按设备类型显示图表/列表 + 指令 ========== -->
      <el-col :xs="24" :md="10">
        <!-- 煎药机：温度曲线 -->
        <template v-if="device?.deviceType === 1">
          <el-card class="detail-card">
            <template #header>
              <div class="chart-header">
                <span class="card-title">温度曲线</span>
                <el-radio-group v-model="timeRange" size="small">
                  <el-radio-button label="1h">1小时</el-radio-button>
                  <el-radio-button label="24h">24小时</el-radio-button>
                  <el-radio-button label="7d">7天</el-radio-button>
                </el-radio-group>
              </div>
            </template>
            <div ref="tempChartRef" class="temp-chart" data-testid="temp-chart" />
          </el-card>
        </template>

        <!-- 包装机：包装任务 -->
        <template v-else-if="device?.deviceType === 2">
          <el-card class="detail-card">
            <template #header><span class="card-title">当前任务</span></template>
            <div v-if="device?.currentPrescriptionCode" class="task-info">
              <p><strong>处方编号:</strong> {{ device.currentPrescriptionCode }}</p>
              <p><strong>任务状态:</strong> <el-tag :type="device.status === 'BUSY' ? 'primary' : 'info'">{{ formatStatusName(device) }}</el-tag></p>
              <p v-if="device.progressPercent > 0"><strong>完成进度:</strong> <el-progress :percentage="device.progressPercent" /></p>
              <p v-if="device.estimatedFinishTime"><strong>预计完成:</strong> {{ formatDateTime(device.estimatedFinishTime) }}</p>
            </div>
            <EmptyState v-else description="暂无包装任务" />
          </el-card>
        </template>

        <!-- 打印机：打印队列 -->
        <template v-else-if="device?.deviceType === 3 || device?.deviceType === 4">
          <el-card class="detail-card">
            <template #header><span class="card-title">打印队列</span></template>
            <div v-if="device?.currentPrescriptionCode" class="task-info">
              <p><strong>当前文档:</strong> {{ device.currentPrescriptionCode }}</p>
              <p><strong>打印状态:</strong> <el-tag :type="printStatusTag(device.printStatus)">{{ printStatusText(device.printStatus) }}</el-tag></p>
              <p v-if="device.printCopies"><strong>打印份数:</strong> {{ device.printCopies }}</p>
            </div>
            <EmptyState v-else description="打印队列为空" />
          </el-card>
        </template>

        <!-- PDA：操作记录 -->
        <template v-else>
          <el-card class="detail-card">
            <template #header><span class="card-title">最近操作</span></template>
            <div class="task-info">
              <p><strong>设备状态:</strong> <el-tag :type="device?.status === 'ONLINE' ? 'success' : 'info'">{{ device?.status === 'ONLINE' ? '在线' : '离线' }}</el-tag></p>
              <p v-if="device?.lastHeartbeat"><strong>最后心跳:</strong> {{ formatDateTime(device.lastHeartbeat) }}</p>
              <p v-if="device?.currentOperatorName"><strong>当前绑定:</strong> {{ device.currentOperatorName }}</p>
            </div>
          </el-card>
        </template>

        <!-- 指令控制：按设备类型 -->
        <el-card class="detail-card">
          <template #header><span class="card-title">指令控制</span></template>
          <div class="command-grid">
            <el-button
              v-for="cmd in deviceCommands"
              :key="cmd.type"
              :type="cmd.type === 'EMERGENCY_STOP' ? 'danger' : 'primary'"
              size="large"
              :data-testid="`command-btn-${cmd.type}`"
              :disabled="device?.status === 'OFFLINE' && cmd.type !== 'EMERGENCY_STOP'"
              @click="sendCommand(cmd.type)"
            >
              {{ cmd.label }}
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- ========== 右侧：操作人 + 最近指令（通用） ========== -->
      <el-col :xs="24" :md="6">
        <el-card class="detail-card">
          <template #header><span class="card-title">当前操作人</span></template>
          <div v-if="currentOperator" class="operator-info">
            <div class="operator-avatar">👤</div>
            <div class="operator-name">{{ currentOperator.operatorName }}</div>
            <div class="operator-time">班次: {{ formatDateTime(currentOperator.shiftStartTime) }}</div>
          </div>
          <EmptyState v-else description="暂无操作人" />
        </el-card>

        <el-card class="detail-card">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span class="card-title">最近指令</span>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="cmd in recentCommands"
              :key="cmd.id"
              :type="getCommandTimelineType(cmd.status)"
              :timestamp="formatDateTime(cmd.createdAt)"
            >
              <div class="command-item">
                <span class="cmd-type">{{ cmd.commandType }}</span>
                <el-tag :type="getCommandTagType(cmd.status)" size="small">{{ cmd.status }}</el-tag>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card class="detail-card">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span class="card-title">维护记录</span>
              <el-button size="small" @click="openMaintenanceDialog()">新增</el-button>
            </div>
          </template>
          <el-timeline v-if="maintenanceList.length">
            <el-timeline-item
              v-for="item in maintenanceList"
              :key="item.id"
              :type="item.status === 'COMPLETED' ? 'success' : 'warning'"
              :timestamp="item.finishDate || item.planDate"
            >
              <div class="command-item">
                <span>{{ formatMaintenanceType(item.maintenanceType) }}</span>
                <el-tag :type="item.status === 'COMPLETED' ? 'success' : 'warning'" size="small">{{ item.status === 'COMPLETED' ? '已完成' : '待执行' }}</el-tag>
              </div>
              <div style="font-size: 12px; color: #666; margin-top: 4px">{{ item.content }}</div>
            </el-timeline-item>
          </el-timeline>
          <EmptyState v-else description="暂无维护记录" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 维护记录弹窗 -->
    <el-dialog v-model="maintenanceDialogVisible" title="新增维护记录" width="500px">
      <el-form :model="maintenanceForm" label-width="100px">
        <el-form-item label="维护类型">
          <el-select v-model="maintenanceForm.maintenanceType" placeholder="请选择" style="width: 100%">
            <el-option label="保养" value="MAINTENANCE" />
            <el-option label="维修" value="REPAIR" />
            <el-option label="巡检" value="INSPECTION" />
          </el-select>
        </el-form-item>
        <el-form-item label="维护内容">
          <el-input v-model="maintenanceForm.content" type="textarea" rows="3" placeholder="请输入维护内容" />
        </el-form-item>
        <el-form-item label="更换配件">
          <el-input v-model="maintenanceForm.parts" placeholder="请输入更换配件" />
        </el-form-item>
        <el-form-item label="费用">
          <el-input-number v-model="maintenanceForm.cost" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划日期">
          <el-date-picker v-model="maintenanceForm.planDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="完成日期">
          <el-date-picker v-model="maintenanceForm.finishDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="maintenanceForm.status">
            <el-radio :label="0">待执行</el-radio>
            <el-radio :label="1">已完成</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="maintenanceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveMaintenance">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDeviceDetail, getLatestStatus, getRecentCommands, createCommand, getTemperatureAggregation } from '@/api/equipment'
import { getDeviceMaintenanceList, createDeviceMaintenance } from '@/api/deviceMaintenance'
import { useDeviceStore } from '@/stores/device'
import EmptyState from '@/components/states/EmptyState.vue'

const router = useRouter()
const route = useRoute()
const deviceCode = route.params.code as string
const deviceStore = useDeviceStore()

const device = ref<any>(null)
const currentOperator = ref<any>(null)
const recentCommands = ref<any[]>([])
const maintenanceList = ref<any[]>([])
const maintenanceDialogVisible = ref(false)
const maintenanceForm = ref<any>({ status: 0 })
const timeRange = ref('1h')
const isAlarm = ref(false)

const gaugeChartRef = ref<HTMLElement>()
const tempChartRef = ref<HTMLElement>()
let gaugeChart: echarts.ECharts | null = null
let tempChart: echarts.ECharts | null = null

const deviceCommands = computed(() => {
  const type = device.value?.deviceType
  if (type === 1) {
    return [
      { type: 'START_SOAK', label: '开始浸泡' },
      { type: 'START_DECOCT', label: '开始煎煮' },
      { type: 'PAUSE', label: '暂停' },
      { type: 'RESUME', label: '继续' },
      { type: 'EMERGENCY_STOP', label: '急停' },
    ]
  }
  if (type === 2) {
    return [
      { type: 'START_PACKAGE', label: '开始包装' },
      { type: 'PAUSE', label: '暂停' },
      { type: 'RESUME', label: '继续' },
      { type: 'EMERGENCY_STOP', label: '急停' },
    ]
  }
  if (type === 3 || type === 4) {
    return [
      { type: 'START_PRINT', label: '开始打印' },
      { type: 'PAUSE_PRINT', label: '暂停打印' },
      { type: 'REPRINT_LABEL', label: '补打标签' },
      { type: 'EMERGENCY_STOP', label: '急停' },
    ]
  }
  return [
    { type: 'EMERGENCY_STOP', label: '急停' },
  ]
})

// 实时设备状态：优先使用 WebSocket store 数据
const liveDevice = computed(() => {
  const storeDevice = deviceStore.devices.get(deviceCode)
  if (storeDevice && device.value) {
    return { ...device.value, ...storeDevice }
  }
  return device.value
})

const headerStatusText = computed(() => {
  const d = liveDevice.value
  if (!d) return '--'
  if (d.status === 'OFFLINE') return '离线'
  const status = d.detailStatus || d.status
  const map: Record<string, string> = {
    IDLE: '空闲', STANDBY: '待机', READY: '就绪',
    SOAKING: '浸泡中', PRE_DECOCTING: '预热中',
    FIRST_DECOCTING: '一煎中', SECOND_DECOCTING: '二煎中',
    ADD_LATE: '后下提醒', DRAINING: '出液中',
    PACKAGING: '包装中', PAUSED: '暂停',
    FAULT: '故障', OFFLINE: '离线',
    BUSY: '运行中', MAINTENANCE: '维护中',
    PACKER_IDLE: '空闲', PRINTING: '打印中', PENDING: '待打印',
  }
  return map[status || ''] || status || '空闲'
})

const headerStatusType = computed(() => {
  const d = liveDevice.value
  if (!d) return 'info'
  if (d.status === 'OFFLINE') return 'info'
  const status = d.detailStatus || d.status
  if (status === 'FAULT') return 'danger'
  if (isAlarm.value) return 'danger'
  // 与监控列表 status-block 颜色保持一致：运行/空闲均为绿色
  return 'success'
})

function formatStatusName(deviceObj: any) {
  if (!deviceObj) return '--'
  if (deviceObj.status === 'OFFLINE') return '离线'
  const status = deviceObj.detailStatus || deviceObj.status
  const map: Record<string, string> = {
    IDLE: '空闲', STANDBY: '待机', READY: '就绪',
    SOAKING: '浸泡中', PRE_DECOCTING: '预热中',
    FIRST_DECOCTING: '一煎中', SECOND_DECOCTING: '二煎中',
    ADD_LATE: '后下提醒', DRAINING: '出液中',
    PACKAGING: '包装中', PAUSED: '暂停',
    FAULT: '故障', OFFLINE: '离线',
    BUSY: '运行中', MAINTENANCE: '维护中',
    PACKER_IDLE: '空闲', PRINTING: '打印中', PENDING: '待打印',
  }
  return map[status || ''] || status || '空闲'
}

function formatDeviceType(type: number) {
  const map: Record<number, string> = { 1: '煎药机', 2: '包装机', 3: '标签打印机', 4: '激光打印机', 5: 'PDA' }
  return map[type] || '未知'
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

function formatDateTime(dt: string) {
  if (!dt) return '-'
  const d = new Date(dt)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

function getCommandTimelineType(status: string) {
  const map: Record<string, any> = { ACKED: 'success', FAILED: 'danger', PENDING: 'info', SENT: 'warning' }
  return map[status] || 'info'
}

function getCommandTagType(status: string) {
  const map: Record<string, any> = { ACKED: 'success', FAILED: 'danger', PENDING: 'info', SENT: 'warning' }
  return map[status] || 'info'
}

function initGaugeChart() {
  if (!gaugeChartRef.value) return
  gaugeChart = echarts.init(gaugeChartRef.value)
  const option = {
    series: [{
      type: 'gauge',
      startAngle: 200,
      endAngle: -20,
      min: 0,
      max: 150,
      splitNumber: 5,
      axisLine: {
        lineStyle: {
          width: 12,
          color: [
            [0.33, '#52C41A'],
            [0.53, '#FAAD14'],
            [0.67, '#F5222D'],
            [0.8, '#CF1322'],
            [1, '#820014']
          ]
        }
      },
      pointer: { itemStyle: { color: 'auto' }, width: 6 },
      axisTick: { distance: -18, length: 6, lineStyle: { color: '#999', width: 1 } },
      splitLine: { distance: -24, length: 12, lineStyle: { color: '#999', width: 2 } },
      axisLabel: { color: '#666', distance: -40, fontSize: 12 },
      detail: { valueAnimation: true, formatter: '{value}°C', color: 'auto', fontSize: 28, fontWeight: 'bold', offsetCenter: [0, '60%'] },
      data: [{ value: 0 }]
    }]
  }
  gaugeChart.setOption(option)
}

function initTempChart() {
  if (!tempChartRef.value) return
  tempChart = echarts.init(tempChartRef.value)
  const option = {
    grid: { top: 30, right: 20, bottom: 30, left: 50 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: [], axisLabel: { fontSize: 12 } },
    yAxis: { type: 'value', name: '温度°C', min: 0, max: 150, axisLabel: { fontSize: 12 } },
    series: [{
      data: [],
      type: 'line',
      smooth: true,
      symbol: 'none',
      lineStyle: { width: 2, color: '#F5222D' },
      areaStyle: { color: 'rgba(245, 34, 45, 0.1)' },
      markLine: {
        data: [{ yAxis: 120, label: { formatter: '告警线', fontSize: 12 }, lineStyle: { color: '#CF1322', type: 'dashed' } }]
      }
    }]
  }
  tempChart.setOption(option)
}

function updateGauge(value: number) {
  if (gaugeChart) {
    gaugeChart.setOption({ series: [{ data: [{ value }] }] })
  }
}

async function updateTempChart() {
  if (!tempChart || !device.value?.id) return

  const now = new Date()
  let start: Date
  let interval = '1min'

  switch (timeRange.value) {
    case '1h':
      start = new Date(now.getTime() - 60 * 60 * 1000)
      interval = '1min'
      break
    case '24h':
      start = new Date(now.getTime() - 24 * 60 * 60 * 1000)
      interval = '5min'
      break
    case '7d':
      start = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
      interval = '1hour'
      break
    default:
      start = new Date(now.getTime() - 60 * 60 * 1000)
  }

  const formatTime = (d: Date) => d.toISOString().slice(0, 19)

  try {
    const res: any = await getTemperatureAggregation(
      device.value.id,
      interval,
      formatTime(start),
      formatTime(now)
    )
    const data = res.data || []
    const times = data.map((d: any) => {
      const t = new Date(d.windowStart)
      return `${t.getHours().toString().padStart(2, '0')}:${t.getMinutes().toString().padStart(2, '0')}`
    })
    const temps = data.map((d: any) => d.avgTemp)

    tempChart.setOption({
      xAxis: { data: times },
      series: [{ data: temps }]
    })
  } catch (err) {
    console.error('加载温度曲线失败', err)
    // 无数据时显示空态，不再使用模拟数据
    tempChart.clear()
  }
}

async function loadDeviceDetail() {
  try {
    // 通过code查询设备（后端暂无直接按code查详情的接口，先用列表查询）
    const res: any = await getDeviceDetail(deviceCode)
    device.value = res.data
    currentOperator.value = res.data?.currentOperator
    isAlarm.value = (device.value?.detailStatus === 'FAULT' || (device.value?.currentTemp || 0) > 120)
    nextTick(() => {
      if (device.value?.deviceType === 1) {
        initGaugeChart()
        initTempChart()
        updateGauge(device.value?.currentTemp || 0)
        updateTempChart()
      }
    })
  } catch (err) {
    ElMessage.error('加载设备详情失败')
  }
}

async function loadCommands() {
  try {
    const res: any = await getRecentCommands(deviceCode, 10)
    recentCommands.value = res.data || []
  } catch (err) {
    // 静默处理
  }
}

async function loadMaintenances() {
  try {
    if (!device.value?.id) return
    const res: any = await getDeviceMaintenanceList({ deviceId: device.value.id, size: 20 })
    maintenanceList.value = (res.data?.records || []).sort((a: any, b: any) => {
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    })
  } catch (err) {
    maintenanceList.value = []
  }
}

function formatMaintenanceType(type?: string) {
  const map: Record<string, string> = { MAINTENANCE: '保养', REPAIR: '维修', INSPECTION: '巡检' }
  return map[type || ''] || type || '其他'
}

function openMaintenanceDialog() {
  maintenanceForm.value = { deviceId: device.value?.id, status: 0 }
  maintenanceDialogVisible.value = true
}

async function handleSaveMaintenance() {
  try {
    await createDeviceMaintenance(maintenanceForm.value)
    ElMessage.success('维护记录已添加')
    maintenanceDialogVisible.value = false
    loadMaintenances()
  } catch (err) {
    ElMessage.error('添加失败')
  }
}

async function sendCommand(commandType: string) {
  try {
    await createCommand({
      deviceCode,
      commandType,
      payload: null
    })
    ElMessage.success(`指令 ${commandType} 已下发`)
    await loadCommands()
  } catch (err) {
    ElMessage.error('指令下发失败')
  }
}

watch(timeRange, () => {
  updateTempChart()
})

// 监听 store 实时更新，同步到 device（温度、进度等）
watch(
  () => deviceStore.devices.get(deviceCode),
  (storeDevice) => {
    if (storeDevice && device.value) {
      device.value = { ...device.value, ...storeDevice }
      isAlarm.value = (device.value.detailStatus === 'FAULT' || (device.value.currentTemp || 0) > 120)
      if (device.value.deviceType === 1) {
        nextTick(() => updateGauge(device.value.currentTemp || 0))
      }
    }
  },
  { deep: true }
)

onMounted(() => {
  loadDeviceDetail()
  loadCommands()
  loadMaintenances()
  window.addEventListener('resize', () => {
    gaugeChart?.resize()
    tempChart?.resize()
  })
})

onUnmounted(() => {
  gaugeChart?.dispose()
  tempChart?.dispose()
})
</script>

<style scoped lang="scss">
.device-detail {
  padding: 16px;

  .detail-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;

    .detail-title {
      font-size: 24px;
      font-weight: bold;
      margin: 0;
      flex: 1;
    }

    .header-actions {
      .alarm-blink {
        animation: btn-blink 1s infinite;
      }
    }
  }

  .detail-card {
    margin-bottom: 20px;

    .card-title {
      font-size: 18px;
      font-weight: bold;
    }

    .chart-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
}

.gauge-chart {
  width: 100%;
  height: 240px;
}

.gauge-value {
  text-align: center;
  font-size: 28px;
  font-weight: bold;
  color: var(--el-text-color-primary);
  margin-top: -20px;
}

.temp-chart {
  width: 100%;
  height: 280px;
}

.command-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;

  .el-button {
    min-height: 48px;
    font-size: 16px;
  }
}

.operator-info {
  text-align: center;
  padding: 16px;

  .operator-avatar {
    font-size: 48px;
    margin-bottom: 8px;
  }

  .operator-name {
    font-size: 20px;
    font-weight: bold;
    margin-bottom: 4px;
  }

  .operator-time {
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }
}

.command-item {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .cmd-type {
    font-size: 14px;
    font-weight: 500;
  }
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 8px 0;

  .stat-item {
    text-align: center;
    padding: 12px;
    background: var(--el-fill-color-light);
    border-radius: 8px;

    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: var(--el-color-primary);
      margin-bottom: 4px;

      &.primary { color: var(--el-color-primary); }
      &.success { color: var(--el-color-success); }
      &.warning { color: var(--el-color-warning); }
      &.danger { color: var(--el-color-danger); }
      &.info { color: var(--el-text-color-secondary); }
    }

    .stat-label {
      font-size: 13px;
      color: var(--el-text-color-secondary);
    }
  }
}

.task-info {
  p {
    margin: 8px 0;
    font-size: 14px;
    color: var(--el-text-color-primary);
  }
}

@keyframes btn-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
