<template>
  <div class="page-container">
    <el-card class="mt-4" shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>待打印工单</span>
          <el-button type="primary" @click="batchPrint">批量打印</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="处方号">
          <el-input v-model="search.prescriptionNo" placeholder="处方号" clearable />
        </el-form-item>
        <el-form-item label="设备">
          <el-input v-model="search.deviceCode" placeholder="设备编码" clearable />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="search.date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="prescriptionNo" label="处方号" min-width="140" />
        <el-table-column prop="patientName" label="患者" width="100" />
        <el-table-column prop="deviceCode" label="设备" width="100" />
        <el-table-column prop="schemeName" label="煎药方案" width="120" />
        <el-table-column prop="decoctTime" label="煎煮时长" width="100" />
        <el-table-column prop="packageType" label="包装" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PRINTED'" type="success">已打印</el-tag>
            <el-tag v-else>待打印</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="preview(row)">预览</el-button>
            <el-button link type="primary" @click="printRow(row)">打印</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="previewVisible" title="工单预览" width="400px">
      <div class="work-order-preview" v-if="previewData">
        <h3>煎药工单</h3>
        <p><strong>处方号：</strong>{{ previewData.prescriptionNo }}</p>
        <p><strong>患者：</strong>{{ previewData.patientName }}</p>
        <p><strong>设备：</strong>{{ previewData.deviceCode }}</p>
        <p><strong>方案：</strong>{{ previewData.schemeName }}</p>
        <p><strong>煎煮时长：</strong>{{ previewData.decoctTime }} 分钟</p>
        <p><strong>包装：</strong>{{ previewData.packageType }}</p>
        <p><strong>注意事项：</strong>{{ previewData.remark || '无' }}</p>
        <div class="barcode-placeholder">[条码区域]</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'

const loading = ref(false)
const tableData = ref<any[]>([])
const previewVisible = ref(false)
const previewData = ref<any>(null)
const selectedRows = ref<any[]>([])
const search = reactive({ prescriptionNo: '', deviceCode: '', date: '' })
const pagination = reactive({ page: 1, size: 20, total: 0 })

const allWorkOrders = [
  { id: 1, prescriptionNo: 'RX2026050024', patientName: '李建国', deviceCode: '煎药机1-1', schemeName: '常规方案', decoctTime: '40min', packageType: '标准袋', status: 'PENDING' },
  { id: 2, prescriptionNo: 'RX20260510023', patientName: '赵大伟', deviceCode: '煎药机1-2', schemeName: '滋补方案', decoctTime: '60min', packageType: '大袋', status: 'PENDING' },
  { id: 3, prescriptionNo: 'RX20260510002', patientName: '韩玉兰', deviceCode: '煎药机1-3', schemeName: '常规方案', decoctTime: '40min', packageType: '标准袋', status: 'PENDING' },
  { id: 4, prescriptionNo: 'RX2026050048', patientName: '刘桂花', deviceCode: '煎药机1-4', schemeName: '解表方案', decoctTime: '20min', packageType: '标准袋', status: 'PENDING' },
  { id: 5, prescriptionNo: 'RX20260510009', patientName: '郑晓燕', deviceCode: '煎药机2-1', schemeName: '常规方案', decoctTime: '40min', packageType: '标准袋', status: 'PENDING' },
  { id: 6, prescriptionNo: 'RX20260510012', patientName: '冯德明', deviceCode: '煎药机2-2', schemeName: '常规方案', decoctTime: '40min', packageType: '标准袋', status: 'PENDING' },
  { id: 7, prescriptionNo: 'RX20260510024', patientName: '蒋春梅', deviceCode: '煎药机2-3', schemeName: '滋补方案', decoctTime: '60min', packageType: '大袋', status: 'PENDING' },
  { id: 8, prescriptionNo: 'RX20260510010', patientName: '王美兰', deviceCode: '煎药机2-4', schemeName: '常规方案', decoctTime: '40min', packageType: '标准袋', status: 'PENDING' },
  { id: 9, prescriptionNo: 'RX2026050037', patientName: '朱秀芳', deviceCode: '煎药机3-1', schemeName: '常规方案', decoctTime: '40min', packageType: '标准袋', status: 'PENDING' },
  { id: 10, prescriptionNo: 'RX2026050038', patientName: '孙文博', deviceCode: '煎药机3-2', schemeName: '解表方案', decoctTime: '20min', packageType: '小袋', status: 'PENDING' }
]

async function loadData() {
  loading.value = true
  try {
    let filtered = allWorkOrders
    if (search.prescriptionNo) {
      const kw = search.prescriptionNo.toLowerCase()
      filtered = filtered.filter(o => o.prescriptionNo.toLowerCase().includes(kw))
    }
    if (search.deviceCode) {
      const kw = search.deviceCode.toLowerCase()
      filtered = filtered.filter(o => o.deviceCode.toLowerCase().includes(kw))
    }
    pagination.total = filtered.length
    const start = (pagination.page - 1) * pagination.size
    tableData.value = filtered.slice(start, start + pagination.size)
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  search.prescriptionNo = ''
  search.deviceCode = ''
  search.date = ''
  pagination.page = 1
  loadData()
}

function preview(row: any) {
  previewData.value = row
  previewVisible.value = true
}

async function printRow(row: any) {
  if (!row.id) {
    ElMessage.warning('任务ID不存在，无法打印')
    return
  }
  try {
    const userStore = useUserStore()
    await request.post(`/v1/prt/tasks/${row.id}/submit`, null, {
      params: { deviceCode: row.deviceCode || '', operatorId: userStore.userInfo?.username || '' }
    })
    ElMessage.success(`工单 ${row.prescriptionNo} 已提交打印`)
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '打印失败')
  }
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

async function batchPrint() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择工单')
    return
  }
  const userStore = useUserStore()
  const results = await Promise.allSettled(selectedRows.value.map(row =>
    request.post(`/v1/prt/tasks/${row.id}/submit`, null, {
      params: { deviceCode: row.deviceCode || '', operatorId: userStore.userInfo?.username || '' }
    })
  ))
  const successCount = results.filter(r => r.status === 'fulfilled').length
  const failCount = results.length - successCount
  if (failCount > 0) {
    const failedIndices = results
      .map((r, i) => r.status === 'rejected' ? selectedRows.value[i].prescriptionNo : null)
      .filter(Boolean)
    ElMessage.warning(`提交完成：成功 ${successCount} 张，失败 ${failCount} 张（处方号：${failedIndices.join('、')}）`)
  } else {
    ElMessage.success(`已提交 ${successCount} 张工单打印`)
  }
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.page-container { padding: 16px; }
.mt-4 { margin-top: 16px; }
.pagination-wrapper { margin-top: 16px; display: flex; justify-content: flex-end; }
.work-order-preview {
  padding: 16px;
  border: 1px dashed var(--el-border-color);
  background: #fafafa;
}
.work-order-preview h3 { text-align: center; margin-bottom: 16px; }
.work-order-preview p { margin: 8px 0; }
.barcode-placeholder {
  margin-top: 16px;
  padding: 20px;
  text-align: center;
  border: 1px dashed #999;
  color: #999;
}
</style>
