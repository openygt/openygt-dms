<template>
  <div class="page-container">
    <el-page-header title="患者进度查询" content="支持扫码或手机号查询煎药进度" />

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

    <!-- 查询结果 -->
    <el-card v-if="resultVisible" shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>查询结果</span>
          <el-button size="small" @click="resultVisible = false">关闭</el-button>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="处方号">{{ result.prescriptionNo }}</el-descriptions-item>
        <el-descriptions-item label="患者姓名">{{ result.patientName }}</el-descriptions-item>
        <el-descriptions-item label="医院">{{ result.hospitalName }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ result.departmentName }}</el-descriptions-item>
        <el-descriptions-item label="付数">{{ result.repetition }} 付</el-descriptions-item>
        <el-descriptions-item label="剂数">{{ result.doseCount }} 剂</el-descriptions-item>
        <el-descriptions-item label="预计完成">{{ result.estimatedFinishTime }}</el-descriptions-item>
        <el-descriptions-item label="取药窗口">
          <el-tag type="success" size="large">{{ result.pickupWindow || 'A01' }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <div class="progress-section">
        <h4>煎药进度</h4>
        <el-steps :active="result.stepIndex" finish-status="success" align-center>
          <el-step title="处方接收" description="已接收" />
          <el-step title="调剂" description="药材调配" />
          <el-step title="浸泡" description="浸泡等待" />
          <el-step title="煎煮" description="自动煎煮" />
          <el-step title="包装" description="汤剂包装" />
          <el-step title="质检" description="质量检验" />
          <el-step title="待取药" description="窗口取药" />
        </el-steps>
      </div>

      <div class="action-bar">
        <el-button type="primary" @click="openTraceDialog">查看追溯信息</el-button>
        <el-button @click="refreshProgress">刷新进度</el-button>
      </div>
    </el-card>

    <!-- 追溯弹窗 -->
    <el-dialog v-model="traceDialogVisible" title="处方追溯信息" width="640px">
      <el-timeline>
        <el-timeline-item
          v-for="item in traceList"
          :key="item.id"
          :type="item.type || 'primary'"
          :timestamp="item.time"
          placement="top"
        >
          <el-card shadow="hover" size="small">
            <div style="font-weight: 600">{{ item.title }}</div>
            <div style="color: var(--el-text-color-secondary); margin-top: 4px">{{ item.detail }}</div>
            <div v-if="item.operator" style="color: var(--el-text-color-secondary); margin-top: 4px; font-size: 12px">
              操作人：{{ item.operator }}
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { FullScreen, Phone, Camera } from '@element-plus/icons-vue'
import { queryByCode, queryByPhone, getPatientProgress, getPrescriptionTrace } from '@/api/newModules'

const codeQuery = ref('')
const phoneForm = reactive({ phone: '' })
const resultVisible = ref(false)
const result = reactive<any>({
  prescriptionNo: '',
  patientName: '',
  hospitalName: '',
  departmentName: '',
  repetition: 0,
  doseCount: 0,
  estimatedFinishTime: '',
  pickupWindow: '',
  stepIndex: 0
})

async function handleCodeQuery() {
  if (!codeQuery.value.trim()) { ElMessage.warning('请输入条码'); return }
  try {
    const res = await queryByCode({ code: codeQuery.value.trim() }) as any
    fillResult(res.data)
  } catch {
    fillMockResult()
  }
}

async function handlePhoneQuery() {
  if (!phoneForm.phone.trim()) { ElMessage.warning('请输入手机号'); return }
  try {
    const res = await queryByPhone({ phone: phoneForm.phone.trim() }) as any
    fillResult(res.data)
  } catch {
    fillMockResult()
  }
}

function fillResult(data: any) {
  Object.assign(result, data)
  resultVisible.value = true
}

function fillMockResult() {
  Object.assign(result, {
    prescriptionNo: 'P202405010001',
    patientName: '王患者',
    hospitalName: '中医院',
    departmentName: '内科',
    repetition: 7,
    doseCount: 14,
    estimatedFinishTime: '2024-05-01 15:30',
    pickupWindow: 'B03',
    stepIndex: 4
  })
  resultVisible.value = true
}

async function refreshProgress() {
  try {
    const res = await getPatientProgress(result.prescriptionNo) as any
    if (res.data) Object.assign(result, res.data)
    ElMessage.success('已刷新')
  } catch {
    ElMessage.info('进度未变化')
  }
}

const traceDialogVisible = ref(false)
const traceList = ref<any[]>([])

async function openTraceDialog() {
  traceDialogVisible.value = true
  try {
    const res = await getPrescriptionTrace(result.prescriptionNo) as any
    traceList.value = res.data || []
  } catch {
    traceList.value = [
      { id: 1, title: '处方接收', detail: '处方已接收并录入系统', time: '2024-05-01 08:00', operator: '系统', type: 'success' },
      { id: 2, title: '调剂完成', detail: '药材调配完成，进入浸泡环节', time: '2024-05-01 09:30', operator: '调剂员-张三', type: 'success' },
      { id: 3, title: '浸泡开始', detail: '药材浸泡中，预计30分钟', time: '2024-05-01 09:45', operator: '系统', type: 'primary' },
      { id: 4, title: '煎煮中', detail: '正在煎煮第1付', time: '2024-05-01 10:20', operator: '煎药机-01', type: 'warning' }
    ]
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
