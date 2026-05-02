<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>留样管理</span>
          <el-button type="warning" @click="fetchExpiring">即将到期预警</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="sampleNo" label="留样编号" width="150" />
        <el-table-column prop="sampleType" label="类型" width="100">
          <template #default="{row}">
            {{ sampleTypeText(row.sampleType) }}
          </template>
        </el-table-column>
        <el-table-column prop="retainTime" label="留样时间" width="160" />
        <el-table-column prop="expireTime" label="到期时间" width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="row.status === 4 ? 'info' : row.status === 3 ? 'success' : 'warning'">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{row}">
            <el-button v-if="row.status !== 4" size="small" type="danger" @click="handleDestroy(row)">销毁</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)

const sampleTypeText = (type: number) => {
  const map: Record<number, string> = { 1: '质检样', 2: '24h留样', 3: '72h留样' }
  return map[type] || '未知'
}
const statusText = (status: number) => {
  const map: Record<number, string> = { 1: '留样中', 2: '已复检', 3: '可销毁', 4: '已销毁' }
  return map[status] || '未知'
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/v1/qt/retain-sample/list')
    list.value = res.data.data?.list || []
  } catch (e) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

const fetchExpiring = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/v1/qt/retain-sample/expiring', { params: { withinHours: 2 } })
    list.value = res.data.data || []
  } catch (e) {
    ElMessage.error('获取预警失败')
  } finally {
    loading.value = false
  }
}

const handleDestroy = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认销毁该留样？', '提示', { type: 'warning' })
    await axios.post(`/api/v1/qt/retain-sample/${row.id}/destroy`, { destroyBy: 1, remark: '到期销毁' })
    ElMessage.success('销毁成功')
    fetchList()
  } catch (e) {
    // cancel
  }
}

onMounted(fetchList)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
