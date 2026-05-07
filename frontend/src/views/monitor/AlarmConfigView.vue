<template>
  <div class="alarm-config">
    <div class="page-header">

      <el-button type="primary" @click="handleAdd">新增配置</el-button>
    </div>

    <el-card>
      <el-table :data="configList" v-loading="loading">
        <el-table-column prop="alarmType" label="告警类型" width="150" />
        <el-table-column label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="row.alarmLevel === 'CRITICAL' ? 'danger' : (row.alarmLevel === 'WARNING' ? 'warning' : 'info')">
              {{ row.alarmLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="thresholdValue" label="阈值" width="120" />
        <el-table-column prop="durationSeconds" label="持续时间(秒)" width="120" />
        <el-table-column prop="notifyType" label="通知方式" width="150" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="(v: boolean) => handleEnableChange(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showDialog" :title="isEdit ? '编辑告警配置' : '新增告警配置'" width="600px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <el-form-item label="告警类型">
          <el-select v-model="form.alarmType">
            <el-option label="温度超限" value="TEMP_HIGH" />
            <el-option label="温度过低" value="TEMP_LOW" />
            <el-option label="超时" value="TIMEOUT" />
            <el-option label="设备故障" value="DEVICE_FAULT" />
          </el-select>
        </el-form-item>
        <el-form-item label="告警级别">
          <el-radio-group v-model="form.alarmLevel">
            <el-radio-button label="INFO">信息</el-radio-button>
            <el-radio-button label="WARNING">警告</el-radio-button>
            <el-radio-button label="CRITICAL">紧急</el-radio-button>
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
            <el-checkbox label="WEB">Web通知</el-checkbox>
            <el-checkbox label="PUSH">推送</el-checkbox>
            <el-checkbox label="SMS">短信</el-checkbox>
            <el-checkbox label="VOICE">语音</el-checkbox>
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
  } catch (err) {
    // cancelled
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
