<template>
  <view class="scan-page" :class="fontSizeClass">
    <!-- 扫码主区域 -->
    <view class="scan-hero" @click="doScan" data-testid="scan-hero">
      <view class="scan-pulse" :class="{ active: scanning }">
        <view class="scan-inner">
          <text class="scan-icon">□</text>
          <text class="scan-text">点击扫码</text>
          <text class="scan-subtext">支持条形码 / 二维码</text>
        </view>
      </view>
    </view>

    <!-- 手动输入区 -->
    <view class="scan-input-area">
      <input
        class="scan-input ygt-num"
        v-model="barcode"
        placeholder="请输入任务编号"
        data-testid="input-barcode"
        confirm-type="search"
        focus
        @confirm="queryTask"
      />
      <button class="ygt-btn-primary scan-btn" @click="queryTask" data-testid="btn-query">
        <text class="btn-text">查询</text>
      </button>
    </view>

    <!-- 扫码成功脉冲反馈 -->
    <view class="scan-success-ring" v-if="showSuccessRing">
      <view class="ring-ripple"></view>
      <view class="ring-ripple ring-ripple-delay"></view>
    </view>

    <!-- 历史记录 -->
    <view class="scan-history" v-if="scanHistory.length > 0">
      <view class="history-header">
        <text class="section-title">最近扫码</text>
        <text class="history-clear" @click="clearHistory">清空</text>
      </view>
      <view class="history-list">
        <view
          class="history-item"
          v-for="(item, idx) in scanHistory"
          :key="idx"
          @click="repeatScan(item.code)"
          data-testid="history-item"
        >
          <view class="history-left">
            <view class="history-dot" :class="{ success: item.success }"></view>
            <text class="history-code ygt-num">{{ item.code }}</text>
          </view>
          <text class="history-time">{{ item.timeAgo }}</text>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="scan-empty" v-else>
      <text class="empty-hint">扫码历史将显示在这里</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { get } from '../../utils/request.js'

const barcode = ref('')
const scanning = ref(false)
const showSuccessRing = ref(false)
const scanHistory = ref([])
const fontSizeClass = ref('')

onMounted(() => {
  loadHistory()
  // 监听字号变化
  uni.$on('ygt-font-size-change', (size) => {
    fontSizeClass.value = size === 'normal' ? '' : `font-${size}`
  })
})

function loadHistory() {
  const raw = uni.getStorageSync('scan_history') || []
  scanHistory.value = raw.map(item => {
    if (typeof item === 'string') {
      return { code: item, time: Date.now(), success: true }
    }
    return item
  })
}

function saveHistory() {
  uni.setStorageSync('scan_history', scanHistory.value)
}

function doScan() {
  scanning.value = true
  uni.scanCode({
    scanType: ['barCode', 'qrCode'],
    success: (res) => {
      barcode.value = res.result
      queryTask()
    },
    fail: (err) => {
      scanning.value = false
      console.error('Scan failed:', err)
      if (uni.$ygtFeedback) uni.$ygtFeedback.error()
      uni.showToast({ title: '扫码失败', icon: 'none' })
    },
    complete: () => {
      scanning.value = false
    }
  })
}

async function queryTask() {
  if (!barcode.value.trim()) {
    if (uni.$ygtFeedback) uni.$ygtFeedback.warning()
    uni.showToast({ title: '请输入或扫描任务编号', icon: 'none' })
    return
  }

  uni.showLoading({ title: '查询中...' })
  try {
    const task = await get(`/task/${barcode.value}`)
    uni.hideLoading()

    // 成功反馈
    if (uni.$ygtFeedback) uni.$ygtFeedback.scanSuccess()
    showSuccessPulse()

    addToHistory(barcode.value, true)
    uni.navigateTo({ url: `/pages/task/detail?taskId=${task.taskId}&barcode=${barcode.value}` })
  } catch (e) {
    uni.hideLoading()
    if (uni.$ygtFeedback) uni.$ygtFeedback.error()
    uni.showToast({ title: '查询失败：' + (e.message || '未知错误'), icon: 'none' })
    addToHistory(barcode.value, false)
  }
}

function showSuccessPulse() {
  showSuccessRing.value = true
  setTimeout(() => { showSuccessRing.value = false }, 1500)
}

