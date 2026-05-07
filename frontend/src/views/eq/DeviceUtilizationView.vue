<template>
  <div class="device-utilization" v-loading="pageLoading" element-loading-text="加载中..." element-loading-background="rgba(255,255,255,0.9)">
    <div class="page-header">
    </div>

    <el-card class="filter-card">
      <el-form :inline="true">
        <el-form-item label="设备">
          <el-select v-model="filter.deviceCode" placeholder="请选择" clearable style="width: 220px">
            <el-option v-for="d in deviceOptions" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-radio-group v-model="filter.days">
            <el-radio-button :label="7">近7天</el-radio-button>
            <el-radio-button :label="30">近30天</el-radio-button>
            <el-radio-button :label="90">近90天</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24">
        <el-card>
          <template #header><span class="card-title">利用率趋势</span></template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="detail-card">
      <el-table :data="utilizationList" v-loading="loading">
        <el-table-column prop="deviceCode" label="设备" width="120" />
        <el-table-column prop="statDate" label="日期" width="120" />
        <el-table-column label="利用率" width="120">
          <template #default="{ row }">
            <span :style="{ color: getUtilColor(row.utilizationRate) }">
              {{ row.utilizationRate }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="runMinutes" label="运行时长" width="120">
          <template #default="{ row }">{{ formatMinutes(row.runMinutes) }}</template>
        </el-table-column>
        <el-table-column prop="idleMinutes" label="空闲时长" width="120">
          <template #default="{ row }">{{ formatMinutes(row.idleMinutes) }}</template>
        </el-table-column>
        <el-table-column prop="faultCount" label="故障次数" width="100" />
        <el-table-column prop="taskCount" label="任务数" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getDeviceList, getDeviceUtilizationStats, getDeviceUtilizationTrend } from '@/api/equipment'

const pageLoading = ref(true)
const loading = ref(false)
const utilizationList = ref<any[]>([])
const deviceOptions = ref<string[]>([])
const trendChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null

const filter = reactive({
  deviceCode: '',
  days: 7
})

async function loadData() {
  loading.value = true
  try {
    const end = new Date().toISOString().split('T')[0]
    const start = new Date(Date.now() - filter.days * 86400000).toISOString().split('T')[0]

    const res: any = await getDeviceUtilizationStats({
      deviceCode: filter.deviceCode,
      startDate: start,
      endDate: end
    })
    utilizationList.value = res.data || []

    if (filter.deviceCode) {
      const trendRes: any = await getDeviceUtilizationTrend(filter.deviceCode, filter.days)
      updateTrendChart(trendRes.data || [])
    }
  } catch (err) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function updateTrendChart(data: any[]) {
  if (!trendChart) return
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.date) },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [{
      type: 'line',
      data: data.map(d => d.utilization),
      smooth: true,
      areaStyle: { opacity: 0.2 },
      itemStyle: { color: '#1890FF' }
    }]
  })
}

function getUtilColor(rate: number) {
  if (rate >= 80) return '#52C41A'
  if (rate >= 60) return '#FAAD14'
  return '#F5222D'
}

function formatMinutes(mins: number) {
  if (!mins) return '0分'
  const h = Math.floor(mins / 60)
  const m = mins % 60
  if (h > 0) return `${h}小时${m}分`
  return `${m}分`
}

function exportData() {
  ElMessage.success('导出功能开发中')
}

async function loadDeviceOptions() {
  try {
    const res: any = await getDeviceList({ page: 1, size: 999 })
    const devices = res.data?.records || res.data || []
    deviceOptions.value = devices.map((d: any) => d.deviceCode).filter(Boolean)
  } catch (err) {
    // 静默失败，保留空列表
  }
}

onMounted(() => {
  nextTick(() => {
    if (trendChartRef.value) {
      trendChart = echarts.init(trendChartRef.value)
      window.addEventListener('resize', () => trendChart?.resize())
    }
    Promise.all([loadDeviceOptions(), loadData()]).finally(() => {
      setTimeout(() => { pageLoading.value = false }, 400)
    })
  })
})

onUnmounted(() => {
  trendChart?.dispose()
})
</script>

<style scoped lang="scss">
.device-utilization {
  padding: 16px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h2 { margin: 0; font-size: 20px; }
  }

  .filter-card { margin-bottom: 16px; }

  .chart-container {
    height: 300px;
  }

  .detail-card {
    margin-top: 16px;
  }
}
</style>
