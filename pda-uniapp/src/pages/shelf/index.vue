<template>
  <view class="container">
    <!-- 模式切换 -->
    <view class="mode-bar">
      <view
        class="mode-item"
        :class="{ active: mode === 'list' }"
        @click="mode = 'list'"
      >货架列表</view>
      <view
        class="mode-item"
        :class="{ active: mode === 'in' }"
        @click="mode = 'in'"
      >上架</view>
      <view
        class="mode-item"
        :class="{ active: mode === 'out' }"
        @click="mode = 'out'"
      >下架</view>
    </view>

    <!-- 货架列表 -->
    <view v-if="mode === 'list'">
      <view class="search-bar">
        <input
          class="search-input"
          v-model="shelfKeyword"
          placeholder="搜索货架编号/区域"
          @confirm="loadShelfList"
        />
        <button class="search-btn" @click="loadShelfList">搜索</button>
      </view>
      <view class="shelf-list" v-if="shelves.length > 0">
        <view class="shelf-card" v-for="shelf in shelves" :key="shelf.shelfCode" @click="viewShelfDetail(shelf)">
          <view class="shelf-header">
            <text class="shelf-code">{{ shelf.shelfCode }}</text>
            <text class="shelf-area">{{ shelf.areaName }}</text>
          </view>
          <view class="shelf-stats">
            <view class="stat-item">
              <text class="stat-num">{{ shelf.capacity || 0 }}</text>
              <text class="stat-label">容量</text>
            </view>
            <view class="stat-item">
              <text class="stat-num">{{ shelf.occupied || 0 }}</text>
              <text class="stat-label">已用</text>
            </view>
            <view class="stat-item">
              <text class="stat-num">{{ (shelf.capacity || 0) - (shelf.occupied || 0) }}</text>
              <text class="stat-label">剩余</text>
            </view>
          </view>
          <view class="shelf-progress">
            <view class="progress-track">
              <view
                class="progress-fill"
                :style="{ width: getPercent(shelf.occupied, shelf.capacity) + '%', background: getPercent(shelf.occupied, shelf.capacity) > 90 ? '#ef5350' : '#4caf50' }"
              ></view>
            </view>
            <text class="progress-text">{{ getPercent(shelf.occupied, shelf.capacity) }}%</text>
          </view>
        </view>
      </view>
      <ygt-empty v-else text="暂无货架数据" />
    </view>

    <!-- 上架操作 -->
    <view v-if="mode === 'in'">
      <view class="op-card">
        <text class="op-title">1. 扫描药袋条码</text>
        <ygt-scan-input
          v-model="inBarcode"
          placeholder="请扫描药袋条码"
          @confirm="queryBagInfo"
          @scan="scanBag('in')"
        />
      </view>

      <view class="op-card" v-if="inBagInfo.bagId">
        <view class="bag-info">
          <text class="bag-barcode">条码: {{ inBagInfo.barcode }}</text>
          <text class="bag-patient">患者: {{ inBagInfo.patientName || '-' }}</text>
          <text class="bag-prescription">处方: {{ inBagInfo.prescriptionNumber || '-' }}</text>
        </view>
      </view>

      <view class="op-card" v-if="inBagInfo.bagId">
        <text class="op-title">2. 选择货架</text>
        <view class="shelf-options">
          <view
            class="shelf-option"
            v-for="s in availableShelves"
            :key="s.shelfCode"
            :class="{ active: selectedShelfCode === s.shelfCode }"
            @click="selectedShelfCode = s.shelfCode"
          >
            <text class="so-code">{{ s.shelfCode }}</text>
            <text class="so-area">{{ s.areaName }}</text>
            <text class="so-remain">剩{{ (s.capacity || 0) - (s.occupied || 0) }}位</text>
          </view>
        </view>
      </view>

      <button
        class="submit-btn"
        :disabled="!inBagInfo.bagId || !selectedShelfCode"
        @click="doPutOnShelf"
        v-if="inBagInfo.bagId"
      >确认上架</button>
    </view>

    <!-- 下架操作 -->
    <view v-if="mode === 'out'">
      <view class="op-card">
        <text class="op-title">扫描药袋条码</text>
        <ygt-scan-input
          v-model="outBarcode"
          placeholder="请扫描药袋条码"
          @confirm="queryOutBagInfo"
          @scan="scanBag('out')"
        />
      </view>

      <view class="op-card" v-if="outBagInfo.bagId">
        <view class="bag-info">
          <text class="bag-barcode">条码: {{ outBagInfo.barcode }}</text>
          <text class="bag-patient">患者: {{ outBagInfo.patientName || '-' }}</text>
          <text class="bag-prescription">处方: {{ outBagInfo.prescriptionNumber || '-' }}</text>
          <text class="bag-shelf" v-if="outBagInfo.shelfCode">当前货架: {{ outBagInfo.shelfCode }}</text>
        </view>
      </view>

      <button
        class="submit-btn danger"
        :disabled="!outBagInfo.bagId"
        @click="doTakeOffShelf"
        v-if="outBagInfo.bagId"
      >确认取走</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, post } from '../../utils/request.js'
