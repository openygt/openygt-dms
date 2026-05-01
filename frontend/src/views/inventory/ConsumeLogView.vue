<template>
  <div>
    <div class="page-header-title">药材消耗：<span class="page-header-sub">药材领用、消耗记录、库存扣减</span></div>
    <el-card>
      <template #header>
        <span>药材消耗</span>
      </template>
      <el-form :inline="true" :model="query" class="query-form">
        <el-form-item label="任务号">
          <el-input v-model="query.taskId" placeholder="任务ID" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="药材名称">
          <el-input v-model="query.medicineName" placeholder="药材名称" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="query.operatorId" placeholder="操作人" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="query.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="流水号" width="80" />
        <el-table-column prop="taskId" label="任务号" width="100">
          <template #default="{ row }">
            <el-link type="primary" @click="goTask(row.taskId)">{{ row.taskId }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="medicineName" label="药材名称" width="140" />
        <el-table-column prop="changeQuantity" label="消耗量" width="100" />
        <el-table-column prop="operatorId" label="操作人" width="120" />
        <el-table-column prop="refNo" label="关联单号" width="140">
          <template #default="{ row }">
            {{ row.refNo || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="时间">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'

const router = useRouter()
const loading = ref(false)
const list = ref<any[]>([])
const query = ref({
  taskId: '',
  medicineName: '',
  operatorId: '',
  dateRange: null as [string, string] | null
})
const pagination = ref({
  page: 1,
  size: 10,
  total: 0
})

function formatDateTime(dt: string) {
  if (!dt) return '-'
  const d = new Date(dt)
  if (isNaN(d.getTime())) return dt
  return d.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.value.page,
      size: pagination.value.size
    }
    if (query.value.taskId) params.taskId = query.value.taskId
    if (query.value.medicineName) params.medicineName = query.value.medicineName
    if (query.value.operatorId) params.operatorId = query.value.operatorId
    if (query.value.dateRange && query.value.dateRange[0]) {
      params.startTime = query.value.dateRange[0] + ' 00:00:00'
      params.endTime = query.value.dateRange[1] + ' 23:59:59'
    }
    const res: any = await request.get('/v1/inv/consume/list', { params })
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  pagination.value.page = 1
  fetchData()
}

function handleReset() {
  query.value = {
    taskId: '',
    medicineName: '',
    operatorId: '',
    dateRange: null
  }
  pagination.value.page = 1
  fetchData()
}

function handleSizeChange(val: number) {
  pagination.value.size = val
  pagination.value.page = 1
  fetchData()
}

function handlePageChange(val: number) {
  pagination.value.page = val
  fetchData()
}

function goTask(taskId: number) {
  router.push('/tasks')
}

onMounted(fetchData)
</script>

<style scoped>
.query-form {
  margin-bottom: 16px;
}
</style>
