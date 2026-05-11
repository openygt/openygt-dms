<template>
  <div class="page-container">

    <el-card class="search-card" shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" @click="openAddRecordDialog">
            <el-icon><Edit /></el-icon> 补录记录
          </el-button>
          <el-text type="info" size="small">日常任务由员工领取自动记录，此处仅用于异常补录</el-text>
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 160px"
            @change="reloadAll"
          />
        </div>
        <div class="toolbar-right">
          <el-statistic title="任务记录" :value="summary.totalAssignments" />
          <el-statistic title="涉及设备" :value="summary.deviceAssignments" />
          <el-statistic title="涉及员工" :value="summary.employeeAssignments" />
        </div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <span>员工当日工作量</span>
          </template>
          <el-table :data="employeeLoad" size="small" border>
            <el-table-column prop="employeeName" label="员工" min-width="120" />
            <el-table-column prop="assignedCount" label="已领取" width="90" />
            <el-table-column prop="completedCount" label="已完成" width="90" />
            <el-table-column prop="pendingCount" label="进行中" width="90" />
            <el-table-column prop="loadRate" label="饱和度" width="120">
              <template #default="{ row }">
                <el-progress :percentage="row.loadRate" :color="loadColor" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <span>设备当日工作量</span>
          </template>
          <el-table :data="deviceLoad" size="small" border>
            <el-table-column prop="deviceName" label="设备" min-width="140" />
            <el-table-column prop="assignedCount" label="已领取" width="90" />
            <el-table-column prop="runningCount" label="进行中" width="90" />
            <el-table-column prop="idleCount" label="待领取" width="90" />
            <el-table-column prop="utilization" label="利用率" width="120">
              <template #default="{ row }">
                <el-progress :percentage="row.utilization" :color="loadColor" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <span>今日任务流水</span>
      </template>
      <div v-if="!ganttRows.length" class="empty-wrap">
        <el-empty description="暂无任务流水" :image-size="72" />
      </div>
      <div v-else class="gantt-wrapper">
        <div class="gantt-header">
          <div class="gantt-resource">设备/员工</div>
          <div class="gantt-timeline">
            <div v-for="slot in timeSlots" :key="slot" class="gantt-hour">{{ slot }}</div>
          </div>
        </div>
        <div v-for="row in ganttRows" :key="row.id" class="gantt-row">
          <div class="gantt-resource">{{ row.name }}</div>
          <div class="gantt-timeline">
            <div
              v-for="task in row.tasks"
              :key="task.id"
              class="gantt-bar"
              :style="barStyle(task)"
              :title="`${task.name} ${task.startLabel}-${task.endLabel}`"
            >
              {{ task.name }}
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="addRecordDialogVisible" title="补录记录" width="520px">
      <el-form ref="addRecordFormRef" :model="addRecordForm" :rules="addRecordRules" label-width="100px">
        <el-form-item label="任务" prop="taskId">
          <el-select
            v-model="addRecordForm.taskId"
            placeholder="选择任务"
            filterable
            style="width: 100%"
          >
            <el-option v-for="task in availableTasks" :key="task.id" :label="task.name" :value="task.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="员工" prop="employeeId">
          <el-select
            v-model="addRecordForm.employeeId"
            placeholder="选择员工"
            filterable
            style="width: 100%"
          >
            <el-option v-for="employee in availableEmployees" :key="employee.id" :label="employee.name" :value="employee.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备" prop="deviceId">
          <el-select
            v-model="addRecordForm.deviceId"
            placeholder="选择设备"
            filterable
            style="width: 100%"
          >
            <el-option v-for="device in availableDevices" :key="device.id" :label="device.name" :value="device.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addRecordForm.reason" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addRecordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddRecord">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reassignDialogVisible" title="修改记录" width="520px">
      <el-form ref="reassignFormRef" :model="reassignForm" :rules="reassignRules" label-width="100px">
        <el-form-item label="当前任务">
          <span>{{ currentAssignment?.taskName }}</span>
        </el-form-item>
        <el-form-item label="新员工" prop="newEmployeeId">
          <el-select v-model="reassignForm.newEmployeeId" clearable filterable placeholder="选择员工" style="width: 100%">
            <el-option v-for="employee in employees" :key="employee.id" :label="employee.name" :value="employee.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="新设备" prop="newDeviceId">
          <el-select v-model="reassignForm.newDeviceId" clearable filterable placeholder="选择设备" style="width: 100%">
            <el-option v-for="device in devices" :key="device.id" :label="device.name" :value="device.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="reassignForm.reason" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reassignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReassign">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import request from '@/api/request'