function addToHistory(code, success) {
  let history = scanHistory.value.filter(h => h.code !== code)
  history.unshift({ code, time: Date.now(), success })
  if (history.length > 10) history = history.slice(0, 10)
  scanHistory.value = history
  saveHistory()
}

function repeatScan(code) {
  barcode.value = code
  queryTask()
}

function clearHistory() {
  uni.showModal({
    title: '确认清空',
    content: '将清空所有扫码历史记录',
    success: (res) => {
      if (res.confirm) {
        scanHistory.value = []
        uni.removeStorageSync('scan_history')
      }
    }
  })
}

function timeAgo(ts) {
  const diff = Date.now() - ts
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return min + '分钟前'
  const hour = Math.floor(min / 60)
  if (hour < 24) return hour + '小时前'
  return Math.floor(hour / 24) + '天前'
}
</script>

<style lang="scss" scoped>
@import "../../uni.scss";

.scan-page {
  min-height: 100vh;
  padding: 32rpx;
}

.scan-hero {
  height: 50vh;
  min-height: 400rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, $ygt-primary 0%, $ygt-primary-dark 100%);
  border-radius: $ygt-radius-lg;
  margin-bottom: 32rpx;
  position: relative;
  overflow: hidden;
}

.scan-pulse {
  display: flex;
  align-items: center;
  justify-content: center;
}

.scan-pulse.active {
  animation: pulse-scale 1s ease-in-out infinite;
}

.scan-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.scan-icon {
  font-weight: 500;
  font-size: 120rpx;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 20rpx;
}

.scan-text {
  font-size: 40rpx;
  font-weight: 600;
  color: #fff;
}

.scan-subtext {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 12rpx;
}

.scan-input-area {
  display: flex;
  gap: 16rpx;
  margin-bottom: 32rpx;
}

.scan-input {
  flex: 1;
  height: 96rpx;
  background: #fff;
  border-radius: $ygt-radius-md;
  padding: 0 32rpx;
  font-size: 32rpx;
  border: 2rpx solid $ygt-gray-200;
  transition: border-color 0.2s;
}
.scan-input:focus {
  border-color: $ygt-primary;
}

.scan-btn {
  width: 200rpx;
  height: 96rpx;
  line-height: 96rpx;
  font-size: 32rpx;
}

/* 成功脉冲 */
.scan-success-ring {
  position: fixed;
  top: 25vh;
  left: 50%;
  transform: translateX(-50%);
  pointer-events: none;
  z-index: 100;
}

.ring-ripple {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 200rpx;
  height: 200rpx;
  margin-top: -100rpx;
  margin-left: -100rpx;
  border-radius: 50%;
  border: 4rpx solid $ygt-success;
  animation: ripple-out 1s ease-out forwards;
  opacity: 0.6;
}

.ring-ripple-delay {
  animation-delay: 0.3s;
}

@keyframes ripple-out {
  0% { transform: scale(0.5); opacity: 0.6; }
  100% { transform: scale(2); opacity: 0; }
}

@keyframes pulse-scale {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

/* 历史 */
.scan-history {
  background: #fff;
  border-radius: $ygt-radius-lg;
  padding: 32rpx;
  box-shadow: $ygt-shadow-card;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $ygt-gray-900;
}

.history-clear {
  font-size: 26rpx;
  color: $ygt-primary;
}

.history-list {
  display: flex;
  flex-direction: column;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid $ygt-gray-100;
}

.history-item:last-child {
  border-bottom: none;
}

.history-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.history-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: $ygt-danger;
}
.history-dot.success {
  background: $ygt-success;
}

.history-code {
  font-size: 30rpx;
  color: $ygt-gray-900;
  font-weight: 500;
}

.history-time {
  font-size: 24rpx;
  color: $ygt-gray-500;
}

.scan-empty {
  text-align: center;
  padding: 60rpx 0;
}

.empty-hint {
  font-size: 28rpx;
  color: $ygt-gray-500;
}

/* 字号适配 */
.scan-page.font-large .scan-text { font-size: 44rpx; }
.scan-page.font-large .scan-input { font-size: 36rpx; }
.scan-page.font-large .history-code { font-size: 34rpx; }

.scan-page.font-xlarge .scan-text { font-size: 48rpx; }
.scan-page.font-xlarge .scan-input { font-size: 40rpx; }
.scan-page.font-xlarge .history-code { font-size: 38rpx; }
</style>
