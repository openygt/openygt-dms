<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="批次号">
          <el-autocomplete
            v-model="searchForm.batchNo"
            :fetch-suggestions="fetchBatchNos"
            clearable
            placeholder="请输入批次号"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="处方编号">
          <el-input v-model="searchForm.prescriptionNo" clearable placeholder="请输入处方编号" style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" data-testid="search-btn" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="main-content">
      <el-card class="table-card" shadow="never">
        <el-table
          v-loading="loading"
          :data="tableData"
          highlight-current-row
          data-testid="data-table"
          @row-click="handleRowClick"
        >
          <el-table-column type="index" width="50" />
          <el-table-column prop="prescriptionNo" label="处方编号" min-width="140" />
          <el-table-column prop="patientName" label="患者姓名" min-width="100" />
          <el-table-column prop="decoctDeviceCode" label="煎药设备" min-width="120" />
          <el-table-column prop="status" label="状态" min-width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="completeTime" label="完成时间" min-width="160">
            <template #default="{ row }">{{ formatTime(row.completeTime) }}</template>
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

      <el-card class="detail-card" shadow="never" title="批次详情">
        <template #header>
          <span>批次详情</span>
          <el-tag v-if="selectedBatchNo" type="primary" style="margin-left: 8px">{{ selectedBatchNo }}</el-tag>
        </template>
        <div v-if="batchDetail">
          <div class="detail-stat">
            <span>追溯节点数：</span>
            <strong>{{ batchDetail.traceCount }}</strong>
          </div>
          <div class="trace-list">
            <el-card
              v-for="(trace, index) in batchDetail.traces"
              :key="index"
              class="trace-item"
              shadow="hover"
            >
              <div class="trace-title">{{ trace.prescriptionNo }} — {{ trace.patientName }}</div>
              <div class="trace-info">
                <span>状态：{{ statusText(trace.status) }}</span>
                <span>设备：{{ trace.decoctDeviceCode || '-' }}</span>
              </div>
              <div class="trace-info">
                <span>接方：{{ formatTime(trace.receiveTime) }}</span>
                <span>完成：{{ formatTime(trace.completeTime) }}</span>
              </div>
            </el-card>
          </div>
        </div>
        <el-empty v-else description="点击左侧行查看批次详情" />
      </el-card>
    </div>

    <el-drawer v-model="timelineVisible" title="追溯时间线" size="400">
      <el-timeline v-if="timelineData.length">
        <el-timeline-item
          v-for="(item, index) in timelineData"
          :key="index"
          :timestamp="formatTime(item.eventTime)"
        >
          {{ item.eventName }}
          <div v-if="item.operatorName" class="timeline-operator">操作人：{{ item.operatorName }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无时间线数据" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { searchBatchTrace, getBatchDetail, getBatchTimeline, getBatchNos } from '@/api/equipment'

interface TraceRecord {
  prescriptionNo: string
  patientName: string
  decoctDeviceCode: string
  status: string
  completeTime: string
  batchNo?: string
}

interface BatchDetail {
  batchNo: string
  traceCount: number
  traces: any[]
}

interface TimelineItem {
  eventName: string
  eventTime: string
  operatorName?: string
}

const loading = ref(false)
const searchForm = reactive({
  batchNo: '',
  prescriptionNo: ''
})
const tableData = ref<TraceRecord[]>([])
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})
const selectedBatchNo = ref('')
const batchDetail = ref<BatchDetail | null>(null)
const timelineVisible = ref(false)
const timelineData = ref<TimelineItem[]>([])

async function handleSearch() {
  loading.value = true
  try {
    const res: any = await searchBatchTrace({
      batchNo: searchForm.batchNo || undefined,
      prescriptionNo: searchForm.prescriptionNo || undefined,
      page: pagination.page,
      size: pagination.size
    })
    const data = res.data || {}
    tableData.value = data.records || []
    pagination.total = data.total || 0
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '查询失败')
  } finally {
    loading.value = false
  }
}

async function handleRowClick(row: TraceRecord) {
  selectedBatchNo.value = row.batchNo || searchForm.batchNo || ''
  if (selectedBatchNo.value) {
    try {
      const res: any = await getBatchDetail(selectedBatchNo.value)
      batchDetail.value = res.data || null
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || '获取批次详情失败')
      batchDetail.value = null
    }
  }
  if (row.prescriptionNo) {
    try {
      const res: any = await getBatchTimeline(row.prescriptionNo)
      timelineData.value = res.data || []
      timelineVisible.value = true
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || '获取时间线失败')
      timelineData.value = []
    }
  }
}

async function fetchBatchNos(queryString: string, cb: (data: any[]) => void) {
  try {
    const res: any = await getBatchNos()
    const list: string[] = res.data || []
    const results = queryString
      ? list.filter((n) => n.toLowerCase().includes(queryString.toLowerCase())).map((n) => ({ value: n }))
      : list.map((n) => ({ value: n }))
    cb(results)
  } catch (e) {
    ElMessage.error('获取批次号失败')
    cb([])
  }
}

function statusTagType(status: string) {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'RECEIVED': return ''
    case 'AUDIT_PASS': return ''
    case 'DISPENSED': return ''
    case 'REVIEWED': return ''
    case 'SOAKING': return 'warning'
    case 'FIRST_DECOCTING': return 'primary'
    case 'SECOND_DECOCTING': return 'primary'
    case 'PROCESSING': return 'warning'
    case 'PACKAGING': return 'primary'
    case 'ABNORMAL': return 'danger'
    default: return 'info'
  }
}

function statusText(status: string) {
  const map: Record<string, string> = {
    PENDING: '待处理',
    RECEIVED: '已接方',
    AUDIT_PASS: '审方通过',
    DISPENSED: '调剂完成',
    REVIEWED: '复核通过',
    SOAKING: '浸泡中',
    FIRST_DECOCTING: '一煎中',
    SECOND_DECOCTING: '二煎中',
    PROCESSING: '处理中',
    PACKAGING: '包装中',
    COMPLETED: '已完成',
    REJECTED: '已拒收',
    ABNORMAL: '异常',
    QUALITY: '质量异常',
    DEVICE: '设备异常'
  }
  return map[status] || status
}

function formatTime(time: string | null | undefined) {
  if (!time) return '--'
  return time.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.page-container {
  padding: var(--ygt-space-4);
}
.search-card {
  margin-bottom: 16px;
}
.main-content {
  display: flex;
  gap: 16px;
}
.table-card {
  flex: 2;
}
.detail-card {
  flex: 1;
  min-width: 320px;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.detail-stat {
  margin-bottom: 12px;
  font-size: 14px;
}
.trace-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.trace-item {
  font-size: 13px;
}
.trace-title {
  font-weight: 600;
  margin-bottom: 4px;
}
.trace-info {
  color: #666;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.timeline-operator {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
