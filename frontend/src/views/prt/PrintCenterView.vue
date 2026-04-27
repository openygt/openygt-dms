<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>打印中心</span>
          <el-button type="primary" @click="fetchData">刷新队列</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="打印ID" width="100" />
        <el-table-column prop="taskId" label="任务号" width="100" />
        <el-table-column prop="labelType" label="标签类型" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试次数" width="100" />
        <el-table-column prop="printerName" label="打印机" />
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" :disabled="row.status !== 'FAILED'" @click="handleRetry(row)">重试</el-button>
            <el-button size="small" @click="handleSubmit(row)">重新提交</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无打印任务" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

interface PrintTask {
  id: number
  taskId: number
  labelType: string
  status: string
  retryCount: number
  printerName: string
  createdAt: string
}

const list = ref<PrintTask[]>([])
const loading = ref(false)

function statusType(status: string) {
  const map: Record<string, string> = {
    'PENDING': 'info', 'PRINTING': 'warning', 'SUCCESS': 'success', 'FAILED': 'danger'
  }
  return map[status] || ''
}
function statusText(status: string) {
  const map: Record<string, string> = {
    'PENDING': '待打印', 'PRINTING': '打印中', 'SUCCESS': '成功', 'FAILED': '失败'
  }
  return map[status] || status
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await request.get('/v1/prt/queue')
    list.value = res.data || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function handleRetry(row: PrintTask) {
  try {
    await request.post(`/v1/prt/tasks/${row.taskId}/retry`)
    ElMessage.success('重试成功')
    fetchData()
  } catch (e) {}
}

async function handleSubmit(row: PrintTask) {
  try {
    await request.post(`/v1/prt/tasks/${row.taskId}/submit`)
    ElMessage.success('提交成功')
    fetchData()
  } catch (e) {}
}

onMounted(fetchData)
</script>
