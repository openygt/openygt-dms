<template>
  <div>
    <el-card>
      <template #header>
        <span>告警日志</span>
      </template>
      <el-form :inline="true" :model="query" style="margin-bottom: 16px">
        <el-form-item label="设备">
          <el-input v-model="query.deviceCode" placeholder="设备编码" clearable />
        </el-form-item>
        <el-form-item label="告警类型">
          <el-select v-model="query.alarmType" placeholder="全部" clearable style="width: 120px">
            <el-option label="高温告警" value="HIGH_TEMP" />
            <el-option label="低温告警" value="LOW_TEMP" />
            <el-option label="离线告警" value="OFFLINE" />
            <el-option label="故障告警" value="FAULT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="deviceCode" label="设备" width="120" />
        <el-table-column prop="alarmType" label="类型" width="120" />
        <el-table-column prop="alarmLevel" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="row.alarmLevel === 'CRITICAL' ? 'danger' : row.alarmLevel === 'WARNING' ? 'warning' : 'info'">{{ row.alarmLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'RESOLVED' ? 'success' : 'danger'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="告警时间" />
      </el-table>

      <el-empty v-if="!loading && list.length === 0" description="暂无告警记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const loading = ref(false)
const list = ref<any[]>([])
const query = ref({ deviceCode: '', alarmType: '' })

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: 1, size: 50 }
    if (query.value.deviceCode) params.deviceCode = query.value.deviceCode
    if (query.value.alarmType) params.alarmType = query.value.alarmType
    const res: any = await request.get('/equipment/alarms', { params })
    list.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
