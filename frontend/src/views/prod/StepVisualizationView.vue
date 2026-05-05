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
              <el-icon v-else-if="step.status === 'PROCESSING'" class="is-loading" :size="20"><Loading /></el-icon>
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
        <el-descriptions-item label="开始时间">{{ formatDateTime(selectedStep.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ formatDateTime(selectedStep.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="持续时长">{{ formatDuration(selectedStep.durationMinutes) }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ selectedStep.operatorName || selectedStep.operatorId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结果">{{ selectedStep.result || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="selectedStep.status === 'COMPLETED'" type="success">已完成</el-tag>
          <el-tag v-else-if="selectedStep.status === 'PROCESSING'" type="primary">进行中</el-tag>
          <el-tag v-else-if="selectedStep.status === 'EXCEPTION'" type="danger">异常</el-tag>
          <el-tag v-else type="info">未开始</el-tag>
        </el-descriptions-item>
      </el-descriptions>
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
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'EXCEPTION'
  startTime?: string
  endTime?: string
  operatorId?: string
  operatorName?: string
  durationMinutes?: number
  result?: string
}

const route = useRoute()
const taskId = computed(() => route.query.taskId as string || '')

// 步骤定义以服务端字典为唯一事实来源
const stepList = ref<StepItem[]>([
  { code: 'RECEIVE', name: '接收', status: 'PENDING' },
  { code: 'ADJUST', name: '调配', status: 'PENDING' },
  { code: 'SOAK', name: '泡药', status: 'PENDING' },
  { code: 'FIRST_DECOCTION', name: '一煎', status: 'PENDING' },
  { code: 'SECOND_DECOCTION', name: '二煎', status: 'PENDING' },
  { code: 'POUR', name: '出液', status: 'PENDING' },
  { code: 'WRAP', name: '包装', status: 'PENDING' },
  { code: 'QC', name: '质检', status: 'PENDING' },
  { code: 'DELIVER', name: '交付', status: 'PENDING' }
])

const currentStatus = computed(() => {
  const hasException = stepList.value.some(s => s.status === 'EXCEPTION')
  if (hasException) return 'EXCEPTION'
  const hasProcessing = stepList.value.some(s => s.status === 'PROCESSING')
  if (hasProcessing) return 'NORMAL'
  const allCompleted = stepList.value.every(s => s.status === 'COMPLETED')
  if (allCompleted) return 'COMPLETED'
  return 'NORMAL'
})
const selectedStep = ref<StepItem | null>(null)

const currentStepIndex = computed(() => {
  const idx = stepList.value.findIndex(s => s.status === 'PROCESSING')
  if (idx >= 0) return idx
  const lastCompleted = stepList.value.map((s, i) => s.status === 'COMPLETED' ? i : -1).filter(i => i >= 0)
  return lastCompleted.length ? lastCompleted[lastCompleted.length - 1] + 1 : 0
})

function getStepStatus(step: StepItem, index: number): string {
  if (step.status === 'EXCEPTION') return 'error'
  if (step.status === 'COMPLETED') return 'success'
  if (step.status === 'PROCESSING') return 'process'
  if (index < currentStepIndex.value) return 'success'
  return 'wait'
}

function getStepIconClass(step: StepItem, index: number) {
  return {
    'icon-completed': step.status === 'COMPLETED',
    'icon-exception': step.status === 'EXCEPTION',
    'icon-running': step.status === 'PROCESSING',
    'icon-pending': step.status === 'PENDING' && index >= currentStepIndex.value
  }
}

function formatDateTime(dt: string | undefined) {
  if (!dt) return '-'
  const d = new Date(dt)
  if (isNaN(d.getTime())) return dt
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function formatDuration(minutes: number | undefined) {
  if (minutes == null || minutes <= 0) return '-'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  if (h > 0) return `${h}小时${m}分`
  return `${m}分`
}

async function handleStepClick(step: StepItem) {
  if (!taskId.value) return
  try {
    const res: any = await getStepDetail(taskId.value, step.code)
    const detail = res.data || {}
    selectedStep.value = {
      ...step,
      startTime: detail.startTime,
      endTime: detail.endTime,
      operatorId: detail.operatorId,
      operatorName: detail.operatorName,
      durationMinutes: detail.durationMinutes,
      result: detail.result
    }
  } catch (e) {
    selectedStep.value = step
  }
}

async function loadTaskSteps() {
  if (!taskId.value) return
  try {
    const res: any = await getTaskSteps(taskId.value)
    const steps = Array.isArray(res.data) ? res.data : []
    if (steps.length) {
      const map = new Map(steps.map((s: any) => [s.stepCode, s]))
      stepList.value = stepList.value.map(s => {
        const backend: any = map.get(s.code)
        if (!backend) return s
        return {
          ...s,
          name: backend.stepName || s.name,
          status: backend.status || s.status,
          startTime: backend.startTime,
          endTime: backend.endTime,
          operatorId: backend.operatorId,
          operatorName: backend.operatorName,
          durationMinutes: backend.durationMinutes,
          result: backend.result
        }
      })
    }
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

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}
</style>
