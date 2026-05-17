<template>
  <div class="app-container" v-loading="loading">
    <!-- 设备列表 -->
    <el-card v-for="d in devices" :key="d.id" class="device-card">
      <template #header>
        <div class="device-header">
          <div class="device-info">
            <span class="device-name">{{ d.name }}</span>
            <el-tag size="small" :type="statusTag(d.status)">{{ statusText(d.status) }}</el-tag>
            <span class="device-code">{{ d.deviceCode }}</span>
          </div>
          <div class="header-actions">
            <el-button size="small" type="success" @click="powerOn(d)" :disabled="d.status === 'IDLE' || d.status === 'BUSY'">开机</el-button>
            <el-button size="small" type="info" @click="powerOff(d)" :disabled="d.status === 'OFFLINE'">关机</el-button>
          </div>
        </div>
      </template>
      <div class="device-body">
        <div class="info-row">
          <span>厂商：{{ d.manufacturer || '-' }}</span>
          <span>型号：{{ d.modelNum || '-' }}</span>
          <span>通信ID：{{ d.communicationId || '-' }}</span>
        </div>
      </div>
    </el-card>
    <el-empty v-if="devices.length === 0" description="暂无 PDA 设备" />

    <!-- 工序流程面板 -->
    <el-card v-if="devices.length > 0" style="margin-top: 20px">
      <template #header>
        <span style="font-weight: 600">PDA 全工序流程模拟</span>
      </template>

      <div class="workflow">
        <!-- 操作员 & 任务选择 -->
        <div class="wf-row">
          <el-form-item label="操作员工号" style="margin-bottom: 0">
            <el-input v-model="operatorId" placeholder="OP001" style="width: 160px" />
          </el-form-item>
          <el-form-item label="处方任务" style="margin-bottom: 0">
            <el-select v-model="selectedTask" placeholder="选择处方" style="width: 220px" clearable>
              <el-option v-for="t in tasks" :key="t.id" :label="`${t.prescriptionNo || t.id} - ${t.patientName || ''}`" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="选择煎药机" style="margin-bottom: 0">
            <el-select v-model="selectedDecoct" placeholder="选择设备" style="width: 180px" clearable>
              <el-option v-for="d in decoctDevices" :key="d.id" :label="d.name" :value="d" />
            </el-select>
          </el-form-item>
          <el-form-item label="选择包装机" style="margin-bottom: 0">
            <el-select v-model="selectedPack" placeholder="选择设备" style="width: 180px" clearable>
              <el-option v-for="d in packDevices" :key="d.id" :label="d.name" :value="d" />
            </el-select>
          </el-form-item>
        </div>

        <el-divider />

        <!-- 工序步骤 -->
        <div class="wf-steps">
          <div class="wf-step" v-for="(step, idx) in steps" :key="idx" :class="{ active: currentStep === idx }">
            <div class="step-header">
              <span class="step-num">{{ idx + 1 }}</span>
              <span class="step-name">{{ step.label }}</span>
              <el-tag v-if="currentStep === idx" size="small" type="warning">当前</el-tag>
            </div>
            <div class="step-actions">
              <el-button v-if="step.startAction" size="small" type="success" @click="doStep(step.startAction)" :disabled="currentStep !== idx">{{ step.startLabel }}</el-button>
              <el-button v-if="step.endAction" size="small" type="danger" @click="doStep(step.endAction)" :disabled="currentStep !== idx">结束</el-button>
            </div>
            <div v-if="step.extraActions" class="step-extra">
              <el-button
                v-for="ea in step.extraActions"
                :key="ea.action"
                size="small"
                :type="ea.type"
                @click="doStep(ea.action)"
                :disabled="currentStep !== idx"
              >{{ ea.label }}</el-button>
            </div>
          </div>
        </div>

        <!-- 进度条 -->
        <el-steps :active="currentStep" finish-status="success" align-center style="margin-top: 20px">
          <el-step v-for="s in steps" :key="s.label" :title="s.label" />
        </el-steps>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useDeviceStore, type DeviceState } from '@/stores/device'

const deviceStore = useDeviceStore()

const loading = ref(false)
const decoctDevices = ref<DeviceState[]>([])
const packDevices = ref<DeviceState[]>([])
const tasks = ref<any[]>([])
const operatorId = ref('OP001')
const selectedTask = ref<any>(null)
const selectedDecoct = ref<DeviceState | null>(null)
const selectedPack = ref<DeviceState | null>(null)
const currentStep = ref(0)

