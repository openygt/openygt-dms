<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span>任务管理</span>
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button label="list">列表视图</el-radio-button>
            <el-radio-button label="kanban">看板视图</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 查询表单 -->
      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="任务号">
          <el-input v-model="queryForm.id" placeholder="任务号" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="处方号">
          <el-input v-model="queryForm.prescriptionId" placeholder="处方号" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="待泡药" value="待泡药" />
            <el-option label="泡药中" value="泡药中" />
            <el-option label="待煎药" value="待煎药" />
            <el-option label="煎药中" value="煎药中" />
            <el-option label="待出液" value="待出液" />
            <el-option label="出液中" value="出液中" />
            <el-option label="待包装" value="待包装" />
            <el-option label="包装中" value="包装中" />
            <el-option label="待贴标" value="待贴标" />
            <el-option label="待质检" value="待质检" />
            <el-option label="待交接" value="待交接" />
            <el-option label="已完成" value="已完成" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="queryForm.operatorId" placeholder="操作人" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 列表视图 -->
      <template v-if="viewMode === 'list'">
        <el-table :data="taskList" v-loading="loading" border>
          <el-table-column prop="id" label="任务号" width="80" />
          <el-table-column prop="prescriptionId" label="处方号" width="100" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="operatorId" label="当前操作人" width="120">
            <template #default="{ row }">
              {{ formatOperator(row.operatorId) }}
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === '待泡药'" size="small" type="primary" @click="startSoak(row)">开始泡药</el-button>
              <el-button size="small" @click="viewDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          style="margin-top: 16px; justify-content: flex-end;"
        />
      </template>

      <!-- 看板视图 -->
      <template v-else>
        <div v-loading="loading" class="kanban-board">
          <div v-for="col in kanbanColumns" :key="col.status" class="kanban-column">
            <div class="kanban-header">
              <span class="kanban-title">{{ col.label }}</span>
              <el-tag size="small" :type="statusType(col.status)">{{ col.tasks.length }}</el-tag>
            </div>
            <div class="kanban-body">
              <div v-for="task in col.tasks" :key="task.id" class="kanban-card" @click="viewDetail(task)">
                <div class="kanban-card-top">
                  <span class="kanban-card-id">#{{ task.id }}</span>
                  <el-tag size="small" :type="statusType(task.status)">{{ task.status }}</el-tag>
                </div>
                <div class="kanban-card-info">处方: {{ task.prescriptionId || '-' }}</div>
                <div class="kanban-card-info">操作人: {{ formatOperator(task.operatorId) }}</div>
                <div class="kanban-card-time">{{ formatDateTime(task.updatedAt) }}</div>
                <div class="kanban-card-actions">
                  <el-button v-if="task.status === '待泡药'" size="small" type="primary" @click.stop="startSoak(task)">开始泡药</el-button>
                </div>
              </div>
              <el-empty v-if="col.tasks.length === 0" description="无任务" :image-size="60" />
            </div>
          </div>
        </div>
      </template>
    </el-card>

    <el-dialog v-model="detailVisible" title="任务详情" width="600px">
      <p>任务号：{{ selectedTask?.id }}</p>
      <p>状态：{{ selectedTask?.status }}</p>
      <p>处方ID：{{ selectedTask?.prescriptionId }}</p>
      <p>当前步骤：{{ selectedTask?.currentStep || '-' }}</p>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

interface Task {
  id: number
  prescriptionId: number
  status: string
  operatorId: string
  currentStep: string
  updatedAt: string
}

const taskList = ref<Task[]>([])
const loading = ref(false)
const detailVisible = ref(false)
const selectedTask = ref<Task | null>(null)
const viewMode = ref<'list' | 'kanban'>('list')

const queryForm = ref({
  id: '',
  prescriptionId: '',
  status: '',
  operatorId: '',
  dateRange: null as [string, string] | null
})

const pagination = ref({
  page: 1,
  size: 100,
  total: 0
})

