<template>
  <div class="page-container">
    <el-page-header title="异常回退处理" content="任务回退申请与审批管理" />

    <el-card class="search-card" shadow="never">
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px">
        <el-form :model="searchForm" inline @submit.prevent>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" clearable placeholder="全部" style="width: 120px" @change="handleSearch">
              <el-option label="待审批" value="PENDING" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已拒绝" value="REJECTED" />
            </el-select>
          </el-form-item>
          <el-form-item label="任务">
            <el-input v-model="searchForm.taskName" clearable placeholder="任务名称" style="width: 180px" />
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
        <el-table-column prop="taskName" label="任务名称" min-width="140" />
        <el-table-column prop="rollbackStage" label="回退阶段" min-width="120" />
        <el-table-column prop="reason" label="回退原因" min-width="160" show-overflow-tooltip />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="applyTime" label="申请时间" min-width="160" />
        <el-table-column prop="approverName" label="审批人" width="100" />
        <el-table-column prop="approveTime" label="审批时间" min-width="160" />
        <el-table-column prop="status" label="审批状态" width="110" fixed="right">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="dark">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button link type="success" @click="handleApprove(row)">通过</el-button>
              <el-button link type="danger" @click="handleReject(row)">拒绝</el-button>
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
          <el-select v-model="rollbackForm.taskId" placeholder="选择任务" style="width: 100%">
            <el-option v-for="t in taskOptions" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="回退阶段" prop="rollbackStage" required>
          <el-select v-model="rollbackForm.rollbackStage" placeholder="选择回退阶段" style="width: 100%">
            <el-option label="调剂" value="DISPENSE" />
            <el-option label="浸泡" value="SOAK" />
            <el-option label="煎煮" value="DECOCT" />
            <el-option label="包装" value="PACKAGE" />
            <el-option label="质检" value="QC" />
          </el-select>
        </el-form-item>
        <el-form-item label="回退原因" prop="reason" required>
          <el-input v-model="rollbackForm.reason" type="textarea" rows="3" placeholder="请详细填写回退原因" />
        </el-form-item>
        <el-form-item label="附件">
          <el-upload action="#" :auto-upload="false" :limit="3">
            <el-button type="primary" size="small">选择文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rollbackDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="submitRollback">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog v-model="approveDialogVisible" :title="approveAction === 'APPROVE' ? '审批通过' : '审批拒绝'" width="480px">
      <el-form :model="approveForm" label-width="80px">
        <el-form-item label="任务">
          <span>{{ currentRow?.taskName }}</span>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.comment" type="textarea" rows="3" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button :type="approveAction === 'APPROVE' ? 'success' : 'danger'" @click="submitApprove">
          {{ approveAction === 'APPROVE' ? '确认通过' : '确认拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="回退详情" width="560px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="任务名称">{{ currentRow?.taskName }}</el-descriptions-item>
        <el-descriptions-item label="回退阶段">{{ currentRow?.rollbackStage }}</el-descriptions-item>
        <el-descriptions-item label="回退原因">{{ currentRow?.reason }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentRow?.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentRow?.applyTime }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ currentRow?.approverName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批时间">{{ currentRow?.approveTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批意见">{{ currentRow?.approveComment || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(currentRow?.status)" effect="dark">{{ statusText(currentRow?.status) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshLeft } from '@element-plus/icons-vue'
import { rollbackTask, approveRollback, getRollbackList } from '@/api/newModules'

const loading = ref(false)
const searchForm = reactive({ status: '', taskName: '' })
const tableData = ref<any[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })

const statusTagType = (status?: string) => {
  if (status === 'PENDING') return 'warning'
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'info'
}
const statusText = (status?: string) => {
  if (status === 'PENDING') return '待审批'
  if (status === 'APPROVED') return '已通过'
  if (status === 'REJECTED') return '已拒绝'
  return status || '-'
}

async function handleSearch() {
  loading.value = true
  try {
    const res = await getRollbackList({ ...searchForm, page: pagination.page, size: pagination.size }) as any
    tableData.value = res.data?.list || []
    pagination.total = res.data?.total || 0
  } catch {
    tableData.value = [
      { id: 1, taskName: '处方A-煎煮', rollbackStage: '煎煮', reason: '温度异常需重新煎煮', applicantName: '张三', applyTime: '2024-05-01 09:00', approverName: '李主管', approveTime: '2024-05-01 09:30', status: 'APPROVED', approveComment: '同意' },
      { id: 2, taskName: '处方B-包装', rollbackStage: '包装', reason: '漏液需重新包装', applicantName: '李四', applyTime: '2024-05-01 10:00', approverName: '', approveTime: '', status: 'PENDING', approveComment: '' },
      { id: 3, taskName: '处方C-质检', rollbackStage: '质检', reason: '质检不合格', applicantName: '王五', applyTime: '2024-05-01 11:00', approverName: '李主管', approveTime: '2024-05-01 11:20', status: 'REJECTED', approveComment: '证据不足' }
    ]
    pagination.total = 3
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchForm.status = ''
  searchForm.taskName = ''
  pagination.page = 1
  handleSearch()
}

const rollbackDialogVisible = ref(false)
const rollbackFormRef = ref<any>(null)
const rollbackForm = reactive({ taskId: null as any, rollbackStage: '', reason: '' })
const rollbackRules = {
  taskId: [{ required: true, message: '请选择任务', trigger: 'change' }],
  rollbackStage: [{ required: true, message: '请选择回退阶段', trigger: 'change' }],
  reason: [{ required: true, message: '请填写回退原因', trigger: 'blur' }]
}
const taskOptions = ref([
  { id: 101, name: '处方A-煎煮' },
  { id: 102, name: '处方B-包装' },
  { id: 103, name: '处方C-质检' }
])

function openRollbackDialog() {
  rollbackDialogVisible.value = true
  rollbackForm.taskId = null
  rollbackForm.rollbackStage = ''
  rollbackForm.reason = ''
}

async function submitRollback() {
  rollbackFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    try {
      await rollbackTask(rollbackForm.taskId, { rollbackStage: rollbackForm.rollbackStage, reason: rollbackForm.reason })
      ElMessage.success('回退申请已提交')
      rollbackDialogVisible.value = false
      handleSearch()
    } catch {
      ElMessage.error('提交失败')
    }
  })
}

const approveDialogVisible = ref(false)
const approveAction = ref<'APPROVE' | 'REJECT'>('APPROVE')
const currentRow = ref<any>(null)
const approveForm = reactive({ comment: '' })

function handleApprove(row: any) {
  currentRow.value = row
  approveAction.value = 'APPROVE'
  approveForm.comment = ''
  approveDialogVisible.value = true
}

function handleReject(row: any) {
  currentRow.value = row
  approveAction.value = 'REJECT'
  approveForm.comment = ''
  approveDialogVisible.value = true
}

async function submitApprove() {
  try {
    await approveRollback(currentRow.value.id, {
      action: approveAction.value,
      comment: approveForm.comment
    })
    ElMessage.success(approveAction.value === 'APPROVE' ? '已通过' : '已拒绝')
    approveDialogVisible.value = false
    handleSearch()
  } catch {
    ElMessage.error('审批操作失败')
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