import { startScan } from '../../utils/scan.js'

const mode = ref('list')
const shelfKeyword = ref('')
const shelves = ref([])

// 上架
const inBarcode = ref('')
const inBagInfo = ref({})
const selectedShelfCode = ref('')
const availableShelves = ref([])

// 下架
const outBarcode = ref('')
const outBagInfo = ref({})

onMounted(() => {
  loadShelfList()
})

function getPercent(occupied, capacity) {
  if (!capacity) return 0
  const p = Math.round(((occupied || 0) / capacity) * 100)
  return p > 100 ? 100 : p
}

async function loadShelfList() {
  try {
    uni.showLoading({ title: '加载中' })
    const res = await get('/shelves', { keyword: shelfKeyword.value })
    shelves.value = res || []
    availableShelves.value = (res || []).filter(s => (s.capacity || 0) > (s.occupied || 0))
  } catch (e) {
    console.error('加载货架失败', e)
    shelves.value = []
  } finally {
    uni.hideLoading()
  }
}

function viewShelfDetail(shelf) {
  uni.navigateTo({ url: `/pages/shelf/detail?shelfCode=${shelf.shelfCode}` })
}

function scanBag(type) {
  startScan({ onlyFromCamera: true, scanType: ['qrCode', 'barCode'] }).then(code => {
    if (type === 'in') {
      inBarcode.value = code
      queryBagInfo()
    } else {
      outBarcode.value = code
      queryOutBagInfo()
    }
  }).catch(() => {})
}

async function queryBagInfo() {
  if (!inBarcode.value.trim()) return
  try {
    uni.showLoading({ title: '查询中' })
    const res = await get(`/bag/${inBarcode.value}`)
    inBagInfo.value = res || {}
    selectedShelfCode.value = ''
  } catch (e) {
    uni.showToast({ title: e.message || '未找到药袋', icon: 'none' })
    inBagInfo.value = {}
  } finally {
    uni.hideLoading()
  }
}

