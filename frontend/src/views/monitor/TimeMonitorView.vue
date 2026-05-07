<template>
  <div class="page-container">

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card stat-normal" shadow="hover" :class="{ active: currentCategory === 'normal' }" @click="switchCategory('normal')">
          <div class="stat-value">{{ stat.normal }}</div>
          <div class="stat-label">正常任务</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-warning" shadow="hover" :class="{ active: currentCategory === 'warning' }" @click="switchCategory('warning')">
          <div class="stat-value">{{ stat.warning }}</div>
          <div class="stat-label">预警任务</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-timeout" shadow="hover" :class="{ active: currentCategory === 'timeout' }" @click="switchCategory('timeout')">
          <div class="stat-value">{{ stat.timeout }}</div>
          <div class="stat-label">超时任务</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-resolved" shadow="hover" :class="{ active: currentCategory === 'resolved' }" @click="switchCategory('resolved')">
          <div class="stat-value">{{ stat.resolved }}</div>
          <div class="stat-label">已处理预警</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 监控明细 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <span>{{ categoryTitle }}</span>
          <div class="table-actions">
            <el-button v-if="currentCategory !== 'all'" @click="switchCategory('all')">查看全部</el-button>
            <el-button type="primary" @click="openRuleDialog()">新增规则</el-button>
            <el-button @click="loadData">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="monitorItems" stripe>
        <el-table-column prop="taskId" label="任务ID" width="100" />
        <el-table-column label="阶段" width="100">
          <template #default="{ row }">{{ stageLabel(row.stage) }}</template>
        </el-table-column>
        <el-table-column label="计划时间" min-width="240">
          <template #default="{ row }">
            {{ formatDateTime(row.plannedStart) }} ~ {{ formatDateTime(row.plannedEnd) }}
          </template>
        </el-table-column>
        <el-table-column prop="remainingSeconds" label="剩余(秒)" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="monitorStatusTag(row.status)">{{ monitorStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="warningCount" label="预警次数" width="100" />
      </el-table>
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <span>活动预警</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="taskId" label="任务ID" width="120" />
        <el-table-column prop="stageName" label="阶段" width="160" />
        <el-table-column prop="alertType" label="类型" width="120" />
        <el-table-column prop="alertContent" label="内容" min-width="220" />
        <el-table-column prop="createdAt" label="创建时间" min-width="170" />
        <el-table-column prop="statusLabel" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.statusTag">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.resolved" link type="primary" @click="handleResolve(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="rule-list">
        <div class="rule-title">规则列表</div>
        <el-table :data="ruleList" size="small" border>
          <el-table-column prop="ruleCode" label="规则编码" width="140" />
          <el-table-column prop="ruleName" label="规则名称" min-width="160" />
          <el-table-column prop="stageLabel" label="阶段" width="160" />
          <el-table-column prop="prescriptionTypeLabel" label="处方类型" width="120" />
          <el-table-column prop="standardDuration" label="标准时长" width="110" />
          <el-table-column prop="warningThreshold" label="预警阈值" width="110" />
          <el-table-column prop="alertThreshold" label="超时阈值" width="110" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRuleDialog(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog v-model="showRuleDialog" :title="ruleForm.id ? '编辑规则' : '新增规则'" width="600px">
      <el-form :model="ruleForm" label-width="120px">
        <el-form-item label="规则编码"><el-input v-model="ruleForm.ruleCode" placeholder="如 NORMAL_SOAK" /></el-form-item>
        <el-form-item label="规则名称"><el-input v-model="ruleForm.ruleName" placeholder="请输入规则名称" /></el-form-item>
        <el-form-item label="处方类型">
          <el-select v-model="ruleForm.prescriptionType" style="width: 100%">
            <el-option v-for="item in prescriptionTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用阶段">
          <el-select v-model="ruleForm.stage" style="width: 100%">
            <el-option v-for="item in stageOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标准时长(分钟)"><el-input-number v-model="ruleForm.standardDuration" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="预警阈值(分钟)"><el-input-number v-model="ruleForm.warningThreshold" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="超时阈值(分钟)"><el-input-number v-model="ruleForm.alertThreshold" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="严重阈值(分钟)"><el-input-number v-model="ruleForm.criticalThreshold" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="默认规则">
          <div class="default-rule-row">
            <el-switch v-model="ruleForm.isDefault" :active-value="1" :inactive-value="0" />
            <span class="default-rule-tip">同一处方类型 + 同一阶段下，仅允许一条默认规则，未命中专属规则时兜底使用。</span>
          </div>
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
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createTimeRule, getActiveAlerts, getTimeMonitorDashboard, getTimeMonitorList, getTimeRules, resolveAlert, updateTimeRule } from '@/api/newModules'

const loading = ref(false)
const tableData = ref<any[]>([])
const ruleList = ref<any[]>([])
const monitorItems = ref<any[]>([])
const stat = reactive({ normal: 0, warning: 0, timeout: 0, resolved: 0 })
const showRuleDialog = ref(false)
const currentCategory = ref('all')

const pagination = ref({ page: 1, size: 20, total: 0 })

const categoryTitle = computed(() => {
  const map: Record<string, string> = {
    all: '监控明细（全部）',
    normal: '监控明细（正常任务）',
    warning: '监控明细（预警任务）',
    timeout: '监控明细（超时任务）',
    resolved: '监控明细（已处理预警）'
  }
  return map[currentCategory.value] || '监控明细'
})

const prescriptionTypeOptions = [
  { label: '普通', value: 'NORMAL' },
  { label: '普通急诊', value: 'EMERGENCY' },
  { label: '危重急诊', value: 'CRITICAL_EMERGENCY' }
]

const stageOptions = [
  { label: '泡药', value: 'SOAK' },
  { label: '一煎', value: 'FIRST_DECOCTION' },
  { label: '二煎', value: 'SECOND_DECOCTION' },
  { label: '煎药', value: 'DECOCT' },
  { label: '包装', value: 'WRAP' }
]

const ruleForm = reactive({
  id: null as number | null,
  ruleCode: '',
  ruleName: '',
  prescriptionType: 'NORMAL',
  stage: 'SOAK',
  standardDuration: 30,
  warningThreshold: 5,
  alertThreshold: 10,
  criticalThreshold: 20,
  isDefault: 1
})

function formatDateTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

function stageLabel(stage?: string) {
  const map: Record<string, string> = {
    SOAK: '泡药',
    FIRST_DECOCTION: '一煎',
    SECOND_DECOCTION: '二煎',
    PACKING: '包装',
    WRAP: '包装',
    DECOCT: '煎药'
  }
  return map[stage || ''] || stage || '-'
}

function prescriptionTypeLabel(type?: string) {
  const map: Record<string, string> = {
    NORMAL: '普通',
    EMERGENCY: '普通急诊',
    CRITICAL_EMERGENCY: '危重急诊'
  }
  return map[type || ''] || type || '-'
}

function monitorStatusLabel(status?: number) {
  if (status == null) return '未知'
  if (status === 1) return '计划中'
  if (status === 2) return '运行中'
  if (status === 3) return '已超时'
  return `状态${status}`
}

function monitorStatusTag(status?: number) {
  if (status == null) return 'info'
  if (status === 1) return 'success'
  if (status === 2) return 'warning'
  if (status === 3) return 'danger'
  return 'info'
}

function openRuleDialog(rule?: any) {
  Object.assign(ruleForm, {
    id: rule?.id ?? null,
    ruleCode: rule?.ruleCode ?? '',
    ruleName: rule?.ruleName ?? '',
    prescriptionType: rule?.prescriptionType ?? 'NORMAL',
    stage: rule?.stage ?? 'SOAK',
    standardDuration: rule?.standardDuration ?? 30,
    warningThreshold: rule?.warningThreshold ?? 5,
    alertThreshold: rule?.alertThreshold ?? 10,
    criticalThreshold: rule?.criticalThreshold ?? 20,
    isDefault: rule?.isDefault ?? 1
  })
  showRuleDialog.value = true
}

function switchCategory(category: string) {
  currentCategory.value = category
  pagination.value.page = 1
  loadData()
}

function handleSizeChange(val: number) {
  pagination.value.size = val
  pagination.value.page = 1
  loadData()
}

function handlePageChange(val: number) {
  pagination.value.page = val
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const [dashboardRes, alertsRes, rulesRes, listRes]: any = await Promise.all([
      getTimeMonitorDashboard(),
      getActiveAlerts(),
      getTimeRules(),
      getTimeMonitorList({
        category: currentCategory.value,
        page: pagination.value.page,
        size: pagination.value.size
      })
    ])

    const dashboard = dashboardRes.data || {}
    const alerts = alertsRes.data?.records || []

    stat.normal = dashboard.onTimeTasks || 0
    stat.warning = dashboard.warningTasks || 0
    stat.timeout = dashboard.alertTasks || 0
    stat.resolved = dashboard.resolvedTasks || 0

    monitorItems.value = listRes.data?.records || []
    pagination.value.total = listRes.data?.total || 0

    tableData.value = alerts.map((item: any) => ({
      id: item.id,
      taskId: item.taskId || '-',
      stageName: stageLabel(item.stage),
      alertType: alertTypeLabel(item.alertType),
      alertContent: item.alertContent || '-',
      createdAt: formatDateTime(item.createdAt),
      resolved: Number(item.isResolved || 0) === 1,
      statusLabel: Number(item.isResolved || 0) === 1 ? '已处理' : '待处理',
      statusTag: Number(item.isResolved || 0) === 1 ? 'success' : 'warning'
    }))

    ruleList.value = (rulesRes.data || []).map((rule: any) => ({
      ...rule,
      stageLabel: stageLabel(rule.stage),
      prescriptionTypeLabel: prescriptionTypeLabel(rule.prescriptionType)
    }))
  } catch (err: any) {
    ElMessage.error(err?.message || '数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function handleResolve(row: any) {
  await resolveAlert(row.id)
  ElMessage.success('预警处理成功')
  await loadData()
}

async function handleSaveRule() {
  if (!ruleForm.ruleName.trim()) {
    ElMessage.warning('请输入规则名称')
    return
  }
  if (ruleForm.alertThreshold < ruleForm.warningThreshold) {
    ElMessage.warning('超时阈值不能小于预警阈值')
    return
  }
  if (ruleForm.criticalThreshold < ruleForm.alertThreshold) {
    ElMessage.warning('严重阈值不能小于超时阈值')
    return
  }

  const payload = {
    ruleCode: ruleForm.ruleCode || `${ruleForm.prescriptionType}_${ruleForm.stage}`,
    ruleName: ruleForm.ruleName,
    prescriptionType: ruleForm.prescriptionType,
    stage: ruleForm.stage,
    standardDuration: ruleForm.standardDuration,
    warningThreshold: ruleForm.warningThreshold,
    alertThreshold: ruleForm.alertThreshold,
    criticalThreshold: ruleForm.criticalThreshold,
    isDefault: ruleForm.isDefault
  }

  if (ruleForm.id) {
    await updateTimeRule(ruleForm.id, payload)
  } else {
    await createTimeRule(payload)
  }
  ElMessage.success('规则保存成功')
  showRuleDialog.value = false
  await loadData()
}

function alertTypeLabel(type?: string) {
  const map: Record<string, string> = {
    TIMEOUT: '超时',
    APPROACHING: '即将超时'
  }
  return map[type || ''] || type || '-'
}

let refreshTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  loadData()
  refreshTimer = setInterval(() => loadData(), 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<style scoped lang="scss">
.page-container { padding: var(--ygt-space-4); }
.stat-row { margin-top: 16px; }
.stat-card { text-align: center; border-left: 4px solid transparent; cursor: pointer; transition: all 0.2s; }
.stat-card:hover { transform: translateY(-2px); }
.stat-card.active { box-shadow: 0 0 0 2px var(--el-color-primary); }
.stat-normal { border-left-color: #67c23a; }
.stat-warning { border-left-color: #e6a23c; }
.stat-timeout { border-left-color: #f56c6c; }
.stat-resolved { border-left-color: #909399; }
.stat-value { font-size: 32px; font-weight: bold; color: var(--ygt-text-primary); }
.stat-label { margin-top: 4px; font-size: 14px; color: var(--ygt-text-secondary); }
.table-card { margin-top: 16px; }
.table-header { display: flex; justify-content: space-between; align-items: center; }
.table-actions { display: flex; gap: 12px; }
.rule-list { margin-top: 16px; }
.rule-title { margin-bottom: 8px; font-weight: 600; }
.default-rule-row { display: flex; align-items: center; gap: 12px; }
.default-rule-tip { color: var(--el-text-color-secondary); line-height: 1.4; }
</style>
