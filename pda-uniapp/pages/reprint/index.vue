<template>
  <view class="container">
    <view class="scan-area">
      <text class="label">扫描处方码或输入条码:</text>
      <view class="input-row">
        <input class="barcode-input" v-model="barcode" placeholder="请输入条码" />
        <view class="scan-btn" @click="handleScan">📷</view>
      </view>
      <button class="query-btn" @click="queryTask">查询</button>
    </view>

    <view class="task-card" v-if="task.taskId">
      <text class="task-title">{{ task.patientName }} | {{ task.prescriptionName || '处方' }}</text>
      <text class="task-barcode">条码: {{ task.barcode }}</text>
      <text class="task-status">状态: {{ task.statusName }}</text>
    </view>

    <view class="print-config" v-if="task.taskId">
      <text class="label">打印类型:</text>
      <view class="type-group">
        <view class="type-item" :class="{ active: printType === 'LABEL' }" @click="printType = 'LABEL'">处方标签</view>
        <view class="type-item" :class="{ active: printType === 'HANDOVER' }" @click="printType = 'HANDOVER'">交接单</view>
      </view>
      <text class="label">打印份数:</text>
      <view class="count-row">
        <button class="count-btn" @click="printCount = Math.max(1, printCount - 1)">-</button>
        <text class="count-value">{{ printCount }}</text>
        <button class="count-btn" @click="printCount++">+</button>
      </view>
      <button class="print-btn" :disabled="loading" :loading="loading" @click="handlePrint">执行打印</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { get, post } from '../../utils/request.js'

const barcode = ref('')
const task = ref({})
const printType = ref('LABEL')
const printCount = ref(1)
const loading = ref(false)

function handleScan() {
  uni.scanCode({
    onlyFromCamera: true, scanType: ['qrCode', 'barCode'],
    success: (res) => { barcode.value = res.result; uni.vibrateShort(); queryTask() },
    fail: () => { uni.showToast({ title: '扫码失败', icon: 'none' }) }
  })
}

async function queryTask() {
  if (!barcode.value.trim()) { uni.showToast({ title: '请输入条码', icon: 'none' }); return }
  try {
    uni.showLoading({ title: '查询中' })
    const res = await get(`/task/${barcode.value}`)
    task.value = res
  } catch (e) {
    uni.showToast({ title: e.message || '未找到该处方', icon: 'none' })
    task.value = {}
  } finally { uni.hideLoading() }
}

async function handlePrint() {
  if (!task.value.taskId) return
  if (printType.value === 'HANDOVER' && task.value.status !== 'COMPLETED') {
    uni.showToast({ title: '该处方尚未完成，无法打印交接单', icon: 'none' }); return
  }
  loading.value = true
  try {
    await post(`/tasks/${task.value.taskId}/reprint`, { printType: printType.value, printCount: printCount.value })
    uni.showToast({ title: '打印任务已下发', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e.message || '打印失败', icon: 'none' })
  } finally { loading.value = false }
}
</script>

<style scoped>
.container { padding: 30rpx; }
.scan-area { background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 30rpx; }
.label { font-size: 28rpx; color: #333; margin-bottom: 16rpx; display: block; }
.input-row { display: flex; align-items: center; gap: 16rpx; margin-bottom: 20rpx; }
.barcode-input { flex: 1; height: 88rpx; background: #f5f5f5; border-radius: 12rpx; padding: 0 24rpx; font-size: 30rpx; }
.scan-btn { width: 88rpx; height: 88rpx; background: #0066CC; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; font-size: 40rpx; }
.query-btn { height: 80rpx; background: #0066CC; color: #fff; font-size: 30rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; }
.task-card { background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 30rpx; }
.task-title { font-size: 32rpx; color: #333; font-weight: 500; display: block; }
.task-barcode { font-size: 26rpx; color: #999; margin-top: 12rpx; display: block; }
.task-status { font-size: 28rpx; color: #0066CC; margin-top: 12rpx; display: block; }
.print-config { background: #fff; border-radius: 16rpx; padding: 30rpx; }
.type-group { display: flex; gap: 20rpx; margin-bottom: 30rpx; }
.type-item { flex: 1; height: 80rpx; background: #f5f5f5; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; font-size: 28rpx; color: #555; }
.type-item.active { background: #0066CC; color: #fff; }
.count-row { display: flex; align-items: center; gap: 20rpx; margin-bottom: 30rpx; }
.count-btn { width: 64rpx; height: 64rpx; background: #f5f5f5; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 32rpx; color: #333; }
.count-value { font-size: 36rpx; font-weight: 600; color: #333; min-width: 60rpx; text-align: center; }
.print-btn { height: 96rpx; background: #0066CC; color: #fff; font-size: 34rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; }
.print-btn[disabled] { background: #99c2e6; }
</style>
