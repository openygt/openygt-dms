<template>
  <div>
    <div class="page-header-title">处方接收：<span class="page-header-sub">处方审核、接收确认、异常退回</span></div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>处方接收</span>
        </div>
      </template>

      <el-form :inline="true" @submit.prevent>
        <el-form-item>
          <el-radio-group v-model="search.status" @change="handleStatusChange">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="PENDING">待接收</el-radio-button>
            <el-radio-button label="RECEIVED">已接收</el-radio-button>
            <el-radio-button label="REJECTED">已驳回</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="search.keyword" placeholder="患者姓名/处方号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" data-testid="search-btn" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" border data-testid="data-table">
        <el-table-column prop="prescriptionNo" label="处方号" width="160" />
        <el-table-column prop="patientName" label="患者姓名" width="120" />
        <el-table-column prop="hospitalName" label="医院" />
        <el-table-column prop="deptName" label="科室" />
        <el-table-column prop="doctorName" label="医师" width="120" />
        <el-table-column prop="doseCount" label="剂数" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PENDING'" type="warning">待接收</el-tag>
            <el-tag v-else-if="row.status === 'RECEIVED'" type="success">已接收</el-tag>
            <el-tag v-else-if="row.status === 'REJECTED'" type="danger">已驳回</el-tag>
            <el-tag v-else>{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" width="160">
          <template #default="{ row }">
            <span v-if="row.status === 'RECEIVED'">{{ row.taskNo || '-' }}</span>
            <span v-else-if="row.status === 'REJECTED'">{{ row.rejectReason || '-' }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <template v-if="row.status === 'PENDING'">
              <el-button size="small" type="primary" @click="handleReceive(row)">接收</el-button>
              <el-button size="small" type="danger" @click="handleReject(row)">驳回</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无数据" />
      <el-pagination
        v-if="total > 0"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <el-drawer v-model="drawerVisible" title="处方详情" size="600px">
      <div v-if="detail" v-loading="detailLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="处方号">{{ detail.prescriptionNo }}</el-descriptions-item>
          <el-descriptions-item label="患者姓名">{{ detail.patientName }}</el-descriptions-item>
          <el-descriptions-item label="医院">{{ detail.hospitalName }}</el-descriptions-item>
          <el-descriptions-item label="科室">{{ detail.deptName }}</el-descriptions-item>
          <el-descriptions-item label="医师">{{ detail.doctorName }}</el-descriptions-item>
          <el-descriptions-item label="剂数">{{ detail.doseCount }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag v-if="detail.status === 'PENDING'" type="warning">待接收</el-tag>
            <el-tag v-else-if="detail.status === 'RECEIVED'" type="success">已接收</el-tag>
            <el-tag v-else-if="detail.status === 'REJECTED'" type="danger">已驳回</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="任务号">{{ detail.taskNo || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin-top: 16px">药品明细</h4>
        <el-table :data="detail.medicineItems || detail.items || []" border size="small">
          <el-table-column prop="medicineName" label="药品名称" />
          <el-table-column prop="dosage" label="剂量" width="100" />
          <el-table-column prop="unit" label="单位" width="80" />
          <el-table-column prop="medUsage" label="用法" width="80" />
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPrescriptionReceiveList,
  getPrescriptionDetail,
  receivePrescription,
  rejectPrescription
} from '@/api/prescription'

interface PrescriptionItem {
  medicineName: string
  dosage: number
  unit: string
}

interface Prescription {
  id: number
  prescriptionNo: string
  patientName: string
  hospitalName: string
  deptName: string
  doctorName: string
  doseCount: number
  status: string
  taskNo?: string
  rejectReason?: string
  items?: PrescriptionItem[]
  medicineItems?: PrescriptionItem[]
}

const list = ref<Prescription[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const search = reactive({ status: '', keyword: '' })

const drawerVisible = ref(false)
const detail = ref<Prescription | null>(null)
const detailLoading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (search.status) params.status = search.status
    if (search.keyword) params.keyword = search.keyword
    const res: any = await getPrescriptionReceiveList(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchData()
}

function resetSearch() {
  search.status = ''
  search.keyword = ''
  page.value = 1
  fetchData()
}

function handleStatusChange() {
  page.value = 1
  fetchData()
}

async function openDetail(row: Prescription) {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    const res: any = await getPrescriptionDetail(row.id)
    detail.value = res.data || null
  } catch (e) {
    detail.value = row
  } finally {
    detailLoading.value = false
  }
}

async function handleReceive(row: Prescription) {
  try {
    await ElMessageBox.confirm('确认接收该处方？', '提示', { type: 'warning' })
    await receivePrescription(row.id)
    ElMessage.success('接收成功')
    fetchData()
  } catch (e) {}
}

async function handleReject(row: Prescription) {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回处方', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '驳回原因不能为空'
    })
    await rejectPrescription(row.id, { rejectType: 'MANUAL', reason: value })
    ElMessage.success('驳回成功')
    fetchData()
  } catch (e) {}
}

onMounted(fetchData)
</script>