const kanbanStatuses = [
  { status: '待泡药', label: '待泡药' },
  { status: '泡药中', label: '泡药中' },
  { status: '待煎药', label: '待煎药' },
  { status: '煎药中', label: '煎药中' },
  { status: '待出液', label: '待出液' },
  { status: '出液中', label: '出液中' },
  { status: '待包装', label: '待包装' },
  { status: '包装中', label: '包装中' },
  { status: '待贴标', label: '待贴标' },
  { status: '待质检', label: '待质检' },
  { status: '待交接', label: '待交接' },
  { status: '已完成', label: '已完成' }
]

const kanbanColumns = computed(() => {
  return kanbanStatuses.map(col => ({
    ...col,
    tasks: taskList.value.filter(t => t.status === col.status)
  }))
})

function statusType(status: string) {
  const map: Record<string, string> = {
    '待泡药': '', '泡药中': 'warning', '待煎药': 'info',
    '煎药中': 'danger', '待出液': 'info', '出液中': 'warning',
    '待包装': 'info', '包装中': 'warning', '待贴标': 'success',
    '待质检': 'primary', '待交接': 'success', '已完成': 'success'
  }
  return map[status] || ''
}

function formatOperator(op: string) {
  if (!op) return '-'
  if (op === 'SYSTEM') return '系统'
  return op
}

function formatDateTime(dt: string) {
  if (!dt) return '-'
  const d = new Date(dt)
  if (isNaN(d.getTime())) return dt
  return d.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

async function fetchTasks() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.value.page,
      size: viewMode.value === 'kanban' ? 500 : pagination.value.size
    }
    if (queryForm.value.id) params.id = queryForm.value.id
    if (queryForm.value.prescriptionId) params.prescriptionId = queryForm.value.prescriptionId
    if (queryForm.value.status) params.status = queryForm.value.status
    if (queryForm.value.operatorId) params.operatorId = queryForm.value.operatorId
    if (queryForm.value.dateRange && queryForm.value.dateRange[0]) {
      params.startTime = queryForm.value.dateRange[0] + ' 00:00:00'
      params.endTime = queryForm.value.dateRange[1] + ' 23:59:59'
    }
    const res: any = await request.get('/v1/prod/tasks', { params })
    taskList.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  pagination.value.page = 1
  fetchTasks()
}

function handleReset() {
  queryForm.value = {
    id: '',
    prescriptionId: '',
    status: '',
    operatorId: '',
    dateRange: null
  }
  pagination.value.page = 1
  fetchTasks()
}

function handleSizeChange(val: number) {
  pagination.value.size = val
  pagination.value.page = 1
  fetchTasks()
}

function handlePageChange(val: number) {
  pagination.value.page = val
  fetchTasks()
}

async function startSoak(row: Task) {
  try {
    await request.post(`/v1/prod/tasks/${row.id}/soak/start`)
    ElMessage.success('任务已开始泡药')
    try {
      await request.post('/v1/inv/consume/record', {
        taskId: row.id,
        operatorId: 'current_user',
        items: []
      })
    } catch (e) {
      ElMessage.warning('消耗记录未写入（不影响任务推进）')
    }
    fetchTasks()
  } catch (e) {
    ElMessage.error('任务推进失败')
  }
}

function viewDetail(row: Task) {
  selectedTask.value = row
  detailVisible.value = true
}

onMounted(fetchTasks)
</script>

<style scoped>
.query-form {
  margin-bottom: 16px;
}

.kanban-board {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.kanban-column {
  flex: 0 0 220px;
  min-width: 220px;
  max-height: calc(100vh - 300px);
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color-page);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
}

.kanban-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-weight: 500;
  font-size: 14px;
  background: var(--el-bg-color);
  border-radius: 8px 8px 0 0;
}

.kanban-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.kanban-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.kanban-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.kanban-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.kanban-card-id {
  font-weight: 600;
  font-size: 13px;
  color: var(--el-color-primary);
}

.kanban-card-info {
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-bottom: 2px;
}

.kanban-card-time {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.kanban-card-actions {
  margin-top: 6px;
}
</style>
