<template>
  <view class="container">
    <!-- 语音总开关 -->
    <view class="switch-card">
      <view class="switch-row">
        <text class="switch-label">语音播报开关</text>
        <switch :checked="voiceEnabled" @change="onToggleVoice" color="#0066CC" />
      </view>
      <text class="switch-hint">开启后，工序完成、异常等情况将自动语音提醒</text>
    </view>

    <!-- 语音设置 -->
    <view class="settings-card" v-if="voiceEnabled">
      <text class="card-title">播报设置</text>

      <view class="setting-item">
        <text class="setting-label">语速</text>
        <slider
          class="setting-slider"
          :value="rateValue"
          min="0.5"
          max="2"
          step="0.1"
          block-size="20"
          active-color="#0066CC"
          @change="onRateChange"
          show-value
        />
      </view>

      <view class="setting-item">
        <text class="setting-label">音量</text>
        <slider
          class="setting-slider"
          :value="volumeValue"
          min="0"
          max="1"
          step="0.1"
          block-size="20"
          active-color="#0066CC"
          @change="onVolumeChange"
          show-value
        />
      </view>

      <view class="setting-item">
        <text class="setting-label">音调</text>
        <slider
          class="setting-slider"
          :value="pitchValue"
          min="0.5"
          max="2"
          step="0.1"
          block-size="20"
          active-color="#0066CC"
          @change="onPitchChange"
          show-value
        />
      </view>

      <view class="setting-item">
        <text class="setting-label">语音类型</text>
        <view class="type-options">
          <view
            class="type-option"
            v-for="v in voiceList"
            :key="v.voiceURI"
            :class="{ active: selectedVoiceURI === v.voiceURI }"
            @click="selectVoice(v)"
          >
            <text class="type-name">{{ v.name }}</text>
            <text class="type-lang">{{ v.lang }}</text>
          </view>
          <view v-if="voiceList.length === 0" class="type-empty">当前环境不支持语音列表查询</view>
        </view>
      </view>
    </view>

    <!-- 测试区域 -->
    <view class="test-card" v-if="voiceEnabled">
      <text class="card-title">播报测试</text>
      <input
        class="test-input"
        v-model="testText"
        placeholder="输入要测试播报的内容"
      />
      <button class="test-btn" @click="doTestSpeak" :disabled="speaking">
        {{ speaking ? '播报中...' : '测试播报' }}
      </button>
    </view>

    <!-- 播报日志 -->
    <view class="log-card">
      <view class="log-header">
        <text class="card-title">播报日志</text>
        <text class="clear-btn" @click="clearLogs">清空</text>
      </view>
      <view class="log-list" v-if="logs.length > 0">
        <view
          class="log-item"
          v-for="(log, idx) in logs"
          :key="idx"
          :class="{ 'log-error': log.type === 'error' }"
        >
          <view class="log-top">
            <text class="log-time">{{ log.time }}</text>
            <text class="log-type" :class="'type-' + (log.type || 'info')">{{ log.typeText }}</text>
          </view>
          <text class="log-content">{{ log.content }}</text>
        </view>
      </view>
      <ygt-empty v-else text="暂无播报记录" />
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const voiceEnabled = ref(false)
const rateValue = ref(1.0)
const volumeValue = ref(1.0)
const pitchValue = ref(1.0)
const selectedVoiceURI = ref('')
const voiceList = ref([])
const testText = ref('泡药工序已完成，请进行头煎操作')
const speaking = ref(false)
const logs = ref([])

// 从本地存储读取配置
onMounted(() => {
  voiceEnabled.value = uni.getStorageSync('pda_voice_enabled') === '1'
  rateValue.value = parseFloat(uni.getStorageSync('pda_voice_rate') || '1')
  volumeValue.value = parseFloat(uni.getStorageSync('pda_voice_volume') || '1')
  pitchValue.value = parseFloat(uni.getStorageSync('pda_voice_pitch') || '1')
  selectedVoiceURI.value = uni.getStorageSync('pda_voice_uri') || ''
  logs.value = uni.getStorageSync('pda_voice_logs') || []

  // H5 环境加载语音列表
  // #ifdef H5
  loadVoices()
  // #endif
})

function loadVoices() {
  if (typeof window === 'undefined' || !window.speechSynthesis) return
  const voices = window.speechSynthesis.getVoices()
  if (voices && voices.length > 0) {
    voiceList.value = voices.filter(v => v.lang.startsWith('zh')).map(v => ({
      voiceURI: v.voiceURI,
      name: v.name,
      lang: v.lang
    }))
    if (!selectedVoiceURI.value && voiceList.value.length > 0) {
      selectedVoiceURI.value = voiceList.value[0].voiceURI
    }
  }
  window.speechSynthesis.onvoiceschanged = () => {
    const v = window.speechSynthesis.getVoices()
    voiceList.value = v.filter(x => x.lang.startsWith('zh')).map(x => ({
      voiceURI: x.voiceURI,
      name: x.name,
      lang: x.lang
    }))
  }
}

