<template>
  <div class="eq-dashboard">
    <div class="page-header">
      <h2>数据看板</h2>
      <el-radio-group v-model="dateRange" @change="loadAll">
        <el-radio-button label="today">今日</el-radio-button>
        <el-radio-button label="week">本周</el-radio-button>
        <el-radio-button label="month">本月</el-radio-button>
      </el-radio-group>
    </div>

    <el-row :gutter="16" class="metrics-row">
      <el-col :xs="12" :sm="8" :md="4" v-for="m in metricsList" :key="m.key">
        <el-card class="metric-card">
          <div class="metric-icon" :style="{ background: m.color + '20', color: m.color }">
            <el-icon :size="28"><component :is="m.icon" /></el-icon>
          </div>
          <div class="metric-value">{{ metrics[m.key] || 0 }}</div>
          <div class="metric-label">{{ m.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header><span class="card-title">阶段分布</span></template>
          <div ref="stageChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header><span class="card-title">工人效率排行</span></template>
          <el-table :data="workerList" size="small" max-height="300">
            <el-table-column prop="operatorName" label="操作人" />
            <el-table-column prop="taskCount" label="任务数" width="80" />
            <el-table-column prop="avgDuration" label="平均耗时" width="100" />
            <el-table-column label="效率" width="100">
              <template #default="{ row }">
                <el-progress :percentage="Math.min(row.efficiencyScore * 10, 100)" :stroke-width="8" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24">
        <el-card>
          <template #header><span class="card-title">24小时趋势</span></template>
          <div ref="hourlyChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header><span class="card-title">设备利用率</span></template>
          <div ref="utilChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header><span class="card-title">异常统计</span></template>
          <el-empty v-if="!abnormalList.length" description="暂无异常" />
          <el-table v-else :data="abnormalList" size="small">
            <el-table-column prop="deviceCode" label="设备" width="120" />
            <el-table-column prop="alarmType" label="类型" />
            <el-table-column prop="count" label="次数" width="80" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Timer, Check, Warning, TrendCharts, Cpu } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getDashboardMetrics, getStageDistribution, getWorkerEfficiency,
  getHourlyTrend, getDashboardDeviceUtilization
} from '@/api/equipment'

const dateRange = ref('today')
const loading = ref(false)
const metrics = reactive<Record<string, any>>({})
const workerList = ref<any[]>([])
const abnormalList = ref<any[]>([])

const stageChartRef = ref<HTMLElement>()
const hourlyChartRef = ref<HTMLElement>()
const utilChartRef = ref<HTMLElement>()
let stageChart: echarts.ECharts | null = null
let hourlyChart: echarts.ECharts | null = null
let utilChart: echarts.ECharts | null = null

const metricsList = [
  { key: 'todayPrescription', label: '今日处方', icon: Document, color: '#409EFF' },
  { key: 'pendingCount', label: '待处理', icon: Timer, color: '#E6A23C' },
  { key: 'todayCompleted', label: '已完成', icon: Check, color: '#67C23A' },
  { key: 'faultCount', label: '故障数', icon: Warning, color: '#F56C6C' },
  { key: 'avgProcessTime', label: '平均耗时', icon: TrendCharts, color: '#909399' },
  { key: 'deviceUtilization', label: '设备利用率', icon: Cpu, color: '#409EFF' },
]

async function loadAll() {
  loading.value = true
  try {
    const date = dateRange.value === 'today' ? undefined : undefined
    const metricsRes: any = await getDashboardMetrics()
    Object.assign(metrics, metricsRes.data || {})

    const stageRes: any = await getStageDistribution()
    updateStageChart(stageRes.data || [])

    const workerRes: any = await getWorkerEfficiency()
    workerList.value = workerRes.data || []

    const hourlyRes: any = await getHourlyTrend()
    updateHourlyChart(hourlyRes.data || [])

    const utilRes: any = await getDashboardDeviceUtilization()
    updateUtilChart(utilRes.data || [])
  } catch (err) {
    ElMessage.error('加载看板数据失败')
  } finally {
    loading.value = false
  }
}

function updateStageChart(data: any[]) {
  if (!stageChart) return
  stageChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
      data: data.map(d => ({ name: d.stage, value: d.count }))
    }]
  })
}

function updateHourlyChart(data: any[]) {
  if (!hourlyChart) return
  hourlyChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.hour + '时') },
    yAxis: { type: 'value' },
    series: [
      { name: '处方数', type: 'bar', data: data.map(d => d.prescriptionCount), itemStyle: { color: '#409EFF' } },
      { name: '完成数', type: 'line', data: data.map(d => d.completedCount), smooth: true, itemStyle: { color: '#67C23A' } }
    ]
  })
}

function updateUtilChart(data: any[]) {
  if (!utilChart) return
  utilChart.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}: {c}%' },
    xAxis: { type: 'category', data: data.map(d => d.deviceCode) },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [{
      type: 'bar',
      data: data.map(d => d.utilization),
      itemStyle: {
        color: (p: any) => {
          const v = p.value as number
          if (v >= 80) return '#67C23A'
          if (v >= 60) return '#E6A23C'
          return '#F56C6C'
        }
      }
    }]
  })
}

function initCharts() {
  nextTick(() => {
    if (stageChartRef.value) {
      stageChart = echarts.init(stageChartRef.value)
    }
    if (hourlyChartRef.value) {
      hourlyChart = echarts.init(hourlyChartRef.value)
    }
    if (utilChartRef.value) {
      utilChart = echarts.init(utilChartRef.value)
    }
    window.addEventListener('resize', () => {
      stageChart?.resize()
      hourlyChart?.resize()
      utilChart?.resize()
    })
  })
}

onMounted(() => {
  initCharts()
  loadAll()
})

onUnmounted(() => {
  stageChart?.dispose()
  hourlyChart?.dispose()
  utilChart?.dispose()
})
</script>

<style scoped lang="scss">
.eq-dashboard {
  padding: 16px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h2 { margin: 0; font-size: 20px; }
  }

  .metrics-row {
    margin-bottom: 16px;

    .metric-card {
      text-align: center;

      .metric-icon {
        width: 48px;
        height: 48px;
        border-radius: 8px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 8px;
      }

      .metric-value {
        font-size: 28px;
        font-weight: bold;
        color: #333;
      }

      .metric-label {
        font-size: 13px;
        color: #666;
        margin-top: 4px;
      }
    }
  }

  .chart-row { margin-bottom: 16px; }

  .chart-container { height: 300px; }

  .card-title { font-weight: bold; }
}
</style>
