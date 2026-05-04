<template>
  <view class="container">
    <!-- 条码输入（无任务时显示） -->
    <view class="barcode-input-area" v-if="!taskId">
      <text class="barcode-label">请输入任务条码</text>
      <input class="barcode-input" v-model="barcodeInput" placeholder="扫描或输入条码" @confirm="loadByBarcode" data-testid="input-photo-barcode" />
      <button class="load-btn" @click="loadByBarcode">加载任务</button>
    </view>

    <template v-if="taskId">
    <view class="task-info">
      <text class="info-label">任务: {{ taskBarcode }}</text>
      <text class="info-label" v-if="isForceMode" style="color:#f44336" data-testid="photo-force-mode">强制拍照留档模式</text>
    </view>

    <view class="photo-type">
      <text class="label">照片类型:</text>
      <view class="type-list">
        <view class="type-item" v-for="type in photoTypes" :key="type.value" :class="{ active: selectedType === type.value }" @click="selectedType = type.value" :data-testid="'photo-type-' + type.value">
          <text class="type-icon">{{ type.icon }}</text>
          <text class="type-name">{{ type.label }}</text>
        </view>
      </view>
    </view>

    <view class="photo-area">
      <text class="label">已拍摄照片 ({{ photos.length }}/{{ maxCount }}):</text>
      <view class="photo-grid">
        <view class="photo-item" v-for="(photo, idx) in photos" :key="idx">
          <image class="photo-img" :src="photo.path" mode="aspectFill" @click="previewPhoto(idx)"/>
          <view class="photo-delete" @click="removePhoto(idx)">×</view>
        </view>
        <view class="photo-add" v-if="photos.length < maxCount" @click="choosePhoto">+</view>
      </view>
    </view>

    <view class="remark-area">
      <text class="label">备注:</text>
      <textarea class="remark-input" v-model="remark" placeholder="请输入备注..." maxlength="200" />
    </view>

    <view class="actions">
      <button class="skip-btn" v-if="isForceMode" @click="handleSkip" data-testid="btn-skip-photo">跳过</button>
      <button class="upload-btn" :loading="loading" :disabled="loading || photos.length === 0" @click="handleUpload" data-testid="btn-upload-photo">{{ isForceMode ? '提交留档' : '上传照片' }}</button>
    </view>
    </template>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import config from '../../utils/config.js'
import { post, upload } from '../../utils/request.js'
import { addPhotoToQueue } from '../../utils/storage.js'

const taskId = ref('')
const taskBarcode = ref('')
const stepType = ref('')
const isForceMode = ref(false)
const barcodeInput = ref('')

function loadByBarcode() {
  if (!barcodeInput.value.trim()) { uni.showToast({ title: '请输入条码', icon: 'none' }); return }
  taskId.value = taskBarcode.value = barcodeInput.value
}
const selectedType = ref('REVIEW')
const photos = ref([])
const remark = ref('')
const loading = ref(false)
const maxCount = config.photoMaxCount

const photoTypes = [
  { value: 'REVIEW', label: '复核', icon: '✓' },
  { value: 'WEIGHING', label: '称重', icon: '⚖' },
  { value: 'EXCEPTION', label: '异常', icon: '!' }
]

onLoad((options) => {
  taskId.value = options.taskId || ''
  taskBarcode.value = options.barcode || ''
  stepType.value = options.stepType || ''
  isForceMode.value = options.mode === 'force'
})

function choosePhoto() {
  uni.chooseImage({
    sourceType: ['camera', 'album'],
    sizeType: ['compressed'],
    count: maxCount - photos.value.length,
    success: (res) => {
      for (const path of res.tempFilePaths) {
        if (photos.value.length >= maxCount) break
        photos.value.push({ path, size: res.tempFiles.find(f => f.path === path)?.size || 0 })
      }
    }
  })
}

function removePhoto(idx) { photos.value.splice(idx, 1) }

function previewPhoto(idx) {
  uni.previewImage({ current: photos.value[idx].path, urls: photos.value.map(p => p.path) })
}

