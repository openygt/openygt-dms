<template>
  <div class="page-container">
    <div class="page-header-title">设备分组</div>
    <el-tabs v-model="activeTab" class="group-tabs">
      <el-tab-pane label="分组管理" name="group">
        <el-empty description="分组管理功能开发中" />
      </el-tab-pane>
      <el-tab-pane label="配对规则" name="pairing">
        <el-empty description="配对规则功能开发中" />
      </el-tab-pane>
      <el-tab-pane label="联动规则" name="rule">
        <el-empty description="联动规则功能开发中" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
const activeTab = ref('group')
</script>

<style scoped lang="scss">
.page-container {
  padding: var(--ygt-space-4);
}
.group-tabs {
  margin-top: 16px;
}
</style>
