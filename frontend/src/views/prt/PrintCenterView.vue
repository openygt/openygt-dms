<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          
          <el-button type="primary" @click="fetchData">刷新队列</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="打印ID" width="100" />
        <el-table-column prop="taskId" label="任务号" width="100" />
        <el-table-column prop="printType" label="打印类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.printType === '标签打印' ? 'warning' : 'info'">
              {{ row.printType || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试次数" width="100" />
        <el-table-column prop="deviceCode" label="打印机" />
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" :disabled="row.status !== 'FAILED'" @click="handleRetry(row)">重试</el-button>
            <el-button size="small" @click="handleSubmit(row)">重新提交</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && allList.length === 0" description="暂无打印任务" />
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          @change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'

interface PrintTask {
  id: number
  taskId: number
  printType?: string
  status: string
  retryCount: number
  deviceCode: string
  createdAt: string
}

const allList = ref<PrintTask[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const list = computed(() => {
  const start = (page.value - 1) * size.value
  const end = start + size.value
  return allList.value.slice(start, end)
})

function statusType(status: string) {
  const map: Record<string, string> = {
    'PENDING': 'info', 'PRINTING': 'warning', 'COMPLETED': 'success', 'FAILED': 'danger'
  }
  return map[status] || ''
}
function statusText(status: string) {
  const map: Record<string, string> = {
    'PENDING': '待打印', 'PRINTING': '打印中', 'COMPLETED': '已完成', 'FAILED': '失败'
  }
  return map[status] || status
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await request.get('/v1/prt/queue')
    allList.value = res.data || []
    total.value = allList.value.length
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleRetry(row: PrintTask) {
  try {
    const userStore = useUserStore()
    await request.post(`/v1/prt/tasks/${row.taskId}/retry`, null, {
      params: { deviceCode: row.deviceCode, operatorId: userStore.userInfo?.username || '' }
    })
    ElMessage.success('重试成功')
    fetchData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '重试失败')
  }
}

onMounted(fetchData)
</script>
