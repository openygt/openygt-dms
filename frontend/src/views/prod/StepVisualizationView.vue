<template>
  <div class="page-container">
    <div class="page-header-title">{{ pageTitle }}<span v-if="pageDesc" class="page-header-sub">：{{ pageDesc }}</span></div>

    <!-- 任务选择区域 -->
    <el-card class="task-selector-card" shadow="never">
      <div class="task-selector">
        <span class="selector-label">任务编号：</span>
        <el-input
          v-model="taskSearchInput"
          placeholder="请输入任务号"
          class="task-select"
          clearable
          @keyup.enter="doSearchTask"
        />
        <el-button type="primary" :loading="taskSearchLoading" @click="doSearchTask">搜索</el-button>
        <el-tag v-if="selectedTaskId && currentStatus === 'COMPLETED'" type="success">已完成</el-tag>
        <el-tag v-else-if="selectedTaskId && currentStatus === 'EXCEPTION'" type="danger">异常</el-tag>
        <el-tag v-else-if="selectedTaskId && currentStatus === 'NORMAL'" type="primary">进行中</el-tag>
        <el-tag v-else-if="selectedTaskId" type="info">未开始</el-tag>
      </div>
    </el-card>

    <!-- 搜索结果列表 -->
    <el-card v-if="taskOptions.length > 0 && !selectedTaskId" class="search-result-card" shadow="never">
      <template #header>
        <span>搜索结果（{{ taskOptions.length }} 条）</span>
      </template>
      <el-table :data="taskOptions" highlight-current-row @row-click="(row: any) => handleTaskChange(row.id)">
        <el-table-column prop="id" label="任务号" width="100" />
        <el-table-column prop="prescriptionNumber" label="处方号" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleTaskChange(row.id)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 空数据提示 -->
    <el-empty
      v-if="!selectedTaskId && taskOptions.length === 0"
      description="请输入任务号搜索"
      class="empty-hint"
    >
      <template #image>
        <el-icon :size="60" color="#c0c4cc"><Document /></el-icon>
      </template>
    </el-empty>

    <!-- 步骤展示区域 -->
    <el-card v-show="selectedTaskId" class="step-card" shadow="never" v-loading="stepLoading">
      <template #header>
        <div class="card-header">
          <span>任务 #{{ selectedTaskId }}</span>
          <el-button v-if="selectedStep" link type="primary" @click="selectedStep = null; drawerVisible = false">关闭详情</el-button>
        </div>
      </template>

      <el-steps :active="currentStepIndex" align-center class="custom-steps">
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

      <!-- 步骤进度提示 -->
      <div v-if="processingStepName" class="step-progress-hint">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>当前正在进行：{{ processingStepName }}</span>
      </div>
    </el-card>

    <!-- 步骤详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="selectedStep ? `${selectedStep.name} - 详情` : '步骤详情'"
      direction="rtl"
      size="400px"
      :loading="detailLoading"
    >
      <el-descriptions v-if="selectedStep" :column="1" border>
        <el-descriptions-item label="步骤编码">{{ selectedStep.code }}</el-descriptions-item>
        <el-descriptions-item label="步骤名称">{{ selectedStep.name }}</el-descriptions-item>
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
        <el-descriptions-item v-if="selectedStep.pauseDuration" label="暂停时长">
          {{ selectedStep.pauseDuration }} 分钟
        </el-descriptions-item>
        <el-descriptions-item v-if="selectedStep.pauseReason" label="暂停原因">
          {{ selectedStep.pauseReason }}
        </el-descriptions-item>
        <el-descriptions-item v-if="selectedStep.delayMinutes" label="延迟时长">
          {{ selectedStep.delayMinutes }} 分钟
        </el-descriptions-item>
        <el-descriptions-item v-if="selectedStep.delayReason" label="延迟原因">
          {{ selectedStep.delayReason }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 工作记录 -->
      <div v-if="selectedStep?.workRecords?.length" class="work-records">
        <el-divider>工作记录</el-divider>
        <el-timeline>
          <el-timeline-item
            v-for="record in selectedStep.workRecords"
            :key="record.createdAt"
            :timestamp="formatDateTime(record.createdAt)"
            placement="top"
          >
            <div class="work-record-item">
              <span class="operator">{{ record.operatorName || record.operatorId || '未知' }}</span>
              <span class="action">{{ record.action }}</span>
              <span v-if="record.workTime" class="duration">{{ record.workTime }}分钟</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMenuDesc } from '@/composables/useMenuDesc'
import { ElMessage } from 'element-plus'
import { Check, Close, Loading, Document } from '@element-plus/icons-vue'
import { getTaskSteps, getStepDetail } from '@/api/newModules'
import request from '@/api/request'

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
  pauseDuration?: number
  pauseReason?: string
  delayMinutes?: number
  delayReason?: string
  workRecords?: WorkRecord[]
}

