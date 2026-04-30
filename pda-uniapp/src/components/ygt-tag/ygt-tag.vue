<template>
  <text class="ygt-tag" :class="[`tag-${type}`, size, { plain, round }]">
    <slot />
  </text>
</template>

<script setup>
defineProps({
  type: { type: String, default: 'primary' },
  size: { type: String, default: 'md' },
  plain: { type: Boolean, default: false },
  round: { type: Boolean, default: false }
})
</script>

<style lang="scss" scoped>
@import "../../uni.scss";

.ygt-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
}

.ygt-tag.sm { padding: 4rpx 12rpx; font-size: 22rpx; }
.ygt-tag.md { padding: 6rpx 16rpx; font-size: 24rpx; }
.ygt-tag.lg { padding: 8rpx 20rpx; font-size: 28rpx; }

.ygt-tag.round { border-radius: 9999rpx; }
.ygt-tag:not(.round) { border-radius: 8rpx; }

/* solid */
.tag-primary { background: rgba(0, 102, 204, 0.1); color: $ygt-primary; }
.tag-success { background: rgba(22, 163, 74, 0.1); color: $ygt-success; }
.tag-warning { background: rgba(217, 119, 6, 0.1); color: $ygt-warning; }
.tag-danger  { background: rgba(220, 38, 38, 0.1); color: $ygt-danger; }
.tag-info    { background: rgba(8, 145, 178, 0.1); color: $ygt-info; }
.tag-default { background: $ygt-gray-100; color: $ygt-gray-500; }

/* plain */
.ygt-tag.plain.tag-primary { background: transparent; border: 2rpx solid $ygt-primary; color: $ygt-primary; }
.ygt-tag.plain.tag-success { background: transparent; border: 2rpx solid $ygt-success; color: $ygt-success; }
.ygt-tag.plain.tag-warning { background: transparent; border: 2rpx solid $ygt-warning; color: $ygt-warning; }
.ygt-tag.plain.tag-danger  { background: transparent; border: 2rpx solid $ygt-danger; color: $ygt-danger; }
.ygt-tag.plain.tag-info    { background: transparent; border: 2rpx solid $ygt-info; color: $ygt-info; }
.ygt-tag.plain.tag-default { background: transparent; border: 2rpx solid $ygt-gray-300; color: $ygt-gray-500; }
</style>
