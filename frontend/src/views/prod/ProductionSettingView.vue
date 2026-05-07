<template>
  <div class="production-setting">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover" @click="$router.push('/voice-setting')" class="setting-card">
          <div class="card-content">
            <el-icon :size="48" color="#409EFF"><Microphone /></el-icon>
            <h3>语音播报设置</h3>
            <p>配置生产现场的语音播报参数，包括播报内容、音量、触发条件等。</p>
            <el-button type="primary" text>前往配置 →</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" @click="$router.push('/herb-group')" class="setting-card">
          <div class="card-content">
            <el-icon :size="48" color="#67C23A"><FirstAidKit /></el-icon>
            <h3>分组投料规则</h3>
            <p>配置药材分组投料规则，定义不同药材的投料顺序和组合方式。</p>
            <el-button type="success" text>前往配置 →</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { Microphone, FirstAidKit } from '@element-plus/icons-vue'
</script>

<style scoped>
.production-setting {
  padding: 20px;
}
.setting-card {
  cursor: pointer;
  transition: all 0.3s;
}
.setting-card:hover {
  transform: translateY(-4px);
}
.card-content {
  text-align: center;
  padding: 30px 20px;
}
.card-content h3 {
  margin: 16px 0 8px;
  font-size: 18px;
  color: var(--el-text-color-primary);
}
.card-content p {
  margin: 0 0 16px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  line-height: 1.6;
}
</style>
