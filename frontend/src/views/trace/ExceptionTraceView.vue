<template>
  <div class="page-container">
    <el-page-header title="异常追溯" content="查询煎药过程中的异常记录及处理" />

    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleSearch"
          />
        </el-form-item>
        <el-form-item label="异常类型">
          <el-select v-model="searchForm.exceptionType" clearable placeholder="全部" style="width: 140px" @change="handleSearch">
            <el-option label="设备异常" value="DEVICE" />
            <el-option label="工艺异常" value="PROCESS" />
            <el-option label="物料异常" value="MATERIAL" />
            <el-option label="环境异常" value="ENVIRONMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="searchForm.handleStatus" clearable placeholder="全部" style="width: 140px" @change="handleSearch">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已忽略" value="IGNORED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" clearable placeholder="异常编号/患者/处方" style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" data-testid="search-btn" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        highlight-current-row
        data-testid="data-table"
        @row-click="handleRowClick"
      >
        <el-table-column type="index" width="50" />
        <el-table-column prop="exceptionNo" label="异常编号" min-width="140" />
        <el-table-column prop="patientName" label="患者姓名" min-width="100" />
        <el-table-column prop="prescriptionNo" label="处方编号" min-width="140" />
        <el-table-column prop="exceptionType" label="异常类型" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.exceptionType === 'DEVICE'" type="danger">设备异常</el-tag>
            <el-tag v-else-if="row.exceptionType === 'PROCESS'" type="warning">工艺异常</el-tag>
            <el-tag v-else-if="row.exceptionType === 'MATERIAL'" type="info">物料异常</el-tag>
            <el-tag v-else-if="row.exceptionType === 'ENVIRONMENT'" type="primary">环境异常</el-tag>
            <el-tag v-else>{{ row.exceptionType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="exceptionLevel" label="异常等级" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.exceptionLevel === 'HIGH'" type="danger">高</el-tag>
            <el-tag v-else-if="row.exceptionLevel === 'MEDIUM'" type="warning">中</el-tag>
            <el-tag v-else-if="row.exceptionLevel === 'LOW'" type="info">低</el-tag>
            <span v-else>{{ row.exceptionLevel }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="handleStatus" label="处理状态" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.handleStatus === 'PENDING'" type="danger">待处理</el-tag>
            <el-tag v-else-if="row.handleStatus === 'PROCESSING'" type="warning">处理中</el-tag>
            <el-tag v-else-if="row.handleStatus === 'RESOLVED'" type="success">已解决</el-tag>
            <el-tag v-else-if="row.handleStatus === 'IGNORED'" type="info">已忽略</el-tag>
            <span v-else>{{ row.handleStatus }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发生时间" min-width="160" />
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

    <div class="stat-row">
      <el-card class="stat-card" shadow="never">
        <div class="stat-label">异常总数</div>
        <div class="stat-value">{{ statTotal }}</div>
      </el-card>
      <el-card class="stat-card" shadow="never">
        <div class="stat-label">待处理</div>
        <div class="stat-value" style="color: #f56c6c">{{ statPending }}</div>
      </el-card>
      <el-card class="stat-card" shadow="never">
        <div class="stat-label">已解决</div>
        <div class="stat-value" style="color: #67c23a">{{ statResolved }}</div>
      </el-card>
    </div>

    <el-drawer v-model="detailVisible" title="异常详情" size="450">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="异常编号">{{ detail.exceptionNo }}</el-descriptions-item>
        <el-descriptions-item label="患者姓名">{{ detail.patientName }}</el-descriptions-item>
        <el-descriptions-item label="处方编号">{{ detail.prescriptionNo }}</el-descriptions-item>
        <el-descriptions-item label="异常类型">{{ detail.exceptionType }}</el-descriptions-item>
        <el-descriptions-item label="异常等级">{{ detail.exceptionLevel }}</el-descriptions-item>
        <el-descriptions-item label="处理状态">{{ detail.handleStatus }}</el-descriptions-item>
        <el-descriptions-item label="异常描述">{{ detail.description }}</el-descriptions-item>
        <el-descriptions-item label="处理结果">{{ detail.handleResult || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发生时间">{{ detail.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ detail.handledAt || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无详情" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getExceptionList, getExceptionById, getExceptionStat } from '@/api/trace'

interface ExceptionRecord {
  id: number | string
  exceptionNo: string
  patientName: string
  prescriptionNo: string
  exceptionType: string
  exceptionLevel: string
  handleStatus: string
  createdAt: string
}

const loading = ref(false)
const searchForm = reactive({
  keyword: '',
  exceptionType: '',
  handleStatus: ''
})
const dateRange = ref<[string, string] | null>(null)
const tableData = ref<ExceptionRecord[]>([])
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})
const detailVisible = ref(false)
const detail = ref<any>(null)
const statTotal = ref(0)
const statPending = ref(0)
const statResolved = ref(0)

async function handleSearch() {
  loading.value = true
  try {
    const res: any = await getExceptionList({
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword || undefined,
      exceptionType: searchForm.exceptionType || undefined,
      handleStatus: searchForm.handleStatus || undefined,
      dateStart: dateRange.value ? dateRange.value[0] : undefined,
      dateEnd: dateRange.value ? dateRange.value[1] : undefined
    })
    const data = res.data || {}
    tableData.value = data.records || []
    pagination.total = data.total || 0
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
  loadStats()
}

async function loadStats() {
  try {
    const res: any = await getExceptionStat({
      dateStart: dateRange.value ? dateRange.value[0] : undefined,
      dateEnd: dateRange.value ? dateRange.value[1] : undefined
    })
    const data = res.data || {}
    const byType = data.byType || []
    const byMonth = data.byMonth || []
    statTotal.value = byType.reduce((sum: number, item: any) => sum + (item.value || 0), 0)
    // pending/resolved are approximated from stats if available; fallback to table counts
    statPending.value = tableData.value.filter((r) => r.handleStatus === 'PENDING').length
    statResolved.value = tableData.value.filter((r) => r.handleStatus === 'RESOLVED').length
  } catch (e) {
    statTotal.value = pagination.total
    statPending.value = tableData.value.filter((r) => r.handleStatus === 'PENDING').length
    statResolved.value = tableData.value.filter((r) => r.handleStatus === 'RESOLVED').length
  }
}

async function handleRowClick(row: ExceptionRecord) {
  detailVisible.value = true
  try {
    const res: any = await getExceptionById(row.id)
    detail.value = res.data || null
  } catch (e) {
    detail.value = null
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
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.stat-row {
  display: flex;
  gap: 16px;
  margin-top: 16px;
}
.stat-card {
  flex: 1;
  text-align: center;
}
.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #333;
}
</style>
