<template>
  <div class="page-container">
    <!-- 急诊统计 -->
    <el-card class="emergency-stat" shadow="never">
      <el-row :gutter="24" align="middle">
        <el-col :span="4">
          <div class="stat-icon">
            <el-icon :size="48" color="#fff"><FirstAidKit /></el-icon>
          </div>
        </el-col>
        <el-col :span="20">
          <el-row :gutter="16">
            <el-col :span="6">
              <div class="emergency-stat-item">
                <div class="emergency-stat-value">{{ emergencyStat.total }}</div>
                <div class="emergency-stat-label">急诊总数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="emergency-stat-item">
                <div class="emergency-stat-value text-warning">{{ emergencyStat.pending }}</div>
                <div class="emergency-stat-label">待处理</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="emergency-stat-item">
                <div class="emergency-stat-value text-danger">{{ emergencyStat.overdue }}</div>
                <div class="emergency-stat-label">已超时</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="emergency-stat-item">
                <div class="emergency-stat-value text-success">{{ emergencyStat.completed }}</div>
                <div class="emergency-stat-label">已完成/已签收</div>
              </div>
            </el-col>
          </el-row>
        </el-col>
      </el-row>
    </el-card>

    <!-- 搜索筛选 -->
    <el-card class="table-card" shadow="never">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="急诊级别">
          <el-select v-model="search.emergencyLevel" placeholder="全部" clearable style="width: 120px">
            <el-option label="普通急诊" :value="1" />
            <el-option label="危重急诊" :value="2" />
            <el-option label="抢救" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="search.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已签收" value="SIGNED" />
          </el-select>
        </el-form-item>
        <el-form-item label="处方号">
          <el-input v-model="search.prescriptionNumber" placeholder="处方号" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="患者姓名">
          <el-input v-model="search.patientName" placeholder="患者姓名" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 急诊列表 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="prescriptionNumber" label="处方号" min-width="140" />
        <el-table-column prop="patientName" label="患者姓名" min-width="100" />
        <el-table-column prop="hospitalName" label="医院" min-width="120" />
        <el-table-column prop="department" label="科室" min-width="100" />
        <el-table-column prop="emergencyLevel" label="优先级" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.emergencyLevel === 3" type="danger">抢救</el-tag>
            <el-tag v-else-if="row.emergencyLevel === 2" type="warning">危重</el-tag>
            <el-tag v-else type="primary">普通</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="promisedFinishTime" label="承诺完成时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.promisedFinishTime) }}</template>
        </el-table-column>
        <el-table-column prop="actualFinishTime" label="实际完成时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.actualFinishTime) }}</template>
        </el-table-column>
        <el-table-column label="时效对比" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'SIGNED'" :type="row.isOnTime === 1 ? 'success' : 'danger'">
              {{ row.isOnTime === 1 ? '按时' : '超时' }}
            </el-tag>
            <el-tag v-else-if="isOverdue(row)" type="danger">已超时</el-tag>
            <el-tag v-else type="info">进行中</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PENDING'" type="warning">待处理</el-tag>
            <el-tag v-else-if="row.status === 'COMPLETED'" type="primary">已完成</el-tag>
            <el-tag v-else-if="row.status === 'SIGNED'" type="success">已签收</el-tag>
            <el-tag v-else type="info">未知</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'SIGNED'" link type="primary" @click="openSignDialog(row)">签收</el-button>
            <el-button link type="info" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 标记急诊对话框 -->
    <!-- 签收对话框 -->
    <el-dialog v-model="signDialogVisible" title="急诊签收" width="400px">
      <el-form :model="signForm" label-width="100px">
        <el-form-item label="处方号">
          <span>{{ signForm.prescriptionNumber }}</span>
        </el-form-item>
        <el-form-item label="患者姓名">
          <span>{{ signForm.patientName }}</span>
        </el-form-item>
        <el-form-item label="护士姓名" required>
          <el-input v-model="signForm.nurseName" placeholder="请输入护士姓名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="signDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSignConfirm">确认签收</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="急诊处方详情" size="500px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="处方号">{{ detail.prescriptionNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="患者姓名">{{ detail.patientName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="医院">{{ detail.hospitalName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ detail.department || '-' }}</el-descriptions-item>
        <el-descriptions-item label="急诊级别">
          <el-tag v-if="detail.emergencyLevel === 3" type="danger">抢救</el-tag>
          <el-tag v-else-if="detail.emergencyLevel === 2" type="warning">危重</el-tag>
          <el-tag v-else type="primary">普通</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="detail.status === 'PENDING'" type="warning">待处理</el-tag>
          <el-tag v-else-if="detail.status === 'COMPLETED'" type="primary">已完成</el-tag>
          <el-tag v-else-if="detail.status === 'SIGNED'" type="success">已签收</el-tag>
          <el-tag v-else type="info">未知</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求时间">{{ formatTime(detail.requestTime) }}</el-descriptions-item>
        <el-descriptions-item label="承诺完成时间">{{ formatTime(detail.promisedFinishTime) }}</el-descriptions-item>
        <el-descriptions-item label="实际完成时间">{{ formatTime(detail.actualFinishTime) }}</el-descriptions-item>
        <el-descriptions-item label="是否按时">
          <span v-if="detail.isOnTime === 1" style="color: #67c23a">是</span>
          <span v-else-if="detail.isOnTime === 0" style="color: #f56c6c">否</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="延迟原因">{{ detail.delayReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="送达方式">{{ detail.deliveryType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="送达位置">{{ detail.deliveryLocation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="护士姓名">{{ detail.nurseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="护士签收时间">{{ formatTime(detail.nurseSignTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FirstAidKit } from '@element-plus/icons-vue'
import { getEmergencyPrescriptions, markEmergency, signEmergency } from '@/api/newModules'

interface EmergencyItem {
  id: number
  prescriptionId: number
  prescriptionNumber: string
  patientName: string
  hospitalName: string
  department: string
  emergencyLevel: number
  requestTime: string
  promisedFinishTime: string
  actualFinishTime: string
  isOnTime: number | null
  delayReason: string
  deliveryType: string
  deliveryLocation: string
  nurseName: string
  nurseSignTime: string
  status: string
}

const loading = ref(false)
const tableData = ref<EmergencyItem[]>([])
const selectedRows = ref<EmergencyItem[]>([])
const emergencyStat = reactive({ total: 0, pending: 0, overdue: 0, completed: 0 })

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const search = reactive({
  emergencyLevel: null as number | null,
  status: '',
  prescriptionNumber: '',
  patientName: ''
})

// 签收对话框
const signDialogVisible = ref(false)
const signForm = reactive({
  emergencyId: 0,
  prescriptionNumber: '',
  patientName: '',
  nurseName: ''
})

// 详情抽屉
const detailVisible = ref(false)
const detail = ref<EmergencyItem | null>(null)

function isOverdue(row: EmergencyItem): boolean {
  if (row.status === 'SIGNED' || row.status === 'COMPLETED') return false
  if (!row.promisedFinishTime) return false
  return new Date(row.promisedFinishTime) < new Date()
}

function formatTime(dt: string | null): string {
  if (!dt) return '-'
  const d = new Date(dt)
  return isNaN(d.getTime()) ? dt : d.toLocaleString('zh-CN', { hour12: false })
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize.value }
    if (search.emergencyLevel != null) params.emergencyLevel = search.emergencyLevel
    if (search.status) params.status = search.status
    const res: any = await getEmergencyPrescriptions(params)
    const data = res.data || {}
    let list = data.records || []

    // Client-side filtering for prescriptionNumber and patientName
    if (search.prescriptionNumber) {
      list = list.filter((item: any) =>
        (item.prescriptionNumber || '').includes(search.prescriptionNumber)
      )
    }
    if (search.patientName) {
      list = list.filter((item: any) =>
        (item.patientName || '').includes(search.patientName)
      )
    }

    tableData.value = list
    total.value = data.total || list.length

    // Stats from current page data
    emergencyStat.total = total.value
    emergencyStat.pending = list.filter((item: any) => item.status === 'PENDING').length
    emergencyStat.overdue = list.filter((item: any) => isOverdue(item)).length
    emergencyStat.completed = list.filter((item: any) => item.status === 'COMPLETED' || item.status === 'SIGNED').length
  } catch (e) {
    ElMessage.error('加载急诊处方失败')
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  page.value = 1
  loadData()
}

function resetSearch() {
  search.emergencyLevel = null
  search.status = ''
  search.prescriptionNumber = ''
  search.patientName = ''
  page.value = 1
  loadData()
}

function handleSelectionChange(selection: EmergencyItem[]) {
  selectedRows.value = selection
}

// 签收
function openSignDialog(row: EmergencyItem) {
  signForm.emergencyId = row.id
  signForm.prescriptionNumber = row.prescriptionNumber || ''
  signForm.patientName = row.patientName || ''
  signForm.nurseName = ''
  signDialogVisible.value = true
}

async function handleSignConfirm() {
  if (!signForm.nurseName.trim()) {
    ElMessage.warning('请输入护士姓名')
    return
  }
  try {
    await signEmergency(signForm.emergencyId, signForm.nurseName.trim())
    ElMessage.success('签收成功')
    signDialogVisible.value = false
    loadData()
  } catch (e) {
    // handled by interceptor
  }
}

// 详情
function viewDetail(row: EmergencyItem) {
  detail.value = row
  detailVisible.value = true
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.page-container {
  padding: var(--ygt-space-4);
}

.emergency-stat {
  margin-top: 0;
  background: linear-gradient(135deg, #f56c6c 0%, #ff9f9f 100%);
  color: #fff;
  border: none;
}

.emergency-stat :deep(.el-card__body) {
  padding: 20px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
}

.emergency-stat-item {
  text-align: center;
}

.emergency-stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #fff;
}

.emergency-stat-label {
  margin-top: 4px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

.table-card {
  margin-top: 16px;
}

.text-success { color: #67c23a; }
.text-warning { color: #ffe58f; }
.text-danger { color: #ffd1d1; }
</style>
