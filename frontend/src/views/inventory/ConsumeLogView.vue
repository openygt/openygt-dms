<template>
  <div>
    <el-card>
      <template #header>
        <span>消耗流水</span>
      </template>
      <el-form :inline="true" :model="query" style="margin-bottom: 16px">
        <el-form-item label="任务号">
          <el-input v-model="query.taskId" placeholder="任务ID" clearable />
        </el-form-item>
        <el-form-item label="药材名称">
          <el-input v-model="query.medicineName" placeholder="药材名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="query = { taskId: '', medicineName: '' }; fetchData()">重置</el-button>
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
        <el-table-column prop="refNo" label="关联单号" />
        <el-table-column prop="createdAt" label="时间" />
      </el-table>

      <el-empty v-if="!loading && list.length === 0" description="暂无消耗流水" />
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
const query = ref({ taskId: '', medicineName: '' })

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: 1, size: 50 }
    if (query.value.taskId) params.taskId = query.value.taskId
    const res: any = await request.get('/inventory/consume/list', { params })
    list.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

function goTask(taskId: number) {
  router.push('/tasks')
}

onMounted(fetchData)
</script>
