<template>
  <div class="page-container">
    <div class="page-header-title">流程跟踪：<span class="page-header-sub">泡药→头煎→二煎→出液→包装→质检，走到哪一步</span></div>
    <el-card class="step-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>任务编号: {{ taskId }}</span>
          <el-tag v-if="currentStatus === 'NORMAL'" type="success">进行中</el-tag>
          <el-tag v-else-if="currentStatus === 'EXCEPTION'" type="danger">异常</el-tag>
          <el-tag v-else type="info">未开始</el-tag>
        </div>
      </template>

      <el-steps :active="currentStepIndex" finish-status="success" align-center class="custom-steps">
        <el-step
          v-for="(step, index) in stepList"
          :key="step.code"
          :title="step.name"
          :status="getStepStatus(step, index)"
          @click="handleStepClick(step)"
        >
          <template #icon>
            <div class="step-icon" :class="getStepIconClass(step, index)">
              <el-icon v-if="step.status === 'COMPLETED'" :size="20"><Check /></el-icon>
              <el-icon v-else-if="step.status === 'EXCEPTION'" :size="20"><Close /></el-icon>
              <el-icon v-else-if="step.status === 'RUNNING'" class="is-loading" :size="20"><Loading /></el-icon>
              <span v-else class="step-number">{{ index + 1 }}</span>
            </div>
          </template>
        </el-step>
      </el-steps>
    </el-card>

    <el-card v-if="selectedStep" class="detail-card" shadow="never">
      <template #header>
        <div class="detail-header">
          <span>{{ selectedStep.name }} - 详情</span>
          <el-button link type="primary" @click="selectedStep = null">关闭</el-button>
        </div>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="步骤编码">{{ selectedStep.code }}</el-descriptions-item>
        <el-descriptions-item label="计划开始">{{ selectedStep.planStartTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划结束">{{ selectedStep.planEndTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际开始">{{ selectedStep.actualStartTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际结束">{{ selectedStep.actualEndTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ selectedStep.operatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备">{{ selectedStep.deviceName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="selectedStep.status === 'COMPLETED'" type="success">已完成</el-tag>
          <el-tag v-else-if="selectedStep.status === 'RUNNING'" type="primary">进行中</el-tag>
          <el-tag v-else-if="selectedStep.status === 'EXCEPTION'" type="danger">异常</el-tag>
          <el-tag v-else type="info">未开始</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="温度">{{ selectedStep.temperature ? selectedStep.temperature + '°C' : '-' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="tempCurveData.length" class="chart-section">
        <div class="chart-title">温度曲线</div>
        <div class="temp-curve">
          <div
            v-for="(item, idx) in tempCurveData"
            :key="idx"
            class="temp-bar"
            :style="{ height: item.value + '%', background: getTempColor(item.value) }"
            :title="`${item.time}: ${item.value}°C`"
          />
        </div>
        <div class="temp-labels">
          <span v-for="(item, idx) in tempCurveData" :key="idx" class="temp-label">{{ item.time }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Close, Loading } from '@element-plus/icons-vue'
import { getTaskSteps, getStepDetail } from '@/api/newModules'

interface StepItem {
  code: string
  name: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'EXCEPTION'
  planStartTime?: string
  planEndTime?: string
  actualStartTime?: string
  actualEndTime?: string
  operatorName?: string
  deviceName?: string
  temperature?: number
}

const route = useRoute()
const taskId = computed(() => route.query.taskId as string || '')

const stepList = ref<StepItem[]>([
  { code: 'RECEIVE', name: '处方接收', status: 'PENDING' },
  { code: 'DISPENSE', name: '调剂', status: 'PENDING' },
  { code: 'SOAK', name: '泡药', status: 'PENDING' },
  { code: 'DECOCT_FIRST', name: '头煎', status: 'PENDING' },
  { code: 'DECOCT_SECOND', name: '二煎', status: 'PENDING' },
  { code: 'MERGE', name: '合并', status: 'PENDING' },
  { code: 'FILTER', name: '过滤', status: 'PENDING' },
  { code: 'PACKAGE', name: '包装', status: 'PENDING' },
  { code: 'QC', name: '质检', status: 'PENDING' },
  { code: 'SHIP', name: '发货', status: 'PENDING' }
])

const currentStatus = ref('NORMAL')
const selectedStep = ref<StepItem | null>(null)
const tempCurveData = ref<{ time: string; value: number }[]>([])

const currentStepIndex = computed(() => {
  const idx = stepList.value.findIndex(s => s.status === 'RUNNING')
  if (idx >= 0) return idx
  const lastCompleted = stepList.value.map((s, i) => s.status === 'COMPLETED' ? i : -1).filter(i => i >= 0)
  return lastCompleted.length ? lastCompleted[lastCompleted.length - 1] + 1 : 0
})

function getStepStatus(step: StepItem, index: number): string {
  if (step.status === 'EXCEPTION') return 'error'
  if (step.status === 'COMPLETED') return 'success'
  if (step.status === 'RUNNING') return 'process'
  if (index < currentStepIndex.value) return 'success'
  return 'wait'
}

function getStepIconClass(step: StepItem, index: number) {
  return {
    'icon-completed': step.status === 'COMPLETED',
    'icon-exception': step.status === 'EXCEPTION',
    'icon-running': step.status === 'RUNNING',
    'icon-pending': step.status === 'PENDING' && index >= currentStepIndex.value
  }
}

function getTempColor(value: number) {
  if (value >= 100) return '#f56c6c'
  if (value >= 80) return '#e6a23c'
  return '#67c23a'
}

async function handleStepClick(step: StepItem) {
  if (!taskId.value) return
  try {
    const res: any = await getStepDetail(taskId.value, step.code)
    const detail = res.data || {}
    selectedStep.value = { ...step, ...detail }
    tempCurveData.value = detail.temperatureCurve || [
      { time: '08:00', value: 25 },
      { time: '08:15', value: 45 },
      { time: '08:30', value: 78 },
      { time: '08:45', value: 95 },
      { time: '09:00', value: 100 },
      { time: '09:15', value: 98 },
      { time: '09:30', value: 85 },
      { time: '09:45', value: 60 }
    ]
  } catch (e) {
    selectedStep.value = step
    tempCurveData.value = []
  }
}

async function loadTaskSteps() {
  if (!taskId.value) return
  try {
    const res: any = await getTaskSteps(taskId.value)
    const data = res.data || {}
    if (data.steps && data.steps.length) {
      const map = new Map(data.steps.map((s: StepItem) => [s.code, s]))
      stepList.value = stepList.value.map(s => ({
        ...s,
        ...(map.get(s.code) || {})
      }))
    }
    currentStatus.value = data.status || 'NORMAL'
  } catch (e) {
    ElMessage.error('加载步骤数据失败')
  }
}

onMounted(() => {
  loadTaskSteps()
})
</script>

<style scoped lang="scss">
.page-container {
  padding: var(--ygt-space-4);
}

.step-card {
  margin-top: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 500;
}

.custom-steps :deep(.el-step__icon) {
  cursor: pointer;
}

.step-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
}

.icon-completed {
  background: #67c23a;
}

.icon-exception {
  background: #f56c6c;
  animation: pulse 1.5s infinite;
}

.icon-running {
  background: #409eff;
}

.icon-pending {
  background: #c0c4cc;
}

.step-number {
  font-size: 14px;
}

.detail-card {
  margin-top: 16px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.chart-section {
  margin-top: 16px;
}

.chart-title {
  font-weight: 500;
  margin-bottom: 12px;
}

.temp-curve {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 160px;
  padding: 12px;
  background: var(--ygt-bg-surface);
  border-radius: var(--ygt-radius-md);
}

.temp-bar {
  flex: 1;
  min-width: 20px;
  border-radius: 4px 4px 0 0;
  transition: height 0.6s ease;
}

.temp-labels {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.temp-label {
  flex: 1;
  text-align: center;
  font-size: 12px;
  color: var(--ygt-text-secondary);
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}
</style>