import { getDeviceList } from '@/api/equipment'
import { getDeviceLoad, getEmployeeLoad, getSchedule, getOccupiedIds, manualAssign, reassign } from '@/api/newModules'

interface AssignmentView {
  id: number
  taskId: number
  taskName: string
  deviceName: string
  employeeName: string
  assignTypeLabel: string
  assignTypeTag: string
  statusLabel: string
  statusTag: string
  assignedAt: string
}

interface GanttTaskView {
  id: string
  name: string
  startRatio: number
  widthRatio: number
  startLabel: string
  endLabel: string
}

const selectedDate = ref(new Date().toISOString().slice(0, 10))
const loading = ref(false)
const assignments = ref<AssignmentView[]>([])
const employeeLoad = ref<any[]>([])
const deviceLoad = ref<any[]>([])
const pendingTasks = ref<any[]>([])
const employees = ref<any[]>([])
const devices = ref<any[]>([])
const ganttRows = ref<any[]>([])

const occupiedIds = ref<{ taskIds: number[]; employeeIds: number[]; deviceIds: number[] }>({
  taskIds: [], employeeIds: [], deviceIds: []
})

const availableEmployees = computed(() =>
  employees.value.filter(e => !occupiedIds.value.employeeIds.includes(e.id))
)
const availableDevices = computed(() =>
  devices.value.filter(d => !occupiedIds.value.deviceIds.includes(d.id))
)
const availableTasks = computed(() =>
  pendingTasks.value.filter(t => !occupiedIds.value.taskIds.includes(t.id))
)

const addRecordDialogVisible = ref(false)
const reassignDialogVisible = ref(false)
const currentAssignment = ref<AssignmentView | null>(null)

const addRecordFormRef = ref<any>(null)
const addRecordForm = reactive({
  taskId: null as number | null,
  employeeId: null as number | null,
  deviceId: null as number | null,
  reason: ''
})
const addRecordRules = {
  taskId: [{ required: true, message: '请选择任务', trigger: 'change' }],
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }]
}

const reassignFormRef = ref<any>(null)
const reassignForm = reactive({
  newEmployeeId: null as number | null,
  newDeviceId: null as number | null,
  reason: ''
})
const reassignRules = {
  newEmployeeId: [{ required: true, message: '请选择新员工', trigger: 'change' }],
  newDeviceId: [{ required: true, message: '请选择新设备', trigger: 'change' }]
}

const loadColor = [
  { color: '#67c23a', percentage: 50 },
  { color: '#e6a23c', percentage: 80 },
  { color: '#f56c6c', percentage: 100 }
]

const summary = computed(() => ({
  totalAssignments: assignments.value.length,
  deviceAssignments: assignments.value.filter(item => item.deviceName !== '-').length,
  employeeAssignments: assignments.value.filter(item => item.employeeName !== '-').length
}))

const timeSlots = computed(() => {
  if (!ganttRows.value.length) return []
  const allTasks = ganttRows.value.flatMap((row: any) => row.tasks)
  const min = Math.floor(Math.min(...allTasks.map((task: GanttTaskView) => parseFloat(task.startLabel.slice(11, 16).replace(':', '.')) || 0)))
  const max = Math.ceil(Math.max(...allTasks.map((task: GanttTaskView) => parseFloat(task.endLabel.slice(11, 16).replace(':', '.')) || 0)))
  const start = Number.isFinite(min) ? min : 0
  const end = Number.isFinite(max) && max > start ? max : start + 8
  return Array.from({ length: Math.max(1, end - start + 1) }, (_, index) => `${start + index}:00`)
})

function formatDateTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

function getAssignTypeMeta(assignType: number | null | undefined) {
  if (assignType === 1) return { label: '自动', tag: 'success' }
  if (assignType === 2) return { label: '手动', tag: 'primary' }
  if (assignType === 3) return { label: '应急', tag: 'warning' }
  return { label: '-', tag: 'info' }
}

