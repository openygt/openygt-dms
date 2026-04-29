<template>
  <view class="login-container">
    <view class="login-header">
      <image class="logo" src="/static/images/logo.png" mode="aspectFit"></image>
      <text class="title">OpenYGT PDA</text>
      <text class="subtitle">煎药室管理终端</text>
    </view>
    
    <view class="login-form">
      <view class="form-item">
        <text class="label">工号</text>
        <input 
          class="input" 
          v-model="form.userCode" 
          placeholder="请扫描或输入工号"
          confirm-type="next"
          @confirm="focusPassword"
        />
      </view>
      
      <view class="form-item">
        <text class="label">密码</text>
        <input 
          ref="passwordRef"
          class="input" 
          v-model="form.password" 
          password
          placeholder="请输入密码"
          confirm-type="done"
          @confirm="handleLogin"
        />
      </view>
      
      <view class="form-item">
        <text class="label">设备编码</text>
        <input 
          class="input" 
          v-model="form.deviceCode" 
          placeholder="请扫描设备码"
        />
      </view>
      
      <button 
        class="login-btn" 
        :loading="loading"
        :disabled="loading"
        @click="handleLogin"
      >
        登录
      </button>
    </view>
    
    <view class="login-tips">
      <text>提示：首次使用请扫描设备绑定码</text>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { post } from '../../utils/request.js'
import config from '../../utils/config.js'
import { startHeartbeat } from '../../utils/heartbeat.js'

const loading = ref(false)
const passwordRef = ref(null)

const form = reactive({
  userCode: '',
  password: '',
  deviceId: 1,
  deviceCode: ''
})

function focusPassword() {
  // 聚焦密码输入框
}

async function handleLogin() {
  if (!form.userCode.trim()) {
    uni.showToast({ title: '请输入工号', icon: 'none' })
    return
  }
  if (!form.password) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }
  if (!form.deviceCode.trim()) {
    uni.showToast({ title: '请输入设备编码', icon: 'none' })
    return
  }
  
  loading.value = true
  try {
    const res = await post('/auth/login', {
      userCode: form.userCode,
      password: form.password,
      deviceId: form.deviceId,
      deviceCode: form.deviceCode
    })
    
    uni.setStorageSync(config.tokenKey, res.token)
    uni.setStorageSync(config.userInfoKey, {
      userCode: res.userCode,
      deviceCode: res.deviceCode,
      recordId: res.recordId
    })
    
    startHeartbeat()
    
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 1000)
  } catch (e) {
    console.error('Login failed:', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #0066CC 0%, #003D7A 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 40rpx;
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 80rpx;
}

.logo {
  width: 160rpx;
  height: 160rpx;
  margin-bottom: 20rpx;
  background: #fff;
  border-radius: 20rpx;
}

.title {
  font-size: 48rpx;
  font-weight: bold;
  color: #fff;
}

.subtitle {
  font-size: 28rpx;
  color: rgba(255,255,255,0.8);
  margin-top: 10rpx;
}

.login-form {
  width: 100%;
  background: #fff;
  border-radius: 20rpx;
  padding: 40rpx;
  box-shadow: 0 10rpx 40rpx rgba(0,0,0,0.1);
}

.form-item {
  margin-bottom: 30rpx;
}

.label {
  font-size: 28rpx;
  color: #333;
  margin-bottom: 10rpx;
  display: block;
}

.input {
  height: 80rpx;
  background: #f5f5f5;
  border-radius: 10rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
}

.login-btn {
  margin-top: 40rpx;
  height: 90rpx;
  line-height: 90rpx;
  background: #0066CC;
  color: #fff;
  font-size: 32rpx;
  border-radius: 10rpx;
}

.login-btn[disabled] {
  background: #a0a0a0;
}

.login-tips {
  margin-top: 40rpx;
}

.login-tips text {
  font-size: 24rpx;
  color: rgba(255,255,255,0.7);
}
</style>