function onToggleVoice(e) {
  voiceEnabled.value = e.detail.value
  uni.setStorageSync('pda_voice_enabled', voiceEnabled.value ? '1' : '0')
  addLog(voiceEnabled.value ? '语音播报已开启' : '语音播报已关闭', 'info')
}

function onRateChange(e) {
  rateValue.value = e.detail.value
  uni.setStorageSync('pda_voice_rate', String(rateValue.value))
}

function onVolumeChange(e) {
  volumeValue.value = e.detail.value
  uni.setStorageSync('pda_voice_volume', String(volumeValue.value))
}

function onPitchChange(e) {
  pitchValue.value = e.detail.value
  uni.setStorageSync('pda_voice_pitch', String(pitchValue.value))
}

function selectVoice(v) {
  selectedVoiceURI.value = v.voiceURI
  uni.setStorageSync('pda_voice_uri', v.voiceURI)
  addLog(`已切换语音: ${v.name}`, 'info')
}

function doTestSpeak() {
  if (!testText.value.trim()) {
    uni.showToast({ title: '请输入测试内容', icon: 'none' })
    return
  }
  // #ifdef H5
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    const utter = new window.SpeechSynthesisUtterance(testText.value)
    utter.rate = rateValue.value
    utter.volume = volumeValue.value
    utter.pitch = pitchValue.value
    if (selectedVoiceURI.value) {
      const v = window.speechSynthesis.getVoices().find(x => x.voiceURI === selectedVoiceURI.value)
      if (v) utter.voice = v
    }
    utter.lang = 'zh-CN'
    utter.onstart = () => { speaking.value = true }
    utter.onend = () => { speaking.value = false }
    utter.onerror = (e) => {
      speaking.value = false
      addLog(`播报失败: ${e.error}`, 'error')
    }
    window.speechSynthesis.cancel()
    window.speechSynthesis.speak(utter)
    addLog(`测试播报: ${testText.value}`, 'info')
  } else {
    uni.showToast({ title: '当前环境不支持语音播报', icon: 'none' })
    addLog('当前环境不支持语音播报', 'error')
  }
  // #endif
  // #ifndef H5
  uni.showToast({ title: '语音功能仅在 H5 环境可用', icon: 'none' })
  addLog('语音功能仅在 H5 环境可用', 'error')
  // #endif
}

function addLog(content, type = 'info') {
  const typeMap = { info: '信息', success: '成功', error: '异常' }
  const now = new Date()
  const timeStr = `${now.getHours().toString().padStart(2,'0')}:${now.getMinutes().toString().padStart(2,'0')}:${now.getSeconds().toString().padStart(2,'0')}`
  logs.value.unshift({ time: timeStr, content, type, typeText: typeMap[type] || '信息' })
  if (logs.value.length > 50) logs.value = logs.value.slice(0, 50)
  uni.setStorageSync('pda_voice_logs', logs.value)
}

function clearLogs() {
  logs.value = []
  uni.removeStorageSync('pda_voice_logs')
}
</script>

<style scoped>
.container { padding: 20rpx; padding-bottom: 40rpx; }
.switch-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
}
.switch-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.switch-label {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}
.switch-hint {
  font-size: 26rpx;
  color: #999;
  margin-top: 12rpx;
  display: block;
}
.settings-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
}
.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
  display: block;
}
.setting-item {
  margin-bottom: 24rpx;
}
.setting-item:last-child {
  margin-bottom: 0;
}
.setting-label {
  font-size: 28rpx;
  color: #555;
  margin-bottom: 12rpx;
  display: block;
}
.setting-slider {
  width: 100%;
}
.type-options {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}
.type-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 24rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}
.type-option.active {
  background: #e3f2fd;
  border-color: #0066CC;
}
.type-name {
  font-size: 28rpx;
  color: #333;
}
.type-lang {
  font-size: 24rpx;
  color: #999;
}
.type-empty {
  font-size: 26rpx;
  color: #999;
  padding: 20rpx 0;
  text-align: center;
}

.test-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
}
.test-input {
  height: 88rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  margin-bottom: 20rpx;
}
.test-btn {
  height: 88rpx;
  background: #0066CC;
  color: #fff;
  font-size: 30rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.test-btn[disabled] {
  background: #99c2e6;
}

.log-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
}
.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}
.clear-btn {
  font-size: 26rpx;
  color: #0066CC;
}
.log-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.log-item {
  background: #f8f8f8;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
}
.log-item.log-error {
  background: #fff5f5;
}
.log-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}
.log-time {
  font-size: 24rpx;
  color: #999;
}
.log-type {
  font-size: 22rpx;
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
}
.log-type.type-info { color: #1565c0; background: #e3f2fd; }
.log-type.type-success { color: #2e7d32; background: #e8f5e9; }
.log-type.type-error { color: #c62828; background: #ffcdd2; }
.log-content {
  font-size: 28rpx;
  color: #333;
  display: block;
}
</style>