function getStatusMeta(status: string | number | null | undefined) {
  if (status === 0 || status === '0' || status === 1 || status === '1') return { label: '待执行', tag: 'info' }
  if (status === 2 || status === '2') return { label: '执行中', tag: 'primary' }
  if (status === 'COMPLETED' || status === 'COMPLETED') return { label: '已完成', tag: 'success' }
  if (status === 'CANCELLED' || status === 'CANCELLED') return { label: '已取消', tag: 'danger' }
  return { label: String(status ?? '-'), tag: 'info' }
}

function barStyle(task: GanttTaskView) {
  return {
    left: `${task.startRatio}%`,
    width: `${Math.max(task.widthRatio, 6)}%`
  }
}

function normalizeEmployeeLoad(items: any[]) {
  return items.map((item) => {
    const assignedCount = Number(item.assignedCount || 0)
    const completedCount = Number(item.completedCount || 0)
    const pendingCount = Number(item.pendingCount || 0)
    const total = Math.max(1, assignedCount + pendingCount)
    return {
      employeeId: item.employeeId,
      employeeName: item.employeeName || `员工-${item.employeeId ?? '-'}`,
      assignedCount,
      completedCount,
      pendingCount,
      loadRate: Math.min(100, Math.round((assignedCount / total) * 100))
    }
  })
}

function normalizeDeviceLoad(items: any[]) {
  return items.map((item) => {
    const assignedCount = Number(item.assignedCount || 0)
    const runningCount = Number(item.runningCount || 0)
    const idleCount = Number(item.idleCount || 0)
    const denominator = Math.max(1, runningCount + idleCount)
    return {
      deviceId: item.deviceId,
      deviceName: item.deviceName || item.deviceCode || `设备-${item.deviceId ?? '-'}`,
      assignedCount,
      runningCount,
      idleCount,
      utilization: Math.min(100, Math.round((runningCount / denominator) * 100))
    }
  })
}

function buildGanttRows(items: any[]) {
  const filtered = items.filter(item => item.startTime && item.endTime && (item.deviceId || item.employeeId))
  if (!filtered.length) {
    ganttRows.value = []
    return
  }

  const minTime = Math.min(...filtered.map(item => new Date(item.startTime).getTime()))
  const maxTime = Math.max(...filtered.map(item => new Date(item.endTime).getTime()))
  const total = Math.max(1, maxTime - minTime)
  const groups = new Map<string, { id: string; name: string; tasks: GanttTaskView[] }>()

  filtered.forEach((item) => {
    const start = new Date(item.startTime).getTime()
    const end = new Date(item.endTime).getTime()
    const task: GanttTaskView = {
      id: `${item.assignmentId}-${item.deviceId ?? item.employeeId ?? 'x'}`,
      name: item.taskName || `任务-${item.taskId}`,
      startRatio: ((start - minTime) / total) * 100,
      widthRatio: ((end - start) / total) * 100,
      startLabel: formatDateTime(item.startTime),
      endLabel: formatDateTime(item.endTime)
    }

    if (item.deviceId) {
      const key = `device-${item.deviceId}`
      if (!groups.has(key)) {
        groups.set(key, { id: key, name: item.deviceName || `设备-${item.deviceId}`, tasks: [] })
      }
      groups.get(key)!.tasks.push(task)
    }

    if (item.employeeId) {
      const key = `employee-${item.employeeId}`
      if (!groups.has(key)) {
        groups.set(key, { id: key, name: item.employeeName || `员工-${item.employeeId}`, tasks: [] })
      }
      groups.get(key)!.tasks.push({ ...task, id: `${task.id}-employee` })
    }
  })

  ganttRows.value = Array.from(groups.values())
}

async function loadTaskOptions() {
  const res: any = await request.get('/v1/prod/tasks', { params: { page: 1, size: 200 } })
  pendingTasks.value = (res.data?.records || []).map((item: any) => ({
    id: item.id,
    name: `任务-${item.id} / 处方-${item.prescriptionId || '-'}`
  }))
}

async function loadEmployees() {
  const res: any = await request.get('/v1/sys/users', { params: { page: 1, size: 200 } })
  employees.value = (res.data?.records || []).map((item: any) => ({
    id: item.id,
    name: item.realName || item.username || `用户-${item.id}`
  }))
}

async function loadDevices() {
  const res: any = await getDeviceList({ page: 1, size: 200 })
  devices.value = (res.data?.records || []).map((item: any) => ({
    id: item.id,
    name: item.name || item.deviceCode || `设备-${item.id}`
  }))
}

