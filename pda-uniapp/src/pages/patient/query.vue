<template>
  <view class="container">
    <!-- 顶部标题 -->
    <view class="header">
      <text class="header-title">处方进度查询</text>
      <text class="header-sub">扫码或输入手机号查询煎药进度</text>
    </view>

    <!-- 查询方式切换 -->
    <view class="tab-bar">
      <view
        class="tab-item"
        :class="{ active: queryMode === 'scan' }"
        @click="queryMode = 'scan'"
      >扫码查询</view>
      <view
        class="tab-item"
        :class="{ active: queryMode === 'phone' }"
        @click="queryMode = 'phone'"
      >手机号查询</view>
    </view>

    <!-- 扫码查询 -->
    <view class="query-card" v-if="queryMode === 'scan'">
      <view class="scan-box" @click="handleScan">
        <text class="scan-icon">📷</text>
        <text class="scan-text">点击扫描处方码/药袋码</text>
      </view>
      <view class="or-divider">或</view>
      <ygt-scan-input
        v-model="barcode"
        placeholder="请输入条码号"
        @confirm="doQuery"
        @scan="handleScan"
      />
      <button class="query-btn" @click="doQuery">查询进度</button>
    </view>

    <!-- 手机号查询 -->
    <view class="query-card" v-else>
      <view class="form-row">
        <text class="form-label">手机号</text>
        <input
          class="form-input"
          v-model="phone"
          type="number"
          maxlength="11"
          placeholder="请输入患者手机号"
        />
      </view>
      <view class="form-row">
        <text class="form-label">验证码</text>
        <view class="code-row">
          <input
            class="form-input code-input"
            v-model="verifyCode"
            type="number"
            maxlength="6"
            placeholder="请输入验证码"
          />
          <button
            class="code-btn"
            :disabled="codeCountdown > 0 || sendingCode"
            @click="sendVerifyCode"
          >{{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}</button>
        </view>
      </view>
      <button class="query-btn" @click="doQueryByPhone">查询进度</button>
    </view>

    <!-- 查询结果 -->
    <view class="result-area" v-if="result.prescriptionId">
      <!-- 处方基本信息 -->
      <view class="result-card">
        <view class="result-header">
          <text class="result-patient">{{ result.patientName || '未知患者' }}</text>
          <text class="result-status" :style="{ color: getStatusColor(result.status) }">{{ result.statusName || result.status }}</text>
        </view>
        <view class="result-info">
          <view class="result-row">
            <text class="result-label">处方号</text>
            <text class="result-value">{{ result.prescriptionNumber || '-' }}</text>
          </view>
          <view class="result-row">
            <text class="result-label">医院</text>
            <text class="result-value">{{ result.hospitalName || '-' }}</text>
          </view>
          <view class="result-row">
            <text class="result-label">剂数</text>
            <text class="result-value">{{ result.repetition || '-' }} 剂</text>
          </view>
          <view class="result-row">
            <text class="result-label">处方日期</text>
            <text class="result-value">{{ result.prescriptionDate || '-' }}</text>
          </view>
        </view>
      </view>

      <!-- 进度时间轴 -->
      <view class="result-card">
        <text class="result-title">煎药进度</text>
        <view class="patient-timeline">
          <view
            class="pt-node"
            v-for="(step, idx) in result.steps"
            :key="idx"
            :class="{
              'pt-done': step.completed,
              'pt-current': step.current,
              'pt-pending': !step.completed && !step.current
            }"
          >
            <view class="pt-line" v-if="idx > 0"></view>
            <view class="pt-dot"></view>
            <view class="pt-body">
              <text class="pt-label">{{ step.label }}</text>
              <text class="pt-time" v-if="step.time">{{ step.time }}</text>
              <text class="pt-hint" v-else-if="step.current">进行中</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 预计完成与取药信息 -->
      <view class="result-card">
        <text class="result-title">取药信息</text>
        <view class="pickup-info">
          <view class="pickup-row">
            <text class="pickup-label">预计完成</text>
            <text class="pickup-value highlight">{{ result.estimatedFinishTime || '计算中...' }}</text>
          </view>
          <view class="pickup-row">
            <text class="pickup-label">取药窗口</text>
            <text class="pickup-value">{{ result.pickupWindow || '-' }}</text>
          </view>
          <view class="pickup-row" v-if="result.deliveryType">
            <text class="pickup-label">配送方式</text>
            <text class="pickup-value">{{ result.deliveryType === 'SELF' ? '自取' : (result.deliveryType === 'DELIVERY' ? '配送' : result.deliveryType) }}</text>
          </view>
          <view class="pickup-row" v-if="result.deliveryAddress">
            <text class="pickup-label">配送地址</text>
            <text class="pickup-value">{{ result.deliveryAddress }}</text>
          </view>
        </view>
      </view>
    </view>

    <ygt-empty v-else-if="hasQueried" text="未查询到处方信息" />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { get, post } from '../../utils/request.js'
import { startScan } from '../../utils/scan.js'

const queryMode = ref('scan')
const barcode = ref('')
const phone = ref('')
const verifyCode = ref('')
const codeCountdown = ref(0)
const sendingCode = ref(false)
const result = ref({})
const hasQueried = ref(false)

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

function handleScan() {
  startScan({ onlyFromCamera: true, scanType: ['qrCode', 'barCode'] }).then(code => {
    barcode.value = code
    doQuery()
  }).catch(() => {})
}