const devices = computed(() =>
  deviceStore.deviceList.filter(d => d.deviceType === 4)
)

const steps = [
  { label: '泡药', startLabel: '开始泡药', startAction: 'soakStart', endAction: 'soakEnd' },
  { label: '煎药', startLabel: '开始煎药', startAction: 'decoctStart', endAction: 'decoctEnd' },
  { label: '出液', startLabel: '开始出液', startAction: 'pourStart', endAction: 'pourEnd' },
  { label: '包装', startLabel: '开始包装', startAction: 'wrapStart', endAction: 'wrapEnd' },
  { label: '贴标', startLabel: '确认贴标', startAction: 'labelConfirm', endAction: null },
  {
    label: '质检', startLabel: '质检',
    startAction: null, endAction: null,
    extraActions: [
      { label: '通过', action: 'qualityPass', type: 'success' },
      { label: '让步', action: 'qualityConcession', type: 'warning' },
      { label: '返工', action: 'qualityRework', type: 'danger' },
      { label: '报废', action: 'qualityScrap', type: 'danger' },
    ]
  },
  {
    label: '交接', startLabel: '交接',
    startAction: null, endAction: null,
    extraActions: [
      { label: '部分交接', action: 'handoverPart', type: 'primary' },
      { label: '完成交接', action: 'handoverFull', type: 'success' },
    ]
  },
]

async function fetchDevices() {
  loading.value = true
  try {
    const [pdaRes, decoctRes, packRes] = await Promise.all([
      request.get('/v1/eq/devices', { params: { deviceType: 4, size: 50 } }),
      request.get('/v1/eq/devices', { params: { deviceType: 1, size: 50 } }),
      request.get('/v1/eq/devices', { params: { deviceType: 2, size: 50 } }),
    ])
    const pdaRecords = (pdaRes as any).data?.records || []
    pdaRecords.forEach((d: any) => {
      deviceStore.updateDevice(d.deviceCode, {
        deviceCode: d.deviceCode, name: d.name, status: d.status || 'IDLE',
        detailStatus: d.detailStatus || d.status || 'IDLE', deviceType: 4,
        manufacturer: d.manufacturer, modelNum: d.modelNum,
        communicationId: d.communicationId, lastHeartbeat: d.lastHeartbeat,
      })
    })
    const decoctRecords = (decoctRes as any).data?.records || []
    decoctRecords.forEach((d: any) => {
      deviceStore.updateDevice(d.deviceCode, {
        deviceCode: d.deviceCode, name: d.name, status: d.status || 'IDLE',
        detailStatus: d.detailStatus || d.status || 'IDLE', deviceType: 1,
        manufacturer: d.manufacturer, modelNum: d.modelNum,
        currentTemp: d.currentTemp, targetTemp: d.targetTemp,
        currentPrescriptionCode: d.currentPrescriptionCode,
        lastHeartbeat: d.lastHeartbeat,
      })
    })
    const packRecords = (packRes as any).data?.records || []
    packRecords.forEach((d: any) => {
      deviceStore.updateDevice(d.deviceCode, {
        deviceCode: d.deviceCode, name: d.name, status: d.status || 'IDLE',
        detailStatus: d.detailStatus || d.status || 'IDLE', deviceType: 2,
        manufacturer: d.manufacturer, modelNum: d.modelNum,
        packageCapacity: d.packageCapacity, packageNum: d.packageNum,
        lastHeartbeat: d.lastHeartbeat,
      })
    })
    // Keep local refs for dropdown selectors
    decoctDevices.value = decoctRecords
    packDevices.value = packRecords
  } catch (e) { ElMessage.error('获取设备列表失败') }
  finally { loading.value = false }
}

async function fetchTasks() {
  try {
    const res: any = await request.get('/v1/prod/tasks', { params: { size: 100 } })
    tasks.value = res.data?.records || []
  } catch (e) { /* ignore */ }
}

async function powerOn(d: DeviceState) {
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'IDLE' })
  deviceStore.updateDevice(d.deviceCode, { status: 'IDLE', detailStatus: 'IDLE' })
  ElMessage.success(`${d.name} 已开机`)
}
async function powerOff(d: DeviceState) {
  await request.put(`/v1/eq/devices/${d.id}`, { ...d, status: 'OFFLINE' })
  deviceStore.updateDevice(d.deviceCode, { status: 'OFFLINE', detailStatus: 'OFFLINE' })
  ElMessage.success(`${d.name} 已关机`)
}

