<template>
  <div class="temperature-curve-page">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="任务条码">
          <el-input v-model="searchForm.barcode" placeholder="请输入任务条码" clearable />
        </el-form-item>
        <el-form-item label="处方号">
          <el-input v-model="searchForm.prescriptionNo" placeholder="请输入处方号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="queryCurve">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="curveData" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>温度曲线</span>
          <div class="stat-tags">
            <el-tag v-if="curveData.maxTemp" type="danger">最高温 {{ curveData.maxTemp }}°C</el-tag>
            <el-tag v-if="curveData.avgTemp" type="warning">平均温 {{ curveData.avgTemp }}°C</el-tag>
            <el-tag v-if="curveData.duration" type="success">时长 {{ curveData.duration }}min</el-tag>
          </div>
        </div>
      </template>
      <div ref="chartRef" class="chart-container"></div>
    </el-card>

    <el-empty v-else description="请输入任务条码或处方号查询温度曲线" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import request from '@/api/request'

const searchForm = reactive({
  barcode: '',
  prescriptionNo: ''
})

const curveData = ref<any>(null)
const chartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null

async function queryCurve() {
  if (!searchForm.prescriptionNo && !searchForm.barcode) {
    return
  }
  try {
    let prescriptionNo = searchForm.prescriptionNo
    if (!prescriptionNo && searchForm.barcode) {
      // 先通过 PDA 接口查询任务获取处方号
      const taskRes: any = await request.get(`/v1/pda/task/${searchForm.barcode}`)
      const task = taskRes.data
      prescriptionNo = task?.prescriptionNumber || ''
    }
    if (!prescriptionNo) {
      return
    }
    const res: any = await request.get(`/v1/equipment/traces/${prescriptionNo}/temperature-curve`, {
      params: { granularity: '1min' }
    })
    curveData.value = res.data
    nextTick(() => renderChart())
  } catch (e) {
    console.error(e)
  }
}

function renderChart() {
  if (!chartRef.value || !curveData.value) return
  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(chartRef.value)

  const points = curveData.value.points || []
  const times = points.map((p: any) => p.time)
  const temps = points.map((p: any) => p.temperature)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0]
        return `${p.name}<br/>温度: ${p.value} °C`
      }
    },
    grid: { left: 60, right: 40, top: 40, bottom: 60 },
    xAxis: {
      type: 'category',
      data: times,
      name: '时间',
      axisLabel: { rotate: 45, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '温度 (°C)',
      min: (value: any) => Math.floor(value.min - 5),
      max: (value: any) => Math.ceil(value.max + 5)
    },
    series: [{
      data: temps,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 3, color: '#0066CC' },
      itemStyle: { color: '#0066CC' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(0,102,204,0.3)' },
            { offset: 1, color: 'rgba(0,102,204,0.05)' }
          ]
        }
      },
      markLine: {
        data: [
          { type: 'average', name: '平均值', lineStyle: { color: '#ff9800' } }
        ]
      }
    }],
    dataZoom: [
      { type: 'inside', start: 0, end: 100 },
      { type: 'slider', start: 0, end: 100, bottom: 10 }
    ]
  }
  chartInstance.setOption(option)
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.temperature-curve-page {
  padding: 20px;
}
.search-card {
  margin-bottom: 20px;
}
.chart-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.stat-tags {
  display: flex;
  gap: 12px;
}
.chart-container {
  width: 100%;
  height: 480px;
}
</style>
