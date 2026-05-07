<template>
  <div class="page-container">
    <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
    <div style="height: 16px"></div>
    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            
          </template>
          <el-form :model="form" label-width="120px">
            <el-form-item label="设备标识">
              <el-input v-model="form.deviceId" placeholder="如 PDA-01，不填则保存为默认设置" />
            </el-form-item>
            <el-form-item label="语音语速">
              <el-slider v-model="form.speechRate" :min="0" :max="100" show-stops show-input />
            </el-form-item>
            <el-form-item label="语音音量">
              <el-slider v-model="form.volume" :min="0" :max="100" show-stops show-input />
            </el-form-item>
            <el-form-item label="语音类型">
              <el-radio-group v-model="form.voiceType">
                <el-radio label="male">男声</el-radio>
                <el-radio label="female">女声</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="免打扰时段">
              <el-time-picker v-model="form.quietStart" value-format="HH:mm" style="width: 140px" />
              <span style="margin: 0 8px">至</span>
              <el-time-picker v-model="form.quietEnd" value-format="HH:mm" style="width: 140px" />
            </el-form-item>
            <el-form-item label="重复播报">
              <el-input-number v-model="form.repeatCount" :min="1" :max="5" style="width: 100%" />
            </el-form-item>
            <el-form-item label="启用语音">
              <el-switch v-model="form.enableVoice" :active-value="1" :inactive-value="0" active-text="开启" inactive-text="关闭" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSettings">保存设置</el-button>
              <el-button @click="testVoice">
                <el-icon><VideoPlay /></el-icon> 测试语音
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <span>说明</span>
          </template>
          <div class="tips">
            <p>当前页面已按后端真实字段对齐：`speechRate`、`volume`、`voiceType`、`quietStart`、`quietEnd`、`repeatCount`、`enableVoice`。</p>
            <p>如果不填 `deviceId`，后端会读取最近一条默认设置；填写后会按该 PDA 设备保存。</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VideoPlay, ArrowLeft } from '@element-plus/icons-vue'
import { getVoiceSettings, tts, updateVoiceSettings } from '@/api/newModules'

const router = useRouter()

const form = reactive({
  id: null as number | null,
  deviceId: '',
  userId: null as number | null,
  speechRate: 50,
  volume: 80,
  voiceType: 'female',
  quietStart: '22:00',
  quietEnd: '07:00',
  repeatCount: 1,
  enableVoice: 1
})

async function loadSettings() {
  const res: any = await getVoiceSettings()
  if (res.data) {
    Object.assign(form, res.data)
  }
}

async function saveSettings() {
  await updateVoiceSettings({ ...form })
  ElMessage.success('设置已保存')
  await loadSettings()
}

async function testVoice() {
  try {
    await tts({
      text: '煎药任务分配成功，请前往指定设备执行任务。',
      deviceId: form.deviceId || undefined
    })
    ElMessage.success('语音测试已发送')
  } catch (e: any) {
    ElMessage.error('语音服务未连接：' + (e?.message || '请检查语音设备'))
  }
}

onMounted(loadSettings)
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tips {
  color: var(--el-text-color-regular);
  line-height: 1.8;
}
</style>