interface WorkRecord {
  operatorId: string
  operatorName: string
  action: string
  workTime: number
  createdAt: string
}

interface TaskOption {
  id: number
  prescriptionNumber?: string
  status?: string
  operatorName?: string
}

const route = useRoute()
const router = useRouter()
const { title, description } = useMenuDesc()
const pageTitle = computed(() => title.value)
const pageDesc = computed(() => description.value)

// 任务选择相关
const selectedTaskId = ref<number | null>(null)
const taskOptions = ref<TaskOption[]>([])
const taskSearchLoading = ref(false)
const taskSearchInput = ref('')
const taskSelectRef = ref<any>(null)

// 步骤相关
const stepList = ref<StepItem[]>(getDefaultSteps())
const stepLoading = ref(false)
const selectedStep = ref<StepItem | null>(null)
const drawerVisible = ref(false)
const detailLoading = ref(false)

// 步骤详情缓存
const stepDetailCache = ref<Map<string, StepItem>>(new Map())

function getDefaultSteps(): StepItem[] {
  return [
    { code: 'RECEIVE', name: '接收', status: 'PENDING' },
    { code: 'ADJUST', name: '调配', status: 'PENDING' },
    { code: 'SOAK', name: '泡药', status: 'PENDING' },
    { code: 'FIRST_DECOCTION', name: '一煎', status: 'PENDING' },
    { code: 'SECOND_DECOCTION', name: '二煎', status: 'PENDING' },
    { code: 'POUR', name: '出液', status: 'PENDING' },
    { code: 'WRAP', name: '包装', status: 'PENDING' },
    { code: 'QC', name: '质检', status: 'PENDING' },
    { code: 'DELIVER', name: '交付', status: 'PENDING' }
  ]
}

function formatTaskLabel(task: TaskOption): string {
  const parts = [`任务 #${task.id}`]
  if (task.prescriptionNumber) parts.push(task.prescriptionNumber)
  if (task.status) parts.push(`[${task.status}]`)
  if (task.operatorName) parts.push(`- ${task.operatorName}`)
  return parts.join(' ')
}

// 计算当前任务整体状态
const currentStatus = computed(() => {
  if (!stepList.value.length) return 'PENDING'
  const hasException = stepList.value.some(s => s.status === 'EXCEPTION')
  if (hasException) return 'EXCEPTION'
  const allCompleted = stepList.value.every(s => s.status === 'COMPLETED')
  if (allCompleted) return 'COMPLETED'
  const hasProcessing = stepList.value.some(s => s.status === 'PROCESSING')
  if (hasProcessing) return 'NORMAL'
  return 'PENDING'
})

// 当前正在进行的步骤名称
const processingStepName = computed(() => {
  const step = stepList.value.find(s => s.status === 'PROCESSING')
  return step?.name || null
})

/**
 * 计算当前激活步骤索引
 * 修复：当全部完成时，返回最后一个步骤索引（8），而不是超出范围的9
 * 这样 el-steps 能正确高亮所有已完成的步骤
 */
