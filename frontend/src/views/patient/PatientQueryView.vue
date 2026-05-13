<template>
  <div class="page-container">
    <!-- 查询区域 -->
    <el-card shadow="never">
      <el-row :gutter="24">
        <el-col :xs="24" :md="12">
          <div class="query-section">
            <h3><el-icon><FullScreen /></el-icon> 扫码查询</h3>
            <el-input
              v-model="codeQuery"
              placeholder="请扫描处方码或药袋条码"
              size="large"
              clearable
              @keyup.enter="handleCodeQuery"
            >
              <template #prefix>
                <el-icon><Camera /></el-icon>
              </template>
              <template #append>
                <el-button type="primary" @click="handleCodeQuery">查询</el-button>
              </template>
            </el-input>
            <p class="query-tip">支持处方二维码、药袋条码、取药码</p>
          </div>
        </el-col>
        <el-col :xs="24" :md="12">
          <div class="query-section">
            <h3><el-icon><Phone /></el-icon> 手机号查询</h3>
            <el-form :model="phoneForm" inline @submit.prevent>
              <el-form-item style="flex: 1">
                <el-input
                  v-model="phoneForm.phone"
                  placeholder="请输入患者手机号"
                  size="large"
                  maxlength="11"
                  clearable
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="large" @click="handlePhoneQuery">查询</el-button>
              </el-form-item>
            </el-form>
            <p class="query-tip">请输入处方登记时填写的手机号</p>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 扫码查询结果 -->
    <el-card v-if="resultVisible && queryType === 'code'" shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>查询结果</span>
          <el-button size="small" @click="resultVisible = false">关闭</el-button>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="患者姓名">{{ result.patientName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ result.patientPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">{{ result.currentStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="进度">{{ result.progressPercent != null ? result.progressPercent + '%' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="预计完成">{{ result.estimatedFinishTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="progress-section">
        <h4>煎药进度</h4>
        <el-steps :active="activeStep" finish-status="success" align-center>
          <el-step v-for="(step, idx) in stepList" :key="idx" :title="step.stepName" :description="step.status" />
        </el-steps>
      </div>

      <div class="action-bar">
        <el-button type="primary" @click="openTraceDialog">查看追溯信息</el-button>
        <el-button @click="refreshProgress">刷新进度</el-button>
      </div>
    </el-card>

    <!-- 手机号查询结果（处方列表） -->
    <el-card v-if="resultVisible && queryType === 'phone'" shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>处方列表</span>
          <el-button size="small" @click="resultVisible = false">关闭</el-button>
        </div>
      </template>
      <el-table :data="prescriptionList" border @row-click="handleSelectPrescription">
        <el-table-column prop="prescriptionNumber" label="处方号" min-width="140" />
        <el-table-column prop="patientName" label="患者姓名" min-width="100" />
        <el-table-column prop="department" label="科室" min-width="100" />
        <el-table-column prop="repetition" label="付数" width="80" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="handleSelectPrescription(row)">查看追溯</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 追溯弹窗 -->
    <el-dialog v-model="traceDialogVisible" title="处方追溯信息" width="640px">
      <el-timeline v-if="traceList.length">
        <el-timeline-item
          v-for="(item, idx) in traceList"
          :key="idx"
          :type="idx === traceList.length - 1 ? 'success' : 'primary'"
          :timestamp="formatTraceTime(item.operateTime)"
          placement="top"
        >
          <el-card shadow="hover" size="small">
            <div style="font-weight: 600">{{ stageLabel(item.stage) }}</div>
            <div style="color: var(--el-text-color-secondary); margin-top: 4px">{{ item.remark || '-' }}</div>
            <div v-if="item.operatorName" style="color: var(--el-text-color-secondary); margin-top: 4px; font-size: 12px">
              操作人：{{ item.operatorName }}
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无追溯记录" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { FullScreen, Phone, Camera } from '@element-plus/icons-vue'
import { queryByCode, queryByPhone, getPatientProgress, getPrescriptionTrace } from '@/api/newModules'

const codeQuery = ref('')
const phoneForm = reactive({ phone: '13800000001' })
const resultVisible = ref(false)
const queryType = ref<'code' | 'phone'>('code')

// 扫码查询保存的 token 和处方ID
const currentToken = ref('')
const currentPrescriptionId = ref<number | string>('')

const result = reactive<any>({
  patientName: '',
  patientPhone: '',
  currentStatus: '',
  progressPercent: 0,
  estimatedFinishTime: '',
  steps: []
})

const prescriptionList = ref<any[]>([])
const stepList = computed(() => result.steps || [])
const activeStep = computed(() => {
  const steps = result.steps || []
  // 找到最后一个"已完成"的索引+1
  let active = 0
  for (let i = 0; i < steps.length; i++) {
    if (steps[i].status === 'COMPLETED') {
      active = i + 1
    } else if (steps[i].status === '进行中') {
      active = i
      break
    }
  }
  return active
})

const traceDialogVisible = ref(false)
const traceList = ref<any[]>([])

function formatTraceTime(time?: string) {
  if (!time) return '--'
  return time.replace('T', ' ').substring(0, 19)
}

function stageLabel(stage?: string) {
  const map: Record<string, string> = {
    WAIT_SOAK: '待浸泡', SOAKING: '浸泡中',
    WAIT_DECOCT: '待煎煮', DECOCTING: '煎煮中',
    FIRST_DECOCTING: '一煎中', SECOND_DECOCTING: '二煎中',
    WAIT_POUR: '待出液', POURING: '出液中',
    WAIT_WRAP: '待包装', WRAPPING: '包装中',
    PACKAGING: '包装中',
    WAIT_LABEL: '待贴标',
    WAIT_QC: '待质检',
    WAIT_HANDOVER: '待交接',
    COMPLETED: '已完成', PARTIAL_COMPLETED: '部分完成',
    RECEIVED: '已接方', AUDIT_PASS: '审方通过',
    DISPENSED: '调剂完成', REVIEWED: '复核通过',
    PENDING: '待处理', ABNORMAL: '异常'
  }
  return map[stage || ''] || stage || '--'
}

async function handleCodeQuery() {
  if (!codeQuery.value.trim()) { ElMessage.warning('请输入条码'); return }
  try {
    const res = await queryByCode({ code: codeQuery.value.trim() }) as any
    const tokenData = res.data
    if (!tokenData || !tokenData.token) {
      ElMessage.warning('未找到查询码对应的记录')
      return
    }
    currentToken.value = tokenData.token
    currentPrescriptionId.value = tokenData.prescriptionId || ''

    // 用 token 查进度
    const progressRes = await getPatientProgress(tokenData.token) as any
    const data = progressRes.data || {}
    Object.assign(result, data)
    queryType.value = 'code'
    resultVisible.value = true
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '查询失败'
    ElMessage.error(msg)
    resultVisible.value = false
  }
}

async function handlePhoneQuery() {
  if (!phoneForm.phone.trim()) { ElMessage.warning('请输入手机号'); return }
  try {
    const res = await queryByPhone({ phone: phoneForm.phone.trim() }) as any
    const list = res.data || []
    if (!list.length) {
      ElMessage.warning('未找到该手机号关联的处方')
      return
    }
    prescriptionList.value = list
    queryType.value = 'phone'
    resultVisible.value = true
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '查询失败'
    ElMessage.error(msg)
    resultVisible.value = false
  }
}

async function handleSelectPrescription(row: any) {
  if (!row.id) return
  currentPrescriptionId.value = row.id
  currentToken.value = ''
  // 显示追溯
  traceDialogVisible.value = true
  traceList.value = []
  try {
    const res = await getPrescriptionTrace(row.id) as any
    traceList.value = res.data?.traces || []
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '查询追溯失败'
    ElMessage.error(msg)
  }
}

async function refreshProgress() {
  if (!currentToken.value) {
    ElMessage.warning('暂无查询令牌，请先扫码查询')
    return
  }
  try {
    const res = await getPatientProgress(currentToken.value) as any
    if (res.data) Object.assign(result, res.data)
    ElMessage.success('已刷新')
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '刷新失败'
    ElMessage.error(msg)
  }
}

async function openTraceDialog() {
  if (!currentPrescriptionId.value) {
    ElMessage.warning('暂无处方信息')
    return
  }
  traceDialogVisible.value = true
  traceList.value = []
  try {
    const res = await getPrescriptionTrace(currentPrescriptionId.value) as any
    traceList.value = res.data?.traces || []
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '查询追溯失败'
    ElMessage.error(msg)
  }
}
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.query-section {
  padding: 16px;
  background: var(--el-fill-color-light);
  border-radius: var(--el-border-radius-base);
}
.query-section h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: var(--el-text-color-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}
.query-tip {
  margin: 8px 0 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.progress-section {
  margin-top: 24px;
}
.progress-section h4 {
  margin-bottom: 16px;
  font-size: 16px;
}
.action-bar {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  justify-content: center;
}
@media (max-width: 768px) {
  .query-section {
    margin-bottom: 16px;
  }
}
</style>