async function doQuery() {
  if (!barcode.value.trim()) {
    uni.showToast({ title: '请输入条码', icon: 'none' })
    return
  }
  hasQueried.value = true
  try {
    uni.showLoading({ title: '查询中' })
    const res = await get('/patient/query', { barcode: barcode.value })
    result.value = res || {}
  } catch (e) {
    uni.showToast({ title: e.message || '查询失败', icon: 'none' })
    result.value = {}
  } finally {
    uni.hideLoading()
  }
}

async function sendVerifyCode() {
  if (!phone.value.trim() || phone.value.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  sendingCode.value = true
  try {
    await post('/patient/verify-code', { phone: phone.value })
    uni.showToast({ title: '验证码已发送', icon: 'success' })
    codeCountdown.value = 60
    const timer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    uni.showToast({ title: e.message || '发送失败', icon: 'none' })
  } finally {
    sendingCode.value = false
  }
}

async function doQueryByPhone() {
  if (!phone.value.trim() || phone.value.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  if (!verifyCode.value.trim()) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return
  }
  hasQueried.value = true
  try {
    uni.showLoading({ title: '查询中' })
    const res = await get('/patient/query-by-phone', { phone: phone.value, code: verifyCode.value })
    result.value = res || {}
  } catch (e) {
    uni.showToast({ title: e.message || '查询失败', icon: 'none' })
    result.value = {}
  } finally {
    uni.hideLoading()
  }
}
</script>

<style scoped>
.container { min-height: 100vh; background: #f5f5f5; padding-bottom: 40rpx; }
.header { background: linear-gradient(135deg, #0066CC 0%, #003D7A 100%); padding: 40rpx 30rpx; text-align: center; }
.header-title { font-size: 40rpx; color: #fff; font-weight: 600; display: block; }
.header-sub { font-size: 26rpx; color: rgba(255,255,255,0.8); margin-top: 12rpx; display: block; }

.tab-bar { display: flex; background: #fff; margin: 20rpx; border-radius: 16rpx; overflow: hidden; }
.tab-item { flex: 1; text-align: center; padding: 24rpx 0; font-size: 30rpx; color: #666; }
.tab-item.active { color: #0066CC; font-weight: 600; background: #e3f2fd; }

.query-card { background: #fff; margin: 0 20rpx 20rpx; border-radius: 16rpx; padding: 30rpx; }
.scan-box { height: 240rpx; border: 2rpx dashed #0066CC; border-radius: 16rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; background: #f0f7ff; }
.scan-icon { font-size: 64rpx; margin-bottom: 16rpx; }
.scan-text { font-size: 30rpx; color: #0066CC; }
.or-divider { text-align: center; font-size: 26rpx; color: #999; margin: 20rpx 0; }
.query-btn { height: 88rpx; background: #0066CC; color: #fff; font-size: 30rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; margin-top: 20rpx; }

.form-row { margin-bottom: 24rpx; }
.form-label { font-size: 28rpx; color: #555; margin-bottom: 12rpx; display: block; }
.form-input { height: 88rpx; background: #f5f5f5; border-radius: 12rpx; padding: 0 24rpx; font-size: 28rpx; }
.code-row { display: flex; gap: 16rpx; }
.code-input { flex: 1; }
.code-btn { width: 200rpx; height: 88rpx; background: #0066CC; color: #fff; font-size: 26rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; padding: 0; }
.code-btn[disabled] { background: #99c2e6; }

.result-area { padding: 0 20rpx; }
.result-card { background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 20rpx; }
.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; border-bottom: 1rpx solid #f0f0f0; padding-bottom: 20rpx; }
.result-patient { font-size: 34rpx; font-weight: 600; color: #333; }
.result-status { font-size: 28rpx; font-weight: 500; }
.result-info { display: flex; flex-direction: column; gap: 12rpx; }
.result-row { display: flex; justify-content: space-between; }
.result-label { font-size: 28rpx; color: #666; }
.result-value { font-size: 28rpx; color: #333; font-weight: 500; }
.result-title { font-size: 30rpx; font-weight: 600; color: #333; margin-bottom: 20rpx; display: block; }

/* 患者端时间轴 */
.patient-timeline { display: flex; flex-direction: column; }
.pt-node { display: flex; align-items: center; gap: 20rpx; padding: 16rpx 0; position: relative; }
.pt-line { position: absolute; left: 20rpx; top: -8rpx; width: 2rpx; height: 16rpx; background: #e0e0e0; }
.pt-dot { width: 40rpx; height: 40rpx; border-radius: 50%; background: #e0e0e0; flex-shrink: 0; position: relative; }
.pt-done .pt-dot { background: #4caf50; }
.pt-done .pt-dot::after { content: '✓'; position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%); color: #fff; font-size: 22rpx; font-weight: 600; }
.pt-current .pt-dot { background: #0066CC; }
.pt-current .pt-dot::after { content: ''; position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%); width: 12rpx; height: 12rpx; background: #fff; border-radius: 50%; }
.pt-done .pt-line { background: #4caf50; }
.pt-body { flex: 1; display: flex; flex-direction: column; }
.pt-label { font-size: 28rpx; color: #333; }
.pt-time { font-size: 24rpx; color: #4caf50; margin-top: 4rpx; }
.pt-hint { font-size: 24rpx; color: #0066CC; margin-top: 4rpx; }
.pt-pending .pt-label { color: #999; }
.pt-pending .pt-dot { background: #e0e0e0; }

/* 取药信息 */
.pickup-info { display: flex; flex-direction: column; gap: 16rpx; }
.pickup-row { display: flex; justify-content: space-between; align-items: center; }
.pickup-label { font-size: 28rpx; color: #666; }
.pickup-value { font-size: 28rpx; color: #333; font-weight: 500; }
.pickup-value.highlight { color: #0066CC; font-weight: 600; }
</style>
