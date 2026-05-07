<template>
  <div class="page-container">
    <div class="page-header-title">返工处理：<span class="page-header-sub">异常回退、重新煎煮、质量追溯</span></div>
    <el-card class="search-card" shadow="never">
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px">
        <el-form :model="searchForm" inline @submit.prevent>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" clearable placeholder="全部" style="width: 120px" @change="handleSearch">
              <el-option label="待审批" :value="0" />
              <el-option label="已通过" :value="1" />
              <el-option label="已拒绝" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="任务">
            <el-input v-model="searchForm.taskName" clearable placeholder="任务名称/条码" style="width: 180px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="warning" @click="openRollbackDialog">
          <el-icon><RefreshLeft /></el-icon> 发起回退
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="originalTaskId" label="原任务ID" width="100" />
        <el-table-column prop="newTaskId" label="新任务ID" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.newTaskId" size="small" type="success">{{ row.newTaskId }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="rollbackTo" label="回退阶段" min-width="120" />
        <el-table-column prop="reasonCode" label="原因编码" width="120" />
        <el-table-column prop="remark" label="回退说明" min-width="160" show-overflow-tooltip />
        <el-table-column prop="operatorId" label="操作人" width="100" />
        <el-table-column prop="createdAt" label="申请时间" min-width="160" />
        <el-table-column prop="approverId" label="审批人" width="100" />
        <el-table-column prop="approvedAt" label="审批时间" min-width="160" />
        <el-table-column prop="approvalStatus" label="审批状态" width="110" fixed="right">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.approvalStatus)" effect="dark">
              {{ statusText(row.approvalStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.approvalStatus === 0">
              <el-button link type="success" @click="handleApprove(row, 1)">通过</el-button>
              <el-button link type="danger" @click="handleApprove(row, 2)">拒绝</el-button>
            </template>
            <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="handleSearch"
        />
      </div>
    </el-card>

    <!-- 发起回退弹窗 -->
    <el-dialog v-model="rollbackDialogVisible" title="发起任务回退" width="520px">
      <el-form :model="rollbackForm" label-width="100px" :rules="rollbackRules" ref="rollbackFormRef">
        <el-form-item label="任务" prop="taskId" required>
          <el-select v-model="rollbackForm.taskId" placeholder="选择任务" style="width: 100%" filterable remote :remote-method="searchTasks" :loading="taskLoading">
            <el-option v-for="t in taskOptions" :key="t.id" :label="`${t.barcode || t.id} (${t.status})`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="回退阶段" prop="rollbackStage" required>
          <el-select v-model="rollbackForm.rollbackStage" placeholder="选择回退阶段" style="width: 100%">
            <el-option label="泡药" value="SOAK" />
            <el-option label="煎药" value="DECOCT" />
            <el-option label="包装" value="PACKAGE" />
            <el-option label="质检" value="QC" />
          </el-select>
        </el-form-item>
        <el-form-item label="回退原因" prop="reasonCode" required>
          <el-select v-model="rollbackForm.reasonCode" placeholder="选择回退原因" style="width: 100%">
            <el-option v-for="r in reasonOptions" :key="r.code" :label="r.name" :value="r.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细说明" prop="remark">
          <el-input v-model="rollbackForm.remark" type="textarea" rows="3" placeholder="请详细填写回退说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rollbackDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="submitRollback">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog v-model="approveDialogVisible" :title="approveStatus === 1 ? '审批通过' : '审批拒绝'" width="480px">
      <el-form :model="approveForm" label-width="80px">
        <el-form-item label="原任务ID">
          <span>{{ currentRow?.originalTaskId }}</span>
        </el-form-item>
        <el-form-item label="回退阶段">
          <span>{{ currentRow?.rollbackTo }}</span>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.comment" type="textarea" rows="3" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button :type="approveStatus === 1 ? 'success' : 'danger'" @click="submitApprove">
          {{ approveStatus === 1 ? '确认通过' : '确认拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="回退详情" width="560px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="原任务ID">{{ currentRow?.originalTaskId }}</el-descriptions-item>
        <el-descriptions-item label="新任务ID">{{ currentRow?.newTaskId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="回退阶段">{{ currentRow?.rollbackTo }}</el-descriptions-item>
        <el-descriptions-item label="回退原因">{{ currentRow?.reasonCode }}</el-descriptions-item>
        <el-descriptions-item label="详细说明">{{ currentRow?.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentRow?.operatorId }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentRow?.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ currentRow?.approverId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批时间">{{ currentRow?.approvedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批意见">{{ currentRow?.approvalComment || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(currentRow?.approvalStatus)" effect="dark">{{ statusText(currentRow?.approvalStatus) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { rollbackTask, approveRollback, getRollbackList, getRollbackReasons } from '@/api/newModules'
import request from '@/api/request'

const userStore = useUserStore()

const loading = ref(false)
const searchForm = reactive({ status: null as number | null, taskName: '' })
const tableData = ref<any[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })

const statusTagType = (status?: number) => {
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}
const statusText = (status?: number) => {
  if (status === 0) return '待审批'
  if (status === 1) return '已通过'
  if (status === 2) return '已拒绝'
  return '-'
}

async function handleSearch() {
  loading.value = true
  try {
    const params: any = { page: pagination.page, size: pagination.size }
    if (searchForm.status !== null && searchForm.status !== undefined) {
      params.approvalStatus = searchForm.status
    }
    const res: any = await getRollbackList(params)
    const pageData = res.data || {}
    tableData.value = pageData.records || []
    pagination.total = pageData.total || 0
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchForm.status = null
  searchForm.taskName = ''
  pagination.page = 1
  handleSearch()
}

const rollbackDialogVisible = ref(false)
const rollbackFormRef = ref<any>(null)
const rollbackForm = reactive({ taskId: null as any, rollbackStage: '', reasonCode: '', remark: '' })
const rollbackRules = {
  taskId: [{ required: true, message: '请选择任务', trigger: 'change' }],
  rollbackStage: [{ required: true, message: '请选择回退阶段', trigger: 'change' }],
  reasonCode: [{ required: true, message: '请选择回退原因', trigger: 'change' }]
}

const taskOptions = ref<any[]>([])
const taskLoading = ref(false)
const reasonOptions = ref<any[]>([])

const stageMap: Record<string, string> = {
  SOAK: '待泡药',
  DECOCT: '待煎药',
  PACKAGE: '待包装',
  QC: '待质检'
}

async function loadReasons() {
  try {
    const res: any = await getRollbackReasons()
    reasonOptions.value = res.data || []
  } catch {
    reasonOptions.value = []
  }
}

async function searchTasks(keyword: string) {
  taskLoading.value = true
  try {
    const res: any = await request.get('/v1/prod/tasks', {
      params: { prescriptionNumber: keyword || undefined, page: 1, size: 20 }
    })
    taskOptions.value = (res.data?.records || []).map((t: any) => ({
      id: t.id,
      barcode: t.barcode,
      status: t.status
    }))
  } catch {
    taskOptions.value = []
  } finally {
    taskLoading.value = false
  }
}

function openRollbackDialog() {
  rollbackDialogVisible.value = true
  rollbackForm.taskId = null
  rollbackForm.rollbackStage = ''
  rollbackForm.reasonCode = ''
  rollbackForm.remark = ''
  loadReasons()
}

async function submitRollback() {
  rollbackFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    try {
      const rollbackTo = stageMap[rollbackForm.rollbackStage]
      await rollbackTask(rollbackForm.taskId, {
        rollbackTo,
        reasonCode: rollbackForm.reasonCode,
        remark: rollbackForm.remark,
        operatorId: userStore.userInfo?.id
      })
      ElMessage.success('回退申请已提交')
      rollbackDialogVisible.value = false
      handleSearch()
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || '提交失败')
    }
  })
}

const approveDialogVisible = ref(false)
const approveStatus = ref<number>(1)
const currentRow = ref<any>(null)
const approveForm = reactive({ comment: '' })

function handleApprove(row: any, status: number) {
  currentRow.value = row
  approveStatus.value = status
  approveForm.comment = ''
  approveDialogVisible.value = true
}

async function submitApprove() {
  if (!currentRow.value) return
  try {
    await approveRollback(currentRow.value.id, {
      approverId: userStore.userInfo?.id,
      approvalStatus: approveStatus.value,
      approvalComment: approveForm.comment
    })
    ElMessage.success(approveStatus.value === 1 ? '已通过' : '已拒绝')
    approveDialogVisible.value = false
    handleSearch()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '审批操作失败')
  }
}

const detailDialogVisible = ref(false)
function viewDetail(row: any) {
  currentRow.value = row
  detailDialogVisible.value = true
}

onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.search-card {
  margin-top: 8px;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
