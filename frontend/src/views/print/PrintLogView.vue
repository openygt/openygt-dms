<template>
  <div class="page-container">
    <div class="page-header-title">打印记录：<span class="page-header-sub">打印队列、失败重试、记录查询</span></div>
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
            <el-tag v-else-if="row.status === 'COMPLETED'" type="success">已打印</el-tag>
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
import { ref, reactive } from 'vue'
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
const tableData = ref<PrintLog[]>([])

async function handleSearch() {
  loading.value = true
  try {
    const res: any = await getPrintLogList({
      status: searchForm.status || undefined,
      deviceCode: searchForm.deviceCode || undefined
    })
    tableData.value = res.data || []
  } catch (e) {
    // handled by interceptor
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
