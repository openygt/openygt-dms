<template>
  <view class="container">
    <view class="task-info">
      <text class="label">关联任务</text>
      <text class="value">{{ taskId || '未选择' }}</text>
    </view>
    
    <view class="photo-type">
      <text class="section-title">照片类型</text>
      <view class="type-list">
        <view 
          class="type-item" 
          v-for="type in photoTypes" 
          :key="type.value"
          :class="{ active: selectedType === type.value }"
          @click="selectedType = type.value"
        >
          <text class="type-icon">{{ type.icon }}</text>
          <text class="type-name">{{ type.label }}</text>
        </view>
      </view>
    </view>
    
    <view class="photo-area">
      <text class="section-title">拍照 / 选择照片（最多 {{ maxCount }} 张）</text>
      <view class="photo-grid">
        <view class="photo-item" v-for="(photo, idx) in photos" :key="idx">
          <image class="photo-img" :src="photo.path" mode="aspectFill" @click="previewPhoto(idx)"/>
          <view class="photo-delete" @click="removePhoto(idx)">×</view>
        </view>
        <view class="photo-add" v-if="photos.length < maxCount" @click="choosePhoto">
          <text class="add-icon">+</text>
          <text class="add-text">添加照片</text>
        </view>
      </view>
    </view>
    
    <view class="remark-area">
      <text class="label">备注</text>
      <textarea class="remark-input" v-model="remark" placeholder="请输入照片备注"/>
    </view>
    
    <button 
      class="upload-btn" 
      :loading="loading"
      :disabled="loading || photos.length === 0"
      @click="handleUpload"
    >
      上传照片
    </button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import config from '../../utils/config.js'
import { post } from '../../utils/request.js'
import { addPhotoToQueue } from '../../utils/storage.js'

const taskId = ref('')
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
})

function choosePhoto() {
  const remain = maxCount - photos.value.length
  uni.chooseImage({
    count: remain,
    sizeType: ['compressed'],
    sourceType: ['camera', 'album'],
    success: (res) => {
      const newPhotos = res.tempFiles.map(file => ({
        path: file.path,
        size: file.size
      }))
      photos.value.push(...newPhotos)
    }
  })
}

function removePhoto(idx) {
  photos.value.splice(idx, 1)
}

function previewPhoto(idx) {
  uni.previewImage({
    current: idx,
    urls: photos.value.map(p => p.path)
  })
}

async function handleUpload() {
  if (photos.value.length === 0) {
    uni.showToast({ title: '请至少添加一张照片', icon: 'none' })
    return
  }
  
  // 检查单张大小
  for (const photo of photos.value) {
    if (photo.size > config.photoMaxSize) {
      uni.showToast({ title: '单张照片不能超过 5MB', icon: 'none' })
      return
    }
  }
  
  loading.value = true
  try {
    // 逐个上传
    for (const photo of photos.value) {
      // 先上传文件到 OSS 或本地服务器获取 URL
      const uploadRes = await uploadFile(photo.path)
      
      // 再调用后端接口记录
      await post('/photo/upload', {
        taskId: taskId.value,
        photoUrl: uploadRes.url,
        photoType: selectedType.value,
        fileSize: photo.size,
        remark: remark.value
      })
    }
    
    uni.showToast({ title: '上传成功', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1000)
  } catch (e) {
    // 上传失败，缓存到本地队列
    photos.value.forEach(photo => {
      addPhotoToQueue({
        taskId: taskId.value,
        photoUrl: photo.path,
        photoType: selectedType.value,
        fileSize: photo.size,
        remark: remark.value
      })
    })
    uni.showToast({ title: '网络异常，已缓存到本地', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function uploadFile(filePath) {
  return new Promise((resolve, reject) => {
    // 这里应调用实际文件上传接口
    // 简化处理：直接返回本地路径作为 URL（实际项目应上传到 OSS）
    setTimeout(() => {
      resolve({ url: filePath })
    }, 500)
  })
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 30rpx;
}

.task-info {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
}

.label {
  font-size: 26rpx;
  color: #666;
  display: block;
}

.value {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-top: 10rpx;
}

.photo-type {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
}

.type-list {
  display: flex;
  gap: 20rpx;
}

.type-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30rpx 0;
  background: #f8f8f8;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}

.type-item.active {
  border-color: #0066CC;
  background: #e8eaf6;
}

.type-icon {
  font-size: 40rpx;
  margin-bottom: 8rpx;
}

.type-name {
  font-size: 26rpx;
  color: #666;
}

.photo-area {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20rpx;
}

.photo-item {
  position: relative;
  aspect-ratio: 1;
}

.photo-img {
  width: 100%;
  height: 100%;
  border-radius: 12rpx;
}

.photo-delete {
  position: absolute;
  top: -10rpx;
  right: -10rpx;
  width: 40rpx;
  height: 40rpx;
  background: #ff5252;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
}

.photo-add {
  aspect-ratio: 1;
  background: #f8f8f8;
  border-radius: 12rpx;
  border: 2rpx dashed #ccc;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.add-icon {
  font-size: 60rpx;
  color: #999;
}

.add-text {
  font-size: 24rpx;
  color: #999;
}

.remark-area {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
}

.remark-input {
  width: 100%;
  height: 120rpx;
  background: #f5f5f5;
  border-radius: 10rpx;
  padding: 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.upload-btn {
  height: 100rpx;
  line-height: 100rpx;
  background: #0066CC;
  color: #fff;
  font-size: 32rpx;
  border-radius: 12rpx;
}

.upload-btn[disabled] {
  background: #a0a0a0;
}
</style>
