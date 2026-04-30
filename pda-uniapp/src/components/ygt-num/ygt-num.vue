<template>
  <text class="ygt-num" :class="[size, { bold, mono }]">
    <slot />
  </text>
</template>

<script setup>
defineProps({
  size: { type: String, default: 'md' },
  bold: { type: Boolean, default: false },
  mono: { type: Boolean, default: false }
})
</script>

<style lang="scss" scoped>
@import "../../uni.scss";

.ygt-num {
  font-variant-numeric: tabular-nums;
  font-feature-settings: "tnum";
}

.ygt-num.xs { font-size: 22rpx; }
.ygt-num.sm { font-size: 24rpx; }
.ygt-num.md { font-size: 28rpx; }
.ygt-num.lg { font-size: 32rpx; }
.ygt-num.xl { font-size: 40rpx; }
.ygt-num.xxl { font-size: 48rpx; }

.ygt-num.bold {
  font-weight: 600;
}

.ygt-num.mono {
  font-family: "SF Mono", "Consolas", monospace;
}
</style>
