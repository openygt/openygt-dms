<template>
  <view class="temp-page">
    <view class="info-card" v-if="taskInfo.barcode">
      <view class="info-row">
        <text class="info-label">任务条码</text>
        <text class="info-value">{{ taskInfo.barcode }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">患者</text>
        <text class="info-value">{{ taskInfo.patientName || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">状态</text>
        <text class="info-value">{{ taskInfo.statusName || '-' }}</text>
      </view>
    </view>

    <view class="stat-row" v-if="curveData.maxTemp">
      <view class="stat-item">
        <text class="stat-value" style="color: #dc2626">{{ curveData.maxTemp }}°C</text>
        <text class="stat-label">最高温</text>
      </view>
      <view class="stat-item">
        <text class="stat-value" style="color: #d97706">{{ curveData.avgTemp }}°C</text>
        <text class="stat-label">平均温</text>
      </view>
      <view class="stat-item">
        <text class="stat-value" style="color: #16a34a">{{ curveData.duration || '-' }}min</text>
        <text class="stat-label">时长</text>
      </view>
    </view>

    <view class="chart-card">
      <canvas canvas-id="tempChart" id="tempChart" class="chart-canvas" :style="{ width: chartW + 'px', height: chartH + 'px' }"></canvas>
    </view>

    <view class="empty-tip" v-if="!curveData.points || curveData.points.length === 0">
      <text class="empty-text">暂无温度数据</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onReady } from '@dcloudio/uni-app'
import { get } from '../../utils/request.js'

const taskInfo = ref({})
const curveData = ref({})
const chartW = ref(375)
const chartH = ref(280)

onLoad((options) => {
  const barcode = options.barcode || ''
  if (barcode) {
    loadCurve(barcode)
  }
})

onReady(() => {
  const sys = uni.getSystemInfoSync()
  chartW.value = sys.windowWidth - 32
})

async function loadCurve(barcode) {
  uni.showLoading({ title: '加载中...' })
  try {
    const taskRes = await get(`/task/${barcode}`)
    taskInfo.value = taskRes || {}

    const curveRes = await get(`/task/${barcode}/temperature-curve`)
    curveData.value = curveRes || {}

    uni.hideLoading()
    drawChart()
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  }
}

function drawChart() {
  const points = curveData.value.points || []
  if (points.length === 0) return

  const ctx = uni.createCanvasContext('tempChart')
  const w = chartW.value
  const h = chartH.value
  const padLeft = 40
  const padRight = 16
  const padTop = 20
  const padBottom = 30

  const temps = points.map(p => p.temperature)
  const maxT = Math.max(...temps) + 2
  const minT = Math.min(...temps) - 2
  const rangeT = maxT - minT || 1

  // 背景
  ctx.setFillStyle('#fff')
  ctx.fillRect(0, 0, w, h)

  // 网格线
  ctx.setStrokeStyle('#f0f0f0')
  ctx.setLineWidth(1)
  for (let i = 0; i <= 4; i++) {
    const y = padTop + (h - padTop - padBottom) * i / 4
    ctx.beginPath()
    ctx.moveTo(padLeft, y)
    ctx.lineTo(w - padRight, y)
    ctx.stroke()

    const t = maxT - rangeT * i / 4
    ctx.setFillStyle('#999')
    ctx.setFontSize(10)
    ctx.fillText(t.toFixed(0) + '°', 4, y + 4)
  }

  // 折线
  ctx.setStrokeStyle('#0066CC')
  ctx.setLineWidth(2)
  ctx.beginPath()
  points.forEach((p, i) => {
    const x = padLeft + (w - padLeft - padRight) * i / (points.length - 1 || 1)
    const y = padTop + (h - padTop - padBottom) * (maxT - p.temperature) / rangeT
    if (i === 0) ctx.moveTo(x, y)
    else ctx.lineTo(x, y)
  })
  ctx.stroke()

  // 点
  ctx.setFillStyle('#0066CC')
  points.forEach((p, i) => {
    if (i % Math.ceil(points.length / 20) !== 0) return
    const x = padLeft + (w - padLeft - padRight) * i / (points.length - 1 || 1)
    const y = padTop + (h - padTop - padBottom) * (maxT - p.temperature) / rangeT
    ctx.beginPath()
    ctx.arc(x, y, 3, 0, Math.PI * 2)
    ctx.fill()
  })

  // X轴标签（只显示首尾和中间）
  const labelIdx = [0, Math.floor(points.length / 2), points.length - 1]
  ctx.setFillStyle('#999')
  ctx.setFontSize(10)
  labelIdx.forEach(i => {
    if (!points[i]) return
    const x = padLeft + (w - padLeft - padRight) * i / (points.length - 1 || 1)
    const timeStr = String(points[i].time || '').slice(11, 16)
    ctx.fillText(timeStr, x - 14, h - 8)
  })

  ctx.draw()
}
</script>

<style lang="scss" scoped>
@import "../../uni.scss";

.temp-page {
  min-height: 100vh;
  padding: 32rpx;
}

.info-card {
  background: #fff;
  border-radius: $ygt-radius-lg;
  padding: 24rpx 32rpx;
  margin-bottom: 24rpx;
  box-shadow: $ygt-shadow-card;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
}

.info-label {
  font-size: 28rpx;
  color: $ygt-gray-600;
}

.info-value {
  font-size: 28rpx;
  color: $ygt-gray-900;
  font-weight: 500;
}

.stat-row {
  display: flex;
  justify-content: space-around;
  background: #fff;
  border-radius: $ygt-radius-lg;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: $ygt-shadow-card;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 40rpx;
  font-weight: 700;
}

.stat-label {
  font-size: 24rpx;
  color: $ygt-gray-500;
  margin-top: 8rpx;
}

.chart-card {
  background: #fff;
  border-radius: $ygt-radius-lg;
  padding: 16rpx;
  box-shadow: $ygt-shadow-card;
}

.chart-canvas {
  display: block;
}

.empty-tip {
  text-align: center;
  padding: 80rpx 0;
}

.empty-text {
  font-size: 28rpx;
  color: $ygt-gray-500;
}
</style>
