<template>
  <div class="ygt-state ygt-empty">
    <el-icon :size="48" class="ygt-state-icon">
      <DocumentRemove />
    </el-icon>
    <div class="ygt-state-title">{{ title || '暂无数据' }}</div>
    <div class="ygt-state-desc" v-if="description">{{ description }}</div>
    <div class="ygt-state-action" v-if="$slots.action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { DocumentRemove } from '@element-plus/icons-vue'

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
  color: var(--ygt-gray-300);
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
