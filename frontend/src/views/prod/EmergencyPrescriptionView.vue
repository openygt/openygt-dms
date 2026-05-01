<template>
  <div class="page-container">
    <el-page-header title="急诊快速通道" content="优先处理急诊处方，确保时效" />

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
                <div class="emergency-stat-label">已完成</div>
              </div>
            </el-col>
          </el-row>
        </el-col>
      </el-row>
    </el-card>

    <!-- 急诊列表 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <span>急诊处方列表</span>
          <el-button type="danger" @click="handleBatchMark">批量标记急诊</el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        :row-class-name="getRowClassName"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="prescriptionNo" label="处方号" min-width="140" />
        <el-table-column prop="patientName" label="患者姓名" min-width="100" />
        <el-table-column prop="hospitalName" label="医院" min-width="120" />
        <el-table-column prop="department" label="科室" min-width="100" />
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.priority === 'CRITICAL'" type="danger">危急</el-tag>
            <el-tag v-else-if="row.priority === 'URGENT'" type="warning">紧急</el-tag>
            <el-tag v-else type="primary">一般</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="promisedTime" label="承诺完成时间" min-width="160" />
        <el-table-column prop="actualTime" label="实际完成时间" min-width="160" />
        <el-table-column label="时效对比" min-width="120">
          <template #default="{ row }">
            <el-tag v-if="row.actualTime && row.actualTime <= row.promisedTime" type="success">按时</el-tag>
            <el-tag v-else-if="row.actualTime && row.actualTime > row.promisedTime" type="danger">超时</el-tag>
            <el-tag v-else type="info">进行中</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PENDING'" type="warning">待处理</el-tag>
            <el-tag v-else-if="row.status === 'PROCESSING'" type="primary">处理中</el-tag>
            <el-tag v-else-if="row.status === 'COMPLETED'" type="success">已完成</el-tag>
            <el-tag v-else type="info">未知</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.isEmergency" link type="danger" @click="handleMark(row)">标记急诊</el-button>
            <el-button v-if="row.status !== 'COMPLETED'" link type="primary" @click="handleSign(row)">签收</el-button>
            <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { FirstAidKit } from '@element-plus/icons-vue'
import { getEmergencyPrescriptions, markEmergency, signEmergency } from '@/api/newModules'

interface EmergencyItem {
  id: number
  prescriptionNo: string
  patientName: string
  hospitalName: string
  department: string
  priority: string
  promisedTime: string
  actualTime: string
  status: string
  isEmergency: boolean
}

const loading = ref(false)
const tableData = ref<EmergencyItem[]>([])
const emergencyStat = reactive({ total: 0, pending: 0, overdue: 0, completed: 0 })

function getRowClassName({ row }: { row: EmergencyItem }) {
  if (row.isEmergency) return 'emergency-row'
  return ''
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getEmergencyPrescriptions()
    const data = res.data || {}
    tableData.value = data.list || []
    emergencyStat.total = data.total || 0
    emergencyStat.pending = data.pending || 0
    emergencyStat.overdue = data.overdue || 0
    emergencyStat.completed = data.completed || 0
  } catch (e) {
    ElMessage.error('加载急诊处方失败')
  } finally {
    loading.value = false
  }
}

async function handleMark(row: EmergencyItem) {
  try {
    await markEmergency(row.id)
    row.isEmergency = true
    ElMessage.success('已标记为急诊')
  } catch (e) {
    // handled by interceptor
  }
}

async function handleSign(row: EmergencyItem) {
  try {
    await signEmergency(row.id)
    ElMessage.success('签收成功')
    loadData()
  } catch (e) {
    // handled by interceptor
  }
}

function handleBatchMark() {
  ElMessage.warning('请选择要标记的处方')
}

function viewDetail(row: EmergencyItem) {
  ElMessage.info(`查看处方 ${row.prescriptionNo} 详情`)
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
  margin-top: 16px;
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

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.text-success { color: #67c23a; }
.text-warning { color: #ffe58f; }
.text-danger { color: #ffd1d1; }
</style>

<style>
.emergency-row {
  background-color: #fff5f5 !important;
}
.emergency-row:hover > td {
  background-color: #ffeaea !important;
}
</style>
