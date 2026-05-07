<template>
  <div class="device-trace">
    <div class="page-header">

    </div>

    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="处方号">
          <el-input v-model="filterForm.prescriptionNo" placeholder="输入处方号" clearable />
        </el-form-item>
        <el-form-item label="患者">
          <el-input v-model="filterForm.patientName" placeholder="患者姓名" clearable />
        </el-form-item>
        <el-form-item label="设备">
          <el-select v-model="filterForm.deviceCode" placeholder="请选择" clearable style="width: 160px">
            <el-option v-for="d in deviceOptions" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="请选择" clearable style="width: 160px">
            <el-option label="接方" value="RECEIVED" />
            <el-option label="审方通过" value="AUDIT_PASS" />
            <el-option label="调剂完成" value="DISPENSED" />
            <el-option label="复核通过" value="REVIEWED" />
            <el-option label="浸泡中" value="SOAKING" />
            <el-option label="一煎中" value="FIRST_DECOCTING" />
            <el-option label="二煎中" value="SECOND_DECOCTING" />
            <el-option label="包装中" value="PACKAGING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="异常" value="ABNORMAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="traceList" v-loading="loading">
        <el-table-column prop="prescriptionNo" label="处方号" width="160" />
        <el-table-column prop="patientName" label="患者" width="100" />
        <el-table-column prop="decoctDeviceCode" label="煎药设备" width="120" />
        <el-table-column prop="packerDeviceCode" label="包装设备" width="120">
          <template #default="{ row }">{{ row.packerDeviceCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="schemeName" label="方案" width="150" />
        <el-table-column prop="receiveTime" label="接方时间" width="160" />
        <el-table-column prop="completeTime" label="完成时间" width="160">
          <template #default="{ row }">{{ row.completeTime || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
            <el-button link type="primary" @click="viewTempCurve(row)">温度曲线</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
        class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTraces, getDeviceList } from '@/api/equipment'

const router = useRouter()
const loading = ref(false)
const traceList = ref<any[]>([])
const deviceOptions = ref<string[]>([])

async function loadDevices() {
  try {
    const res: any = await getDeviceList({ page: 1, size: 999 })
    const records = res?.data?.records ?? res?.data?.list ?? []
    if (Array.isArray(records)) {
      deviceOptions.value = records.map((d: any) => d.deviceCode || d.code || d.device_code || '').filter(Boolean)
    }
  } catch (e: any) {
    ElMessage.warning('设备列表加载失败：' + (e?.message || '未知错误'))
  }
}

/** 默认不选日期：进入页面即查全库分页，由用户自行筛选 */
const filterForm = reactive({
  prescriptionNo: '',
  patientName: '',
  deviceCode: '',
  status: '',
  dateRange: null as [string, string] | null
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

function handleSearch() {
  pagination.page = 1
  loadTraces()
}

function handlePageChange() {
  loadTraces()
}

function handleSizeChange() {
  pagination.page = 1
  loadTraces()
}

function resetFilter() {
  filterForm.prescriptionNo = ''
  filterForm.patientName = ''
  filterForm.deviceCode = ''
  filterForm.status = ''
  filterForm.dateRange = null
  pagination.page = 1
  loadTraces()
}

async function loadTraces() {
  loading.value = true
  try {
    const params: Record<string, string | number> = {
      page: pagination.page,
      size: pagination.size
    }
    const pn = filterForm.prescriptionNo?.trim()
    const ptn = filterForm.patientName?.trim()
    if (pn) params.prescriptionNo = pn
    if (ptn) params.patientName = ptn
    if (filterForm.deviceCode) params.deviceCode = filterForm.deviceCode
    if (filterForm.status) params.status = filterForm.status
    const dr = filterForm.dateRange
    if (dr && dr.length === 2 && dr[0] && dr[1]) {
      params.startTime = dr[0]
      params.endTime = dr[1]
    }
    const res: any = await getTraces(params)
    const page = res?.data ?? {}
    const records = page.records ?? page.list ?? []
    const total = Number(page.total ?? 0)
    traceList.value = Array.isArray(records) ? records : []
    pagination.total = Number.isFinite(total) ? total : 0
  } catch (err) {
    ElMessage.error('加载追溯数据失败')
  } finally {
    loading.value = false
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
    PACKAGING: '包装中',
    COMPLETED: '已完成',
    ABNORMAL: '异常'
  }
  return map[status] || status
}

function viewDetail(row: any) {
  router.push(`/traces/${row.prescriptionNo}`)
}

function viewTempCurve(row: any) {
  router.push(`/traces/${row.prescriptionNo}?tab=temperature`)
}

onMounted(() => {
  loadDevices()
  loadTraces()
})
</script>

<style scoped lang="scss">
.device-trace {
  padding: 16px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h2 { margin: 0; font-size: 20px; }
  }

  .filter-card { margin-bottom: 16px; }

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }
}
</style>
