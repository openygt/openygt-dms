<template>
  <div class="page-container">
    <div class="page-header-title">排产调度：<span class="page-header-sub">甘特图排产、设备分配、员工指派、负载均衡</span></div>
    <!-- 顶部操作栏 -->
    <el-card class="search-card" shadow="never">
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px">
        <div style="display: flex; gap: 12px; flex-wrap: wrap">
          <el-button type="primary" @click="handleAutoAssign">
            <el-icon><MagicStick /></el-icon> 自动分配
          </el-button>
          <el-button type="success" @click="openManualDialog">
            <el-icon><Edit /></el-icon> 手动分配
          </el-button>
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 160px"
            @change="fetchLoadData"
          />
        </div>
        <div style="display: flex; gap: 12px">
          <el-statistic title="今日任务" :value="summary.totalTasks" />
          <el-statistic title="已分配" :value="summary.assigned" />
          <el-statistic title="待分配" :value="summary.pending" />
        </div>
      </div>
    </el-card>

    <!-- 负载统计 -->
    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <span>员工负载</span>
          </template>
          <el-table :data="employeeLoad" size="small" border>
            <el-table-column prop="employeeName" label="员工" min-width="100" />
            <el-table-column prop="roleName" label="岗位" min-width="100" />
            <el-table-column prop="assignedCount" label="已分配" width="90" />
            <el-table-column prop="completedCount" label="已完成" width="90" />
            <el-table-column prop="loadRate" label="负载率" width="120">
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
            <span>设备负载</span>
          </template>
          <el-table :data="deviceLoad" size="small" border>
            <el-table-column prop="deviceName" label="设备" min-width="120" />
            <el-table-column prop="deviceType" label="类型" min-width="100" />
            <el-table-column prop="runningTasks" label="运行中" width="90" />
            <el-table-column prop="queuedTasks" label="排队中" width="90" />
            <el-table-column prop="utilization" label="利用率" width="120">
              <template #default="{ row }">
                <el-progress :percentage="row.utilization" :color="loadColor" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 甘特图 -->
    <el-card shadow="never">
      <template #header>
        <span>任务排程甘特图</span>
      </template>
      <div class="gantt-wrapper">
        <div class="gantt-header">
          <div class="gantt-resource">设备/员工</div>
          <div class="gantt-timeline">
            <div v-for="h in timeSlots" :key="h" class="gantt-hour">{{ h }}:00</div>
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
              :title="`${task.name} (${task.start}-${task.end})`"
            >
              {{ task.name }}
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 分配记录 -->
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>分配记录</span>
          <el-button size="small" @click="fetchAssignments">刷新</el-button>
        </div>
      </template>
      <el-table :data="assignments" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="taskName" label="任务" min-width="140" />
        <el-table-column prop="assigneeName" label="分配对象" min-width="100" />
        <el-table-column prop="assignType" label="分配类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.assignType === 'AUTO' ? 'success' : 'primary'">
              {{ row.assignType === 'AUTO' ? '自动' : '手动' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignedAt" label="分配时间" min-width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReassignDialog(row)">重新分配</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchAssignments"
        />
      </div>
    </el-card>

    <!-- 手动分配弹窗 -->
    <el-dialog v-model="manualDialogVisible" title="手动分配任务" width="520px">
      <el-form :model="manualForm" label-width="100px">
        <el-form-item label="任务" required>
          <el-select v-model="manualForm.taskId" placeholder="选择任务" style="width: 100%">
            <el-option v-for="t in pendingTasks" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分配类型" required>
          <el-radio-group v-model="manualForm.targetType">
            <el-radio label="EMPLOYEE">员工</el-radio>
            <el-radio label="DEVICE">设备</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="员工" v-if="manualForm.targetType === 'EMPLOYEE'" required>
          <el-select v-model="manualForm.employeeId" placeholder="选择员工" style="width: 100%">
            <el-option v-for="e in employees" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备" v-if="manualForm.targetType === 'DEVICE'" required>
          <el-select v-model="manualForm.deviceId" placeholder="选择设备" style="width: 100%">
            <el-option v-for="d in devices" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="manualForm.priority" :min="1" :max="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="manualForm.remark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleManualAssign">确认分配</el-button>
      </template>
    </el-dialog>

    <!-- 重新分配弹窗 -->
    <el-dialog v-model="reassignDialogVisible" title="重新分配" width="520px">
      <el-form :model="reassignForm" label-width="100px">
        <el-form-item label="当前任务">
          <span>{{ currentAssignment?.taskName }}</span>
        </el-form-item>
        <el-form-item label="新分配对象" required>
          <el-select v-model="reassignForm.newAssigneeId" placeholder="选择新对象" style="width: 100%">
            <el-option v-for="e in employees" :key="e.id" :label="e.name" :value="e.id" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Edit } from '@element-plus/icons-vue'
import {
  autoAssign,
  manualAssign,
  reassign,
  getSchedule,
  getEmployeeLoad,
  getDeviceLoad
} from '@/api/newModules'

const selectedDate = ref(new Date().toISOString().slice(0, 10))
const loading = ref(false)
const summary = reactive({ totalTasks: 128, assigned: 96, pending: 32 })

const employeeLoad = ref<any[]>([])
const deviceLoad = ref<any[]>([])
const assignments = ref<any[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })

const timeSlots = Array.from({ length: 12 }, (_, i) => i + 8)
const ganttRows = ref<any[]>([
  {
    id: 'D01', name: '煎药机-01', tasks: [
      { id: 1, name: '处方A', start: 8, end: 10 },
      { id: 2, name: '处方B', start: 10.5, end: 12.5 }
    ]
  },
  {
    id: 'D02', name: '煎药机-02', tasks: [
      { id: 3, name: '处方C', start: 9, end: 11 }
    ]
  },
  {
    id: 'E01', name: '员工-张三', tasks: [
      { id: 4, name: '调剂', start: 8, end: 9 },
      { id: 5, name: '复核', start: 11, end: 12 }
    ]
  }
])

