<template>
  <div class="page-container">
    <el-page-header title="批次追溯" content="按批次号追溯煎药全流程" />

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
              <el-tag v-if="row.status === 'COMPLETED'" type="success">已完成</el-tag>
              <el-tag v-else-if="row.status === 'PROCESSING'" type="warning">进行中</el-tag>
              <el-tag v-else type="info">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="completeTime" label="完成时间" min-width="160" />
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
              <div class="trace-title">{{ trace.stepName || trace.nodeName }}</div>
              <div class="trace-info">
                <span>设备：{{ trace.deviceCode || '-' }}</span>
                <span>时间：{{ trace.eventTime || trace.createTime || '-' }}</span>
              </div>
              <div class="trace-info">
                <span>操作人：{{ trace.operatorName || '-' }}</span>
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
          :timestamp="item.eventTime"
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
import { searchBatchTrace, getBatchDetail, getBatchTimeline, getBatchNos } from '@/api/trace'

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
  } catch (e) {
    // handled by interceptor
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
    } catch (e) {
      batchDetail.value = null
    }
  }
  if (row.prescriptionNo) {
    try {
      const res: any = await getBatchTimeline(row.prescriptionNo)
      timelineData.value = res.data || []
      timelineVisible.value = true
    } catch (e) {
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
    cb([])
  }
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
