<template>
  <div class="page-container">
    <div class="page-header-title">身份条码：<span class="page-header-sub">员工身份条码生成、打印</span></div>
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline @submit.prevent>
        <el-form-item label="部门">
          <el-select v-model="searchForm.dept" clearable placeholder="全部" style="width: 140px" @change="handleSearch">
            <el-option label="调剂部" value="DISPENSE" />
            <el-option label="煎煮部" value="DECOCT" />
            <el-option label="包装部" value="PACKAGE" />
            <el-option label="质检部" value="QC" />
          </el-select>
        </el-form-item>
        <el-form-item label="姓名/工号">
          <el-input v-model="searchForm.keyword" clearable placeholder="姓名或工号" style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="employeeNo" label="工号" min-width="120" />
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column prop="deptName" label="部门" min-width="100" />
        <el-table-column prop="roleName" label="岗位" min-width="100" />
        <el-table-column label="条码预览" min-width="220">
          <template #default="{ row }">
            <div style="display: flex; gap: 16px; align-items: center">
              <div class="barcode-preview">
                <div class="barcode-lines">
                  <div v-for="i in 30" :key="i" class="bar-line" :style="{ width: (Math.random() * 4 + 1) + 'px' }"></div>
                </div>
                <div class="barcode-text">{{ row.employeeNo }}</div>
              </div>
              <div class="qrcode-preview">
                <div class="qrcode-box"></div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handlePrint(row)">
              <el-icon><Printer /></el-icon> 打印
            </el-button>
            <el-button link type="success" @click="downloadBarcode(row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="handleSearch"
        />
      </div>
    </el-card>

    <!-- 打印历史 -->
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>打印历史记录</span>
          <el-button size="small" @click="fetchHistory">刷新</el-button>
        </div>
      </template>
      <el-table :data="historyData" size="small" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="employeeName" label="员工姓名" min-width="100" />
        <el-table-column prop="employeeNo" label="工号" min-width="120" />
        <el-table-column prop="printType" label="打印类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.printType === 'QR' ? 'primary' : 'success'">
              {{ row.printType === 'QR' ? '二维码' : '一维码' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="printerName" label="打印机" min-width="140" />
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="printTime" label="打印时间" min-width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="historyPagination.page"
          v-model:page-size="historyPagination.size"
          :total="historyPagination.total"
          layout="total, prev, pager, next"
          small
          @current-change="fetchHistory"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Printer } from '@element-plus/icons-vue'
import { getEmployeeBarcode, printEmployeeBarcode } from '@/api/newModules'

const loading = ref(false)
const searchForm = reactive({ dept: '', keyword: '' })
const tableData = ref<any[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })

async function handleSearch() {
  loading.value = true
  try {
    // Simulated fetch since backend may not have full list endpoint
    tableData.value = [
      { id: 1, employeeNo: 'EMP001', name: '张三', deptName: '调剂部', roleName: '调剂员' },
      { id: 2, employeeNo: 'EMP002', name: '李四', deptName: '煎煮部', roleName: '煎煮员' },
      { id: 3, employeeNo: 'EMP003', name: '王五', deptName: '包装部', roleName: '包装员' },
      { id: 4, employeeNo: 'EMP004', name: '赵六', deptName: '质检部', roleName: '质检员' }
    ].filter(item => {
      if (searchForm.dept && item.deptName !== { DISPENSE: '调剂部', DECOCT: '煎煮部', PACKAGE: '包装部', QC: '质检部' }[searchForm.dept]) return false
      if (searchForm.keyword && !item.name.includes(searchForm.keyword) && !item.employeeNo.includes(searchForm.keyword)) return false
      return true
    })
    pagination.total = tableData.value.length
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchForm.dept = ''
  searchForm.keyword = ''
  pagination.page = 1
  handleSearch()
}

async function handlePrint(row: any) {
  try {
    await printEmployeeBarcode(row.id)
    ElMessage.success('打印指令已发送')
    fetchHistory()
  } catch {
    ElMessage.error('打印失败')
  }
}

async function downloadBarcode(row: any) {
  try {
    const res = await getEmployeeBarcode(row.id) as any
    ElMessage.success('条码数据已获取')
    // In real app, trigger file download using res.data.url or blob
  } catch {
    ElMessage.error('获取条码失败')
  }
}

const historyData = ref<any[]>([])
const historyPagination = reactive({ page: 1, size: 10, total: 0 })

async function fetchHistory() {
  historyData.value = [
    { id: 1, employeeName: '张三', employeeNo: 'EMP001', printType: 'QR', printerName: '标签打印机-01', operatorName: '管理员', printTime: '2024-05-01 08:00', status: 'SUCCESS' },
    { id: 2, employeeName: '李四', employeeNo: 'EMP002', printType: 'BARCODE', printerName: '标签打印机-01', operatorName: '管理员', printTime: '2024-05-01 08:05', status: 'SUCCESS' },
    { id: 3, employeeName: '王五', employeeNo: 'EMP003', printType: 'QR', printerName: '标签打印机-02', operatorName: '管理员', printTime: '2024-05-01 09:00', status: 'FAIL' }
  ]
  historyPagination.total = 3
}

onMounted(() => {
  handleSearch()
  fetchHistory()
})
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.search-card {
  margin-top: 8px;
}
.barcode-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.barcode-lines {
  display: flex;
  gap: 2px;
  height: 36px;
  align-items: flex-end;
}
.bar-line {
  background: #333;
  height: 100%;
}
.barcode-text {
  font-size: 10px;
  color: var(--el-text-color-secondary);
  letter-spacing: 1px;
}
.qrcode-box {
  width: 40px;
  height: 40px;
  background: repeating-conic-gradient(#333 0% 25%, transparent 0% 50%) 50% / 8px 8px;
  border: 1px solid #ccc;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
