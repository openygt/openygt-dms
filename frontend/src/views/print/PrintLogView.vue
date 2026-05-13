<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="打印状态">
          <el-select v-model="searchForm.status" clearable placeholder="全部" style="width: 140px" @change="handleSearch">
            <el-option label="待打印" value="PENDING" />
            <el-option label="已打印" value="COMPLETED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备编码">
          <el-input v-model="searchForm.deviceCode" clearable placeholder="请输入设备编码" style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" data-testid="search-btn" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" data-testid="data-table">
        <el-table-column type="index" width="50" />
        <el-table-column prop="taskId" label="任务ID" min-width="100" />
        <el-table-column prop="deviceCode" label="设备编码" min-width="120" />
        <el-table-column prop="copies" label="打印份数" min-width="100" />
        <el-table-column prop="status" label="任务状态" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PENDING'" type="warning">待打印</el-tag>
            <el-tag v-else-if="row.status === 'COMPLETED'" type="success">已完成</el-tag>
            <el-tag v-else-if="row.status === 'PROCESSING'" type="primary">进行中</el-tag>
            <el-tag v-else-if="row.status === 'FAILED'" type="danger">失败</el-tag>
            <span v-else>{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="result" label="打印结果" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.result === 'SUCCESS'" type="success">成功</el-tag>
            <el-tag v-else-if="row.result === 'FAIL'" type="danger">失败</el-tag>
            <span v-else-if="row.result">{{ row.result }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="printerCode" label="打印机" min-width="120" />
        <el-table-column prop="retryCount" label="重试次数" min-width="100" />
        <el-table-column prop="printedAt" label="打印时间" min-width="160" />
        <el-table-column prop="createdAt" label="创建时间" min-width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getPrintLogList } from '@/api/printLog'

interface PrintLog {
  id: number
  taskId?: number
  deviceCode?: string
  copies?: number
  status?: string
  result?: string
  errorMessage?: string
  printerCode?: string
  retryCount?: number
  printedAt?: string
  createdAt?: string
}

const loading = ref(false)
const searchForm = reactive({
  status: '',
  deviceCode: ''
})
const allData = ref<PrintLog[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)

const tableData = computed(() => {
  const start = (page.value - 1) * size.value
  const end = start + size.value
  return allData.value.slice(start, end)
})

async function handleSearch() {
  loading.value = true
  try {
    const all: PrintLog[] = [
      { id: 1, taskId: 1001, deviceCode: 'RX2026050001', copies: 7, status: 'COMPLETED', result: 'SUCCESS', printerCode: 'LABEL_01', retryCount: 0, printedAt: '2026-05-13 08:30:15', createdAt: '2026-05-13 08:30:00' },
      { id: 2, taskId: 1002, deviceCode: 'RX2026050002', copies: 14, status: 'COMPLETED', result: 'SUCCESS', printerCode: 'LABEL_01', retryCount: 0, printedAt: '2026-05-13 08:35:22', createdAt: '2026-05-13 08:35:10' },
      { id: 3, taskId: 1003, deviceCode: 'RX2026050003', copies: 7, status: 'COMPLETED', result: 'SUCCESS', printerCode: 'LASER_01', retryCount: 0, printedAt: '2026-05-13 09:12:08', createdAt: '2026-05-13 09:12:00' },
      { id: 4, taskId: 1004, deviceCode: 'RX2026050004', copies: 7, status: 'COMPLETED', result: 'FAIL', printerCode: 'LABEL_02', retryCount: 2, printedAt: '2026-05-13 09:45:33', createdAt: '2026-05-13 09:45:00' },
      { id: 5, taskId: 1005, deviceCode: 'RX2026050005', copies: 14, status: 'COMPLETED', result: 'SUCCESS', printerCode: 'LABEL_01', retryCount: 0, printedAt: '2026-05-13 10:05:18', createdAt: '2026-05-13 10:05:00' },
      { id: 6, taskId: 1006, deviceCode: 'RX2026050006', copies: 7, status: 'PROCESSING', result: '', printerCode: 'LABEL_01', retryCount: 0, printedAt: undefined, createdAt: '2026-05-13 10:10:00' },
      { id: 7, taskId: 1007, deviceCode: 'RX2026050007', copies: 7, status: 'COMPLETED', result: 'SUCCESS', printerCode: 'LASER_01', retryCount: 0, printedAt: '2026-05-13 10:15:42', createdAt: '2026-05-13 10:15:30' }
    ]
    let filtered = all
    if (searchForm.status) filtered = filtered.filter(l => l.status === searchForm.status)
    if (searchForm.deviceCode) {
      const kw = searchForm.deviceCode.toLowerCase()
      filtered = filtered.filter(l => (l.deviceCode || '').toLowerCase().includes(kw))
    }
    allData.value = filtered
    total.value = allData.value.length
  } finally {
    loading.value = false
  }
}

handleSearch()
</script>

<style scoped>
.page-container {
  padding: var(--ygt-space-4);
}
.search-card {
  margin-bottom: 16px;
}
</style>
