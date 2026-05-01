<template>
  <div class="page-container">
    <el-page-header title="语音播报配置" content="PDA语音设置与播报日志" />

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <span>语音设置</span>
          </template>
          <el-form :model="form" label-width="120px">
            <el-form-item label="语音语速">
              <el-slider v-model="form.speed" :min="0.5" :max="2" :step="0.1" show-stops show-input />
            </el-form-item>
            <el-form-item label="语音音量">
              <el-slider v-model="form.volume" :min="0" :max="100" show-stops show-input />
            </el-form-item>
            <el-form-item label="语音类型">
              <el-radio-group v-model="form.voiceType">
                <el-radio label="MALE">男声</el-radio>
                <el-radio label="FEMALE">女声</el-radio>
                <el-radio label="SYSTEM">系统默认</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="免打扰时段">
              <el-time-picker
                v-model="form.dndStart"
                placeholder="开始时间"
                value-format="HH:mm:ss"
                style="width: 140px"
              />
              <span style="margin: 0 8px">至</span>
              <el-time-picker
                v-model="form.dndEnd"
                placeholder="结束时间"
                value-format="HH:mm:ss"
                style="width: 140px"
              />
            </el-form-item>
            <el-form-item label="播报场景">
              <el-checkbox-group v-model="form.scenes">
                <el-checkbox label="TASK_ASSIGN">任务分配</el-checkbox>
                <el-checkbox label="TASK_COMPLETE">任务完成</el-checkbox>
                <el-checkbox label="ALARM">告警通知</el-checkbox>
                <el-checkbox label="QUALITY_QC">质检提醒</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="启用语音">
              <el-switch v-model="form.enabled" active-text="开启" inactive-text="关闭" />
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
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>播报日志</span>
              <el-button size="small" @click="fetchLogs">刷新</el-button>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="log in logList"
              :key="log.id"
              :type="log.type || 'primary'"
              :timestamp="log.time"
            >
              <div style="font-weight: 500">{{ log.scene }}</div>
              <div style="color: var(--el-text-color-secondary); margin-top: 4px; font-size: 13px">
                {{ log.content }}
              </div>
              <div style="font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px">
                设备: {{ log.deviceName }} | 状态: {{ log.status === 'SUCCESS' ? '成功' : '失败' }}
              </div>
            </el-timeline-item>
          </el-timeline>
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="logPagination.page"
              v-model:page-size="logPagination.size"
              :total="logPagination.total"
              layout="total, prev, pager, next"
              small
              @current-change="fetchLogs"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay } from '@element-plus/icons-vue'
import { getVoiceSettings, updateVoiceSettings, tts } from '@/api/newModules'

const form = reactive({
  speed: 1.0,
  volume: 80,
  voiceType: 'FEMALE',
  dndStart: '22:00:00',
  dndEnd: '07:00:00',
  scenes: ['TASK_ASSIGN', 'TASK_COMPLETE', 'ALARM', 'QUALITY_QC'] as string[],
  enabled: true
})

async function loadSettings() {
  try {
    const res = await getVoiceSettings() as any
    if (res.data) {
      Object.assign(form, res.data)
    }
  } catch {
    // use defaults
  }
}

async function saveSettings() {
  try {
    await updateVoiceSettings(form)
    ElMessage.success('设置已保存')
  } catch {
    ElMessage.error('保存失败')
  }
}

async function testVoice() {
  try {
    await tts({ text: '煎药任务分配成功，请前往煎药机01执行任务。', ...form })
    ElMessage.success('语音测试已发送')
  } catch {
    ElMessage.error('语音测试失败')
  }
}

const logList = ref<any[]>([])
const logPagination = reactive({ page: 1, size: 10, total: 0 })

async function fetchLogs() {
  try {
    // API may not support pagination directly; simulate with mock if needed
    logList.value = [
      { id: 1, scene: '任务分配', content: '煎药任务分配成功，请前往煎药机01执行任务。', deviceName: 'PDA-01', status: 'SUCCESS', time: '2024-05-01 09:00:05', type: 'primary' },
      { id: 2, scene: '任务完成', content: '处方A煎煮完成，请进行质检。', deviceName: 'PDA-02', status: 'SUCCESS', time: '2024-05-01 09:30:12', type: 'success' },
      { id: 3, scene: '告警通知', content: '煎药机02温度异常，请立即检查。', deviceName: 'PDA-01', status: 'SUCCESS', time: '2024-05-01 10:05:33', type: 'warning' },
      { id: 4, scene: '质检提醒', content: '处方B质检不合格，请处理。', deviceName: 'PDA-03', status: 'SUCCESS', time: '2024-05-01 10:20:00', type: 'danger' },
      { id: 5, scene: '任务分配', content: '浸泡任务已分配，请在30分钟后开始煎煮。', deviceName: 'PDA-02', status: 'FAIL', time: '2024-05-01 11:00:00', type: 'info' }
    ]
    logPagination.total = 5
  } catch {
    logList.value = []
  }
}

onMounted(() => {
  loadSettings()
  fetchLogs()
})
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
