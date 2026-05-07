<template>
  <div class="workload-stat">
    <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
    <div style="height: 16px"></div>
    <div class="page-header">

      <el-button type="primary" @click="exportExcel">导出Excel</el-button>
    </div>

    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="filterForm.operatorName" placeholder="操作人姓名" clearable />
        </el-form-item>
        <el-form-item label="工作类型">
          <el-select v-model="filterForm.workType" placeholder="全部" clearable>
            <el-option label="调剂" value="DISPENSE" />
            <el-option label="复核" value="REVIEW" />
            <el-option label="泡药" value="SOAK" />
            <el-option label="煎药" value="DECOCT" />
            <el-option label="包装" value="PACKAGE" />
            <el-option label="发货" value="DELIVER" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16" class="summary-row">
      <el-col :xs="12" :sm="8" :md="4" v-for="item in summaryList" :key="item.type">
        <el-card class="summary-card">
          <div class="summary-type">{{ item.name }}</div>
          <div class="summary-count">{{ item.count }}<span class="unit"> 处方</span></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <el-table :data="statList" v-loading="loading">
        <el-table-column prop="statDate" label="日期" width="120" />
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="workType" label="工作类型" width="120">
          <template #default="{ row }">{{ workTypeText(row.workType) }}</template>
        </el-table-column>
        <el-table-column prop="prescriptionCount" label="处方数" width="100" />
        <el-table-column prop="taskCount" label="任务数" width="100" />
        <el-table-column prop="packageCount" label="包装数" width="100" />
        <el-table-column prop="durationMinutes" label="工作时长" width="120">
          <template #default="{ row }">{{ formatDuration(row.durationMinutes) }}</template>
        </el-table-column>
        <el-table-column prop="efficiency" label="效率" width="100">
          <template #default="{ row }">
            <span :style="{ color: getEfficiencyColor(row.efficiency) }">{{ row.efficiency || '--' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @change="handleSearch"
        class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getWorkloadStats } from '@/api/equipment'

const router = useRouter()
const loading = ref(false)
const statList = ref<any[]>([])
const summaryList = ref<any[]>([
  { type: 'DISPENSE', name: '调剂', count: 0 },
  { type: 'REVIEW', name: '复核', count: 0 },
  { type: 'SOAK', name: '泡药', count: 0 },
  { type: 'DECOCT', name: '煎药', count: 0 },
  { type: 'PACKAGE', name: '包装', count: 0 },
  { type: 'DELIVER', name: '发货', count: 0 },
])

const filterForm = reactive({
  dateRange: [] as string[],
  operatorName: '',
  workType: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

function handleSearch() {
  loadStats()
}

function resetFilter() {
  filterForm.dateRange = []
  filterForm.operatorName = ''
  filterForm.workType = ''
  pagination.page = 1
  loadStats()
}

async function loadStats() {
  loading.value = true
  try {
    const params: any = { page: pagination.page, size: pagination.size }
    if (filterForm.dateRange?.length === 2) {
      params.startDate = filterForm.dateRange[0]
      params.endDate = filterForm.dateRange[1]
    }
    if (filterForm.workType) params.workType = filterForm.workType
    const res: any = await getWorkloadStats(params)
    statList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (err) {
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

function workTypeText(type: string) {
  const map: Record<string, string> = {
    DISPENSE: '调剂', REVIEW: '复核', SOAK: '泡药',
    DECOCT: '煎药', PACKAGE: '包装', DELIVER: '发货'
  }
  return map[type] || type
}

function formatDuration(minutes: number) {
  if (!minutes) return '--'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  if (h > 0) return `${h}小时${m}分`
  return `${m}分钟`
}

function getEfficiencyColor(eff: number) {
  if (!eff) return '#999'
  if (eff >= 3.0) return '#52C41A'
  if (eff >= 2.0) return '#1890FF'
  return '#FAAD14'
}

function exportExcel() {
  ElMessage.success('导出功能开发中')
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped lang="scss">
.workload-stat {
  padding: 16px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h2 { margin: 0; font-size: 20px; }
  }

  .filter-card { margin-bottom: 16px; }

  .summary-row {
    margin-bottom: 16px;

    .summary-card {
      text-align: center;

      .summary-type {
        font-size: 14px;
        color: #666;
        margin-bottom: 8px;
      }

      .summary-count {
        font-size: 28px;
        font-weight: bold;
        color: #1890FF;

        .unit {
          font-size: 14px;
          color: #666;
          font-weight: normal;
        }
      }
    }
  }

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }
}
</style>
