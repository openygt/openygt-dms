<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>异常工单管理</span>
        </div>
      </template>
      <el-form :inline="true" :model="query" class="demo-form-inline">
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部">
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已解决" :value="2" />
            <el-option label="已升级" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="query.level" clearable placeholder="全部">
            <el-option label="一般" :value="1" />
            <el-option label="严重" :value="2" />
            <el-option label="紧急" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading">
        <el-table-column prop="exceptionNo" label="异常单号" width="150" />
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
            {{ statusText(row.currentStatus) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{row}">
            <el-button v-if="row.currentStatus === 0" size="small" type="primary" @click="handleHandle(row)">处理</el-button>
            <el-button v-if="row.currentStatus !== 2" size="small" type="warning" @click="handleEscalate(row)">升级</el-button>
          </template>
        </el-table-column>
      </el-table>
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
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const list = ref([])
const loading = ref(false)
const query = ref<any>({})
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

const fetchList = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/v1/prod/exception-order/list', { params: query.value })
    list.value = res.data.data?.list || []
  } catch (e) {
    ElMessage.error('获取列表失败')
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
    await axios.post(`/api/v1/prod/exception-order/${currentRow.value.id}/handle`, {
      handlerId: 1,
      handleResult: handleForm.value.handleResult
    })
    ElMessage.success('处理成功')
    handleDialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error('处理失败')
  }
}

const handleEscalate = async (row: any) => {
  try {
    await axios.post(`/api/v1/prod/exception-order/${row.id}/escalate`, { escalationReason: '需要上级支援' })
    ElMessage.success('升级成功')
    fetchList()
  } catch (e) {
    ElMessage.error('升级失败')
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
