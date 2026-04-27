<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="item in stats" :key="item.label">
        <el-card>
          <div style="font-size: 14px; color: #666">{{ item.label }}</div>
          <div style="font-size: 28px; font-weight: bold; margin-top: 8px">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <template #header>任务状态分布</template>
          <div v-for="s in taskDist" :key="s.status" style="display: flex; justify-content: space-between; margin: 8px 0">
            <span>{{ s.status }}</span>
            <el-tag>{{ s.count }}</el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>设备类型分布</template>
          <div v-for="d in deviceDist" :key="d.deviceType" style="display: flex; justify-content: space-between; margin: 8px 0">
            <span>{{ deviceTypeName(d.deviceType) }}</span>
            <el-tag type="info">{{ d.count }}</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>系统信息</span>
      </template>
      <p>OpenYGT 智能煎药管理系统 V2.0</p>
      <p>当前用户：{{ userStore.userInfo?.username || '-' }}</p>
      <p>当前角色：{{ userStore.userInfo?.roles?.join(', ') || '-' }}</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const userStore = useUserStore()

const stats = ref([
  { label: '今日任务', value: 0 },
  { label: '已完成', value: 0 },
  { label: '进行中', value: 0 },
  { label: '活跃告警', value: 0 }
])
const taskDist = ref<any[]>([])
const deviceDist = ref<any[]>([])

function deviceTypeName(type: string) {
  const map: Record<string, string> = { '1': '煎药机', '2': '包装机', '3': '贴标机', '4': '泡药罐' }
  return map[type] || type
}

async function fetchDashboard() {
  try {
    const res: any = await request.get('/v1/ops/dashboard/realtime')
    const data = res.data || {}
    stats.value = [
      { label: '今日任务', value: data.todayTotalTasks || 0 },
      { label: '已完成', value: data.todayCompletedTasks || 0 },
      { label: '进行中', value: data.todayInProgressTasks || 0 },
      { label: '活跃告警', value: data.activeAlarmCount || 0 }
    ]
    taskDist.value = data.taskStatusDistribution || []
    deviceDist.value = data.deviceTypeDistribution || []
  } catch (e) {}
}

onMounted(fetchDashboard)
</script>
