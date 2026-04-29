<template>
  <div class="ygt-state ygt-loading">
    <el-icon :size="48" class="ygt-state-icon is-loading">
      <Loading />
    </el-icon>
    <div class="ygt-state-title">{{ title || '加载中...' }}</div>
    <div class="ygt-state-desc" v-if="description">{{ description }}</div>
  </div>
</template>

<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'

defineProps<{
  title?: string
  description?: string
}>()
</script>

<style scoped>
.ygt-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--ygt-space-12) var(--ygt-space-4);
  color: var(--ygt-text-tertiary);
}
.ygt-state-icon {
  color: var(--ygt-primary-400);
  margin-bottom: var(--ygt-space-3);
  animation: rotate 1s linear infinite;
}
@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.ygt-state-title {
  font-size: var(--ygt-text-md);
  margin-bottom: var(--ygt-space-2);
  color: var(--ygt-text-secondary);
}
.ygt-state-desc {
  font-size: var(--ygt-text-sm);
  color: var(--ygt-text-tertiary);
  max-width: 360px;
  text-align: center;
}
</style>
