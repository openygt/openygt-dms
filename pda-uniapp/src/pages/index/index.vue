<template>
  <view class="container">
    <!-- 待同步提示条 -->
    <view class="sync-banner" v-if="pendingCount > 0" @click="doSync">
      <text class="sync-text">⚠ 有 {{ pendingCount }} 条操作待同步</text>
      <text class="sync-btn">立即同步</text>
    </view>

    <view class="header">
      <view class="greeting-area">
        <text class="greeting" data-testid="user-name">{{ greeting }}，{{ userInfo.userName || userInfo.userCode || '操作员' }}</text>
        <text class="device" data-testid="device-code">设备：{{ userInfo.deviceCode || '未绑定' }}</text>
      </view>
    </view>

    <!-- 主扫码区 -->
    <view class="main-scan" @click="goScan" data-testid="btn-scan">
      <view class="scan-circle">
        <text class="scan-icon">📷</text>
        <text class="scan-label">扫描处方码</text>
      </view>
    </view>

    <view class="sub-actions">
      <view class="sub-btn" @click="goScanManual">手动输入</view>
      <view class="sub-btn primary" @click="goReprint" data-testid="btn-reprint">重打印</view>
    </view>

    <!-- 快捷操作 -->
    <view class="quick-actions">
      <view class="action-grid">
        <view class="action-item" @click="goScan" data-testid="btn-scan-query">
          <view class="action-icon scan">📷</view>
          <text class="action-text">扫码查询</text>
        </view>
        <view class="action-item" @click="goConfirm" data-testid="btn-confirm">
          <view class="action-icon confirm">✓</view>
          <text class="action-text">工序确认</text>
        </view>
        <view class="action-item" @click="goPhoto" data-testid="btn-photo">
          <view class="action-icon photo">📸</view>
          <text class="action-text">拍照上传</text>
        </view>
        <view class="action-item" @click="goLog" data-testid="btn-log">
          <view class="action-icon log">📋</view>
          <text class="action-text">操作日志</text>
        </view>
        <view class="action-item" @click="goVoice" data-testid="btn-voice">
          <view class="action-icon voice">🔊</view>
          <text class="action-text">语音提醒</text>
        </view>
        <view class="action-item" @click="goPatientQuery" data-testid="btn-patient" v-if="canPatientQuery">
          <view class="action-icon patient">👤</view>
          <text class="action-text">患者查询</text>
        </view>
        <view class="action-item" @click="goShelf" data-testid="btn-shelf">
          <view class="action-icon shelf">📦</view>
          <text class="action-text">货架管理</text>
        </view>
      </view>
    </view>

    <!-- 最近处理 -->
    <view class="recent-section" v-if="recentTasks.length > 0">
      <view class="section-header">
        <text class="section-title">最近处理</text>
        <text class="section-more" @click="goLog">查看更多</text>
      </view>
      <view class="recent-list">
        <view class="recent-item" v-for="item in recentTasks" :key="item.taskId" @click="goTaskDetail(item.barcode)" data-testid="recent-task-item">
          <view class="recent-info">
            <text class="recent-patient">{{ item.patientName || '未知患者' }}</text>
            <text class="recent-barcode">条码: {{ item.barcode }}</text>
          </view>
          <view class="recent-meta">
            <text class="recent-status" :style="{ color: getStatusColor(item.status) }">{{ item.statusName }}</text>
            <text class="recent-time">{{ formatTime(item.updatedAt) }}</text>
          </view>
        </view>
      </view>
    </view>
    <ygt-empty v-else text="暂无最近处理任务" />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import config from '../../utils/config.js'
import { getQueue } from '../../utils/storage.js'
import { syncAll } from '../../utils/sync.js'
import { get } from '../../utils/request.js'

const userInfo = ref(uni.getStorageSync(config.userInfoKey) || {})
const pendingCount = ref(0)
const recentTasks = ref([])

