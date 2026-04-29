<template>
  <view class="ygt-card" :class="[size, { shadow, borderless: !border }]">
    <view class="ygt-card-header" v-if="title || $slots.header">
      <text class="ygt-card-title" v-if="title">{{ title }}</text>
      <slot name="header" />
      <slot name="header-extra" />
    </view>
    <view class="ygt-card-body">
      <slot />
    </view>
    <view class="ygt-card-footer" v-if="$slots.footer">
      <slot name="footer" />
    </view>
  </view>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' },
  size: { type: String, default: 'md' },
  shadow: { type: Boolean, default: true },
  border: { type: Boolean, default: false }
})
</script>

<style lang="scss" scoped>
@import "../../uni.scss";

.ygt-card {
  background: #fff;
  border-radius: $ygt-radius-lg;
  overflow: hidden;
}

.ygt-card.shadow {
  box-shadow: $ygt-shadow-card;
}

.ygt-card.borderless {
  border: none;
}

.ygt-card.sm { padding: 24rpx; }
.ygt-card.md { padding: 32rpx; }
.ygt-card.lg { padding: 40rpx; }

.ygt-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.ygt-card-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $ygt-gray-900;
}

.ygt-card-body {
  /* 由 slot 内容决定 */
}

.ygt-card-footer {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid $ygt-gray-100;
}
</style>
