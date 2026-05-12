<template>
  <div class="production-setting">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
      <h2 style="margin: 0; font-size: 18px">语音播报设置</h2>
      <el-button type="primary" @click="openAddDialog">新增</el-button>
    </div>

    <el-card shadow="never" class="default-card">
      <template #header>
        <span style="font-weight: 500">默认设置</span>
      </template>
      <el-form :model="defaultForm" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="语音语速">
              <el-slider v-model="defaultForm.speechRate" :min="0" :max="100" show-input />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="语音音量">
              <el-slider v-model="defaultForm.volume" :min="0" :max="100" show-input />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="语音类型">
              <el-radio-group v-model="defaultForm.voiceType">
                <el-radio label="male">男声</el-radio>
                <el-radio label="female">女声</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="重复播报">
              <el-input-number v-model="defaultForm.repeatCount" :min="1" :max="5" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="免打扰时段">
              <el-time-picker v-model="defaultForm.quietStart" value-format="HH:mm" style="width: 120px" />
              <span style="margin: 0 8px">至</span>
              <el-time-picker v-model="defaultForm.quietEnd" value-format="HH:mm" style="width: 120px" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用语音">
              <el-switch v-model="defaultForm.enableVoice" :active-value="1" :inactive-value="0" active-text="开启" inactive-text="关闭" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" @click="saveDefault">保存默认设置</el-button>
          <el-button @click="testVoice()">测试语音</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <span style="font-weight: 500">设备专属设置</span>
      </template>
      <el-table :data="deviceList" border>
        <el-table-column prop="deviceId" label="设备标识" />
        <el-table-column prop="deviceTypeLabel" label="设备类型" width="100" />
        <el-table-column prop="speechRate" label="语速" width="70" />
        <el-table-column prop="volume" label="音量" width="70" />
        <el-table-column label="语音类型" width="80">
          <template #default="{ row }">{{ row.voiceType === 'male' ? '男声' : '女声' }}</template>
        </el-table-column>
        <el-table-column label="免打扰" width="140">
          <template #default="{ row }">{{ row.quietStart || '--' }} - {{ row.quietEnd || '--' }}</template>
        </el-table-column>
        <el-table-column label="重复" width="60">
          <template #default="{ row }">{{ row.repeatCount }}次</template>
        </el-table-column>
        <el-table-column label="启用" width="70">
          <template #default="{ row }">
            <el-tag :type="row.enableVoice ? 'success' : 'info'" size="small">{{ row.enableVoice ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除此设置？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!deviceList.length" description="暂无设备专属设置，点击新增按设备配置" :image-size="48" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑设备设置' : '新增设备设置'" width="520px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="设备标识" required>
          <el-input v-model="editForm.deviceId" placeholder="如 PDA-01 / SPEAKER-01" />
        </el-form-item>
        <el-form-item label="设备类型" required>
          <el-select v-model="editForm.deviceType" style="width: 100%">
            <el-option label="PAD（手持终端）" value="PAD" />
            <el-option label="喇叭（广播播报）" value="SPEAKER" />
          </el-select>
        </el-form-item>
        <el-form-item label="语音语速">
          <el-slider v-model="editForm.speechRate" :min="0" :max="100" show-input />
        </el-form-item>
        <el-form-item label="语音音量">
          <el-slider v-model="editForm.volume" :min="0" :max="100" show-input />
        </el-form-item>
        <el-form-item label="语音类型">
          <el-radio-group v-model="editForm.voiceType">
            <el-radio label="male">男声</el-radio>
            <el-radio label="female">女声</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="免打扰时段">
          <el-time-picker v-model="editForm.quietStart" value-format="HH:mm" style="width: 120px" />
          <span style="margin: 0 8px">至</span>
          <el-time-picker v-model="editForm.quietEnd" value-format="HH:mm" style="width: 120px" />
        </el-form-item>
        <el-form-item label="重复播报">
          <el-input-number v-model="editForm.repeatCount" :min="1" :max="5" />
        </el-form-item>
        <el-form-item label="启用语音">
          <el-switch v-model="editForm.enableVoice" :active-value="1" :inactive-value="0" active-text="开启" inactive-text="关闭" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">{{ isEdit ? '保存' : '添加' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getVoiceSettings, updateVoiceSettings, tts } from '@/api/newModules'
import request from '@/api/request'

const defaultForm = reactive({
  id: null as number | null,
  deviceId: '',
  deviceType: 'DEFAULT',
  speechRate: 50,
  volume: 80,
  voiceType: 'female',
  quietStart: '22:00',
  quietEnd: '07:00',
  repeatCount: 1,
  enableVoice: 1
})

const deviceList = ref<any[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editForm = reactive({
  id: null as number | null,
  deviceId: '',
  deviceType: 'PAD',
  speechRate: 50,
  volume: 80,
  voiceType: 'female',
  quietStart: '22:00',
  quietEnd: '07:00',
  repeatCount: 1,
  enableVoice: 1
})

async function loadSettings() {
  try {
    const res: any = await request.get('/v1/eq/voice-settings/list')
    const all = res.data || []
    const def = all.find((s: any) => !s.deviceId || s.deviceType === 'DEFAULT')
    if (def) Object.assign(defaultForm, def)
    deviceList.value = all.filter((s: any) => s.deviceId && s.deviceType !== 'DEFAULT').map((s: any) => ({
      ...s,
      deviceTypeLabel: s.deviceType === 'SPEAKER' ? '喇叭' : 'PAD'
    }))
  } catch {
    try {
      const res: any = await getVoiceSettings()
      if (res.data) Object.assign(defaultForm, res.data)
    } catch { /* use defaults */ }
    deviceList.value = []
  }
}

async function saveDefault() {
  await updateVoiceSettings({ ...defaultForm })
  ElMessage.success('默认设置已保存')
  loadSettings()
}

async function testVoice(deviceId?: string) {
  try {
    await tts({
      text: '煎药任务分配成功，请前往指定设备执行任务。',
      deviceId: deviceId || undefined
    })
    ElMessage.success('语音测试已发送')
  } catch (e: any) {
    ElMessage.error('语音服务未连接：' + (e?.message || '请检查语音设备'))
  }
}

function openAddDialog() {
  isEdit.value = false
  editForm.id = null
  editForm.deviceId = ''
  editForm.deviceType = 'PAD'
  editForm.speechRate = 50
  editForm.volume = 80
  editForm.voiceType = 'female'
  editForm.quietStart = '22:00'
  editForm.quietEnd = '07:00'
  editForm.repeatCount = 1
  editForm.enableVoice = 1
  dialogVisible.value = true
}

function openEditDialog(row: any) {
  isEdit.value = true
  Object.assign(editForm, row)
  dialogVisible.value = true
}

async function saveEdit() {
  try {
    await request.post('/v1/eq/voice-settings/save', { ...editForm })
    ElMessage.success(isEdit.value ? '已更新' : '已添加')
    dialogVisible.value = false
    loadSettings()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  }
}

async function handleDelete(row: any) {
  try {
    await request.delete(`/v1/eq/voice-settings/${row.id}`)
    ElMessage.success('已删除')
    loadSettings()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

onMounted(loadSettings)
</script>

<style scoped>
.production-setting {
  padding: 20px;
}

.default-card {
  margin-bottom: 0;
}
</style>
