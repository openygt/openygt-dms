<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          
          <el-button type="primary" data-testid="create-btn" @click="openDialog()">新增医师</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="search.keyword" placeholder="编码/姓名" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" data-testid="search-btn" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border data-testid="data-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="doctorCode" label="编码" width="120" />
        <el-table-column prop="doctorName" label="姓名" />
        <el-table-column prop="title" label="职称" width="120" />
        <el-table-column label="所属医院" width="160">
          <template #default="{ row }">
            <span>{{ hospitalMap[row.hospitalId] || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="所属科室" width="160">
          <template #default="{ row }">
            <span>{{ departmentMap[row.departmentId] || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑医师' : '新增医师'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="医师编码" required>
          <el-input v-model="form.doctorCode" />
        </el-form-item>
        <el-form-item label="医师姓名" required>
          <el-input v-model="form.doctorName" />
        </el-form-item>
        <el-form-item label="职称">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="所属医院" required>
          <el-select v-model="form.hospitalId" placeholder="请选择医院" clearable style="width: 100%" @change="form.departmentId = undefined">
            <el-option v-for="h in hospitalList" :key="h.id" :label="h.name" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属科室" required>
          <el-select v-model="form.departmentId" placeholder="请先选择医院" clearable style="width: 100%" :disabled="!form.hospitalId">
            <el-option v-for="d in filteredDepartments" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="状态" required>
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button data-testid="dialog-cancel-btn" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" data-testid="dialog-save-btn" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDoctorList,
  getAllHospitals,
  getAllDepartments,
  createDoctor,
  updateDoctor,
  deleteDoctor
} from '@/api/baseData'

interface Doctor {
  id: number
  doctorCode: string
  doctorName: string
  title: string
  departmentId: number
  hospitalId: number
  phone: string
  status: number
}

const list = ref<Doctor[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const search = reactive({ keyword: '' })
const form = ref<Partial<Doctor>>({ status: 1 })
const hospitalList = ref<any[]>([])
const hospitalMap = ref<Record<number, string>>({})
const departmentList = ref<any[]>([])
const departmentMap = ref<Record<number, string>>({})

const filteredDepartments = computed(() => {
  if (!form.value.hospitalId) return []
  return departmentList.value.filter((d: any) => d.hospitalId === form.value.hospitalId)
})

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (search.keyword) params.keyword = search.keyword
    const res: any = await getDoctorList(params)
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
  search.keyword = ''
  page.value = 1
  fetchData()
}

function openDialog(row?: Doctor) {
  form.value = row ? { ...row } : { status: 1 }
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (form.value.id) {
      await updateDoctor(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createDoctor(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  }
}

async function handleDelete(row: Doctor) {
  try {
    await ElMessageBox.confirm('确认删除该医师？', '提示', { type: 'warning' })
    await deleteDoctor(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || e?.message || '删除失败')
    }
  }
}

async function loadHospitals() {
  try {
    const res: any = await getAllHospitals()
    hospitalList.value = res.data || []
    const map: Record<number, string> = {}
    hospitalList.value.forEach((h: any) => { map[h.id] = h.name })
    hospitalMap.value = map
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载医院列表失败')
  }
}

async function loadDepartments() {
  try {
    const res: any = await getAllDepartments()
    departmentList.value = res.data || []
    const map: Record<number, string> = {}
    departmentList.value.forEach((d: any) => { map[d.id] = d.deptName })
    departmentMap.value = map
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载科室列表失败')
  }
}

onMounted(() => {
  loadHospitals()
  loadDepartments()
  fetchData()
})
</script>
