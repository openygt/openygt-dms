<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>处方管理</span>
          <el-button type="primary" @click="openDialog()">新增处方</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="医院">
          <el-input v-model="search.hospitalName" placeholder="医院名称" clearable />
        </el-form-item>
        <el-form-item label="患者类型">
          <el-select v-model="search.patientType" placeholder="请选择" clearable style="width: 120px">
            <el-option label="门诊" value="OUTPATIENT" />
            <el-option label="住院" value="INPATIENT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="处方号" width="80" />
        <el-table-column prop="hospitalName" label="医院" />
        <el-table-column prop="patientName" label="患者姓名" />
        <el-table-column prop="patientType" label="患者类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ row.patientType === 'INPATIENT' ? '住院' : '门诊' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="decoctSchemeName" label="煎煮方案" />
        <el-table-column prop="totalDose" label="付数" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑处方' : '新增处方'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="医院" required>
          <el-input v-model="form.hospitalName" />
        </el-form-item>
        <el-form-item label="患者姓名" required>
          <el-input v-model="form.patientName" />
        </el-form-item>
        <el-form-item label="患者类型" required>
          <el-radio-group v-model="form.patientType">
            <el-radio label="OUTPATIENT">门诊</el-radio>
            <el-radio label="INPATIENT">住院</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="煎煮方案">
          <el-select v-model="form.schemeId" placeholder="请选择" clearable style="width: 100%">
            <el-option v-for="s in schemes" :key="s.id" :label="s.schemeName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="付数">
          <el-input-number v-model="form.totalDose" :min="1" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="处方详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="处方号">{{ detail?.id }}</el-descriptions-item>
        <el-descriptions-item label="医院">{{ detail?.hospitalName }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ detail?.patientName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail?.patientType === 'INPATIENT' ? '住院' : '门诊' }}</el-descriptions-item>
        <el-descriptions-item label="方案">{{ detail?.decoctSchemeName }}</el-descriptions-item>
        <el-descriptions-item label="付数">{{ detail?.totalDose }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail?.status) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail?.createdAt }}</el-descriptions-item>
      </el-descriptions>
      <div style="margin-top: 16px">
        <h4>药材明细</h4>
        <el-table :data="detail?.medicines || []" border size="small">
          <el-table-column prop="medicineName" label="药材名称" />
          <el-table-column prop="dosage" label="剂量" />
          <el-table-column prop="unit" label="单位" />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

interface Prescription {
  id: number
  hospitalName: string
  patientName: string
  patientType: string
  schemeId?: number
  decoctSchemeName?: string
  totalDose: number
  status: string
  remark?: string
  createdAt: string
  medicines?: any[]
}

interface Scheme {
  id: number
  schemeName: string
}

const list = ref<Prescription[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<Prescription | null>(null)
const schemes = ref<Scheme[]>([])

const search = ref({ hospitalName: '', patientType: '' })
const pagination = ref({ page: 1, size: 10, total: 0 })
const form = ref<Partial<Prescription>>({ patientType: 'OUTPATIENT', totalDose: 1 })

function statusType(status?: string) {
  const map: Record<string, string> = {
    'PENDING': 'info', 'PROCESSING': 'warning', 'COMPLETED': 'success', 'CANCELLED': 'danger'
  }
  return map[status || ''] || ''
}
function statusText(status?: string) {
  const map: Record<string, string> = {
    'PENDING': '待处理', 'PROCESSING': '处理中', 'COMPLETED': '已完成', 'CANCELLED': '已取消'
  }
  return map[status || ''] || status
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await request.get('/production/prescriptions', {
      params: { page: pagination.value.page, size: pagination.value.size, ...search.value }
    })
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  search.value = { hospitalName: '', patientType: '' }
  fetchData()
}

function openDialog(row?: Prescription) {
  form.value = row ? { ...row } : { patientType: 'OUTPATIENT', totalDose: 1 }
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (form.value.id) {
      await request.put(`/production/prescriptions/${form.value.id}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/production/prescriptions', form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}

async function viewDetail(row: Prescription) {
  try {
    const res: any = await request.get(`/production/prescriptions/${row.id}`)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) {}
}

async function fetchSchemes() {
  try {
    const res: any = await request.get('/masterdata/schemes', { params: { page: 1, size: 999 } })
    schemes.value = res.data?.records || []
  } catch (e) {}
}

onMounted(() => {
  fetchData()
  fetchSchemes()
})
</script>
