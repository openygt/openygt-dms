<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>产能报表</span>
          <el-button type="primary" @click="fetchData">刷新</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-row :gutter="16" style="margin-bottom: 16px">
        <el-col :span="6">
          <el-card shadow="hover">
            <div style="text-align: center">
              <div style="font-size: 24px; font-weight: bold; color: var(--ygt-primary-500)">{{ summary.totalTasks }}</div>
              <div style="color: #999; margin-top: 8px">总任务数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div style="text-align: center">
              <div style="font-size: 24px; font-weight: bold; color: #67C23A">{{ summary.completedTasks }}</div>
              <div style="color: #999; margin-top: 8px">已完成</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div style="text-align: center">
              <div style="font-size: 24px; font-weight: bold; color: #E6A23C">{{ summary.totalDoses }}</div>
              <div style="color: #999; margin-top: 8px">总付数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div style="text-align: center">
              <div style="font-size: 24px; font-weight: bold; color: #F56C6C">{{ summary.avgDuration }}h</div>
              <div style="color: #999; margin-top: 8px">平均耗时</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="statDate" label="日期" />
        <el-table-column prop="taskCount" label="任务数" />
        <el-table-column prop="completedCount" label="已完成" />
        <el-table-column prop="doseCount" label="付数" />
        <el-table-column prop="avgDuration" label="平均耗时(小时)" />
        <el-table-column prop="deviceUtilization" label="设备利用率">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(row.deviceUtilization * 100)" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/api/request'

interface ReportItem {
  statDate: string
  taskCount: number
  completedCount: number
  doseCount: number
  avgDuration: number
  deviceUtilization: number
}

const list = ref<ReportItem[]>([])
const loading = ref(false)
const dateRange = ref<string[]>([])
const summary = ref({ totalTasks: 0, completedTasks: 0, totalDoses: 0, avgDuration: 0 })

async function fetchData() {
  loading.value = true
  try {
    const params: any = {}
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res: any = await request.get('/v1/ops/capacity/daily', { params })
    list.value = res.data?.records || res.data || []
    // compute summary
    summary.value.totalTasks = list.value.reduce((s, r) => s + r.taskCount, 0)
    summary.value.completedTasks = list.value.reduce((s, r) => s + r.completedCount, 0)
    summary.value.totalDoses = list.value.reduce((s, r) => s + r.doseCount, 0)
    // 加权平均：按每日 completedCount 加权，避免天数简单平均导致偏差
    const totalCompleted = list.value.reduce((s, r) => s + r.completedCount, 0)
    const avg = totalCompleted > 0
      ? list.value.reduce((s, r) => s + r.avgDuration * r.completedCount, 0) / totalCompleted
      : 0
    summary.value.avgDuration = Math.round(avg * 10) / 10
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
