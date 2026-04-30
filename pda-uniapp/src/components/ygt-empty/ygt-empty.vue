<template>
  <view class="ygt-empty">
    <view class="ygt-empty-icon">
      <text class="empty-symbol">○</text>
    </view>
    <text class="ygt-empty-title">{{ title }}</text>
    <text class="ygt-empty-desc" v-if="description">{{ description }}</text>
    <view class="ygt-empty-action" v-if="$slots.action">
      <slot name="action" />
    </view>
  </view>
</template>

<script setup>
defineProps({
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '' }
})
</script>

<style scoped>
.ygt-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 40rpx;
}

.ygt-empty-icon {
  margin-bottom: 24rpx;
}

.ygt-empty-icon .empty-symbol {
  font-size: 80rpx;
  color: #D4D4D8;
  font-weight: 300;
}

.ygt-empty-title {
  font-size: 30rpx;
  font-weight: 500;
  color: #52525B;
  margin-bottom: 12rpx;
}

.ygt-empty-desc {
  font-size: 26rpx;
  color: #71717A;
  text-align: center;
  max-width: 480rpx;
  line-height: 1.6;
}

.ygt-empty-action {
  margin-top: 32rpx;
}
</style>
