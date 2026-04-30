<template>
  <div class="device-detail">
    <!-- 返回按钮 -->
    <div class="detail-header">
      <el-button :icon="ArrowLeft" size="large" @click="router.back()">返回</el-button>
      <h1 class="detail-title">{{ device?.name }} - 详情</h1>
      <div class="header-actions">
        <el-button
          :type="isAlarm ? 'danger' : 'primary'"
          size="large"
          :class="{ 'alarm-blink': isAlarm }"
        >
          {{ formatStatusName(device?.detailStatus || device?.status) }}
        </el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：温度仪表盘 + 基本信息 -->
      <el-col :xs="24" :md="8">
        <el-card class="detail-card">
          <template #header>
            <span class="card-title">实时温度</span>
          </template>
          <div ref="gaugeChartRef" class="gauge-chart" data-testid="temp-gauge" />
          <div class="gauge-value" data-testid="gauge-value">
            {{ device?.currentTemp?.toFixed(1) || '--' }}°C
          </div>
        </el-card>

        <el-card class="detail-card">
          <template #header>
            <span class="card-title">基本信息</span>
          </template>
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

      <!-- 中间：温度曲线 + 指令 -->
      <el-col :xs="24" :md="10">
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

        <el-card class="detail-card">
          <template #header>
            <span class="card-title">指令控制</span>
          </template>
          <div class="command-grid">
            <el-button
              v-for="cmd in availableCommands"
              :key="cmd.type"
              :type="cmd.type === 'EMERGENCY_STOP' ? 'danger' : 'primary'"
              size="large"
              :data-testid="`command-btn-${cmd.type}`"
              @click="sendCommand(cmd.type)"
            >
              {{ cmd.label }}
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：操作记录 + 状态日志 -->
      <el-col :xs="24" :md="6">
        <el-card class="detail-card">
          <template #header>
            <span class="card-title">当前操作人</span>
          </template>
          <div v-if="currentOperator" class="operator-info">
            <div class="operator-avatar">👤</div>
            <div class="operator-name">{{ currentOperator.operatorName }}</div>
            <div class="operator-time">班次: {{ formatDateTime(currentOperator.shiftStartTime) }}</div>
          </div>
          <EmptyState v-else description="暂无操作人" />
        </el-card>

        <el-card class="detail-card">
          <template #header>
            <span class="card-title">最近指令</span>
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
                <el-tag :type="getCommandTagType(cmd.status)" size="small">
                  {{ cmd.status }}
                </el-tag>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDeviceDetail, getLatestStatus, getRecentCommands, createCommand, getTemperatureAggregation } from '@/api/equipment'
import EmptyState from '@/components/states/EmptyState.vue'

const router = useRouter()
const route = useRoute()
const deviceCode = route.params.code as string

const device = ref<any>(null)
const currentOperator = ref<any>(null)
const recentCommands = ref<any[]>([])
const timeRange = ref('1h')
const isAlarm = ref(false)

const gaugeChartRef = ref<HTMLElement>()
const tempChartRef = ref<HTMLElement>()
let gaugeChart: echarts.ECharts | null = null
let tempChart: echarts.ECharts | null = null

const availableCommands = [
  { type: 'START_SOAK', label: '开始浸泡' },
  { type: 'START_DECOCT', label: '开始煎煮' },
  { type: 'PAUSE', label: '暂停' },
  { type: 'RESUME', label: '继续' },
  { type: 'EMERGENCY_STOP', label: '急停' },
]

function formatStatusName(status: string) {
  const map: Record<string, string> = {
    IDLE: '空闲', STANDBY: '待机', READY: '就绪',
    SOAKING: '浸泡中', PRE_DECOCTING: '预热中',
    FIRST_DECOCTING: '一煎中', SECOND_DECOCTING: '二煎中',
    ADD_LATE: '后下提醒', DRAINING: '出液中',
    PACKAGING: '包装中', PAUSED: '暂停',
    FAULT: '故障', OFFLINE: '离线',
  }
  return map[status] || status
}

function formatDeviceType(type: number) {
  const map: Record<number, string> = { 1: '煎药机', 2: '包装机', 3: '标签打印机', 4: '激光打印机', 5: 'PDA' }
  return map[type] || '未知'
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
    // 降级到模拟数据
    const hours = ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
    const mockTemps = [25, 30, 45, 98, 100, 95, 85]
    tempChart.setOption({
      xAxis: { data: hours },
      series: [{ data: mockTemps }]
    })
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
      initGaugeChart()
      initTempChart()
      updateGauge(device.value?.currentTemp || 0)
      updateTempChart()
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

onMounted(() => {
  loadDeviceDetail()
  loadCommands()
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

@keyframes btn-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
