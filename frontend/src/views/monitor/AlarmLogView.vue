<template>
  <div>
    <el-card>
      <template #header>
        <div></div>
      </template>
      <el-form :inline="true" :model="query" style="margin-bottom: 16px">
        <el-form-item label="设备编码">
          <el-input v-model="query.deviceCode" placeholder="设备编码" clearable />
        </el-form-item>
        <el-form-item label="告警类型">
          <el-select v-model="query.alarmType" placeholder="全部" clearable style="width: 140px">
            <el-option label="高温告警" value="HIGH_TEMP" />
            <el-option label="低温告警" value="LOW_TEMP" />
            <el-option label="离线告警" value="OFFLINE" />
            <el-option label="故障告警" value="FAULT" />
            <el-option label="连接丢失" value="CONNECT_LOST" />
            <el-option label="压力异常" value="PRESSURE_ABNORMAL" />
            <el-option label="流量异常" value="FLOW_ABNORMAL" />
            <el-option label="温度过高" value="TEMP_HIGH" />
            <el-option label="标签堵塞" value="LABEL_JAM" />
            <el-option label="压力过低" value="PRESSURE_LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="告警状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="未处理" value="PENDING" />
            <el-option label="已修复" value="RESOLVED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="起止时间">
          <el-date-picker
            v-model="query.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 340px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="deviceCode" label="设备编码" width="120" />
        <el-table-column prop="alarmType" label="类型" width="120">
          <template #default="{ row }">
            {{ alarmTypeText(row.alarmType) }}
          </template>
        </el-table-column>
        <el-table-column prop="alarmLevel" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="levelType(row.alarmLevel)">{{ levelText(row.alarmLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="告警时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="handleResolve(row)">标识修复</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" @click="handleCancel(row)">取消警示</el-button>
            <span v-if="row.status === 'RESOLVED'" style="color: var(--ygt-gray-400); font-size: var(--ygt-text-sm)">已修复</span>
            <span v-if="row.status === 'CANCELLED'" style="color: var(--ygt-gray-400); font-size: var(--ygt-text-sm)">已取消</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无告警记录。告警由设备传感器自动触发，可在设备监控页查看实时状态。" />
        </template>
      </el-table>

      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="fetchData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const list = ref<any[]>([])
const query = ref({
  deviceCode: '',
  alarmType: '',
  status: '',
  dateRange: [] as string[]
})
const pagination = ref({ page: 1, size: 20, total: 0 })

function alarmTypeText(type?: string) {
  const map: Record<string, string> = {
    'HIGH_TEMP': '高温告警',
    'LOW_TEMP': '低温告警',
    'OFFLINE': '离线告警',
    'FAULT': '故障告警',
    'CONNECT_LOST': '连接丢失',
    'PRESSURE_ABNORMAL': '压力异常',
    'FLOW_ABNORMAL': '流量异常',
    'TEMP_HIGH': '温度过高',
    'LABEL_JAM': '标签堵塞',
    'PRESSURE_LOW': '压力过低'
  }
  return map[type || ''] || type || '-'
}

function levelText(level?: string) {
  const map: Record<string, string> = {
    'CRITICAL': '紧急',
    'WARNING': '警告',
    'INFO': '提示'
  }
  return map[level || ''] || level || '-'
}

function levelType(level?: string) {
  if (level === 'CRITICAL') return 'danger'
  if (level === 'WARNING') return 'warning'
  return 'info'
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    'RESOLVED': '已修复',
    'CANCELLED': '已取消',
    'PENDING': '未处理'
  }
  return map[status || ''] || status || '-'
}

function statusType(status?: string) {
  if (status === 'RESOLVED') return 'success'
  if (status === 'CANCELLED') return 'info'
  return 'danger'
}

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
    if (query.value.deviceCode) params.deviceCode = query.value.deviceCode
    if (query.value.alarmType) params.alarmType = query.value.alarmType
    if (query.value.status) params.status = query.value.status
    if (query.value.dateRange && query.value.dateRange.length === 2) {
      params.startTime = query.value.dateRange[0]
      params.endTime = query.value.dateRange[1]
    }
    const res: any = await request.get('/v1/eq/alarms', { params })
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
  query.value = { deviceCode: '', alarmType: '', status: '', dateRange: [] }
  pagination.value.page = 1
  fetchData()
}

async function handleResolve(row: any) {
  try {
    await ElMessageBox.confirm('确认标识该告警已修复？', '提示', { type: 'warning' })
    await request.put(`/v1/eq/alarms/${row.id}/resolve`)
    ElMessage.success('已标识修复')
    fetchData()
  } catch (e) {
    // 取消
  }
}

async function handleCancel(row: any) {
  try {
    await ElMessageBox.confirm('确认取消该警示？', '提示', { type: 'info' })
    await request.put(`/v1/eq/alarms/${row.id}/cancel`)
    ElMessage.success('已取消警示')
    fetchData()
  } catch (e) {
    // 取消
  }
}

onMounted(() => {
  pagination.value.page = 1
  fetchData()
})
</script>