async function doPutOnShelf() {
  if (!inBagInfo.value.bagId || !selectedShelfCode.value) return
  try {
    uni.showLoading({ title: '上架中' })
    await post('/shelf/put-on', {
      bagId: inBagInfo.value.bagId,
      shelfCode: selectedShelfCode.value,
      barcode: inBarcode.value
    })
    uni.showToast({ title: '上架成功', icon: 'success' })
    inBarcode.value = ''
    inBagInfo.value = {}
    selectedShelfCode.value = ''
    loadShelfList()
  } catch (e) {
    uni.showToast({ title: e.message || '上架失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

async function queryOutBagInfo() {
  if (!outBarcode.value.trim()) return
  try {
    uni.showLoading({ title: '查询中' })
    const res = await get(`/bag/${outBarcode.value}`)
    outBagInfo.value = res || {}
  } catch (e) {
    uni.showToast({ title: e.message || '未找到药袋', icon: 'none' })
    outBagInfo.value = {}
  } finally {
    uni.hideLoading()
  }
}

async function doTakeOffShelf() {
  if (!outBagInfo.value.bagId) return
  uni.showModal({
    title: '确认取走',
    content: `确认从货架 ${outBagInfo.value.shelfCode || '-'} 取走该药袋？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          uni.showLoading({ title: '处理中' })
          await post('/shelf/take-off', {
            bagId: outBagInfo.value.bagId,
            shelfCode: outBagInfo.value.shelfCode,
            barcode: outBarcode.value
          })
          uni.showToast({ title: '取走成功', icon: 'success' })
          outBarcode.value = ''
          outBagInfo.value = {}
          loadShelfList()
        } catch (e) {
          uni.showToast({ title: e.message || '操作失败', icon: 'none' })
        } finally {
          uni.hideLoading()
        }
      }
    }
  })
}
</script>

<style scoped>
.container { padding: 20rpx; padding-bottom: 40rpx; }
.mode-bar { display: flex; background: #fff; border-radius: 16rpx; overflow: hidden; margin-bottom: 20rpx; }
.mode-item { flex: 1; text-align: center; padding: 24rpx 0; font-size: 30rpx; color: #666; }
.mode-item.active { color: #0066CC; font-weight: 600; background: #e3f2fd; }

/* 列表 */
.search-bar { display: flex; gap: 16rpx; margin-bottom: 20rpx; }
.search-input { flex: 1; height: 88rpx; background: #fff; border-radius: 12rpx; padding: 0 24rpx; font-size: 28rpx; }
.search-btn { width: 160rpx; height: 88rpx; background: #0066CC; color: #fff; font-size: 28rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; padding: 0; }
.shelf-list { display: flex; flex-direction: column; gap: 20rpx; }
.shelf-card { background: #fff; border-radius: 16rpx; padding: 30rpx; }
.shelf-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.shelf-code { font-size: 32rpx; font-weight: 600; color: #333; }
.shelf-area { font-size: 26rpx; color: #0066CC; background: #e3f2fd; padding: 4rpx 16rpx; border-radius: 8rpx; }
.shelf-stats { display: flex; justify-content: space-around; margin-bottom: 20rpx; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 36rpx; font-weight: 600; color: #333; }
.stat-label { font-size: 24rpx; color: #999; margin-top: 4rpx; }
.shelf-progress { display: flex; align-items: center; gap: 16rpx; }
.progress-track { flex: 1; height: 16rpx; background: #f0f0f0; border-radius: 8rpx; overflow: hidden; }
.progress-fill { height: 100%; border-radius: 8rpx; transition: width 0.3s; }
.progress-text { font-size: 24rpx; color: #666; width: 80rpx; text-align: right; }

/* 操作卡片 */
.op-card { background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 20rpx; }
.op-title { font-size: 30rpx; font-weight: 600; color: #333; margin-bottom: 20rpx; display: block; }
.bag-info { display: flex; flex-direction: column; gap: 12rpx; }
.bag-barcode { font-size: 30rpx; color: #333; font-weight: 500; }
.bag-patient { font-size: 28rpx; color: #555; }
.bag-prescription { font-size: 28rpx; color: #555; }
.bag-shelf { font-size: 28rpx; color: #0066CC; font-weight: 500; }

.shelf-options { display: flex; flex-direction: column; gap: 16rpx; }
.shelf-option { display: flex; align-items: center; gap: 16rpx; padding: 20rpx 24rpx; background: #f5f5f5; border-radius: 12rpx; border: 2rpx solid transparent; }
.shelf-option.active { background: #e3f2fd; border-color: #0066CC; }
.so-code { font-size: 30rpx; font-weight: 600; color: #333; }
.so-area { font-size: 26rpx; color: #666; flex: 1; }
.so-remain { font-size: 26rpx; color: #4caf50; font-weight: 500; }

.submit-btn { height: 96rpx; background: #0066CC; color: #fff; font-size: 34rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; }
.submit-btn[disabled] { background: #99c2e6; }
.submit-btn.danger { background: #ef5350; }
.submit-btn.danger[disabled] { background: #ef9a9a; }
</style>
