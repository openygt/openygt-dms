<template>
  <view class="ygt-step-timeline">
    <view
      v-for="(step, idx) in steps"
      :key="step.value || idx"
      class="timeline-node"
      :class="{
        'node-done': step.status === 'done' || step.completed === true,
        'node-current': step.status === 'current' || step.current === true,
        'node-pending': step.status === 'pending' || (!step.completed && !step.current)
      }"
      @click="$emit('click', step, idx)"
    >
      <!-- 竖线（除第一个） -->
      <view class="node-line" v-if="idx > 0"></view>

      <view class="node-marker">
        <view class="node-circle">
          <text class="node-check" v-if="step.status === 'done' || step.completed === true">✓</text>
          <text class="node-num" v-else>{{ idx + 1 }}</text>
        </view>
      </view>

      <view class="node-body">
        <text class="node-label">{{ step.label }}</text>
        <text class="node-time ygt-num" v-if="step.time">{{ step.time }}</text>
        <text class="node-hint" v-else-if="step.status === 'current' || step.current === true">{{ step.hint || '待确认' }}</text>
      </view>

      <view class="node-extra" v-if="$slots.extra">
        <slot name="extra" :step="step" :index="idx" />
      </view>
    </view>
  </view>
</template>

<script setup>
defineProps({
  steps: {
    type: Array,
    default: () => []
  }
})
defineEmits(['click'])
</script>

<style lang="scss" scoped>
@import "../../uni.scss";

.ygt-step-timeline {
  display: flex;
  flex-direction: column;
}

.timeline-node {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 20rpx 24rpx;
  border-radius: $ygt-radius-md;
  margin-bottom: 8rpx;
  position: relative;
  transition: all 0.2s;
}

.timeline-node:last-child {
  margin-bottom: 0;
}

.node-done {
  background: rgba(22, 163, 74, 0.04);
}

.node-current {
  background: rgba(0, 102, 204, 0.06);
  border: 2rpx solid rgba(0, 102, 204, 0.25);
}

.node-pending {
  opacity: 0.55;
}

.node-line {
  position: absolute;
  left: 52rpx;
  top: -14rpx;
  width: 2rpx;
  height: 28rpx;
  background: $ygt-gray-200;
}

.node-done .node-line {
  background: $ygt-success;
}

.node-marker {
  flex-shrink: 0;
}

.node-circle {
  width: 68rpx;
  height: 68rpx;
  border-radius: 50%;
  background: $ygt-gray-200;
  display: flex;
  align-items: center;
  justify-content: center;
}

.node-done .node-circle {
  background: $ygt-success;
}

.node-current .node-circle {
  background: $ygt-primary;
  animation: pulse-ring 2s ease-in-out infinite;
}

.node-check {
  font-weight: 600;
  font-size: 32rpx;
  color: #fff;
}

.node-num {
  font-size: 28rpx;
  font-weight: 600;
  color: $ygt-gray-500;
}

.node-done .node-num,
.node-current .node-num {
  color: #fff;
}

.node-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.node-label {
  font-size: 28rpx;
  font-weight: 500;
  color: $ygt-gray-900;
}

.node-time {
  font-size: 22rpx;
  color: $ygt-success;
}

.node-hint {
  font-size: 22rpx;
  color: $ygt-primary;
}

.node-extra {
  flex-shrink: 0;
}

@keyframes pulse-ring {
  0%, 100% { box-shadow: 0 0 0 0 rgba(0, 102, 204, 0.2); }
  50% { box-shadow: 0 0 0 10rpx rgba(0, 102, 204, 0); }
}
</style>
