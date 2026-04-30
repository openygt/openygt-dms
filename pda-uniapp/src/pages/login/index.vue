<template>
  <view class="login-container">
    <view class="login-header">
      <image class="logo" src="/static/images/logo.png" mode="aspectFit"></image>
      <text class="title">OpenYGT PDA</text>
      <text class="subtitle">煎药室管理终端</text>
    </view>

    <!-- Tab 切换 -->
    <view class="login-tabs">
      <view class="tab-item" :class="{ active: loginType === 'scan' }" @click="loginType = 'scan'" data-testid="tab-scan">扫码登录</view>
      <view class="tab-item" :class="{ active: loginType === 'account' }" @click="loginType = 'account'" data-testid="tab-account">账号登录</view>
    </view>

    <!-- 扫码登录面板 -->
    <view class="login-form" v-if="loginType === 'scan'">
      <view class="scan-panel" @click="handleScan">
        <text class="scan-icon">📷</text>
        <text class="scan-text" data-testid="input-scan-code">{{ scanCode ? scanCode : '请扫描员工条码' }}</text>
      </view>
      <!-- 测试用隐藏输入：用于 E2E 模拟扫码 -->
      <input type="text" v-model="scanCode" class="scan-hidden-input" data-testid="input-scan-code-hidden" />
      <view class="form-item">
        <text class="label">设备编码</text>
        <input class="input" v-model="form.deviceCode" placeholder="请扫描设备码" data-testid="input-scan-device-code" />
      </view>
      <button class="login-btn" :loading="loading" :disabled="loading || !scanCode" @click="handleScanLogin" data-testid="btn-scan-login">登录</button>
    </view>

    <!-- 账号登录面板 -->
    <view class="login-form" v-else>
      <view class="form-item">
        <text class="label">工号</text>
        <input class="input" v-model="form.userCode" placeholder="请扫描或输入工号" data-testid="input-username" confirm-type="next" @confirm="focusPassword" />
      </view>
      <view class="form-item">
        <text class="label">密码</text>
        <input ref="passwordRef" class="input" v-model="form.password" password placeholder="请输入密码" data-testid="input-password" confirm-type="done" @confirm="handleLogin" />
      </view>
      <view class="form-item">
        <text class="label">设备编码</text>
        <input class="input" v-model="form.deviceCode" placeholder="请扫描设备码" data-testid="input-device-code" />
      </view>
      <button class="login-btn" :loading="loading" :disabled="loading" @click="handleLogin" data-testid="btn-login">登录</button>
    </view>

    <view class="version-info">
      <text>v{{ version }}</text>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { post } from '../../utils/request.js'
import config from '../../utils/config.js'
import { startHeartbeat } from '../../utils/heartbeat.js'

const loading = ref(false)
const loginType = ref('scan')
const scanCode = ref('')
const version = ref('1.0.0')
const form = reactive({ userCode: '', password: '', deviceId: 1, deviceCode: '' })

onMounted(() => {
  checkVersion()
})

async function checkVersion() {
  try {
    const res = await post('/config/version')
    version.value = res.version || '1.0.0'
  } catch (e) {
    console.log('版本检查失败', e)
  }
}

function handleScan() {
  uni.scanCode({
    onlyFromCamera: true,
    scanType: ['qrCode', 'barCode'],
    success: (res) => {
      scanCode.value = res.result
      uni.vibrateShort()
    },
    fail: () => {
      uni.showToast({ title: '扫码失败', icon: 'none' })
    }
  })
}

async function handleScanLogin() {
  if (!scanCode.value) { uni.showToast({ title: '请先扫描员工条码', icon: 'none' }); return }
  if (!form.deviceCode.trim()) { uni.showToast({ title: '请输入设备编码', icon: 'none' }); return }
  loading.value = true
  try {
    const res = await post('/auth/scan-login', { scanCode: scanCode.value, deviceCode: form.deviceCode })
    doLoginSuccess(res)
  } catch (e) {
    uni.showToast({ title: e.message || '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function handleLogin() {
  if (!form.userCode.trim()) { uni.showToast({ title: '请输入工号', icon: 'none' }); return }
  if (!form.password) { uni.showToast({ title: '请输入密码', icon: 'none' }); return }
  if (!form.deviceCode.trim()) { uni.showToast({ title: '请输入设备编码', icon: 'none' }); return }
  loading.value = true
  try {
    const res = await post('/auth/login', { userCode: form.userCode, password: form.password, deviceId: form.deviceId, deviceCode: form.deviceCode })
    doLoginSuccess(res)
  } catch (e) {
    uni.showToast({ title: e.message || '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function doLoginSuccess(res) {
  uni.setStorageSync(config.tokenKey, res.token)
  uni.setStorageSync(config.userInfoKey, {
    userCode: res.userCode,
    userName: res.userName || res.userCode,
    deviceCode: res.deviceCode,
    recordId: res.recordId
  })
  startHeartbeat()
  uni.showToast({ title: (res.userName || res.userCode) + ' 登录成功', icon: 'success' })
  setTimeout(() => { uni.switchTab({ url: '/pages/index/index' }) }, 1000)
}

function focusPassword() {
  // 在 uni-app 中 ref 获取组件不太方便，这里简单处理
}
</script>

<style scoped>
.login-container { min-height: 100vh; background: linear-gradient(135deg, #0066CC 0%, #003D7A 100%); display: flex; flex-direction: column; align-items: center; padding: 60rpx 40rpx; }
.login-header { display: flex; flex-direction: column; align-items: center; margin-bottom: 40rpx; }
.logo { width: 120rpx; height: 120rpx; margin-bottom: 20rpx; }
.title { font-size: 40rpx; font-weight: 600; color: #fff; }
.subtitle { font-size: 28rpx; color: rgba(255,255,255,0.8); margin-top: 8rpx; }
.login-tabs { display: flex; background: rgba(255,255,255,0.15); border-radius: 12rpx; margin-bottom: 40rpx; overflow: hidden; }
.tab-item { padding: 20rpx 60rpx; font-size: 30rpx; color: rgba(255,255,255,0.7); }
.tab-item.active { background: rgba(255,255,255,0.25); color: #fff; font-weight: 600; }
.login-form { width: 100%; background: #fff; border-radius: 20rpx; padding: 40rpx; }
.scan-panel { height: 240rpx; border: 2rpx dashed #0066CC; border-radius: 16rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; margin-bottom: 30rpx; background: #f0f7ff; }
.scan-icon { font-size: 64rpx; margin-bottom: 12rpx; }
.scan-text { font-size: 32rpx; color: #0066CC; }
.form-item { margin-bottom: 30rpx; }
.label { font-size: 28rpx; color: #333; margin-bottom: 12rpx; display: block; }
.input { height: 88rpx; background: #f5f5f5; border-radius: 12rpx; padding: 0 24rpx; font-size: 30rpx; }
.login-btn { height: 96rpx; background: #0066CC; color: #fff; font-size: 34rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; margin-top: 20rpx; }
.login-btn[disabled] { background: #99c2e6; }
.version-info { margin-top: 40rpx; color: rgba(255,255,255,0.6); font-size: 24rpx; }
.scan-hidden-input { position: absolute; opacity: 0; height: 0; width: 0; pointer-events: none; }
</style>
