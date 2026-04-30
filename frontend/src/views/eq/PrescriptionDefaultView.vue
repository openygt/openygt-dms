<template>
  <div class="prescription-default">
    <div class="page-header">
      <h2>处方默认设置</h2>
      <el-button @click="resetDefaults">恢复默认</el-button>
    </div>

    <el-card v-loading="loading">
      <el-form :model="settings" label-width="180px">
        <el-divider content-position="left">煎煮参数</el-divider>
        <el-form-item label="默认浸泡时间(分钟)">
          <el-input-number v-model="settings.soakTime" :min="5" :max="120" />
        </el-form-item>
        <el-form-item label="默认一煎时间(分钟)">
          <el-input-number v-model="settings.firstDecoctTime" :min="10" :max="120" />
        </el-form-item>
        <el-form-item label="默认二煎时间(分钟)">
          <el-input-number v-model="settings.secondDecoctTime" :min="10" :max="120" />
        </el-form-item>
        <el-form-item label="默认先煎时间(分钟)">
          <el-input-number v-model="settings.preDecoctTime" :min="5" :max="60" />
        </el-form-item>
        <el-form-item label="默认后下时间(分钟)">
          <el-input-number v-model="settings.addLateTime" :min="3" :max="30" />
        </el-form-item>

        <el-divider content-position="left">包装参数</el-divider>
        <el-form-item label="默认包装容量(ml/袋)">
          <el-input-number v-model="settings.packageVolume" :min="50" :max="500" :step="10" />
        </el-form-item>
        <el-form-item label="默认留样数量(袋)">
          <el-input-number v-model="settings.sampleCount" :min="0" :max="5" />
        </el-form-item>

        <el-divider content-position="left">加水量</el-divider>
        <el-form-item label="默认加水量公式">
          <el-select v-model="settings.waterFormula" placeholder="选择公式">
            <el-option label="标准公式" value="FORMULA_001" />
            <el-option label="浓缩配方" value="FORMULA_002" />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="form-actions">
        <el-button type="primary" size="large" @click="saveSettings">保存设置</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPrescriptionDefaults, updatePrescriptionDefault } from '@/api/equipment'

const loading = ref(false)
const originalSettings = ref<any>({})

const settings = reactive({
  soakTime: 30,
  firstDecoctTime: 30,
  secondDecoctTime: 20,
  preDecoctTime: 15,
  addLateTime: 10,
  packageVolume: 200,
  sampleCount: 1,
  waterFormula: 'FORMULA_001'
})

const keyMap: Record<string, string> = {
  soakTime: 'default.soak.time',
  firstDecoctTime: 'default.first.decoct.time',
  secondDecoctTime: 'default.second.decoct.time',
  preDecoctTime: 'default.preDecoct.time',
  addLateTime: 'default.addLate.time',
  packageVolume: 'default.package.volume',
  sampleCount: 'default.sample.count',
  waterFormula: 'default.water.formula'
}

const defaultValues = {
  soakTime: 30,
  firstDecoctTime: 30,
  secondDecoctTime: 20,
  preDecoctTime: 15,
  addLateTime: 10,
  packageVolume: 200,
  sampleCount: 1,
  waterFormula: 'FORMULA_001'
}

async function loadSettings() {
  loading.value = true
  try {
    const res: any = await getPrescriptionDefaults()
    const list = res.data || []
    for (const item of list) {
      const key = Object.keys(keyMap).find(k => keyMap[k] === item.settingKey)
      if (key) {
        if (item.settingType === 'INT') {
          (settings as any)[key] = parseInt(item.settingValue) || 0
        } else {
          (settings as any)[key] = item.settingValue
        }
      }
    }
  } catch (err) {
    ElMessage.error('加载设置失败')
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  try {
    for (const [key, settingKey] of Object.entries(keyMap)) {
      await updatePrescriptionDefault(settingKey, {
        settingValue: String((settings as any)[key])
      })
    }
    ElMessage.success('设置保存成功')
  } catch (err) {
    ElMessage.error('保存失败')
  }
}

async function resetDefaults() {
  try {
    await ElMessageBox.confirm('确认恢复所有默认设置?', '提示', { type: 'warning' })
    Object.assign(settings, defaultValues)
    await saveSettings()
    ElMessage.success('已恢复默认设置')
  } catch (err) {
    // cancelled
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<style scoped lang="scss">
.prescription-default {
  padding: 16px;
  max-width: 800px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h2 { margin: 0; font-size: 20px; }
  }

  .form-actions {
    margin-top: 24px;
    text-align: center;
  }
}
</style>
