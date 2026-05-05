<template>
  <div>
    <div class="page-header-title">操作日志：<span class="page-header-sub">用户操作审计、系统事件记录</span></div>
    <el-card>
      <template #header>
        <span>系统日志</span>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="search.keyword" placeholder="操作人/内容" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="search.keyword = ''; fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="action" label="操作" min-width="160" />
        <el-table-column prop="module" label="操作对象" min-width="120" />
        <el-table-column prop="userId" label="操作人" min-width="100" />
        <el-table-column prop="ipAddress" label="IP" min-width="140" />
        <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" min-width="160" />
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无日志" />
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
  ipAddress: string
  detail: string
  createdAt: string
}

const list = ref<SysLog[]>([])
const loading = ref(false)
const search = ref({ keyword: '' })

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: 1, size: 50 }
    if (search.value.keyword) params.keyword = search.value.keyword
    const res: any = await request.get('/v1/sys/logs', { params })
    list.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
