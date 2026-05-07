<template>
  <div>
    <el-card>
      <template #header>
        <span>系统日志</span>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="search.keyword" placeholder="操作/详情" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="search.module" placeholder="全部模块" clearable style="width: 160px">
            <el-option label="全部" value="all" />
            <el-option label="认证" value="auth" />
            <el-option label="系统管理" value="system" />
            <el-option label="基础数据" value="masterdata" />
            <el-option label="设备管理" value="equipment" />
            <el-option label="生产管理" value="production" />
            <el-option label="质量管理" value="quality" />
            <el-option label="打印管理" value="print" />
            <el-option label="库存管理" value="inventory" />
            <el-option label="统计分析" value="analytics" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="action" label="操作动作" min-width="180" show-overflow-tooltip />
        <el-table-column prop="module" label="所属模块" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="moduleTagType(row.module)">
              {{ moduleText(row.module) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="操作人" width="110" />
        <el-table-column prop="result" label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.result === 'SUCCESS'" size="small" type="success">成功</el-tag>
            <el-tag v-else-if="row.result === 'FAILED'" size="small" type="danger">失败</el-tag>
            <el-tag v-else size="small" type="info">—</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetId" label="目标ID" width="100" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP 地址" width="130" />
        <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="操作时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无日志" />
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/api/request'

interface SysLog {
  id: number
  action: string
  module: string
  userId: string
  result: string
  targetId: string
  targetName: string
  ipAddress: string
  detail: string
  createdAt: string
}

const list = ref<SysLog[]>([])
const loading = ref(false)
const search = ref({ keyword: '', module: 'all' })
const pagination = ref({ page: 1, size: 20, total: 0 })

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.value.page,
      size: pagination.value.size
    }
    if (search.value.keyword) params.keyword = search.value.keyword
    if (search.value.module && search.value.module !== 'all') params.module = search.value.module
    const res: any = await request.get('/v1/sys/logs', { params })
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.value.page = 1
  fetchData()
}

function handleReset() {
  search.value = { keyword: '', module: 'all' }
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

function formatDateTime(val: string | null) {
  if (!val) return '—'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function moduleText(module: string) {
  const map: Record<string, string> = {
    auth: '认证',
    system: '系统',
    masterdata: '基础数据',
    equipment: '设备',
    production: '生产',
    quality: '质量',
    print: '打印',
    inventory: '库存',
    analytics: '统计'
  }
  return map[module] || module || '未知'
}

function moduleTagType(module: string) {
  const map: Record<string, any> = {
    auth: 'info',
    system: 'primary',
    masterdata: '',
    equipment: 'warning',
    production: 'success',
    quality: 'danger',
    print: '',
    inventory: '',
    analytics: 'info'
  }
  return map[module] || ''
}

onMounted(fetchData)
</script>

<style scoped>
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