const canPatientQuery = computed(() => {
  // 根据用户角色判断是否有患者查询权限（管理员/客服/药房窗口）
  const roles = userInfo.value.roles || []
  return roles.includes('ADMIN') || roles.includes('RECEPTION') || roles.includes('PHARMACY')
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

onShow(() => {
  userInfo.value = uni.getStorageSync(config.userInfoKey) || {}
  pendingCount.value = getQueue().length
  loadRecentTasks()
})

async function loadRecentTasks() {
  try {
    const res = await get('/tasks/recent', { limit: config.recentLimit })
    recentTasks.value = res || []
  } catch (e) {
    console.error('加载最近任务失败', e)
  }
}

function getStatusColor(status) {
  const colors = {
    PENDING: '#999', SOAKING: '#2196f3', SOAKED: '#64b5f6',
    DECOCTING: '#ff9800', DECOCTED: '#ffb74d', POURING: '#9c27b0',
    POURED: '#ba68c8', PACKAGING: '#795548', PACKAGED: '#a1887f',
    LABELING: '#607d8b', LABELED: '#78909c', INSPECTING: '#e91e63',
    COMPLETED: '#4caf50', CANCELLED: '#9e9e9e'
  }
  return colors[status] || '#999'
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  return date.getHours().toString().padStart(2, '0') + ':' + date.getMinutes().toString().padStart(2, '0')
}

function goScan() { uni.switchTab({ url: '/pages/task/scan' }) }
function goScanManual() { uni.navigateTo({ url: '/pages/task/scan?mode=manual' }) }
function goReprint() { uni.navigateTo({ url: '/pages/reprint/index' }) }
function goConfirm() { uni.switchTab({ url: '/pages/task/scan' }) }
function goPhoto() { uni.switchTab({ url: '/pages/task/scan' }) }
function goLog() { uni.switchTab({ url: '/pages/log/list' }) }
function goVoice() { uni.navigateTo({ url: '/pages/voice/index' }) }
function goPatientQuery() { uni.navigateTo({ url: '/pages/patient/query' }) }
function goShelf() { uni.navigateTo({ url: '/pages/shelf/index' }) }
function goTaskDetail(barcode) {
  uni.navigateTo({ url: `/pages/task/detail?barcode=${barcode}` })
}
async function doSync() {
  try {
    await syncAll()
    pendingCount.value = 0
    uni.showToast({ title: '同步成功', icon: 'success' })
    loadRecentTasks()
  } catch (e) {
    uni.showToast({ title: '同步失败', icon: 'none' })
  }
}
</script>

<style scoped>
.container { min-height: 100vh; background: #f5f5f5; }
.sync-banner { background: #ffebee; padding: 20rpx 30rpx; display: flex; justify-content: space-between; align-items: center; }
.sync-text { color: #c62828; font-size: 28rpx; }
.sync-btn { color: #c62828; font-size: 28rpx; font-weight: 600; }
.header { background: linear-gradient(135deg, #0066CC 0%, #003D7A 100%); padding: 40rpx 30rpx 60rpx; }
.greeting { font-size: 36rpx; color: #fff; font-weight: 600; }
.device { font-size: 26rpx; color: rgba(255,255,255,0.8); margin-top: 8rpx; display: block; }
.main-scan { display: flex; justify-content: center; margin-top: -40rpx; padding: 0 30rpx; }
.scan-circle { width: 280rpx; height: 280rpx; border-radius: 50%; background: linear-gradient(135deg, #0066CC, #003D7A); display: flex; flex-direction: column; align-items: center; justify-content: center; box-shadow: 0 8rpx 32rpx rgba(0,102,204,0.3); }
.scan-icon { font-size: 80rpx; margin-bottom: 16rpx; }
.scan-label { font-size: 32rpx; color: #fff; font-weight: 600; }
.sub-actions { display: flex; justify-content: center; gap: 24rpx; margin-top: 30rpx; padding: 0 30rpx; }
.sub-btn { padding: 16rpx 40rpx; background: #fff; border-radius: 12rpx; font-size: 28rpx; color: #333; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.06); }
.sub-btn.primary { background: #0066CC; color: #fff; }
.quick-actions { padding: 30rpx; }
.action-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20rpx; background: #fff; border-radius: 16rpx; padding: 30rpx 20rpx; }
.action-item { display: flex; flex-direction: column; align-items: center; }
.action-icon { width: 88rpx; height: 88rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 40rpx; margin-bottom: 12rpx; }
.action-icon.scan { background: #e3f2fd; }
.action-icon.confirm { background: #e8f5e9; }
.action-icon.photo { background: #fff3e0; }
.action-icon.log { background: #f3e5f5; }
.action-icon.voice { background: #e8eaf6; }
.action-icon.patient { background: #fce4ec; }
.action-icon.shelf { background: #e0f2f1; }
.action-text { font-size: 26rpx; color: #555; }
.recent-section { padding: 0 30rpx 30rpx; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.section-title { font-size: 32rpx; font-weight: 600; color: #333; }
.section-more { font-size: 26rpx; color: #0066CC; }
.recent-list { background: #fff; border-radius: 16rpx; overflow: hidden; }
.recent-item { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 30rpx; border-bottom: 1rpx solid #f0f0f0; }
.recent-item:last-child { border-bottom: none; }
.recent-patient { font-size: 30rpx; color: #333; font-weight: 500; display: block; }
.recent-barcode { font-size: 24rpx; color: #999; margin-top: 6rpx; display: block; }
.recent-meta { text-align: right; }
.recent-status { font-size: 26rpx; font-weight: 500; display: block; }
.recent-time { font-size: 22rpx; color: #bbb; margin-top: 6rpx; display: block; }
</style>
