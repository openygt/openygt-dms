<template>
  <div class="page-container">
    <div class="page-header-title">时效监控：<span class="page-header-sub">各设备倒计时、超时预警、时效达成率</span></div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card stat-normal" shadow="hover">
          <div class="stat-value">{{ stat.normal }}</div>
          <div class="stat-label">正常</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-warning" shadow="hover">
          <div class="stat-value">{{ stat.warning }}</div>
          <div class="stat-label">预警</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-timeout" shadow="hover">
          <div class="stat-value">{{ stat.timeout }}</div>
          <div class="stat-label">超时</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-resolved" shadow="hover">
          <div class="stat-value">{{ stat.resolved }}</div>
          <div class="stat-label">已处理</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作栏 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <span>时效监控列表</span>
          <div class="table-actions">
            <el-button type="primary" @click="showRuleDialog = true">配置规则</el-button>
            <el-button @click="loadData">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="taskId" label="任务ID" width="120" />
        <el-table-column prop="stageName" label="阶段" width="120" />
        <el-table-column prop="planTime" label="计划时间" width="160" />
        <el-table-column prop="remainingTime" label="剩余时间" width="120">
          <template #default="{ row }">
            <span :class="getRemainingClass(row)">{{ row.remainingTime }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'NORMAL'" type="success">正常</el-tag>
            <el-tag v-else-if="row.status === 'WARNING'" type="warning">预警</el-tag>
            <el-tag v-else-if="row.status === 'TIMEOUT'" type="danger">超时</el-tag>
            <el-tag v-else type="info">已处理</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="prescriptionNo" label="处方号" min-width="140" />
        <el-table-column prop="operatorName" label="负责人" width="100" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'WARNING' || row.status === 'TIMEOUT'"
              link
              type="primary"
              @click="handleResolve(row)"
            >
              处理
            </el-button>
            <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 规则配置弹窗 -->
    <el-dialog v-model="showRuleDialog" title="时效规则配置" width="600px">
      <el-form :model="ruleForm" label-width="120px">
        <el-form-item label="规则名称">
          <el-input v-model="ruleForm.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="适用阶段">
          <el-select v-model="ruleForm.stage" placeholder="请选择阶段" style="width: 100%">
            <el-option label="处方接收" value="RECEIVE" />
            <el-option label="调剂" value="DISPENSE" />
            <el-option label="泡药" value="SOAK" />
            <el-option label="头煎" value="DECOCT_FIRST" />
            <el-option label="二煎" value="DECOCT_SECOND" />
            <el-option label="合并" value="MERGE" />
            <el-option label="过滤" value="FILTER" />
            <el-option label="包装" value="PACKAGE" />
            <el-option label="质检" value="QC" />
            <el-option label="发货" value="SHIP" />
          </el-select>
        </el-form-item>
        <el-form-item label="标准时长(分钟)">
          <el-input-number v-model="ruleForm.standardDuration" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预警提前(分钟)">
          <el-input-number v-model="ruleForm.warningThreshold" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="超时阈值(分钟)">
          <el-input-number v-model="ruleForm.alertThreshold" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="ruleForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRuleDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getTimeMonitorDashboard,
  getActiveAlerts,
  resolveAlert,
  createTimeRule,
  getAlertStatistics
} from '@/api/newModules'

interface MonitorItem {
  id: number
  taskId: string
  stageName: string
  planTime: string
  remainingTime: string
  status: 'NORMAL' | 'WARNING' | 'TIMEOUT' | 'RESOLVED'
  prescriptionNo: string
  operatorName: string
  alertId?: number
}

const loading = ref(false)
const tableData = ref<MonitorItem[]>([])
const stat = reactive({ normal: 0, warning: 0, timeout: 0, resolved: 0 })

const showRuleDialog = ref(false)
const ruleForm = reactive({
  ruleCode: '',
  ruleName: '',
  stage: '',
  standardDuration: 30,
  warningThreshold: 5,
  alertThreshold: 10,
  isDefault: 1
})

function getRemainingClass(row: MonitorItem) {
  if (row.status === 'TIMEOUT') return 'text-danger'
  if (row.status === 'WARNING') return 'text-warning'
  return 'text-success'
}

async function loadData() {
  loading.value = true
  try {
    const [dashboardRes, alertsRes]: any = await Promise.all([
      getTimeMonitorDashboard(),
      getActiveAlerts()
    ])
    const dashboardData = dashboardRes.data || {}
    const alertsPage = alertsRes.data || { records: [] }
    const alertsData = alertsPage.records || []

    tableData.value = alertsData.map((item: any) => ({
      id: item.id,
      taskId: item.taskId || '-',
      stageName: item.stageName || '-',
      planTime: item.planTime || '-',
      remainingTime: item.remainingTime || '-',
      status: item.status || 'NORMAL',
      prescriptionNo: item.prescriptionNo || '-',
      operatorName: item.operatorName || '-',
      alertId: item.alertId
    }))

    stat.normal = dashboardData.onTimeTasks || 0
    stat.warning = dashboardData.warningTasks || 0
    stat.timeout = dashboardData.alertTasks || 0
    stat.resolved = 0
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

async function handleResolve(row: MonitorItem) {
  if (!row.alertId) return
  try {
    await resolveAlert(row.alertId)
    ElMessage.success('预警处理成功')
    loadData()
  } catch (e) {
    // handled by interceptor
  }
}

function viewDetail(row: MonitorItem) {
  ElMessage.info(`查看任务 ${row.taskId} 详情`)
}

async function handleSaveRule() {
  try {
    const payload = { ...ruleForm }
    if (!payload.ruleCode) {
      payload.ruleCode = 'RULE_' + Date.now()
    }
    await createTimeRule(payload)
    ElMessage.success('规则保存成功')
    showRuleDialog.value = false
    loadData()
  } catch (e) {
    // handled by interceptor
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.page-container {
  padding: var(--ygt-space-4);
}

.stat-row {
  margin-top: 16px;
}

.stat-card {
  text-align: center;
  border-left: 4px solid transparent;
}

.stat-normal { border-left-color: #67c23a; }
.stat-warning { border-left-color: #e6a23c; }
.stat-timeout { border-left-color: #f56c6c; }
.stat-resolved { border-left-color: #909399; }

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: var(--ygt-text-primary);
}

.stat-label {
  margin-top: 4px;
  font-size: 14px;
  color: var(--ygt-text-secondary);
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
.text-warning { color: #e6a23c; }
.text-danger { color: #f56c6c; }
</style>
