<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
        </div>
      </template>
      <el-form :inline="true" :model="query" class="demo-form-inline">
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 160px">
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已解决" :value="2" />
            <el-option label="已升级" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="query.level" clearable placeholder="全部" style="width: 160px">
            <el-option label="一般" :value="1" />
            <el-option label="严重" :value="2" />
            <el-option label="紧急" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="exceptionNo" label="异常单号" width="160" />
        <el-table-column prop="taskId" label="关联任务" width="100" />
        <el-table-column prop="exceptionType" label="类型" width="100">
          <template #default="{row}">
            {{ typeText(row.exceptionType) }}
          </template>
        </el-table-column>
        <el-table-column prop="exceptionLevel" label="等级" width="80">
          <template #default="{row}">
            <el-tag :type="row.exceptionLevel === 3 ? 'danger' : row.exceptionLevel === 2 ? 'warning' : 'info'">
              {{ levelText(row.exceptionLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="currentStatus" label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="statusTagType(row.currentStatus)">{{ statusText(row.currentStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{row}">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{row}">
            <el-button v-if="row.currentStatus !== 2" size="small" type="primary" @click="handleHandle(row)">处理</el-button>
            <el-button v-if="row.currentStatus !== 2 && row.currentStatus !== 3" size="small" type="warning" @click="handleEscalate(row)">升级</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="fetchList"
      />
    </el-card>

    <el-dialog title="处理异常工单" v-model="handleDialogVisible" width="500px">
      <el-form :model="handleForm" label-width="100px">
        <el-form-item label="处理结果">
          <el-input v-model="handleForm.handleResult" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.id || 0)

const list = ref([])
const loading = ref(false)
const query = ref<any>({})
const pagination = ref({ page: 1, size: 20, total: 0 })
const handleDialogVisible = ref(false)
const handleForm = ref<any>({})
const currentRow = ref<any>(null)

const typeText = (type: number) => {
  const map: Record<number, string> = { 1: '设备故障', 2: '停电', 3: '药材缺货', 4: '操作异常', 5: '打印失败', 6: '其他' }
  return map[type] || '未知'
}
const levelText = (level: number) => {
  const map: Record<number, string> = { 1: '一般', 2: '严重', 3: '紧急' }
  return map[level] || '未知'
}
const statusText = (status: number) => {
  const map: Record<number, string> = { 0: '待处理', 1: '处理中', 2: '已解决', 3: '已升级' }
  return map[status] || '未知'
}
const statusTagType = (status: number) => {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return map[status] || ''
}
const formatDateTime = (dt?: string) => {
  if (!dt) return '-'
  const d = new Date(dt)
  if (isNaN(d.getTime())) return dt
  return d.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      ...query.value,
      page: pagination.value.page,
      size: pagination.value.size
    }
    const res: any = await request.get('/v1/prod/exception-order/list', { params })
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '获取列表失败')
  } finally {
    loading.value = false
  }
}

const handleHandle = (row: any) => {
  currentRow.value = row
  handleForm.value = {}
  handleDialogVisible.value = true
}

const submitHandle = async () => {
  try {
    await request.post(`/v1/prod/exception-order/${currentRow.value.id}/handle`, {
      handlerId: currentUserId.value,
      handleResult: handleForm.value.handleResult
    })
    ElMessage.success('处理成功')
    handleDialogVisible.value = false
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '处理失败')
  }
}

const handleEscalate = async (row: any) => {
  try {
    await request.post(`/v1/prod/exception-order/${row.id}/escalate`, { escalationReason: '需要上级支援' })
    ElMessage.success('升级成功')
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '升级失败')
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
