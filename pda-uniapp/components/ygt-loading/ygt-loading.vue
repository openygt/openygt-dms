<template>
  <view class="ygt-loading">
    <view class="ygt-loading-spinner">
      <view class="spinner-dot" v-for="i in 3" :key="i" :style="{ animationDelay: (i - 1) * 0.15 + 's' }"></view>
    </view>
    <text class="ygt-loading-text" v-if="text">{{ text }}</text>
  </view>
</template>

<script setup>
defineProps({
  text: { type: String, default: '加载中...' }
})
</script>

<style lang="scss" scoped>
@import "../../uni.scss";

.ygt-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60rpx 40rpx;
}

.ygt-loading-spinner {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.spinner-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: $ygt-primary;
  animation: bounce 1.2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.ygt-loading-text {
  font-size: 26rpx;
  color: $ygt-gray-500;
}
</style>
