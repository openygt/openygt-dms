<template>
  <div>
    <el-card>
      <template #header>
        <span>任务管理</span>
      </template>
      <el-table :data="taskList" v-loading="loading" border>
        <el-table-column prop="id" label="任务号" width="80" />
        <el-table-column prop="prescriptionId" label="处方号" width="100" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorId" label="当前操作人" width="120" />
        <el-table-column prop="currentTemp" label="温度" width="100" />
        <el-table-column prop="updatedAt" label="更新时间" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '待泡药'" size="small" type="primary" @click="startSoak(row)">开始泡药</el-button>
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="任务详情" width="600px">
      <p>任务号：{{ selectedTask?.id }}</p>
      <p>状态：{{ selectedTask?.status }}</p>
      <p>处方ID：{{ selectedTask?.prescriptionId }}</p>
      <p>当前温度：{{ selectedTask?.currentTemp }}</p>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

interface Task {
  id: number
  prescriptionId: number
  status: string
  operatorId: string
  currentTemp: number
  updatedAt: string
}

const taskList = ref<Task[]>([])
const loading = ref(false)
const detailVisible = ref(false)
const selectedTask = ref<Task | null>(null)

function statusType(status: string) {
  const map: Record<string, string> = {
    '待泡药': '', '泡药中': 'warning', '待煎药': 'info',
    '煎药中': 'danger', '待出液': 'info', '出液中': 'warning',
    '待包装': 'info', '包装中': 'warning', '待贴标': 'success',
    '待质检': 'primary', '待交接': 'success', '已完成': 'success'
  }
  return map[status] || ''
}

async function fetchTasks() {
  loading.value = true
  try {
    const res: any = await request.get('/production/tasks?page=1&size=50')
    taskList.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

async function startSoak(row: Task) {
  try {
    await request.post(`/production/tasks/${row.id}/start-soak`)
    ElMessage.success('任务已开始泡药')
    // 可选：调用消耗记录（即使失败也不阻断）
    try {
      await request.post('/inventory/consume/record', {
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
