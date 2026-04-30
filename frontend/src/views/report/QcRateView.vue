<template>
  <div class="page-container">
    <el-page-header title="质检合格率" content="统计分析质检合格率趋势" />

    <el-card class="filter-card" shadow="never">
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
        <el-form-item label="汇总方式">
          <el-radio-group v-model="searchForm.groupBy" @change="handleSearch">
            <el-radio-button label="day">日</el-radio-button>
            <el-radio-button label="week">周</el-radio-button>
            <el-radio-button label="month">月</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" data-testid="search-btn" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="summary-row">
      <el-card class="summary-card" shadow="never">
        <div class="summary-label">质检总数</div>
        <div class="summary-value">{{ summary.total }}</div>
      </el-card>
      <el-card class="summary-card" shadow="never">
        <div class="summary-label">合格数</div>
        <div class="summary-value" style="color: #67c23a">{{ summary.passCount }}</div>
      </el-card>
      <el-card class="summary-card" shadow="never">
        <div class="summary-label">合格率</div>
        <div class="summary-value" style="color: #409eff">
          {{ summary.total > 0 ? ((summary.passCount / summary.total) * 100).toFixed(2) + '%' : '0.00%' }}
        </div>
      </el-card>
    </div>

    <el-card class="chart-card" shadow="never">
      <template #header>
        <span>合格率趋势</span>
      </template>
      <div class="chart-container" />
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header>
        <span>不合格原因分布</span>
      </template>
      <el-table :data="reasonData" data-testid="data-table">
        <el-table-column type="index" width="50" />
        <el-table-column prop="item" label="原因类别" min-width="160" />
        <el-table-column prop="pass" label="合格数" min-width="100" />
        <el-table-column prop="fail" label="不合格数" min-width="100" />
        <el-table-column label="合格率" min-width="120">
          <template #default="{ row }">
            <span>{{ row.pass + row.fail > 0 ? ((row.pass / (row.pass + row.fail)) * 100).toFixed(2) + '%' : '0.00%' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getQcRateTrend, getQcRateReason, getQcRateSummary } from '@/api/report'

interface TrendItem {
  period: string
  total: number
  passCount: number
  rate: number
}

interface ReasonItem {
  item: string
  pass: number
  fail: number
}

const searchForm = reactive({
  groupBy: 'day'
})
const dateRange = ref<[string, string] | null>(null)
const trendData = ref<TrendItem[]>([])
const reasonData = ref<ReasonItem[]>([])
const summary = reactive({
  total: 0,
  passCount: 0
})

async function handleSearch() {
  const params = {
    dateStart: dateRange.value ? dateRange.value[0] : undefined,
    dateEnd: dateRange.value ? dateRange.value[1] : undefined,
    groupBy: searchForm.groupBy
  }
  try {
    const trendRes: any = await getQcRateTrend(params)
    trendData.value = trendRes.data || []
  } catch (e) {
    trendData.value = []
  }
  try {
    const reasonRes: any = await getQcRateReason({
      dateStart: params.dateStart,
      dateEnd: params.dateEnd
    })
    reasonData.value = reasonRes.data || []
  } catch (e) {
    reasonData.value = []
  }
  try {
    const summaryRes: any = await getQcRateSummary({
      dateStart: params.dateStart,
      dateEnd: params.dateEnd
    })
    const s = summaryRes.data || {}
    summary.total = s.total || 0
    summary.passCount = s.passCount || 0
  } catch (e) {
    summary.total = 0
    summary.passCount = 0
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
.filter-card {
  margin-bottom: 16px;
}
.summary-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.summary-card {
  flex: 1;
  text-align: center;
}
.summary-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}
.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}
.chart-card {
  margin-bottom: 16px;
}
.chart-container {
  width: 100%;
  height: 300px;
}
</style>
