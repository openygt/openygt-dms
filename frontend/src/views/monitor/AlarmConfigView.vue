<template>
  <div class="alarm-config">
    <div class="page-header">

      <el-button type="primary" @click="handleAdd">新增配置</el-button>
    </div>

    <el-card>
      <el-table :data="configList" v-loading="loading">
        <el-table-column label="告警类型" width="140">
          <template #default="{ row }">{{ alarmTypeText(row.alarmType) }}</template>
        </el-table-column>
        <el-table-column label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="alarmLevelTag(row.alarmLevel)">{{ alarmLevelText(row.alarmLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="thresholdValue" label="阈值" width="80" />
        <el-table-column prop="durationSeconds" label="持续时间(秒)" width="110" />
        <el-table-column label="通知方式" width="170">
          <template #default="{ row }">{{ notifyTypeText(row.notifyType) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="(v: boolean) => handleEnableChange(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" :data-testid="`edit-alarm-config-${row.id}`" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" :data-testid="`delete-alarm-config-${row.id}`" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showDialog" :title="isEdit ? '编辑告警配置' : '新增告警配置'" width="600px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <el-form-item label="告警类型">
          <el-select v-model="form.alarmType">
            <el-option label="温度超高" value="TEMP_OVER_105" />
            <el-option label="温度偏高" value="TEMP_OVER_100" />
            <el-option label="温度异常" value="TEMP_ABNORMAL" />
            <el-option label="煎药超时" value="DECOCT_TIMEOUT" />
            <el-option label="设备离线" value="DEVICE_OFFLINE" />
            <el-option label="包装机卡袋" value="PACKER_JAM" />
            <el-option label="标签缺纸" value="LABEL_LOW" />
            <el-option label="急停触发" value="EMERGENCY_STOP" />
          </el-select>
        </el-form-item>
        <el-form-item label="告警级别">
          <el-radio-group v-model="form.alarmLevel">
            <el-radio-button label="INFO">一般</el-radio-button>
            <el-radio-button label="WARNING">警告</el-radio-button>
            <el-radio-button label="CRITICAL">严重</el-radio-button>
            <el-radio-button label="URGENT">紧急</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="阈值">
          <el-input-number v-model="form.thresholdValue" :precision="2" />
        </el-form-item>
        <el-form-item label="持续时间(秒)">
          <el-input-number v-model="form.durationSeconds" :min="0" />
        </el-form-item>
        <el-form-item label="通知方式">
          <el-checkbox-group v-model="notifyTypes">
            <el-checkbox label="POPUP">弹窗</el-checkbox>
            <el-checkbox label="VOICE">语音</el-checkbox>
            <el-checkbox label="SMS">短信</el-checkbox>
            <el-checkbox label="PHONE">电话</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAlarmConfigs, createAlarmConfig, updateAlarmConfig, deleteAlarmConfig } from '@/api/equipment'

const loading = ref(false)
const configList = ref<any[]>([])
const showDialog = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)
const notifyTypes = ref<string[]>([])

const form = reactive({
  alarmType: 'TEMP_HIGH',
  alarmLevel: 'WARNING',
  thresholdValue: 110,
  durationSeconds: 10,
  notifyType: ''
})

function alarmTypeText(type: string) {
  const map: Record<string, string> = {
    TEMP_OVER_105: '温度超高', TEMP_OVER_100: '温度偏高', TEMP_ABNORMAL: '温度异常',
    DECOCT_TIMEOUT: '煎药超时', DEVICE_OFFLINE: '设备离线', PACKER_JAM: '包装机卡袋',
    LABEL_LOW: '标签缺纸', EMERGENCY_STOP: '急停触发',
    TEMP_HIGH: '温度超限', TEMP_LOW: '温度过低', TIMEOUT: '超时', DEVICE_FAULT: '设备故障'
  }
  return map[type] || type
}

function alarmLevelText(level: string) {
  const map: Record<string, string> = { INFO: '一般', WARNING: '警告', CRITICAL: '严重', URGENT: '紧急' }
  return map[level] || level
}

function alarmLevelTag(level: string) {
  const map: Record<string, string> = { URGENT: 'danger', CRITICAL: 'danger', WARNING: 'warning', INFO: 'info' }
  return map[level] || 'info'
}

function notifyTypeText(types: string) {
  if (!types) return '-'
  const map: Record<string, string> = { VOICE: '语音', POPUP: '弹窗', SMS: '短信', PHONE: '电话', PUSH: '推送', WEB: 'Web通知' }
  return types.split(',').map((t) => map[t.trim()] || t.trim()).join(' + ')
}

const formRef = ref<any>(null)
const formRules = {
  alarmType: [{ required: true, message: '告警类型不能为空', trigger: 'change' }],
  alarmLevel: [{ required: true, message: '告警级别不能为空', trigger: 'change' }],
  thresholdValue: [{ required: true, message: '阈值不能为空', trigger: 'change' }],
  enabled: [{ required: true, message: '启用状态不能为空', trigger: 'change' }],
}

async function loadConfigs() {
  loading.value = true
  try {
    const res: any = await getAlarmConfigs()
    configList.value = res.data || []
  } catch (err) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  currentId.value = null
  form.alarmType = 'TEMP_HIGH'
  form.alarmLevel = 'WARNING'
  form.thresholdValue = 110
  form.durationSeconds = 10
  notifyTypes.value = []
  showDialog.value = true
}

function handleEdit(row: any) {
  isEdit.value = true
  currentId.value = row.id
  form.alarmType = row.alarmType
  form.alarmLevel = row.alarmLevel
  form.thresholdValue = row.thresholdValue
  form.durationSeconds = row.durationSeconds
  notifyTypes.value = row.notifyType ? row.notifyType.split(',') : []
  showDialog.value = true
}

async function handleEnableChange(row: any, enabled: boolean) {
  try {
    await updateAlarmConfig(row.id, { ...row, enabled })
    ElMessage.success(enabled ? '已启用' : '已禁用')
  } catch (err) {
    row.enabled = !enabled
    ElMessage.error('操作失败')
  }
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate()
  try {
    const data = { ...form, notifyType: notifyTypes.value.join(',') }
    if (isEdit.value) {
      await updateAlarmConfig(currentId.value!, data)
    } else {
      await createAlarmConfig(data)
    }
    ElMessage.success('保存成功')
    showDialog.value = false
    loadConfigs()
  } catch (err) {
    ElMessage.error('保存失败')
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确认删除该配置?', '提示', { type: 'warning' })
    await deleteAlarmConfig(row.id)
    ElMessage.success('删除成功')
    loadConfigs()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped lang="scss">
.alarm-config {
  padding: 16px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h2 { margin: 0; font-size: 20px; }
  }
}
</style>