const loadColor = [
  { color: '#67c23a', percentage: 50 },
  { color: '#e6a23c', percentage: 80 },
  { color: '#f56c6c', percentage: 100 }
]

function barStyle(task: any) {
  const left = (task.start - 8) / 12 * 100
  const width = (task.end - task.start) / 12 * 100
  return { left: `${left}%`, width: `${width}%` }
}

function statusTagType(status: string) {
  if (status === '已完成') return 'success'
  if (status === '进行中') return 'primary'
  if (status === '已取消') return 'danger'
  return 'info'
}

async function fetchLoadData() {
  try {
    const date = selectedDate.value
    const empRes = await getEmployeeLoad(date) as any
    employeeLoad.value = empRes.data || []
    const devRes = await getDeviceLoad(date) as any
    deviceLoad.value = devRes.data || []
  } catch {
    employeeLoad.value = [
      { employeeName: '张三', roleName: '调剂员', assignedCount: 12, completedCount: 8, loadRate: 75 },
      { employeeName: '李四', roleName: '煎煮员', assignedCount: 10, completedCount: 6, loadRate: 60 }
    ]
    deviceLoad.value = [
      { deviceName: '煎药机-01', deviceType: '煎药机', runningTasks: 2, queuedTasks: 1, utilization: 66 },
      { deviceName: '煎药机-02', deviceType: '煎药机', runningTasks: 1, queuedTasks: 0, utilization: 33 }
    ]
  }
}

async function fetchAssignments() {
  loading.value = true
  try {
    const res = await getSchedule({ page: pagination.page, size: pagination.size }) as any
    assignments.value = res.data?.list || []
    pagination.total = res.data?.total || 0
  } catch {
    assignments.value = [
      { id: 1, taskName: '处方A-煎煮', assigneeName: '张三', assignType: 'AUTO', assignedAt: '2024-05-01 08:00', status: '进行中' },
      { id: 2, taskName: '处方B-调剂', assigneeName: '李四', assignType: 'MANUAL', assignedAt: '2024-05-01 08:30', status: '已完成' }
    ]
    pagination.total = 2
  } finally {
    loading.value = false
  }
}

const pendingTasks = ref([
  { id: 101, name: '处方C-煎煮' },
  { id: 102, name: '处方D-调剂' }
])
const employees = ref([
  { id: 1, name: '张三' },
  { id: 2, name: '李四' },
  { id: 3, name: '王五' }
])
const devices = ref([
  { id: 'D01', name: '煎药机-01' },
  { id: 'D02', name: '煎药机-02' },
  { id: 'D03', name: '煎药机-03' }
])

const manualDialogVisible = ref(false)
const manualForm = reactive({ taskId: null as any, targetType: 'EMPLOYEE', employeeId: null as any, deviceId: null as any, priority: 5, remark: '' })

function openManualDialog() {
  manualDialogVisible.value = true
  manualForm.taskId = null
  manualForm.employeeId = null
  manualForm.deviceId = null
  manualForm.priority = 5
  manualForm.remark = ''
}

async function handleAutoAssign() {
  try {
    await ElMessageBox.confirm('是否对未分配任务执行智能分配？', '自动分配', { type: 'warning' })
    await autoAssign({ date: selectedDate.value })
    ElMessage.success('自动分配成功')
    fetchAssignments()
    fetchLoadData()
  } catch {
    // cancel
  }
}

async function handleManualAssign() {
  if (!manualForm.taskId) { ElMessage.warning('请选择任务'); return }
  if (manualForm.targetType === 'EMPLOYEE' && !manualForm.employeeId) { ElMessage.warning('请选择员工'); return }
  if (manualForm.targetType === 'DEVICE' && !manualForm.deviceId) { ElMessage.warning('请选择设备'); return }
  try {
    await manualAssign(manualForm)
    ElMessage.success('手动分配成功')
    manualDialogVisible.value = false
    fetchAssignments()
    fetchLoadData()
  } catch {
    ElMessage.error('分配失败')
  }
}

const reassignDialogVisible = ref(false)
const currentAssignment = ref<any>(null)
const reassignForm = reactive({ newAssigneeId: null as any, reason: '' })

function openReassignDialog(row: any) {
  currentAssignment.value = row
  reassignDialogVisible.value = true
  reassignForm.newAssigneeId = null
  reassignForm.reason = ''
}

async function handleReassign() {
  if (!reassignForm.newAssigneeId) { ElMessage.warning('请选择新分配对象'); return }
  try {
    await reassign(currentAssignment.value.id, reassignForm)
    ElMessage.success('重新分配成功')
    reassignDialogVisible.value = false
    fetchAssignments()
    fetchLoadData()
  } catch {
    ElMessage.error('重新分配失败')
  }
}

onMounted(() => {
  fetchLoadData()
  fetchAssignments()
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
.gantt-wrapper {
  overflow-x: auto;
}
.gantt-header, .gantt-row {
  display: flex;
  min-width: 800px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.gantt-header {
  font-weight: 600;
  background: var(--el-fill-color-light);
}
.gantt-resource {
  width: 140px;
  padding: 8px 12px;
  border-right: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
}
.gantt-timeline {
  flex: 1;
  position: relative;
  display: flex;
  height: 40px;
  align-items: center;
}
.gantt-hour {
  flex: 1;
  text-align: center;
  border-right: 1px dashed var(--el-border-color-lighter);
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.gantt-row .gantt-timeline {
  position: relative;
}
.gantt-bar {
  position: absolute;
  top: 6px;
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
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