async function doStep(action: string | null) {
  if (!action) return

  const task = selectedTask.value
  const decoct = selectedDecoct.value
  const pack = selectedPack.value

  if (!task) { ElMessage.warning('请先选择处方任务'); return }

  const payload: any = { operatorId: operatorId.value, taskId: task.id || task.prescriptionNo }

  if (action.startsWith('decoct') || action.startsWith('pour')) {
    if (!decoct) { ElMessage.warning('请选择煎药机'); return }
    payload.deviceCode = decoct.deviceCode
  }
  if (action.startsWith('wrap')) {
    if (!pack) { ElMessage.warning('请选择包装机'); return }
    payload.deviceCode = pack.deviceCode
  }

  try {
    await request.post('/v1/eq/commands', {
      deviceCode: payload.deviceCode || 'PDA',
      commandType: action.toUpperCase(),
      payload: JSON.stringify(payload)
    })
    ElMessage.success(`工序执行成功：${action}`)

    // Update store for linked devices
    if (action.startsWith('decoct') && decoct) {
      if (action === 'decoctStart') {
        deviceStore.updateDevice(decoct.deviceCode, { status: 'BUSY', detailStatus: 'BUSY' })
      } else if (action === 'decoctEnd') {
        deviceStore.updateDevice(decoct.deviceCode, { status: 'IDLE', detailStatus: 'IDLE' })
      }
    }
    if (action.startsWith('wrap') && pack) {
      if (action === 'wrapStart') {
        deviceStore.updateDevice(pack.deviceCode, { status: 'BUSY', detailStatus: 'BUSY' })
      } else if (action === 'wrapEnd') {
        deviceStore.updateDevice(pack.deviceCode, { status: 'IDLE', detailStatus: 'IDLE' })
      }
    }

    // Auto-advance step for end/completion actions
    if (action === 'soakEnd' || action === 'decoctEnd' || action === 'pourEnd' ||
        action === 'wrapEnd' || action === 'labelConfirm' ||
        action.startsWith('quality') || action.startsWith('handover')) {
      if (currentStep.value < steps.length - 1) {
        currentStep.value++
      }
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '工序执行失败')
  }
}

function statusTag(s: string) {
  if (s === 'IDLE' || s === 'ONLINE') return 'success'
  if (s === 'OFFLINE') return 'info'
  if (s === 'BUSY' || s === 'RUNNING') return 'warning'
  return 'danger'
}
function statusText(s: string) {
  const map: Record<string, string> = { IDLE: '空闲', ONLINE: '在线', OFFLINE: '离线', BUSY: '运行中', FAULT: '故障' }
  return map[s] || s
}

onMounted(() => { fetchDevices(); fetchTasks() })
</script>

<style scoped>
.device-card { margin-bottom: 16px; }
.device-header { display: flex; justify-content: space-between; align-items: center; }
.device-info { display: flex; align-items: center; gap: 10px; }
.device-name { font-weight: 600; font-size: 16px; }
.device-code { font-size: 13px; color: var(--el-text-color-secondary); }
.header-actions { display: flex; gap: 8px; }
.device-body { display: flex; flex-direction: column; gap: 8px; }
.info-row { display: flex; flex-wrap: wrap; gap: 24px; color: var(--el-text-color-regular); font-size: 14px; }

.workflow { }
.wf-row {
  display: flex; flex-wrap: wrap; gap: 16px; align-items: flex-start;
}
.wf-steps {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}
.wf-step {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 12px;
  background: var(--el-fill-color-light);
}
.wf-step.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.step-header {
  display: flex; align-items: center; gap: 8px; margin-bottom: 8px;
}
.step-num {
  display: inline-flex; align-items: center; justify-content: center;
  width: 24px; height: 24px; border-radius: 50%;
  background: var(--el-color-primary); color: #fff;
  font-size: 12px; font-weight: 600;
}
.step-name { font-weight: 600; font-size: 14px; }
.step-actions { display: flex; gap: 6px; margin-bottom: 6px; }
.step-extra { display: flex; flex-wrap: wrap; gap: 4px; }
</style>
