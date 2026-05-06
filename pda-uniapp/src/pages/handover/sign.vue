<template>
  <view class="container">
    <view class="task-info">
      <text class="info-label" data-testid="sign-task-barcode">任务号: {{ taskBarcode || taskId }}</text>
      <text class="info-label" v-if="patientName">患者: {{ patientName }}</text>
      <text class="info-label" v-if="prescriptionNumber">处方号: {{ prescriptionNumber }}</text>
      <text class="info-label" v-if="taskStatus">当前状态: {{ taskStatus }}</text>
      <text class="info-label">从: {{ handoverFrom }}</text>
      <text class="info-label">到: {{ handoverTo }}</text>
    </view>

    <view class="sign-area">
      <text class="sign-title">请在下方区域手写签字</text>
      <canvas canvas-id="signCanvas" class="sign-canvas" @touchstart="startDraw" @touchmove="draw" @touchend="endDraw" data-testid="sign-canvas" />
    </view>

    <view class="remark-area">
      <text class="label">备注:</text>
      <textarea class="remark-input" v-model="remark" placeholder="请输入备注..." maxlength="200" />
    </view>

    <view class="actions">
      <button class="clear-btn" @click="clearSign">清空</button>
      <button class="confirm-btn" :disabled="!hasSigned || loading" :loading="loading" @click="handleConfirm" data-testid="btn-sign-confirm">确认签字</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onReady } from '@dcloudio/uni-app'
import { get, post } from '../../utils/request.js'

const taskId = ref('')
const taskBarcode = ref('')
const patientName = ref('')
const prescriptionNumber = ref('')
const taskStatus = ref('')
const handoverFrom = ref('')
const handoverTo = ref('')
const remark = ref('')
const loading = ref(false)
const hasSigned = ref(false)

let ctx = null
let isDrawing = false

onLoad((options) => {
  taskId.value = options.taskId || ''
  taskBarcode.value = options.barcode || ''
  const userInfo = uni.getStorageSync('pda_user_info') || {}
  handoverFrom.value = userInfo.userName || userInfo.userCode || '当前操作员'
  handoverTo.value = options.handoverTo || '下一工序'
  loadTaskInfo()
})

async function loadTaskInfo() {
  if (!taskId.value) return
  try {
    const res = await get(`/task/${taskBarcode.value || taskId.value}`)
    patientName.value = res.patientName || ''
    prescriptionNumber.value = res.prescriptionNumber || ''
    taskStatus.value = res.statusName || res.status || ''
  } catch (e) {
    console.error('加载任务信息失败', e)
  }
}

onReady(() => {
  ctx = uni.createCanvasContext('signCanvas')
  ctx.setStrokeStyle('#333')
  ctx.setLineWidth(3)
  ctx.setLineCap('round')
  ctx.setLineJoin('round')
})

function startDraw(e) {
  isDrawing = true
  const { x, y } = e.touches[0]
  ctx.moveTo(x, y)
}

function draw(e) {
  if (!isDrawing) return
  const { x, y } = e.touches[0]
  ctx.lineTo(x, y)
  ctx.stroke()
  ctx.draw(true)
  ctx.moveTo(x, y)
  hasSigned.value = true
}

function endDraw() { isDrawing = false }

function clearSign() {
  ctx.clearRect(0, 0, 600, 300)
  ctx.draw()
  hasSigned.value = false
}

async function handleConfirm() {
  if (!hasSigned.value) { uni.showToast({ title: '请先签字', icon: 'none' }); return }
  const { confirm } = await uni.showModal({
    title: '确认交接签字',
    content: `患者：${patientName.value || '-'}
处方号：${prescriptionNumber.value || '-'}
交接对象：${handoverTo.value}

签字后不可撤销，是否确认？`
  })
  if (!confirm) return
  loading.value = true
  try {
    const tempPath = await new Promise((resolve, reject) => {
      uni.canvasToTempFilePath({ canvasId: 'signCanvas', success: (res) => resolve(res.tempFilePath), fail: reject })
    })
    const fs = uni.getFileSystemManager()
    const base64 = fs.readFileSync(tempPath, 'base64')
    await post(`/tasks/${taskId.value}/sign`, {
      signType: 'HANDOVER', signImageBase64: 'data:image/png;base64,' + base64,
      handoverFrom: handoverFrom.value, handoverTo: handoverTo.value, remark: remark.value
    })
    uni.showToast({ title: '签字成功', icon: 'success' })
    setTimeout(() => { uni.navigateBack() }, 800)
  } catch (e) {
    uni.showToast({ title: e.message || '签字失败', icon: 'none' })
  } finally { loading.value = false }
}
</script>

<style scoped>
.container { padding: 30rpx; padding-bottom: 160rpx; }
.task-info { background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 30rpx; }
.info-label { font-size: 30rpx; color: #333; display: block; margin-bottom: 12rpx; }
.sign-area { background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 30rpx; }
.sign-title { font-size: 28rpx; color: #666; margin-bottom: 20rpx; display: block; }
.sign-canvas { width: 100%; height: 300rpx; border: 2rpx solid #ddd; border-radius: 12rpx; background: #fafafa; }
.remark-area { background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 30rpx; }
.label { font-size: 28rpx; color: #333; margin-bottom: 12rpx; display: block; }
.remark-input { width: 100%; height: 160rpx; background: #f5f5f5; border-radius: 12rpx; padding: 20rpx; font-size: 28rpx; }
.actions { position: fixed; bottom: 0; left: 0; right: 0; background: #fff; padding: 20rpx 30rpx; display: flex; gap: 20rpx; box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.06); }
.clear-btn { flex: 1; height: 88rpx; background: #f5f5f5; color: #555; font-size: 30rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; }
.confirm-btn { flex: 2; height: 88rpx; background: #0066CC; color: #fff; font-size: 30rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; }
.confirm-btn[disabled] { background: #99c2e6; }
</style>
