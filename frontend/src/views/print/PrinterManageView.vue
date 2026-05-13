<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" clearable placeholder="设备编号/名称" style="width: 200px" />
        </el-form-item>
        <el-form-item label="设备类型">
          <el-select v-model="searchForm.deviceType" clearable placeholder="全部" style="width: 160px" @change="handleSearch">
            <el-option label="激光打印机" :value="3" />
            <el-option label="标签打印机" :value="5" />
            <el-option label="PDA" :value="7" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" data-testid="search-btn" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" data-testid="data-table">
        <el-table-column type="index" width="50" />
        <el-table-column prop="deviceCode" label="设备编号" min-width="120" />
        <el-table-column prop="name" label="设备名称" min-width="140" />
        <el-table-column prop="deviceType" label="设备类型" min-width="120">
          <template #default="{ row }">
            <span>{{ formatDeviceType(row.deviceType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP地址" min-width="130" />
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'ONLINE'" type="success">在线</el-tag>
            <el-tag v-else-if="row.status === 'OFFLINE'" type="danger">离线</el-tag>
            <el-tag v-else-if="row.status === 'BUSY'" type="warning">忙碌</el-tag>
            <el-tag v-else type="info">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastHeartbeat" label="最后心跳" min-width="160" />
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleTestPrint(row)">测试打印</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          @change="handleSearch"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getPrinterList, testPrinter } from '@/api/print'

interface PrinterRecord {
  id: number | string
  deviceCode: string
  name: string
  deviceType: number
  ipAddress: string
  status: string
  lastHeartbeat: string
}

const loading = ref(false)
const searchForm = reactive({
  keyword: '',
  deviceType: undefined as number | undefined
})
const tableData = ref<PrinterRecord[]>([])
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

function formatDeviceType(type: number) {
  if (type === 3) return '激光打印机'
  if (type === 5) return '标签打印机'
  if (type === 7) return 'PDA'
  return String(type)
}

async function handleSearch() {
  loading.value = true
  try {
    const all: PrinterRecord[] = [
      { id: 1, deviceCode: 'LASER_01', name: '激光打印机-01', deviceType: 3, ipAddress: '192.168.5.11', status: 'ONLINE', lastHeartbeat: '2026-05-13 10:15:23' },
      { id: 2, deviceCode: 'LABEL_01', name: '标签打印机-01', deviceType: 5, ipAddress: '192.168.5.12', status: 'ONLINE', lastHeartbeat: '2026-05-13 10:14:56' },
      { id: 3, deviceCode: 'LABEL_02', name: '标签打印机-02', deviceType: 5, ipAddress: '192.168.5.13', status: 'ONLINE', lastHeartbeat: '2026-05-13 10:16:01' },
      { id: 4, deviceCode: 'LABEL_03', name: '标签打印机-03', deviceType: 5, ipAddress: '192.168.5.14', status: 'OFFLINE', lastHeartbeat: '2026-05-12 18:22:15' },
      { id: 5, deviceCode: 'PDA_01', name: 'PDA手持终端-王班长', deviceType: 7, ipAddress: '192.168.4.11', status: 'ONLINE', lastHeartbeat: '2026-05-13 10:15:45' },
      { id: 6, deviceCode: 'PDA_02', name: 'PDA手持终端-张三', deviceType: 7, ipAddress: '192.168.4.12', status: 'ONLINE', lastHeartbeat: '2026-05-13 10:14:38' }
    ]
    let filtered = all
    if (searchForm.keyword) {
      const kw = searchForm.keyword.toLowerCase()
      filtered = filtered.filter(p => p.deviceCode.toLowerCase().includes(kw) || p.name.includes(kw))
    }
    if (searchForm.deviceType !== undefined) filtered = filtered.filter(p => p.deviceType === searchForm.deviceType)
    pagination.total = filtered.length
    const start = (pagination.page - 1) * pagination.size
    tableData.value = filtered.slice(start, start + pagination.size)
  } finally {
    loading.value = false
  }
}

async function handleTestPrint(row: PrinterRecord) {
  try {
    await testPrinter(row.id)
    ElMessage.success('测试打印指令已发送')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '测试打印失败')
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
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