async function handleUpload() {
  if (photos.value.length === 0) { uni.showToast({ title: '请至少拍摄1张照片', icon: 'none' }); return }
  loading.value = true
  try {
    for (const photo of photos.value) {
      // 第一步：上传文件到服务器，获取可访问 URL
      const uploadRes = await upload('/file/upload', photo.path, {
        taskId: taskId.value,
        photoType: selectedType.value,
        remark: remark.value
      })
      // 第二步：保存照片元数据
      await post('/photo/upload', {
        taskId: taskId.value,
        photoUrl: uploadRes.url,
        photoType: selectedType.value,
        fileSize: uploadRes.size || photo.size,
        remark: remark.value
      })
    }
    uni.showToast({ title: isForceMode.value ? '留档提交成功' : '上传成功', icon: 'success' })
    setTimeout(() => { uni.navigateBack() }, 800)
  } catch (e) {
    // 网络失败时缓存到离线队列（含文件路径，以便后续同步时上传）
    for (const photo of photos.value) {
      addPhotoToQueue({
        taskId: taskId.value,
        path: photo.path,
        photoType: selectedType.value,
        fileSize: photo.size,
        remark: remark.value
      })
    }
    uni.showToast({ title: '已缓存到本地，稍后同步', icon: 'none' })
  } finally { loading.value = false }
}

function handleSkip() {
  if (photos.value.length === 0) {
    uni.showModal({
      title: '跳过确认',
      content: '您尚未拍摄留档照片，确认跳过？',
      success: (res) => {
        if (res.confirm) uni.navigateBack()
      }
    })
  } else {
    handleUpload()
  }
}

// 不再使用占位的 uploadFile，由 upload() 函数实现真实文件上传

</script>

<style scoped>
.container { padding: 30rpx; padding-bottom: 160rpx; }
.barcode-input-area { background: #fff; border-radius: 16rpx; padding: 40rpx; margin-bottom: 30rpx; text-align: center; }
.barcode-label { font-size: 28rpx; color: #999; display: block; margin-bottom: 16rpx; }
.barcode-input { width: 100%; height: 80rpx; border: 2rpx solid #ddd; border-radius: 12rpx; padding: 0 20rpx; font-size: 32rpx; box-sizing: border-box; margin-bottom: 16rpx; }
.load-btn { width: 100%; height: 80rpx; background: #0066CC; color: #fff; font-size: 30rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; }
.task-info { background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 30rpx; }
.info-label { font-size: 30rpx; color: #333; display: block; }
.photo-type { background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 30rpx; }
.label { font-size: 28rpx; color: #333; margin-bottom: 16rpx; display: block; }
.type-list { display: flex; gap: 20rpx; }
.type-item { flex: 1; height: 120rpx; background: #f5f5f5; border-radius: 12rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.type-item.active { background: #e3f2fd; border: 2rpx solid #0066CC; }
.type-icon { font-size: 40rpx; margin-bottom: 8rpx; }
.type-name { font-size: 26rpx; color: #555; }
.photo-area { background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 30rpx; }
.photo-grid { display: flex; flex-wrap: wrap; gap: 20rpx; }
.photo-item { position: relative; width: 200rpx; height: 200rpx; }
.photo-img { width: 200rpx; height: 200rpx; border-radius: 12rpx; }
.photo-delete { position: absolute; top: -10rpx; right: -10rpx; width: 40rpx; height: 40rpx; background: #f44336; color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 28rpx; }
.photo-add { width: 200rpx; height: 200rpx; border: 2rpx dashed #ccc; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; font-size: 60rpx; color: #999; }
.remark-area { background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 30rpx; }
.remark-input { width: 100%; height: 160rpx; background: #f5f5f5; border-radius: 12rpx; padding: 20rpx; font-size: 28rpx; }
.actions { position: fixed; bottom: 0; left: 0; right: 0; background: #fff; padding: 20rpx 30rpx; display: flex; gap: 20rpx; box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.06); }
.skip-btn { flex: 1; height: 88rpx; background: #f5f5f5; color: #555; font-size: 30rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; }
.upload-btn { flex: 2; height: 88rpx; background: #0066CC; color: #fff; font-size: 30rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; }
.upload-btn[disabled] { background: #99c2e6; }
</style>
