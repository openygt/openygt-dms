<template>
  <div class="page-container">
    <div class="page-header-title">设备联网</div>
    <el-tabs v-model="activeTab" class="network-tabs">
      <el-tab-pane label="协议配置" name="config">
        <el-empty description="协议配置功能开发中" />
      </el-tab-pane>
      <el-tab-pane label="在线心跳" name="heartbeat">
        <el-empty description="在线心跳功能开发中" />
      </el-tab-pane>
      <el-tab-pane label="指令通道" name="channel">
        <el-empty description="指令通道功能开发中" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
const activeTab = ref('config')
</script>

<style scoped lang="scss">
.page-container {
  padding: var(--ygt-space-4);
}
.network-tabs {
  margin-top: 16px;
}
</style>
