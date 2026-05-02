<template>
  <view class="container">
    <view class="scan-area">
      <button class="scan-btn" @click="doScan">点击扫码</button>
      <input class="barcode-input" v-model="barcode" placeholder="或手动输入条码" @confirm="handleScan" />
    </view>

    <view v-if="result" class="result-card">
      <view class="status-header" :style="{ backgroundColor: result.display?.statusColor || '#999' }">
        <text class="status-name">{{ result.display?.statusName || result.taskStatus }}</text>
        <text class="status-desc">{{ result.display?.description || '' }}</text>
      </view>

      <view class="task-info" v-if="result.taskId">
        <text class="label">任务ID:</text>
        <text class="value">{{ result.taskId }}</text>
      </view>

      <view class="action-bar">
        <text class="action-text">{{ result.actionName }}</text>
      </view>

      <view v-if="result.errorMsg" class="error-msg">
        <text>{{ result.errorMsg }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'

const barcode = ref('')
const result = ref(null)

const doScan = () => {
  uni.scanCode({
    scanType: ['barCode', 'qrCode'],
    success: (res) => {
      barcode.value = res.result
      handleScan()
    },
    fail: () => {
      uni.showToast({ title: '扫码失败', icon: 'none' })
    }
  })
}

const handleScan = async () => {
  if (!barcode.value) {
    uni.showToast({ title: '请输入条码', icon: 'none' })
    return
  }
  try {
    const res = await uni.request({
      url: `${getApp().globalData.baseUrl}/pda/scan`,
      method: 'POST',
      data: { barcode: barcode.value, operatorId: uni.getStorageSync('operatorId') || 1 }
    })
    result.value = res.data.data
    if (res.data.code !== 200) {
      uni.showToast({ title: res.data.message || '请求失败', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: '网络错误', icon: 'none' })
  }
}
</script>

<style scoped>
.container {
  padding: 20rpx;
  background-color: #f5f5f5;
  min-height: 100vh;
}
.scan-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30rpx;
}
.scan-btn {
  width: 300rpx;
  height: 300rpx;
  border-radius: 50%;
  background-color: #1890ff;
  color: #fff;
  font-size: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
}
.barcode-input {
  width: 80%;
  height: 80rpx;
  background-color: #fff;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 32rpx;
}
.result-card {
  background-color: #fff;
  border-radius: 12rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.1);
}
.status-header {
  padding: 30rpx;
  color: #fff;
  text-align: center;
}
.status-name {
  font-size: 48rpx;
  font-weight: bold;
  display: block;
}
.status-desc {
  font-size: 28rpx;
  margin-top: 10rpx;
  display: block;
}
.task-info {
  padding: 20rpx 30rpx;
  display: flex;
  border-bottom: 1rpx solid #eee;
}
.label {
  color: #666;
  width: 150rpx;
}
.value {
  color: #333;
  font-weight: bold;
}
.action-bar {
  padding: 30rpx;
  text-align: center;
}
.action-text {
  font-size: 36rpx;
  color: #52c41a;
  font-weight: bold;
}
.error-msg {
  padding: 30rpx;
  color: #f5222d;
  text-align: center;
}
</style>
