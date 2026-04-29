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
          <el-select v-model="search.hospitalId" placeholder="全部医院" clearable style="width: 160px">
            <el-option v-for="h in hospitals" :key="h.id" :label="h.hospitalName" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="患者类型">
          <el-select v-model="search.patientType" placeholder="全部类型" clearable style="width: 120px">
            <el-option label="门诊" :value="0" />
            <el-option label="住院" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="search.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="待处理" value="待处理" />
            <el-option label="处理中" value="处理中" />
            <el-option label="处理完毕" value="处理完毕" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="search.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="搜索">
          <el-input v-model="search.keyword" placeholder="姓名" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="处方号" width="80" />
        <el-table-column label="医院">
          <template #default="{ row }">
            {{ hospitalMap[row.hospitalId] || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="patientName" label="患者姓名" />
        <el-table-column prop="patientType" label="患者类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ row.patientType === 1 ? '住院' : '门诊' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="煎煮方案">
          <template #default="{ row }">
            {{ schemeMap[row.schemeId] || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="repetition" label="付数" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
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
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑处方' : '新增处方'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="医院" required>
          <el-select v-model="form.hospitalId" placeholder="请选择" clearable style="width: 100%">
            <el-option v-for="h in hospitals" :key="h.id" :label="h.hospitalName" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="患者姓名" required>
          <el-input v-model="form.patientName" />
        </el-form-item>
        <el-form-item label="患者类型" required>
          <el-radio-group v-model="form.patientType">
            <el-radio :label="0">门诊</el-radio>
            <el-radio :label="1">住院</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="煎煮方案">
          <el-select v-model="form.schemeId" placeholder="请选择" clearable style="width: 100%">
            <el-option v-for="s in schemes" :key="s.id" :label="s.schemeName || s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="付数">
          <el-input-number v-model="form.repetition" :min="1" />
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
        <el-descriptions-item label="医院">{{ detail?.hospitalName || hospitalMap[detail?.hospitalId!] || '-' }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ detail?.patientName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail?.patientType === 1 ? '住院' : '门诊' }}</el-descriptions-item>
        <el-descriptions-item label="方案">{{ detail?.decoctSchemeName || schemeMap[detail?.schemeId!] || '-' }}</el-descriptions-item>
        <el-descriptions-item label="付数">{{ detail?.repetition }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail?.status }}</el-descriptions-item>
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

interface Hospital {
  id: number
  hospitalName: string
}

interface Prescription {
  id: number
  hospitalId?: number
  hospitalName?: string
  patientName: string
  patientType: number
  schemeId?: number
  decoctSchemeName?: string
  repetition: number
  status?: string
  remark?: string
  createdAt: string
  medicines?: any[]
}

interface Scheme {
  id: number
  schemeName?: string
  name?: string
}

const list = ref<Prescription[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<Prescription | null>(null)
const schemes = ref<Scheme[]>([])
const hospitals = ref<Hospital[]>([])
const hospitalMap = ref<Record<number, string>>({})
const schemeMap = ref<Record<number, string>>({})

const search = ref({
  hospitalId: undefined as number | undefined,
  patientType: undefined as number | undefined,
  status: '',
  keyword: '',
  dateRange: null as [string, string] | null
})

const pagination = ref({ page: 1, size: 10, total: 0 })
const form = ref<Partial<Prescription>>({ patientType: 0, repetition: 1 })

function statusType(status?: string) {
  const map: Record<string, string> = {
    '待处理': 'info', '处理中': 'warning', '处理完毕': 'success'
  }
  return map[status || ''] || ''
}

function formatDateTime(dt: string) {
  if (!dt) return '-'
  const d = new Date(dt)
  if (isNaN(d.getTime())) return dt
  return d.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.value.page,
      size: pagination.value.size
    }
    if (search.value.hospitalId != null) params.hospitalId = search.value.hospitalId
    if (search.value.patientType != null) params.patientType = search.value.patientType
    if (search.value.status) params.status = search.value.status
    if (search.value.keyword) params.keyword = search.value.keyword
    if (search.value.dateRange && search.value.dateRange[0]) {
      params.startTime = search.value.dateRange[0]
      params.endTime = search.value.dateRange[1]
    }
    const res: any = await request.get('/v1/prod/prescriptions', { params })
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  pagination.value.page = 1
  fetchData()
}

function resetSearch() {
  search.value = {
    hospitalId: undefined,
    patientType: undefined,
    status: '',
    keyword: '',
    dateRange: null
  }
  pagination.value.page = 1
  fetchData()
}

function handleSizeChange(val: number) {
  pagination.value.size = val
  pagination.value.page = 1
  fetchData()
}

function handlePageChange(val: number) {
  pagination.value.page = val
  fetchData()
}

function openDialog(row?: Prescription) {
  form.value = row ? { ...row } : { patientType: 0, repetition: 1 }
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (form.value.id) {
      await request.put(`/v1/prod/prescriptions/${form.value.id}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/v1/prod/prescriptions', form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}

async function viewDetail(row: Prescription) {
  try {
    const res: any = await request.get(`/v1/prod/prescriptions/${row.id}`)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) {}
}

async function fetchSchemes() {
  try {
    const res: any = await request.get('/v1/md/schemes', { params: { page: 1, size: 999 } })
    const list = res.data?.records || []
    schemes.value = list
    const map: Record<number, string> = {}
    list.forEach((s: any) => { map[s.id] = s.schemeName || s.name })
    schemeMap.value = map
  } catch (e) {}
}

async function fetchHospitals() {
  try {
    const res: any = await request.get('/v1/md/hospitals', { params: { page: 1, size: 999 } })
    const list = res.data?.records || []
    hospitals.value = list
    const map: Record<number, string> = {}
    list.forEach((h: any) => { map[h.id] = h.hospitalName || h.name })
    hospitalMap.value = map
  } catch (e) {}
}

onMounted(() => {
  fetchData()
  fetchSchemes()
  fetchHospitals()
})
</script>
