<template>
  <el-dialog
    v-model="visible"
    title="打印设备标签"
    width="480px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div class="label-preview">
      <div class="device-info">
        <h3>{{ device?.name }}</h3>
        <p>编码: {{ device?.deviceCode }}</p>
        <p>型号: {{ device?.modelNum || '-' }}</p>
      </div>
      <div class="code-images">
        <div class="code-item">
          <img v-if="qrCode" :src="qrCode" alt="二维码" class="code-img" />
          <el-skeleton v-else :rows="3" animated />
          <span class="code-label">二维码</span>
        </div>
        <div class="code-item">
          <img v-if="barcode" :src="barcode" alt="条码" class="code-img barcode" />
          <el-skeleton v-else :rows="2" animated />
          <span class="code-label">一维条码</span>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="handlePrint">
        <el-icon><Printer /></el-icon>
        打印标签
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Printer } from '@element-plus/icons-vue'
import { getDeviceQrCode, getDeviceBarcode } from '@/api/equipment'

const props = defineProps<{
  modelValue: boolean
  device: any
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = ref(props.modelValue)
const qrCode = ref('')
const barcode = ref('')
const loading = ref(false)

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.device?.id) {
    loadCodes()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

async function loadCodes() {
  if (!props.device?.id) return
  loading.value = true
  try {
    const [qrRes, barRes] = await Promise.all([
      getDeviceQrCode(props.device.id, 200),
      getDeviceBarcode(props.device.id, 300, 100)
    ])
    qrCode.value = qrRes.data?.qrCode || ''
    barcode.value = barRes.data?.barcode || ''
  } catch (err) {
    ElMessage.error('加载标签失败')
  } finally {
    loading.value = false
  }
}

function handlePrint() {
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    ElMessage.warning('请允许弹出窗口以打印标签')
    return
  }
  printWindow.document.write(`
    <html>
      <head>
        <title>设备标签 - ${props.device?.deviceCode}</title>
        <style>
          body { font-family: Arial, sans-serif; text-align: center; padding: 20px; }
          .label { border: 2px solid #333; padding: 16px; display: inline-block; }
          h2 { margin: 0 0 8px; font-size: 18px; }
          p { margin: 4px 0; font-size: 14px; color: #666; }
          img { max-width: 100%; }
          .qr { width: 160px; height: 160px; }
          .bar { width: 260px; height: 80px; }
          @media print { .no-print { display: none; } }
        </style>
      </head>
      <body>
        <div class="no-print" style="margin-bottom: 16px;">
          <button onclick="window.print()">打印</button>
          <button onclick="window.close()">关闭</button>
        </div>
        <div class="label">
          <h2>${props.device?.name || ''}</h2>
          <p>编码: ${props.device?.deviceCode || ''}</p>
          <p>型号: ${props.device?.modelNum || '-'}</p>
          <img class="qr" src="${qrCode.value}" alt="二维码" />
          <br/>
          <img class="bar" src="${barcode.value}" alt="条码" />
        </div>
      </body>
    </html>
  `)
  printWindow.document.close()
}
</script>

<style scoped lang="scss">
.label-preview {
  .device-info {
    text-align: center;
    margin-bottom: 20px;
    h3 {
      font-size: 20px;
      margin: 0 0 8px;
      color: var(--el-text-color-primary);
    }
    p {
      font-size: 14px;
      color: var(--el-text-color-secondary);
      margin: 4px 0;
    }
  }
  .code-images {
    display: flex;
    justify-content: space-around;
    align-items: flex-start;
    gap: 16px;
    .code-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      .code-img {
        width: 180px;
        height: 180px;
        object-fit: contain;
        border: 1px solid var(--el-border-color);
        border-radius: 8px;
        padding: 8px;
        background: #fff;
        &.barcode {
          width: 240px;
          height: 90px;
        }
      }
      .code-label {
        margin-top: 8px;
        font-size: 14px;
        color: var(--el-text-color-secondary);
      }
    }
  }
}
</style>
