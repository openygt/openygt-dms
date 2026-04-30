<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>科室管理</span>
          <el-button type="primary" data-testid="create-btn" @click="openDialog()">新增科室</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="search.keyword" placeholder="编码/名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" data-testid="search-btn" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border data-testid="data-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="deptCode" label="编码" width="120" />
        <el-table-column prop="deptName" label="名称" />
        <el-table-column prop="hospitalId" label="医院ID" width="100" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑科室' : '新增科室'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="科室编码" required>
          <el-input v-model="form.deptCode" />
        </el-form-item>
        <el-form-item label="科室名称" required>
          <el-input v-model="form.deptName" />
        </el-form-item>
        <el-form-item label="医院ID">
          <el-input v-model.number="form.hospitalId" type="number" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input v-model.number="form.sortOrder" type="number" />
        </el-form-item>
        <el-form-item label="状态">
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDepartmentList,
  createDepartment,
  updateDepartment,
  deleteDepartment
} from '@/api/baseData'

interface Department {
  id: number
  deptCode: string
  deptName: string
  hospitalId: number
  description: string
  sortOrder: number
  status: number
}

const list = ref<Department[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const search = reactive({ keyword: '' })
const form = ref<Partial<Department>>({ status: 1 })

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (search.keyword) params.keyword = search.keyword
    const res: any = await getDepartmentList(params)
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

function openDialog(row?: Department) {
  form.value = row ? { ...row } : { status: 1 }
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (form.value.id) {
      await updateDepartment(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createDepartment(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}

async function handleDelete(row: Department) {
  try {
    await ElMessageBox.confirm('确认删除该科室？', '提示', { type: 'warning' })
    await deleteDepartment(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {}
}

onMounted(fetchData)
</script>
