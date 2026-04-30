<template>
  <view class="ygt-scan-input">
    <input
      class="scan-field ygt-num"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.detail.value)"
      :placeholder="placeholder"
      :focus="focus"
      confirm-type="search"
      @confirm="$emit('confirm')"
    />
    <button class="scan-btn" @click="$emit('scan')">
      <text class="btn-icon">□</text>
      <text class="btn-text">扫码</text>
    </button>
  </view>
</template>

<script setup>
defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '请输入或扫描...' },
  focus: { type: Boolean, default: false }
})
defineEmits(['update:modelValue', 'confirm', 'scan'])
</script>

<style lang="scss" scoped>
@import "../../uni.scss";

.ygt-scan-input {
  display: flex;
  gap: 16rpx;
}

.scan-field {
  flex: 1;
  height: 96rpx;
  background: #fff;
  border-radius: $ygt-radius-md;
  padding: 0 32rpx;
  font-size: 32rpx;
  border: 2rpx solid $ygt-gray-200;
}

.scan-btn {
  width: 180rpx;
  height: 96rpx;
  background: $ygt-primary;
  color: #fff;
  border-radius: $ygt-radius-md;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0;
  line-height: 1.2;
}

.btn-icon {
  font-size: 36rpx;
  font-weight: 500;
}

.btn-text {
  font-size: 22rpx;
  margin-top: 4rpx;
}
</style>