async function fetchLoadData() {
  const [employeeRes, deviceRes]: any = await Promise.all([getEmployeeLoad(selectedDate.value), getDeviceLoad(selectedDate.value)])
  employeeLoad.value = normalizeEmployeeLoad(employeeRes.data || [])
  deviceLoad.value = normalizeDeviceLoad(deviceRes.data || [])
}

async function fetchAssignments() {
  loading.value = true
  try {
    const params: any = {}
    if (selectedDate.value) {
      params.startTime = selectedDate.value + 'T00:00:00'
      params.endTime = selectedDate.value + 'T23:59:59'
    }
    const res: any = await getSchedule(params)
    const list = res.data || []
    assignments.value = list.map((item: any) => {
      const assignType = getAssignTypeMeta(item.assignType)
      const status = getStatusMeta(item.status)
      return {
        id: item.assignmentId,
        taskId: item.taskId,
        taskName: item.taskName || `任务-${item.taskId}`,
        deviceName: item.deviceName || '-',
        employeeName: item.employeeName || '-',
        assignTypeLabel: assignType.label,
        assignTypeTag: assignType.tag,
        statusLabel: status.label,
        statusTag: status.tag,
        assignedAt: formatDateTime(item.createdAt || item.startTime)
      }
    })
    buildGanttRows(list)
  } finally {
    loading.value = false
  }
}

async function reloadAll() {
  await Promise.all([fetchLoadData(), fetchAssignments(), loadTaskOptions()])
}

async function openAddRecordDialog() {
  try {
    const occRes: any = await getOccupiedIds(selectedDate.value)
    occupiedIds.value = occRes.data || { taskIds: [], employeeIds: [], deviceIds: [] }
  } catch {
    occupiedIds.value = { taskIds: [], employeeIds: [], deviceIds: [] }
  }
  addRecordDialogVisible.value = true
  addRecordForm.taskId = null
  addRecordForm.employeeId = null
  addRecordForm.deviceId = null
  addRecordForm.reason = ''
}

async function handleAddRecord() {
  const valid = await addRecordFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    await manualAssign({
      taskId: addRecordForm.taskId,
      employeeId: addRecordForm.employeeId,
      deviceId: addRecordForm.deviceId,
      reason: addRecordForm.reason || '',
      scheduledDate: selectedDate.value
    })
    ElMessage.success('补录记录成功')
    addRecordDialogVisible.value = false
    await reloadAll()
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '补录失败'
    if (msg !== '取消') ElMessage.error(msg)
  }
}

function openReassignDialog(row: AssignmentView) {
  currentAssignment.value = row
  reassignDialogVisible.value = true
  reassignForm.newEmployeeId = null
  reassignForm.newDeviceId = null
  reassignForm.reason = ''
}

async function handleReassign() {
  if (!currentAssignment.value) return
  const valid = await reassignFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    await reassign(currentAssignment.value.id, {
      newEmployeeId: reassignForm.newEmployeeId,
      newDeviceId: reassignForm.newDeviceId,
      reason: reassignForm.reason || ''
    })
    ElMessage.success('修改成功')
    reassignDialogVisible.value = false
    await reloadAll()
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '修改失败'
    if (msg !== '取消') ElMessage.error(msg)
  }
}

onMounted(async () => {
  await Promise.all([loadEmployees(), loadDevices()])
  await reloadAll()
})
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card {
  margin-top: 8px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left,
.toolbar-right,
.record-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.gantt-wrapper {
  overflow-x: auto;
}

.gantt-header,
.gantt-row {
  display: flex;
  min-width: 900px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.gantt-header {
  font-weight: 600;
  background: var(--el-fill-color-light);
}

.gantt-resource {
  width: 160px;
  padding: 8px 12px;
  border-right: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
}

.gantt-timeline {
  flex: 1;
  position: relative;
  display: flex;
  min-height: 42px;
  align-items: center;
}

.gantt-hour {
  flex: 1;
  text-align: center;
  border-right: 1px dashed var(--el-border-color-lighter);
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.gantt-bar {
  position: absolute;
  top: 8px;
  height: 26px;
  line-height: 26px;
  background: var(--el-color-primary);
  color: #fff;
  border-radius: 4px;
  font-size: 12px;
  padding: 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty-wrap {
  padding: 24px 0;
}
</style>
