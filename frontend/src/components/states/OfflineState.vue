<template>
  <div class="ygt-state ygt-offline">
    <el-icon :size="48" class="ygt-state-icon">
      <Warning />
    </el-icon>
    <div class="ygt-state-title">{{ title || '网络已断开' }}</div>
    <div class="ygt-state-desc" v-if="description">{{ description }}</div>
    <div class="ygt-state-action" v-if="$slots.action">
      <slot name="action" />
    </div>
    <el-button v-else-if="onRetry" type="primary" :icon="Refresh" @click="onRetry">
      刷新重试
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { Warning, Refresh } from '@element-plus/icons-vue'

defineProps<{
  title?: string
  description?: string
  onRetry?: () => void
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
  color: var(--ygt-warning);
  margin-bottom: var(--ygt-space-3);
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
.ygt-state-action {
  margin-top: var(--ygt-space-4);
}
</style>
