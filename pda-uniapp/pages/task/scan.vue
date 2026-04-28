<template>
  <view class="container">
    <view class="scan-area" @click="doScan">
      <view class="scan-box">
        <text class="scan-icon">📷</text>
        <text class="scan-text">点击扫码</text>
        <text class="scan-subtext">支持条形码 / 二维码</text>
      </view>
    </view>
    
    <view class="input-area">
      <text class="label">或手动输入任务编号</text>
      <view class="input-row">
        <input 
          class="input" 
          v-model="barcode"
          placeholder="请输入任务条形码"
          confirm-type="search"
          @confirm="queryTask"
        />
        <button class="query-btn" @click="queryTask">查询</button>
      </view>
    </view>
    
    <view class="history" v-if="scanHistory.length > 0">
      <text class="section-title">最近扫描</text>
      <view class="history-list">
        <view class="history-item" v-for="(item, idx) in scanHistory" :key="idx" @click="barcode = item; queryTask()">
          <text class="history-code">{{ item }}</text>
          <text class="history-arrow">></text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { get } from '../../utils/request.js'

const barcode = ref('')
const scanHistory = ref(uni.getStorageSync('scan_history') || [])

function doScan() {
  uni.scanCode({
    scanType: ['barCode', 'qrCode'],
    success: (res) => {
      barcode.value = res.result
      queryTask()
    },
    fail: (err) => {
      console.error('Scan failed:', err)
      uni.showToast({ title: '扫码失败', icon: 'none' })
    }
  })
}

async function queryTask() {
  if (!barcode.value.trim()) {
    uni.showToast({ title: '请输入或扫描任务编号', icon: 'none' })
    return
  }
  
  uni.showLoading({ title: '查询中...' })
  try {
    const task = await get(`/task/${barcode.value}`)
    // 保存到历史
    addToHistory(barcode.value)
    uni.hideLoading()
    // 跳转到任务详情
    uni.navigateTo({ url: `/pages/task/detail?taskId=${task.taskId}&barcode=${barcode.value}` })
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: '查询失败：' + e.message, icon: 'none' })
  }
}

function addToHistory(code) {
  let history = scanHistory.value.filter(h => h !== code)
  history.unshift(code)
  if (history.length > 10) history = history.slice(0, 10)
  scanHistory.value = history
  uni.setStorageSync('scan_history', history)
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 40rpx;
}

.scan-area {
  background: #fff;
  border-radius: 20rpx;
  padding: 80rpx;
  display: flex;
  justify-content: center;
  margin-bottom: 40rpx;
}

.scan-box {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.scan-icon {
  font-size: 120rpx;
  margin-bottom: 20rpx;
}

.scan-text {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.scan-subtext {
  font-size: 26rpx;
  color: #999;
  margin-top: 10rpx;
}

.input-area {
  background: #fff;
  border-radius: 20rpx;
  padding: 40rpx;
  margin-bottom: 40rpx;
}

.label {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 20rpx;
  display: block;
}

.input-row {
  display: flex;
  gap: 20rpx;
}

.input {
  flex: 1;
  height: 80rpx;
  background: #f5f5f5;
  border-radius: 10rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
}

.query-btn {
  width: 140rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: #667eea;
  color: #fff;
  font-size: 28rpx;
  border-radius: 10rpx;
  padding: 0;
}

.history {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.history-code {
  font-size: 28rpx;
  color: #333;
}

.history-arrow {
  font-size: 28rpx;
  color: #999;
}
</style>