const currentStepIndex = computed(() => {
  // 找到正在进行的步骤
  const processingIdx = stepList.value.findIndex(s => s.status === 'PROCESSING')
  if (processingIdx >= 0) return processingIdx

  // 找到最后一个已完成的步骤
  const completedIndices = stepList.value
    .map((s, i) => s.status === 'COMPLETED' ? i : -1)
    .filter(i => i >= 0)

  if (completedIndices.length === 0) return 0

  // 关键修复：如果全部完成，返回最后一个索引（8），而不是9
  // 这样 el-steps 的 active 不会超出范围
  const lastCompletedIdx = completedIndices[completedIndices.length - 1]
  return Math.min(lastCompletedIdx, stepList.value.length - 1)
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

// 搜索任务
async function searchTasks(query: string) {
  taskSearchLoading.value = true
  try {
    const params: any = { page: 1, size: 20 }
    if (query) {
      // 支持任务号搜索
      if (/^\d+$/.test(query)) {
        params.id = query
      } else {
        params.prescriptionNumber = query
      }
    }
    const res: any = await request.get('/v1/prod/tasks', { params })
    taskOptions.value = Array.isArray(res.data?.records) ? res.data.records : []
  } catch (e) {
    console.error('搜索任务失败', e)
  } finally {
    taskSearchLoading.value = false
  }
}

// 加载初始任务列表
async function loadInitialTasks() {
  taskSearchLoading.value = true
  try {
    const res: any = await request.get('/v1/prod/tasks', { params: { page: 1, size: 20 } })
    taskOptions.value = Array.isArray(res.data?.records) ? res.data.records : []
  } catch (e) {
    console.error('加载任务列表失败', e)
  } finally {
    taskSearchLoading.value = false
  }
}

// 任务切换处理
function handleTaskChange(taskId: number | undefined) {
  if (taskId) {
    router.replace({ query: { taskId: String(taskId) } })
    loadTaskSteps()
    stepDetailCache.value.clear()
    selectedStep.value = null
    drawerVisible.value = false
  } else {
    // 清空选择
    router.replace({ query: {} })
    stepList.value = getDefaultSteps()
    selectedStep.value = null
    drawerVisible.value = false
  }
}

// 聚焦任务选择框
function focusTaskSelect() {
  taskSearchInput.value = ''
}

// 搜索任务按钮
async function doSearchTask() {
  const query = taskSearchInput.value.trim()
  if (!query) {
    ElMessage.warning('请输入任务号')
    return
  }
  taskSearchLoading.value = true
  try {
    const params: any = { page: 1, size: 20 }
    if (/^\d+$/.test(query)) {
      params.id = query
    } else {
      params.prescriptionNumber = query
    }
    const res: any = await request.get('/v1/prod/tasks', { params })
    const records = Array.isArray(res.data?.records) ? res.data.records : []
    if (records.length === 0) {
      ElMessage.warning('未找到匹配的任务')
      return
    }
    if (records.length === 1) {
      handleTaskChange(records[0].id)
    } else {
      taskOptions.value = records
      ElMessage.info(`找到 ${records.length} 条任务，请点击选择`)
    }
  } catch (e) {
    console.error('搜索任务失败', e)
    ElMessage.error('搜索失败')
  } finally {
    taskSearchLoading.value = false
  }
}

// 点击步骤
async function handleStepClick(step: StepItem) {
  if (!selectedTaskId.value) return

  // 检查缓存
  const cacheKey = `${selectedTaskId.value}-${step.code}`
  if (stepDetailCache.value.has(cacheKey)) {
    selectedStep.value = stepDetailCache.value.get(cacheKey) || null
    drawerVisible.value = true
    return
  }

  detailLoading.value = true
  drawerVisible.value = true
  try {
    const res: any = await getStepDetail(selectedTaskId.value, step.code)
    const detail = res.data || {}
    const stepDetail: StepItem = {
      ...step,
      startTime: detail.startTime,
      endTime: detail.endTime,
      operatorId: detail.operatorId,
      operatorName: detail.operatorName,
      durationMinutes: detail.durationMinutes,
      result: detail.result,
      pauseDuration: detail.pauseDuration,
      pauseReason: detail.pauseReason,
      delayMinutes: detail.delayMinutes,
      delayReason: detail.delayReason,
      workRecords: detail.workRecords || []
    }
    stepDetailCache.value.set(cacheKey, stepDetail)
    selectedStep.value = stepDetail
  } catch (e) {
    selectedStep.value = step
  } finally {
    detailLoading.value = false
  }
}

// 加载任务步骤
async function loadTaskSteps() {
  if (!selectedTaskId.value) {
    stepList.value = getDefaultSteps()
    return
  }

  stepLoading.value = true
  try {
    const res: any = await getTaskSteps(selectedTaskId.value)
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
  } finally {
    stepLoading.value = false
  }
}

// 初始化
onMounted(async () => {
  // 从 URL 获取 taskId
  const urlTaskId = route.query.taskId
  if (urlTaskId) {
    selectedTaskId.value = Number(urlTaskId)
    loadTaskSteps()
  }
})

// 监听路由变化
watch(() => route.query.taskId, (newId) => {
  if (newId && Number(newId) !== selectedTaskId.value) {
    selectedTaskId.value = Number(newId)
    loadTaskSteps()
    stepDetailCache.value.clear()
    selectedStep.value = null
    drawerVisible.value = false
  } else if (!newId && selectedTaskId.value) {
    selectedTaskId.value = null
    stepList.value = getDefaultSteps()
    selectedStep.value = null
    drawerVisible.value = false
  }
})
</script>

<style scoped lang="scss">
.page-container {
  padding: 0;
}
.page-header-title {
  margin-bottom: 16px;
}

.task-selector-card {
  margin-bottom: 16px;
}

.task-selector {
  display: flex;
  align-items: center;
  gap: 12px;
}

.selector-label {
  font-weight: 500;
  white-space: nowrap;
}

.task-select {
  width: 360px;
}

.empty-hint {
  margin: 60px 0;
}

.step-card {
  margin-top: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 500;
}

.custom-steps :deep(.el-step__icon) {
  cursor: pointer;
  transition: transform 0.2s;

  &:hover {
    transform: scale(1.1);
  }
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

.step-progress-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 20px;
  padding: 12px;
  background: #ecf5ff;
  border-radius: 8px;
  color: #409eff;
  font-size: 14px;

  .el-icon {
    font-size: 18px;
  }
}

.work-records {
  margin-top: 16px;
}

.work-record-item {
  display: flex;
  gap: 8px;
  align-items: center;

  .operator {
    font-weight: 500;
  }

  .action {
    color: var(--el-text-color-secondary);
  }

  .duration {
    color: var(--el-color-primary);
    font-size: 12px;
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}
</style>
