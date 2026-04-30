<template>
  <div class="device-trace-detail" v-loading="loading">
    <div class="page-header">
      <h2>追溯详情 - {{ prescriptionNo }}</h2>
      <el-button @click="router.back()">返回</el-button>
    </div>

    <el-card class="info-card">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="处方号">{{ trace?.prescriptionNo }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ trace?.patientName }}</el-descriptions-item>
        <el-descriptions-item label="患者电话">{{ trace?.patientPhone }}</el-descriptions-item>
        <el-descriptions-item label="煎药设备">{{ trace?.decoctDeviceCode }}</el-descriptions-item>
        <el-descriptions-item label="包装设备">{{ trace?.packerDeviceCode }}</el-descriptions-item>
        <el-descriptions-item label="方案">{{ trace?.schemeName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(trace?.status)">{{ statusText(trace?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="留样">{{ trace?.sampleCount || 0 }} 袋</el-descriptions-item>
        <el-descriptions-item label="包装容量">{{ trace?.packageVolume || '--' }} ml</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-tabs v-model="activeTab" class="detail-tabs">
      <el-tab-pane label="工序时间轴" name="timeline">
        <el-empty v-if="!events.length" description="暂无工序记录" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="(evt, idx) in events"
            :key="idx"
            :type="evt.eventType === 'ERROR' ? 'danger' : 'success'"
            :timestamp="formatTime(evt.eventTime)"
          >
            <div class="step-card">
              <div class="step-name">{{ evt.eventName || evt.eventCode }}</div>
              <div class="step-operator" v-if="evt.operatorName">操作人: {{ evt.operatorName }}</div>
              <div class="step-device" v-if="evt.deviceCode">设备: {{ evt.deviceCode }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </el-tab-pane>

      <el-tab-pane label="温度曲线" name="temperature">
        <el-empty v-if="!tempCurveData.length" description="暂无温度数据" />
        <div v-else ref="tempChartRef" class="temp-chart"></div>
      </el-tab-pane>

      <el-tab-pane label="操作记录" name="events">
        <el-empty v-if="!events.length" description="暂无操作记录" />
        <el-table v-else :data="events" size="small">
          <el-table-column prop="eventTime" label="时间" width="160">
            <template #default="{ row }">{{ formatTime(row.eventTime) }}</template>
          </el-table-column>
          <el-table-column prop="eventName" label="事件" width="150">
            <template #default="{ row }">{{ row.eventName || row.eventCode }}</template>
          </el-table-column>
          <el-table-column prop="eventType" label="类型" width="100" />
          <el-table-column prop="operatorName" label="操作人" width="120" />
          <el-table-column prop="deviceCode" label="设备" width="120" />
          <el-table-column prop="remark" label="备注" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getTraceByPrescriptionNo, getTraceEvents, getTraceTemperatureCurve } from '@/api/equipment'

const route = useRoute()
const router = useRouter()
const prescriptionNo = ref(route.params.prescriptionNo as string)
const loading = ref(false)
const trace = ref<any>(null)
const events = ref<any[]>([])
const tempCurveData = ref<any[]>([])
const activeTab = ref('timeline')
const tempChartRef = ref<HTMLElement>()
let tempChart: echarts.ECharts | null = null

function statusTagType(status?: string) {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'SOAKING': return 'warning'
    case 'FIRST_DECOCTING': return 'primary'
    case 'SECOND_DECOCTING': return 'primary'
    case 'PACKAGING': return 'primary'
    case 'ABNORMAL': return 'danger'
    default: return 'info'
  }
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    PENDING: '待处理', RECEIVED: '已接方', AUDIT_PASS: '审方通过',
    DISPENSED: '调剂完成', REVIEWED: '复核通过', SOAKING: '浸泡中',
    FIRST_DECOCTING: '一煎中', SECOND_DECOCTING: '二煎中',
    PACKAGING: '包装中', COMPLETED: '已完成', ABNORMAL: '异常'
  }
  return map[status || ''] || status || '--'
}

function formatTime(time?: string) {
  if (!time) return '--'
  return time.replace('T', ' ').substring(0, 19)
}

async function loadTrace() {
  loading.value = true
  try {
    const res: any = await getTraceByPrescriptionNo(prescriptionNo.value)
    trace.value = res.data
  } catch (err) {
    ElMessage.error('加载追溯详情失败')
  } finally {
    loading.value = false
  }
}

async function loadEvents() {
  try {
    const res: any = await getTraceEvents(prescriptionNo.value)
    events.value = res.data || []
  } catch (err) {
    events.value = []
  }
}

async function loadTemperature() {
  try {
    const res: any = await getTraceTemperatureCurve(prescriptionNo.value)
    const result = res.data || {}
    if (result.data && typeof result.data === 'string') {
      try {
        tempCurveData.value = JSON.parse(result.data)
      } catch (e) {
        tempCurveData.value = []
      }
    } else if (Array.isArray(result.data)) {
      tempCurveData.value = result.data
    } else {
      tempCurveData.value = []
    }
  } catch (err) {
    tempCurveData.value = []
  }
}

function updateTempChart() {
  if (!tempChart || !tempCurveData.value.length) return
  const data = tempCurveData.value
  const isPointArray = Array.isArray(data[0])
  tempChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: isPointArray ? 'time' : 'category', data: isPointArray ? undefined : data.map((d: any) => d.time || d.timestamp) },
    yAxis: { type: 'value', name: '温度(°C)' },
    series: [{
      type: 'line',
      data: isPointArray ? data : data.map((d: any) => d.temperature || d.value || d.temp),
      smooth: true,
      lineStyle: { color: '#F56C6C' },
      itemStyle: { color: '#F56C6C' }
    }]
  })
}

watch(activeTab, (tab) => {
  if (tab === 'temperature') {
    nextTick(() => {
      if (tempChartRef.value) {
        tempChart = echarts.init(tempChartRef.value)
        window.addEventListener('resize', () => tempChart?.resize())
        loadTemperature().then(() => updateTempChart())
      }
    })
  }
})

onMounted(() => {
  loadTrace()
  loadEvents()
  if (route.query.tab === 'temperature') {
    activeTab.value = 'temperature'
  }
})

onUnmounted(() => {
  tempChart?.dispose()
})
</script>

<style scoped lang="scss">
.device-trace-detail {
  padding: 16px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h2 { margin: 0; font-size: 20px; }
  }

  .info-card { margin-bottom: 16px; }

  .detail-tabs {
    .step-card {
      .step-name { font-weight: bold; font-size: 14px; }
      .step-operator { color: #666; font-size: 13px; margin-top: 4px; }
      .step-device { color: #999; font-size: 12px; margin-top: 2px; }
    }

    .temp-chart {
      height: 400px;
    }
  }
}
</style>
