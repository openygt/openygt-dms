<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <el-tag v-if="mode === 'expiring'" type="warning" style="margin-right: 8px">即将到期预警</el-tag>
            <el-button v-if="mode === 'expiring'" @click="fetchList">返回全部</el-button>
            <el-button v-else type="warning" @click="fetchExpiring">即将到期预警</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="sampleNo" label="留样编号" width="150" />
        <el-table-column prop="taskId" label="关联任务" width="100" />
        <el-table-column label="留样类型" width="100">
          <template #default>7天留样</template>
        </el-table-column>
        <el-table-column label="留样时间" width="160">
          <template #default="{row}">
            {{ formatDateTime(row.retainTime) }}
          </template>
        </el-table-column>
        <el-table-column label="到期时间" width="160">
          <template #default="{row}">
            {{ row.expireTime ? formatDateTime(row.expireTime) : '-' }}
          </template>
        </el-table-column>
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

      <el-empty v-if="!loading && list.length === 0" description="暂无留样记录">
        <template #description>
          <div>
            <p>暂无留样记录</p>
            <p style="font-size: 12px; color: #999; margin-top: 8px">质检通过/不通过后将自动创建留样</p>
          </div>
        </template>
      </el-empty>

      <el-pagination
        v-if="mode === 'list' && list.length > 0"
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="fetchList"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.id || 0)

const list = ref([])
const loading = ref(false)
const mode = ref<'list' | 'expiring'>('list')
const pagination = ref({ page: 1, size: 20, total: 0 })

const formatDateTime = (dt: string) => {
  if (!dt) return '-'
  const d = new Date(dt)
  if (isNaN(d.getTime())) return dt
  return d.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

const statusText = (status: number) => {
  const map: Record<number, string> = { 1: '在库', 2: '已销毁', 3: '已过期', 4: '已复检' }
  return map[status] || '未知'
}

const fetchList = async () => {
  mode.value = 'list'
  loading.value = true
  try {
    const res = await request.get('/v1/qt/retain-sample/list', {
      params: { page: pagination.value.page, size: pagination.value.size }
    })
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } catch (e) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

const fetchExpiring = async () => {
  mode.value = 'expiring'
  loading.value = true
  try {
    const res = await request.get('/v1/qt/retain-sample/expiring', { params: { withinHours: 2 } })
    list.value = res.data || []
  } catch (e) {
    ElMessage.error('获取预警失败')
  } finally {
    loading.value = false
  }
}

const handleDestroy = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认销毁该留样？', '提示', { type: 'warning' })
    await request.post(`/v1/qt/retain-sample/${row.id}/destroy`, { destroyBy: currentUserId.value, remark: '到期销毁' })
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
