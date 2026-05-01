<template>
  <view class="password-page">
    <view class="form-card">
      <view class="form-item">
        <text class="form-label">旧密码</text>
        <input class="form-input" v-model="form.oldPassword" type="password" placeholder="请输入旧密码" />
      </view>
      <view class="form-item">
        <text class="form-label">新密码</text>
        <input class="form-input" v-model="form.newPassword" type="password" placeholder="6-32位字符" />
      </view>
      <view class="form-item">
        <text class="form-label">确认密码</text>
        <input class="form-input" v-model="form.confirmPassword" type="password" placeholder="再次输入新密码" />
      </view>
    </view>

    <view class="submit-area">
      <button class="submit-btn" :class="{ disabled: !canSubmit }" :disabled="!canSubmit" @click="submit">
        确认修改
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { post } from '../../utils/request.js'
import config from '../../utils/config.js'

const form = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const canSubmit = computed(() => {
  return form.value.oldPassword && form.value.newPassword && form.value.confirmPassword
})

async function submit() {
  if (form.value.newPassword.length < 6) {
    uni.showToast({ title: '新密码至少6位', icon: 'none' })
    return
  }
  if (form.value.newPassword !== form.value.confirmPassword) {
    uni.showToast({ title: '两次输入不一致', icon: 'none' })
    return
  }

  uni.showLoading({ title: '修改中...' })
  try {
    await post('/auth/change-password', {
      oldPassword: form.value.oldPassword,
      newPassword: form.value.newPassword,
      confirmPassword: form.value.confirmPassword
    })
    uni.hideLoading()
    uni.showToast({ title: '修改成功，请重新登录', icon: 'success' })
    setTimeout(() => {
      uni.removeStorageSync(config.tokenKey)
      uni.removeStorageSync(config.userInfoKey)
      uni.reLaunch({ url: '/pages/login/index' })
    }, 1500)
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '修改失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
@import "../../uni.scss";

.password-page {
  min-height: 100vh;
  padding: 32rpx;
}

.form-card {
  background: #fff;
  border-radius: $ygt-radius-lg;
  padding: 24rpx 32rpx;
  box-shadow: $ygt-shadow-card;
}

.form-item {
  display: flex;
  flex-direction: column;
  padding: 24rpx 0;
  border-bottom: 1rpx solid $ygt-gray-100;
}

.form-item:last-child {
  border-bottom: none;
}

.form-label {
  font-size: 28rpx;
  color: $ygt-gray-700;
  margin-bottom: 16rpx;
}

.form-input {
  height: 80rpx;
  background: $ygt-gray-50;
  border-radius: $ygt-radius-md;
  padding: 0 24rpx;
  font-size: 30rpx;
}

.submit-area {
  margin-top: 48rpx;
}

.submit-btn {
  height: 96rpx;
  line-height: 96rpx;
  background: $ygt-primary;
  color: #fff;
  font-size: 32rpx;
  border-radius: $ygt-radius-lg;
  font-weight: 600;
}

.submit-btn.disabled {
  background: $ygt-gray-300;
}
</style>
